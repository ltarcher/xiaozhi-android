package com.thinkerror.xiaozhi;

import android.content.Context;
import androidx.annotation.NonNull;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.BinaryMessenger;
import java.util.Map;

/**
 * Flutter音频播放插件
 * 连接Flutter层和原生Android音频播放器
 */
public class AudioPlayerPlugin implements MethodCallHandler {
    private static final String CHANNEL_NAME = "com.thinkerror.xiaozhi/audio_player";
    private AudioPlayerHelper audioPlayerHelper;
    private Context context;
    
    private AudioPlayerPlugin(Context context) {
        this.context = context;
        this.audioPlayerHelper = new AudioPlayerHelper(context);
    }
    
    /**
     * 注册插件到Flutter引擎
     */
    public static void registerWith(FlutterEngine flutterEngine, Context context) {
        BinaryMessenger messenger = flutterEngine.getDartExecutor().getBinaryMessenger();
        MethodChannel channel = new MethodChannel(messenger, CHANNEL_NAME);
        channel.setMethodCallHandler(new AudioPlayerPlugin(context));
    }
    
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        try {
            switch (call.method) {
                case "initialize":
                    initialize(call, result);
                    break;
                case "playAssetAudio":
                    playAssetAudio(call, result);
                    break;
                case "playPcmStream":
                    playPcmStream(call, result);
                    break;
                case "pause":
                    pause(call, result);
                    break;
                case "resume":
                    resume(call, result);
                    break;
                case "stop":
                    stop(call, result);
                    break;
                case "setVolume":
                    setVolume(call, result);
                    break;
                case "release":
                    release(call, result);
                    break;
                default:
                    result.notImplemented();
                    break;
            }
        } catch (Exception e) {
            result.error("AUDIO_PLAYER_ERROR", e.getMessage(), null);
        }
    }
    
    /**
     * 初始化音频播放器
     */
    private void initialize(MethodCall call, Result result) {
        try {
            // AudioPlayerHelper在构造函数中已经初始化
            result.success(true);
        } catch (Exception e) {
            result.error("INIT_ERROR", "Failed to initialize audio player: " + e.getMessage(), null);
        }
    }
    
    /**
     * 播放assets中的音频文件
     */
    private void playAssetAudio(MethodCall call, Result result) {
        Map<String, Object> args = (Map<String, Object>) call.arguments;
        String assetPath = (String) args.get("assetPath");
        
        if (assetPath == null) {
            result.error("INVALID_ARGUMENT", "Asset path cannot be null", null);
            return;
        }
        
        try {
            audioPlayerHelper.playAssetAudio(assetPath);
            result.success(true);
        } catch (Exception e) {
            result.error("PLAY_ERROR", "Failed to play asset audio: " + e.getMessage(), null);
        }
    }
    
    /**
     * 播放PCM音频数据流
     */
    private void playPcmStream(MethodCall call, Result result) {
        Map<String, Object> args = (Map<String, Object>) call.arguments;
        byte[] pcmData = (byte[]) args.get("pcmData");
        
        if (pcmData == null) {
            result.error("INVALID_ARGUMENT", "PCM data cannot be null", null);
            return;
        }
        
        try {
            audioPlayerHelper.playPcmStream(pcmData);
            result.success(true);
        } catch (Exception e) {
            result.error("PLAY_ERROR", "Failed to play PCM stream: " + e.getMessage(), null);
        }
    }
    
    /**
     * 暂停播放
     */
    private void pause(MethodCall call, Result result) {
        try {
            audioPlayerHelper.pause();
            result.success(true);
        } catch (Exception e) {
            result.error("PAUSE_ERROR", "Failed to pause: " + e.getMessage(), null);
        }
    }
    
    /**
     * 恢复播放
     */
    private void resume(MethodCall call, Result result) {
        try {
            audioPlayerHelper.resume();
            result.success(true);
        } catch (Exception e) {
            result.error("RESUME_ERROR", "Failed to resume: " + e.getMessage(), null);
        }
    }
    
    /**
     * 停止播放
     */
    private void stop(MethodCall call, Result result) {
        try {
            audioPlayerHelper.stop();
            result.success(true);
        } catch (Exception e) {
            result.error("STOP_ERROR", "Failed to stop: " + e.getMessage(), null);
        }
    }
    
    /**
     * 设置音量
     */
    private void setVolume(MethodCall call, Result result) {
        Map<String, Object> args = (Map<String, Object>) call.arguments;
        Double volume = (Double) args.get("volume");
        
        if (volume == null) {
            result.error("INVALID_ARGUMENT", "Volume cannot be null", null);
            return;
        }
        
        if (volume < 0.0 || volume > 1.0) {
            result.error("INVALID_ARGUMENT", "Volume must be between 0.0 and 1.0", null);
            return;
        }
        
        try {
            audioPlayerHelper.setVolume(volume.floatValue());
            result.success(true);
        } catch (Exception e) {
            result.error("VOLUME_ERROR", "Failed to set volume: " + e.getMessage(), null);
        }
    }
    
    /**
     * 释放资源
     */
    private void release(MethodCall call, Result result) {
        try {
            audioPlayerHelper.release();
            result.success(true);
        } catch (Exception e) {
            result.error("RELEASE_ERROR", "Failed to release: " + e.getMessage(), null);
        }
    }
}