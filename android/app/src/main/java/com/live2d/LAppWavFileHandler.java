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
        int framesPerSecond = 90; // 提高到每秒更新90次口型，使变化更流畅
        int samplesPerFrame = sampleRate / framesPerSecond;
        int overlap = samplesPerFrame / 4; // 添加重叠窗口，提高平滑度
        
        for (int i = 0; i < samples.length; i += samplesPerFrame - overlap) {
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
        // 进一步降低静音阈值，使更多音频信号能够触发口型变化
        double threshold = 0.003; // 从0.005降低到0.003，更敏感
        if (rms < threshold) {
            return 0.0;
        }
        
        // 使用更激进的缩放算法，增强口型变化幅度
        double normalizedRms = (rms - threshold) / (0.15 - threshold); // 降低分母，增强敏感度
        normalizedRms = Math.max(0.0, Math.min(1.0, normalizedRms));
        
        // 使用更激进的函数增强中等音量的敏感性，并添加更大放大系数
        double enhancedValue = Math.pow(normalizedRms, 0.3) * 2.0; // 放大2.0倍，使用0.3次幂增强低音量部分
        
        // 应用更陡峭的S型曲线，使高音量部分更敏感
        double sigmoidValue = enhancedValue / (1.0 + Math.exp(-8 * (enhancedValue - 0.3))); // 更陡峭的曲线
        
        // 限制在0.0-1.0范围内
        return Math.min(1.0, Math.max(0.0, sigmoidValue));
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
                    // 直接使用当前活动的模型更新口型同步值
                    try {
                        int currentModelIndex = live2DManager.getCurrentModel();
                        if (currentModelIndex >= 0 && currentModelIndex < live2DManager.getModelNum()) {
                            LAppModel model = live2DManager.getModel(currentModelIndex);
                            if (model != null) {
                                model.setLipSyncValue(value);
                            }
                        }
                    } catch (Exception e) {
                        // 静默处理异常，避免影响音频播放
                        System.err.println("Error updating lip sync: " + e.getMessage());
                    }
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