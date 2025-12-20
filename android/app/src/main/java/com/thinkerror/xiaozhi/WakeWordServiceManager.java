package com.thinkerror.xiaozhi;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * 唤醒词服务管理器
 * 提供Flutter与Android服务之间的通信接口
 */
public class WakeWordServiceManager implements MethodCallHandler {
    private static final String TAG = "WakeWordServiceManager";
    private static final String CHANNEL_NAME = "xiaozhi/wake_word_service";
    private static WakeWordServiceManager instance;
    private Context context;
    
    private WakeWordServiceManager(Context context) {
        this.context = context;
    }
    
    /**
     * 获取单例实例
     */
    public static WakeWordServiceManager getInstance(Context context) {
        if (instance == null) {
            instance = new WakeWordServiceManager(context);
        }
        return instance;
    }
    
    /**
     * 注册方法通道
     */
    public static void registerWith(io.flutter.embedding.engine.FlutterEngine flutterEngine) {
        MethodChannel channel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL_NAME);
        // 使用单例模式，不需要context
        if (instance == null) {
            instance = new WakeWordServiceManager(null);
        }
        channel.setMethodCallHandler(instance);
    }
    
    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "startWakeWordService":
                startWakeWordService();
                result.success(true);
                break;
                
            case "stopWakeWordService":
                stopWakeWordService();
                result.success(true);
                break;
                
            case "isWakeWordServiceRunning":
                boolean isRunning = isWakeWordServiceRunning();
                result.success(isRunning);
                break;
                
            case "updateWakeWord":
                String wakeWord = call.argument("wakeWord");
                if (wakeWord != null && !wakeWord.isEmpty()) {
                    updateWakeWord(wakeWord);
                    result.success(true);
                } else {
                    result.error("INVALID_ARGUMENT", "Wake word cannot be null or empty", null);
                }
                break;
                
            default:
                result.notImplemented();
                break;
        }
    }
    
    /**
     * 启动唤醒词服务
     */
    private void startWakeWordService() {
        try {
            Intent serviceIntent = new Intent(context, WakeWordService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            Log.d(TAG, "WakeWordService started from Flutter");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start WakeWordService: " + e.getMessage());
        }
    }
    
    /**
     * 停止唤醒词服务
     */
    private void stopWakeWordService() {
        try {
            Intent serviceIntent = new Intent(context, WakeWordService.class);
            boolean stopped = context.stopService(serviceIntent);
            
            Log.d(TAG, "WakeWordService stopped from Flutter, success: " + stopped);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop WakeWordService: " + e.getMessage());
        }
    }
    
    /**
     * 检查唤醒词服务是否正在运行
     */
    private boolean isWakeWordServiceRunning() {
        // 这里可以实现更精确的检查逻辑
        // 目前简化处理，返回一个假设值
        // 实际实现可以使用ActivityManager检查服务状态
        return true;
    }
    
    /**
     * 更新唤醒词
     * 这里只是示例，实际实现可能需要重新启动服务
     */
    private void updateWakeWord(String wakeWord) {
        try {
            // 唤醒词已经存储在SharedPreferences中
            // WakeWordService会在启动时读取最新的唤醒词
            // 如果需要立即生效，可能需要重启服务
            
            // 先停止当前服务
            stopWakeWordService();
            
            // 延迟一段时间后重新启动服务
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        startWakeWordService();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Interrupted while restarting WakeWordService: " + e.getMessage());
                    }
                }
            }).start();
            
            Log.d(TAG, "WakeWord updated to: " + wakeWord);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update wake word: " + e.getMessage());
        }
    }
}