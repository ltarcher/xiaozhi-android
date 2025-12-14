package com.thinkerror.xiaozhi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioFocusRequest;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.Locale;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class WakeWordService extends Service implements RecognitionListener {
    private static final String TAG = "WakeWordService";
    private static final String CHANNEL = "wake_word_channel";
    private static final String NOTIFICATION_CHANNEL_ID = "wake_word_service_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private boolean isListening = false;
    private boolean isWakeWordEnabled = false;
    private String wakeWord = "你好小清";
    private MethodChannel methodChannel;
    private SharedPreferences sharedPreferences;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean hasAudioFocus = false;
    private NotificationManager notificationManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "WakeWordService onCreate");
        
        // 初始化通知管理器
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        
        // 初始化SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        // 初始化AudioManager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        // 初始化语音识别器
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
        
        // 创建识别意图
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINA.toString());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        
        // 加载设置
        loadSettings();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "WakeWordService onStartCommand");
        Log.d(TAG, "Intent: " + (intent != null ? intent.toString() : "null"));
        Log.d(TAG, "Flags: " + flags + ", startId: " + startId);
        
        // 启动前台服务
        try {
            startForeground(NOTIFICATION_ID, createNotification());
            Log.d(TAG, "Foreground service started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service", e);
        }
        
        // 加载最新设置
        loadSettings();
        
        // 如果唤醒词功能已启用，开始监听
        if (isWakeWordEnabled) {
            Log.d(TAG, "Wake word is enabled, requesting audio focus and starting listening");
            requestAudioFocus();
            startListening();
        } else {
            Log.d(TAG, "Wake word is disabled, not starting listening");
        }
        
        return START_STICKY; // 服务被杀死后自动重启
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "WakeWordService onDestroy");
        
        // 停止监听
        stopListening();
        
        // 释放音频焦点
        abandonAudioFocus();
        
        // 销毁语音识别器
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
    
    /**
     * 加载设置
     */
    private void loadSettings() {
        isWakeWordEnabled = sharedPreferences.getBoolean("WAKE_WORD_ENABLED", false);
        wakeWord = sharedPreferences.getString("WAKE_WORD", "你好小清");
        
        Log.d(TAG, "Settings loaded - enabled: " + isWakeWordEnabled + ", wakeWord: " + wakeWord);
        
        // 更新通知内容
        updateNotification();
    }
    
    /**
     * 开始监听唤醒词
     */
    private void startListening() {
        Log.d(TAG, "startListening called - speechRecognizer: " + (speechRecognizer != null ? "not null" : "null") +
                  ", isListening: " + isListening + ", hasAudioFocus: " + hasAudioFocus);
        
        if (speechRecognizer != null && !isListening && hasAudioFocus) {
            try {
                Log.d(TAG, "Starting speech recognition with intent: " + recognizerIntent.toString());
                speechRecognizer.startListening(recognizerIntent);
                isListening = true;
                Log.d(TAG, "Started listening for wake word: " + wakeWord);
            } catch (Exception e) {
                Log.e(TAG, "Error starting speech recognition", e);
                isListening = false;
            }
        } else {
            if (speechRecognizer == null) {
                Log.w(TAG, "Cannot start listening: SpeechRecognizer is null");
            }
            if (isListening) {
                Log.w(TAG, "Cannot start listening: Already listening");
            }
            if (!hasAudioFocus) {
                Log.w(TAG, "Cannot start listening: No audio focus");
            }
        }
    }
    
    /**
     * 停止监听唤醒词
     */
    private void stopListening() {
        if (speechRecognizer != null && isListening) {
            try {
                speechRecognizer.stopListening();
                isListening = false;
                Log.d(TAG, "Stopped listening for wake word");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping speech recognition", e);
            }
        }
    }
    
    /**
     * 重新开始监听（用于连续监听）
     */
    private void restartListening() {
        stopListening();
        
        // 短暂延迟后重新开始监听，避免频繁重启
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isWakeWordEnabled) {
                    startListening();
                }
            }
        }, 1000); // 1秒延迟
    }
    
    /**
     * 检测唤醒词
     */
    private boolean checkWakeWord(String result) {
        Log.d(TAG, "checkWakeWord called - result: '" + result + "', wakeWord: '" + wakeWord + "'");
        
        if (result == null || result.trim().isEmpty()) {
            Log.d(TAG, "Result is null or empty, returning false");
            return false;
        }
        
        // 简单的字符串匹配，可以优化为更复杂的匹配算法
        boolean isWakeWord = result.contains(wakeWord);
        
        Log.d(TAG, "Contains check result: " + isWakeWord);
        
        if (isWakeWord) {
            Log.d(TAG, "Wake word detected: " + result);
            onWakeWordDetected();
        }
        
        return isWakeWord;
    }
    
    /**
     * 唤醒词检测到后的处理
     */
    private void onWakeWordDetected() {
        Log.d(TAG, "Wake word detected, launching CallPage");
        
        // 发送广播通知Flutter端
        Intent wakeWordIntent = new Intent("com.thinkerror.xiaozhi.WAKE_WORD_DETECTED");
        sendBroadcast(wakeWordIntent);
        
        // 启动通话页面
        Intent callIntent = new Intent(this, io.flutter.app.MainActivity.class);
        callIntent.setAction("android.intent.action.MAIN");
        callIntent.addCategory("android.intent.category.LAUNCHER");
        callIntent.putExtra("navigate_to_call", true);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(callIntent);
        
        // 短暂停止监听，避免重复触发
        stopListening();
        
        // 5秒后重新开始监听
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isWakeWordEnabled) {
                    startListening();
                }
            }
        }, 5000);
    }
        
        /**
         * 请求音频焦点
         */
        private void requestAudioFocus() {
            Log.d(TAG, "requestAudioFocus called - audioManager: " + (audioManager != null ? "not null" : "null") +
                      ", SDK_INT: " + Build.VERSION.SDK_INT);
            
            if (audioManager == null) {
                Log.e(TAG, "AudioManager is null");
                return;
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Using new AudioFocusRequest API");
                if (audioFocusRequest == null) {
                    audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                            .setOnAudioFocusChangeListener(audioFocusChangeListener)
                            .build();
                    Log.d(TAG, "Created AudioFocusRequest");
                }
                
                int result = audioManager.requestAudioFocus(audioFocusRequest);
                Log.d(TAG, "Audio focus request result: " + result);
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    hasAudioFocus = true;
                    Log.d(TAG, "Audio focus granted");
                } else {
                    hasAudioFocus = false;
                    Log.w(TAG, "Audio focus denied");
                }
            } else {
                Log.d(TAG, "Using legacy AudioFocus API");
                // 兼容旧版本Android
                int result = audioManager.requestAudioFocus(audioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                Log.d(TAG, "Audio focus request result (legacy): " + result);
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    hasAudioFocus = true;
                    Log.d(TAG, "Audio focus granted (legacy)");
                } else {
                    hasAudioFocus = false;
                    Log.w(TAG, "Audio focus denied (legacy)");
                }
            }
        }
        
        /**
         * 释放音频焦点
         */
        private void abandonAudioFocus() {
            if (audioManager == null) {
                return;
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
                audioFocusRequest = null;
            } else {
                // 兼容旧版本Android
                audioManager.abandonAudioFocus(audioFocusChangeListener);
            }
            
            hasAudioFocus = false;
            Log.d(TAG, "Audio focus abandoned");
        }
        
        /**
         * 音频焦点变化监听器
         */
        private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                Log.d(TAG, "Audio focus changed: " + focusChange);
                
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        hasAudioFocus = true;
                        Log.d(TAG, "Audio focus gained");
                        if (isWakeWordEnabled && !isListening) {
                            startListening();
                        }
                        break;
                        
                    case AudioManager.AUDIOFOCUS_LOSS:
                        hasAudioFocus = false;
                        Log.d(TAG, "Audio focus lost");
                        stopListening();
                        break;
                        
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        hasAudioFocus = false;
                        Log.d(TAG, "Audio focus lost transient");
                        stopListening();
                        break;
                        
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        // 可以继续监听，但降低优先级
                        Log.d(TAG, "Audio focus lost transient can duck");
                        // 这里可以添加降低监听灵敏度的逻辑
                        break;
                }
            }
        };
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "唤醒词服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于监听唤醒词的后台服务");
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * 创建通知
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, io.flutter.app.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );
        
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("唤醒词服务")
                .setContentText("正在监听唤醒词：" + wakeWord)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
    
    /**
     * 更新通知内容
     */
    private void updateNotification() {
        if (notificationManager != null) {
            Notification notification = createNotification();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }
    
    // RecognitionListener接口实现
    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "Ready for speech - params: " + (params != null ? params.toString() : "null"));
    }
    
    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech");
    }
    
    @Override
    public void onRmsChanged(float rmsdB) {
        // 音量变化，可以用于可视化
    }
    
    @Override
    public void onBufferReceived(byte[] buffer) {
        // 音频缓冲区接收
    }
    
    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "End of speech");
    }
    
    @Override
    public void onError(int error) {
        String errorMsg = getErrorText(error);
        Log.e(TAG, "Speech recognition error: " + error + " - " + errorMsg);
        
        // 根据错误类型决定是否重启监听
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
            case SpeechRecognizer.ERROR_CLIENT:
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_SERVER:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                // 这些错误需要重启监听
                Log.d(TAG, "Restarting listening due to error: " + error);
                restartListening();
                break;
            default:
                // 其他错误暂时不重启
                Log.d(TAG, "Not restarting listening for error: " + error);
                break;
        }
    }
    
    /**
     * 获取错误描述
     */
    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognizer busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Error from server";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Didn't understand, please try again.";
        }
    }
    
    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        Log.d(TAG, "onResults called - matches: " + (matches != null ? matches.toString() : "null"));
        
        if (matches != null && !matches.isEmpty()) {
            String result = matches.get(0);
            Log.d(TAG, "Speech recognition result: " + result);
            
            // 检测唤醒词
            if (checkWakeWord(result)) {
                return; // 如果检测到唤醒词，不继续处理
            }
        }
        
        // 没有检测到唤醒词，继续监听
        Log.d(TAG, "No wake word detected, restarting listening");
        restartListening();
    }
    
    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        Log.d(TAG, "onPartialResults called - matches: " + (matches != null ? matches.toString() : "null"));
        
        if (matches != null && !matches.isEmpty()) {
            String result = matches.get(0);
            Log.d(TAG, "Partial speech recognition result: " + result);
            
            // 检测唤醒词
            if (checkWakeWord(result)) {
                return; // 如果检测到唤醒词，不继续处理
            }
        }
    }
    
    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d(TAG, "Speech recognition event: " + eventType);
    }
}