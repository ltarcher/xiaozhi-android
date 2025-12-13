/*
 *
 *  * Copyright(c) Live2D Inc. All rights reserved.
 *  *
 *  * Use of this source code is governed by the Live2D Open Software license
 *  * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 *
 */

package com.live2d;

import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LAppWavFileHandler extends Thread {
    public LAppWavFileHandler(String filePath) {
        this.filePath = filePath;
        this.lipSyncContext = new LipSyncContext();
    }

    @Override
    public void run() {
        loadWavFile();
    }

    public void loadWavFile() {
        // 如果不支持API(API24以下)则不进行音频播放。
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            AssetFileDescriptor afd = LAppDelegate.getInstance().getActivity().getAssets().openFd(filePath);
            mediaExtractor.setDataSource(afd);
        } catch (IOException e) {
            // 发生异常时只输出错误并不进行播放直接return。
            e.printStackTrace();
            return;
        }

        MediaFormat mf = mediaExtractor.getTrackFormat(0);
        int samplingRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        int bufferSize = AudioTrack.getMinBufferSize(
            samplingRate,
            channelCount == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        );

        AudioTrack audioTrack;
        audioTrack = new AudioTrack.Builder()
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setAudioFormat(new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(samplingRate)
                .setChannelMask(channelCount == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO)
                .build())
            .setBufferSizeInBytes(bufferSize)
            .build();
        audioTrack.play();

        // 避免断断续续的声音
        int offset = 100;
        byte[] voiceBuffer = LAppPal.loadFileAsBytes(filePath);
        audioTrack.write(voiceBuffer, offset, voiceBuffer.length - offset);
        
        // 处理音频数据用于口型同步
        processAudioDataForLipSync(voiceBuffer, samplingRate, channelCount);
    }
    
    /**
     * 处理音频数据用于口型同步
     * @param audioData 音频数据
     * @param sampleRate 采样率
     * @param channelCount 声道数
     */
    private void processAudioDataForLipSync(byte[] audioData, int sampleRate, int channelCount) {
        // 将byte数组转换为short数组（16位音频数据）
        short[] samples = new short[(audioData.length - 100) / 2]; // 减去offset
        ByteBuffer.wrap(audioData, 100, audioData.length - 100)
                 .order(ByteOrder.LITTLE_ENDIAN)
                 .asShortBuffer()
                 .get(samples);
        
        // 计算RMS值用于口型同步
        int framesPerSecond = 60; // 每秒更新60次口型
        int samplesPerFrame = sampleRate / framesPerSecond;
        
        for (int i = 0; i < samples.length; i += samplesPerFrame) {
            int endIndex = Math.min(i + samplesPerFrame, samples.length);
            double rms = calculateRMS(samples, i, endIndex);
            
            // 将RMS值转换为口型同步值(0.0-1.0)
            double lipSyncValue = rmsToLipSyncValue(rms);
            
            // 更新口型同步上下文
            lipSyncContext.updateLipSyncValue((float) lipSyncValue);
            
            try {
                // 控制更新频率
                Thread.sleep(1000 / framesPerSecond);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * 计算音频片段的RMS值
     * @param samples 音频样本数组
     * @param start 起始索引
     * @param end 结束索引
     * @return RMS值
     */
    private double calculateRMS(short[] samples, int start, int end) {
        double sum = 0.0;
        for (int i = start; i < end; i++) {
            double sample = samples[i] / 32768.0; // 转换为-1.0到1.0范围
            sum += sample * sample;
        }
        return Math.sqrt(sum / (end - start));
    }
    
    /**
     * 将RMS值转换为口型同步值
     * @param rms RMS值
     * @return 口型同步值(0.0-1.0)
     */
    private double rmsToLipSyncValue(double rms) {
        // 应用对数缩放使低音量更敏感
        double threshold = 0.01; // 静音阈值
        if (rms < threshold) {
            return 0.0;
        }
        
        // 对数缩放
        double logRms = Math.log10(rms + 1);
        // 限制在0.0-1.0范围内
        return Math.min(1.0, Math.max(0.0, logRms * 2));
    }
    
    /**
     * 获取当前口型同步值
     * @return 口型同步值
     */
    public float getCurrentLipSyncValue() {
        return lipSyncContext.getCurrentLipSyncValue();
    }

    private final String filePath;
    private final LipSyncContext lipSyncContext;
    
    /**
     * 口型同步上下文类
     */
    private static class LipSyncContext {
        private volatile float currentLipSyncValue = 0.0f;
        private final Object lock = new Object();
        
        /**
         * 更新口型同步值
         * @param value 新的口型同步值
         */
        public void updateLipSyncValue(float value) {
            synchronized (lock) {
                currentLipSyncValue = value;
                // 通知Live2D模型更新口型
                LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
                if (live2DManager != null) {
                    // 这里应该通知当前活动的模型更新口型同步值
                    // 实际实现中可能需要通过回调或其他机制通知模型
                }
            }
        }
        
        /**
         * 获取当前口型同步值
         * @return 当前口型同步值
         */
        public float getCurrentLipSyncValue() {
            synchronized (lock) {
                return currentLipSyncValue;
            }
        }
    }
}