package com.thinkerror.xiaozhi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/**
 * 唤醒词检测服务
 * 在后台持续监听音频，检测唤醒词
 */
public class WakeWordService extends Service {
    private static final String TAG = "WakeWordService";
    private static final String CHANNEL_ID = "WakeWordServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREFS_NAME = "FlutterSharedPreferences";
    private static final String WAKE_WORD_KEY = "flutter.WAKE_WORD";
    
    // 音频录制参数
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    private String wakeWord = "你好，小清"; // 默认唤醒词
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "WakeWordService created");
        
        // 创建通知渠道
        createNotificationChannel();
        
        // 读取唤醒词设置
        loadWakeWord();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "WakeWordService started");
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification());
        
        // 开始录音
        startRecording();
        
        // 如果服务被杀死，自动重启
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "WakeWordService destroyed");
        
        // 停止录音
        stopRecording();
        
        super.onDestroy();
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "唤醒词检测服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于检测唤醒词的服务");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.launcher_icon)
                .setContentTitle("清岭AI")
                .setContentText("正在监听唤醒词...")
                .setContentIntent(pendingIntent)
                .build();
    }
    
    /**
     * 读取唤醒词设置
     */
    private void loadWakeWord() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedWakeWord = prefs.getString(WAKE_WORD_KEY, null);
        if (savedWakeWord != null && !savedWakeWord.isEmpty()) {
            wakeWord = savedWakeWord;
        }
        Log.d(TAG, "Loaded wake word: " + wakeWord);
    }
    
    /**
     * 开始录音
     */
    private void startRecording() {
        if (isRecording) {
            return;
        }
        
        try {
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return;
            }
            
            isRecording = true;
            
            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    audioRecord.startRecording();
                    byte[] audioBuffer = new byte[BUFFER_SIZE];
                    
                    Log.d(TAG, "Recording started for wake word detection");
                    
                    while (isRecording) {
                        int bytesRead = audioRecord.read(audioBuffer, 0, BUFFER_SIZE);
                        if (bytesRead > 0) {
                            // 检测音频中的唤醒词
                            processAudioData(audioBuffer, bytesRead);
                        }
                    }
                    
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                }
            });
            
            recordingThread.start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage());
        }
    }
    
    /**
     * 停止录音
     */
    private void stopRecording() {
        if (!isRecording) {
            return;
        }
        
        isRecording = false;
        
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio record: " + e.getMessage());
            }
            audioRecord = null;
        }
        
        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error joining recording thread: " + e.getMessage());
            }
            recordingThread = null;
        }
        
        Log.d(TAG, "Recording stopped");
    }
    
    /**
     * 处理音频数据，检测唤醒词
     * 这里是一个简化的实现，实际应用中需要使用更复杂的语音识别技术
     */
    private void processAudioData(byte[] audioBuffer, int bytesRead) {
        // 计算音频能量
        double energy = calculateAudioEnergy(audioBuffer, bytesRead);
        
        // 设置能量阈值
        double energyThreshold = 0.01;
        
        // 检测到有声音
        if (energy > energyThreshold) {
            Log.d(TAG, "Detected voice activity, energy: " + energy);
            
            // 这里应该调用语音识别服务来识别具体的语音内容
            // 由于项目可能没有集成语音识别API，这里使用简化的检测逻辑
            // 在实际应用中，您需要调用语音识别服务来识别用户说的是什么
            
            // 模拟检测到唤醒词（在实际应用中应该是真正的语音识别）
            boolean isWakeWordDetected = simulateWakeWordDetection();
            
            if (isWakeWordDetected) {
                Log.d(TAG, "Wake word detected: " + wakeWord);
                
                // 启动主应用并进入对话模式
                startMainActivity();
            }
        }
    }
    
    /**
     * 计算音频能量
     */
    private double calculateAudioEnergy(byte[] audioBuffer, int bytesRead) {
        long sum = 0;
        int sampleCount = bytesRead / 2; // 16位音频，每样本2字节
        
        for (int i = 0; i < bytesRead - 1; i += 2) {
            // 小端序转换
            int sample = ((audioBuffer[i + 1] << 8) | (audioBuffer[i] & 0xff));
            // 转换为有符号16位整数
            if (sample > 32767) sample -= 65536;
            sum += (long)sample * sample;
        }
        
        if (sampleCount == 0) return 0;
        
        return (double)sum / (sampleCount * 32768.0 * 32768.0);
    }
    
    /**
     * 模拟唤醒词检测
     * 在实际应用中，这里应该调用语音识别API来识别用户说的内容
     * 然后检查识别结果是否包含唤醒词
     */
    private boolean simulateWakeWordDetection() {
        // 在实际应用中，这里应该调用语音识别API来识别用户说的内容
        // 然后检查识别结果是否包含唤醒词
        
        // 为了测试，我们使用更高的概率（50%）和更多调试信息
        double random = Math.random();
        boolean result = random < 0.5;
        
        Log.d(TAG, "Simulating wake word detection: random=" + random + ", result=" + result + ", threshold=0.5, wakeWord=" + wakeWord);
        
        return result;
    }
    
    /**
     * 启动主应用并进入对话模式
     */
    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("wake_word_detected", true);
        startActivity(intent);
        
        Log.d(TAG, "Started MainActivity with wake word flag");
    }
}