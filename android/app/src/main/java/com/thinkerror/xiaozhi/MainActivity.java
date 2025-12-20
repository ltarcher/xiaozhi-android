package com.thinkerror.xiaozhi;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import com.thinkerror.xiaozhi.AudioPlayerCompat;
import com.thinkerror.xiaozhi.DeviceInfoCompat;

public class MainActivity extends FlutterActivity {
    private static final String TAG = "MainActivity";
    
    @Override
    public void configureFlutterEngine(FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        // 注册Android 9兼容音频播放器
        AudioPlayerCompat.registerWith(flutterEngine, getApplicationContext());
        // 注册设备信息兼容性工具
        DeviceInfoCompat.registerWith(flutterEngine);
        // 注册唤醒词服务管理器，直接传递FlutterEngine实例
        WakeWordServiceManager.registerWith(flutterEngine);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 检查是否由唤醒词服务启动
        boolean wakeWordDetected = getIntent().getBooleanExtra("wake_word_detected", false);
        if (wakeWordDetected) {
            Log.d(TAG, "MainActivity started by wake word detection");
            // 这里可以通过Flutter MethodChannel通知Flutter应用进入对话模式
            // 具体实现取决于您的应用架构
        }
        
        // 启动唤醒词服务
        startWakeWordService();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // 确保唤醒词服务正在运行
        startWakeWordService();
    }
    
    /**
     * 启动唤醒词服务
     */
    private void startWakeWordService() {
        Intent serviceIntent = new Intent(this, WakeWordService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Log.d(TAG, "WakeWordService started");
    }
    
    /**
     * 停止唤醒词服务
     * 可以在Flutter端通过MethodChannel调用此方法
     */
    public void stopWakeWordService() {
        Intent serviceIntent = new Intent(this, WakeWordService.class);
        stopService(serviceIntent);
        
        Log.d(TAG, "WakeWordService stopped");
    }
}
