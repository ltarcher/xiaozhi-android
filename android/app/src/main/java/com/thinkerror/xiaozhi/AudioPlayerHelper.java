package com.thinkerror.xiaozhi;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 音频播放助手类，兼容Android 9 (API 28)
 * 使用原生Android API实现音频播放功能
 */
public class AudioPlayerHelper {
    private static final String TAG = "AudioPlayerHelper";
    
    private Context context;
    private AudioTrack audioTrack;
    private Thread playbackThread;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    
    // 音频参数
    private int sampleRate = 16000;
    private int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize;
    
    public AudioPlayerHelper(Context context) {
        this.context = context;
        this.bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        initializeAudioTrack();
    }
    
    /**
     * 初始化AudioTrack
     */
    private void initializeAudioTrack() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+ 使用AudioAttributes
                AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
                    
                AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build();
                    
                audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .build();
            } else {
                // Android 6.0以下的兼容处理
                int streamType = AudioManager.STREAM_MUSIC;
                audioTrack = new AudioTrack(streamType, sampleRate, channelConfig, 
                    audioFormat, bufferSize, AudioTrack.MODE_STREAM);
            }
            
            Log.d(TAG, "AudioTrack initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize AudioTrack: " + e.getMessage(), e);
        }
    }
    
    /**
     * 播放assets中的音频文件
     * 类似Live2D官方演示的实现方式
     */
    public void playAssetAudio(String assetPath) {
        if (isPlaying) {
            stop();
        }
        
        playbackThread = new Thread(() -> {
            try {
                // 从assets读取音频文件
                InputStream inputStream = context.getAssets().open(assetPath);
                byte[] audioData = readStreamToByteArray(inputStream);
                
                // 如果是WAV文件，跳过文件头
                int offset = 44; // WAV文件头通常是44字节
                if (audioData.length > offset) {
                    playPcmData(audioData, offset);
                }
                
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error playing asset audio: " + e.getMessage(), e);
            }
        });
        
        playbackThread.start();
    }
    
    /**
     * 播放PCM音频数据流
     * 这是最核心的音频播放方法，用于实时音频播放
     */
    public void playPcmStream(byte[] pcmData) {
        if (audioTrack == null) {
            Log.e(TAG, "AudioTrack is not initialized");
            return;
        }
        
        playbackThread = new Thread(() -> {
            try {
                if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioTrack is not properly initialized");
                    return;
                }
                
                isPlaying = true;
                audioTrack.play();
                
                // 分块写入音频数据
                int chunkSize = Math.min(bufferSize, pcmData.length);
                int offset = 0;
                
                while (offset < pcmData.length && isPlaying) {
                    int remainingBytes = pcmData.length - offset;
                    int bytesToWrite = Math.min(chunkSize, remainingBytes);
                    
                    if (isPaused) {
                        Thread.sleep(50); // 暂停时等待
                        continue;
                    }
                    
                    int bytesWritten = audioTrack.write(pcmData, offset, bytesToWrite);
                    if (bytesWritten < 0) {
                        Log.e(TAG, "AudioTrack write error: " + bytesWritten);
                        break;
                    }
                    
                    offset += bytesWritten;
                    
                    // 避免CPU占用过高
                    Thread.sleep(1);
                }
                
                // 等待播放完成
                if (isPlaying) {
                    Thread.sleep(100);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error during PCM stream playback: " + e.getMessage(), e);
            } finally {
                stop();
            }
        });
        
        playbackThread.start();
    }
    
    /**
     * 播放PCM数据，从指定偏移量开始
     * 类似Live2D官方演示的实现
     */
    private void playPcmData(byte[] audioData, int offset) {
        if (audioTrack == null || audioData.length <= offset) {
            return;
        }
        
        try {
            isPlaying = true;
            audioTrack.play();
            
            // 跳过offset后的数据写入AudioTrack
            int bytesToWrite = audioData.length - offset;
            audioTrack.write(audioData, offset, bytesToWrite);
            
            Log.d(TAG, "PCM data playback started, bytes: " + bytesToWrite);
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing PCM data: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将InputStream转换为字节数组
     */
    private byte[] readStreamToByteArray(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * 暂停播放
     */
    public void pause() {
        isPaused = true;
        if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack.pause();
        }
    }
    
    /**
     * 恢复播放
     */
    public void resume() {
        isPaused = false;
        if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack.play();
        }
    }
    
    /**
     * 停止播放
     */
    public void stop() {
        isPlaying = false;
        isPaused = false;
        
        if (audioTrack != null) {
            try {
                if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.stop();
                    audioTrack.flush();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioTrack: " + e.getMessage(), e);
            }
        }
        
        if (playbackThread != null && playbackThread.isAlive()) {
            try {
                playbackThread.interrupt();
                playbackThread.join(1000); // 最多等待1秒
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting for playback thread to finish");
            }
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stop();
        
        if (audioTrack != null) {
            try {
                audioTrack.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing AudioTrack: " + e.getMessage(), e);
            }
            audioTrack = null;
        }
        
        Log.d(TAG, "AudioPlayerHelper resources released");
    }
    
    /**
     * 检查是否正在播放
     */
    public boolean isPlaying() {
        return isPlaying && !isPaused;
    }
    
    /**
     * 设置音量 (0.0 - 1.0)
     */
    public void setVolume(float volume) {
        if (audioTrack != null) {
            audioTrack.setStereoVolume(volume, volume);
        }
    }
}