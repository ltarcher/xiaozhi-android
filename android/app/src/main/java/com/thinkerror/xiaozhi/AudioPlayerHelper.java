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
    private volatile boolean isPlaying = false;
    private volatile boolean isPaused = false;
    private volatile boolean isStopped = false;
    
    // 音频参数
    private int sampleRate = 16000;
    private int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize;
    
    // 音频缓冲队列
    private java.util.concurrent.BlockingQueue<byte[]> audioQueue;
    
    // 对象锁用于线程同步
    private final Object lock = new Object();
    
    public AudioPlayerHelper(Context context) {
        this.context = context;
        this.bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        this.audioQueue = new java.util.concurrent.LinkedBlockingQueue<>();
        initializeAudioTrack();
        startPlaybackThread();
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
     * 启动播放线程
     */
    private void startPlaybackThread() {
        if (playbackThread != null && playbackThread.isAlive()) {
            return;
        }
        
        isStopped = false;
        playbackThread = new Thread(() -> {
            try {
                if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioTrack is not properly initialized");
                    return;
                }
                
                audioTrack.play();
                Log.d(TAG, "Playback thread started");
                
                while (!isStopped) {
                    try {
                        // 从队列获取音频数据，最多等待100ms
                        byte[] audioData = audioQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                        
                        if (audioData == null) {
                            // 没有数据时继续等待
                            continue;
                        }
                        
                        if (isPaused) {
                            // 暂停时将数据重新放回队列
                            audioQueue.offer(audioData);
                            Thread.sleep(50);
                            continue;
                        }
                        
                        // 写入音频数据
                        int bytesWritten = audioTrack.write(audioData, 0, audioData.length);
                        if (bytesWritten < 0) {
                            Log.e(TAG, "AudioTrack write error: " + bytesWritten);
                        } else {
                            Log.d(TAG, "Playing PCM stream, size: " + audioData.length);
                        }
                        
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Playback thread interrupted");
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "Error in playback thread: " + e.getMessage(), e);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Fatal error in playback thread: " + e.getMessage(), e);
            } finally {
                try {
                    if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                        audioTrack.stop();
                        audioTrack.flush();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping AudioTrack: " + e.getMessage(), e);
                }
                Log.d(TAG, "Playback thread ended");
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
        
        if (pcmData == null || pcmData.length == 0) {
            Log.w(TAG, "Empty PCM data received");
            return;
        }
        
        synchronized (lock) {
            // 清空队列中的旧数据，避免延迟
            audioQueue.clear();
            
            // 将新数据加入队列
            audioQueue.offer(pcmData.clone());
            
            // 如果播放线程已停止，重新启动
            if (isStopped || (playbackThread != null && !playbackThread.isAlive())) {
                startPlaybackThread();
            }
            
            isPlaying = true;
        }
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
        synchronized (lock) {
            isPaused = true;
            if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                try {
                    audioTrack.pause();
                } catch (Exception e) {
                    Log.e(TAG, "Error pausing AudioTrack: " + e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * 恢复播放
     */
    public void resume() {
        synchronized (lock) {
            isPaused = false;
            isStopped = false;
            if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                try {
                    audioTrack.play();
                } catch (Exception e) {
                    Log.e(TAG, "Error resuming AudioTrack: " + e.getMessage(), e);
                }
            }
            
            // 如果播放线程已停止，重新启动
            if (playbackThread == null || !playbackThread.isAlive()) {
                startPlaybackThread();
            }
        }
    }
    
    /**
     * 停止播放
     */
    public void stop() {
        synchronized (lock) {
            isPlaying = false;
            isPaused = false;
            isStopped = true;
            
            // 清空音频队列
            audioQueue.clear();
        }
        
        // 中断播放线程
        if (playbackThread != null && playbackThread.isAlive()) {
            try {
                playbackThread.interrupt();
                playbackThread.join(500); // 最多等待500ms
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
        
        synchronized (lock) {
            if (audioTrack != null) {
                try {
                    if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                        audioTrack.release();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing AudioTrack: " + e.getMessage(), e);
                }
                audioTrack = null;
            }
            
            // 清空队列
            audioQueue.clear();
        }
        
        Log.d(TAG, "AudioPlayerHelper resources released");
    }
    
    /**
     * 检查是否正在播放
     */
    public boolean isPlaying() {
        return isPlaying && !isPaused && !isStopped;
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