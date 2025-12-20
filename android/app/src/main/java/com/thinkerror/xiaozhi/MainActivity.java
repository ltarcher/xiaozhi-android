package com.thinkerror.xiaozhi;

import android.content.Context;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import com.thinkerror.xiaozhi.AudioPlayerCompat;
import com.thinkerror.xiaozhi.DeviceInfoCompat;
import com.thinkerror.xiaozhi.VoiceWakeUpService;

public class MainActivity extends FlutterActivity {
    private VoiceWakeUpService voiceWakeUpService;
    private MethodChannel voiceWakeUpChannel;
    
    @Override
    public void configureFlutterEngine(FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        
        // 注册Android 9兼容音频播放器
        AudioPlayerCompat.registerWith(flutterEngine, getApplicationContext());
        // 注册设备信息兼容性工具
        DeviceInfoCompat.registerWith(flutterEngine);
        
        // 注册语音唤醒服务
        voiceWakeUpChannel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), "voice_wakeup_channel");
        voiceWakeUpService = new VoiceWakeUpService(getApplicationContext(), voiceWakeUpChannel);
        voiceWakeUpChannel.setMethodCallHandler(voiceWakeUpService);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 释放语音唤醒服务资源
        if (voiceWakeUpService != null) {
            voiceWakeUpService.dispose();
            voiceWakeUpService = null;
        }
    }
}
