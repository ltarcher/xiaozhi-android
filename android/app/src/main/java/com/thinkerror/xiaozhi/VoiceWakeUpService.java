package com.thinkerror.xiaozhi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class VoiceWakeUpService implements MethodCallHandler, RecognitionListener {
    private static final String TAG = "VoiceWakeUpService";
    private static final String PREFS_NAME = "xiaozhi_prefs";
    private static final String WAKE_WORD_KEY = "WAKE_WORD";
    private static final String DEFAULT_WAKE_WORD = "你好，小清";
    
    private Context context;
    private MethodChannel methodChannel;
    private Model model = null;
    private SpeechService speechService = null;
    private String currentWakeWord = DEFAULT_WAKE_WORD;
    private boolean isListening = false;
    private Handler mainHandler;
    private SharedPreferences sharedPreferences;
    
    public VoiceWakeUpService(Context context, MethodChannel methodChannel) {
        this.context = context;
        this.methodChannel = methodChannel;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 初始化Vosk日志级别
        LibVosk.setLogLevel(LogLevel.INFO);
        
        // 加载保存的唤醒词
        loadWakeWord();
    }
    
    private void loadWakeWord() {
        currentWakeWord = sharedPreferences.getString(WAKE_WORD_KEY, DEFAULT_WAKE_WORD);
        Log.i(TAG, "Loaded wake word: " + currentWakeWord);
    }
    
    private void initModel(Result result) {
        // 使用StorageService异步加载模型
        // 添加调试日志
        Log.i(TAG, "Initializing model from assets...");
        
        try {
            // 尝试列出assets目录下的文件
            String[] assetList = context.getAssets().list("");
            if (assetList != null) {
                Log.i(TAG, "Assets in root directory:");
                for (String asset : assetList) {
                    Log.i(TAG, "  " + asset);
                }
            }
            
            // 尝试列出vosk目录下的文件
            String[] voskList = context.getAssets().list("vosk");
            if (voskList != null) {
                Log.i(TAG, "Assets in vosk directory:");
                for (String asset : voskList) {
                    Log.i(TAG, "  " + asset);
                }
            }
            
            // 尝试列出vosk/models目录下的文件
            String[] modelList = context.getAssets().list("vosk/models");
            if (modelList != null) {
                Log.i(TAG, "Assets in vosk/models directory:");
                for (String asset : modelList) {
                    Log.i(TAG, "  " + asset);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error listing assets", e);
        }
        
        // 检查assets目录结构，找到正确的模型路径
        Log.i(TAG, "Checking assets directory structure for Vosk model");
        
        // 根据Vosk示例，使用简单的模型名称
        // 将vosk-model-small-cn-0.22目录下的文件复制到应用的files目录
        StorageService.unpack(context, "vosk-model-small-cn-0.22",
            "vosk-model-small-cn-0.22",
            (model) -> {
                this.model = model;
                if (result != null) {
                    result.success(true);
                }
                Log.i(TAG, "Model loaded successfully");
            },
            (exception) -> {
                Log.e(TAG, "Failed to unpack model", exception);
                if (result != null) {
                    result.error("MODEL_LOAD_ERROR", "Failed to unpack model: " + exception.getMessage(), null);
                }
            });
    }
    
    public void initializeModel(Result result) {
        if (model != null) {
            if (result != null) {
                result.success(true);
            }
            return;
        }
        
        initModel(result);
    }
    
    public void startListening(Result result) {
        if (model == null) {
            if (result != null) {
                result.error("MODEL_NOT_INITIALIZED", "Model is not initialized", null);
            }
            return;
        }
        
        if (isListening) {
            if (result != null) {
                result.success(true);
            }
            return;
        }
        
        try {
            // 创建带有唤醒词列表的识别器
            StringBuilder grammar = new StringBuilder();
            grammar.append("[");
            grammar.append("\"").append(currentWakeWord).append("\"");
            grammar.append(", ");
            grammar.append("\"[unk]\"");
            grammar.append("]");
            
            Log.i(TAG, "Using grammar: " + grammar.toString());
            
            Recognizer recognizer = new Recognizer(model, 16000.0f, grammar.toString());
            speechService = new SpeechService(recognizer, 16000.0f);
            speechService.startListening(this);
            isListening = true;
            
            if (result != null) {
                result.success(true);
            }
            
            Log.i(TAG, "Started listening for wake word: " + currentWakeWord);
        } catch (IOException e) {
            Log.e(TAG, "Failed to start listening", e);
            if (result != null) {
                result.error("LISTENING_START_ERROR", "Failed to start listening: " + e.getMessage(), null);
            }
        }
    }
    
    public void stopListening(Result result) {
        if (!isListening) {
            if (result != null) {
                result.success(true);
            }
            return;
        }
        
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
        }
        
        isListening = false;
        
        if (result != null) {
            result.success(true);
        }
        
        Log.i(TAG, "Stopped listening");
    }
    
    public void setWakeWord(String wakeWord, Result result) {
        if (wakeWord == null || wakeWord.trim().isEmpty()) {
            if (result != null) {
                result.error("INVALID_WAKE_WORD", "Wake word cannot be empty", null);
            }
            return;
        }
        
        currentWakeWord = wakeWord.trim();
        
        // 保存到SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(WAKE_WORD_KEY, currentWakeWord);
        editor.apply();
        
        // 如果正在监听，重新启动以使用新的唤醒词
        boolean wasListening = isListening;
        if (wasListening) {
            stopListening(null);
            startListening(null);
        }
        
        if (result != null) {
            result.success(true);
        }
        
        Log.i(TAG, "Wake word updated to: " + currentWakeWord);
    }
    
    public String getWakeWord() {
        return currentWakeWord;
    }
    
    public boolean isListening() {
        return isListening;
    }
    
    public void dispose() {
        stopListening(null);
        
        // Model对象不需要显式销毁，由垃圾回收器处理
        model = null;
        
        Log.i(TAG, "VoiceWakeUpService disposed");
    }
    
    // RecognitionListener 实现
    @Override
    public void onResult(String hypothesis) {
        // 检测是否包含唤醒词
        if (hypothesis.toLowerCase().contains(currentWakeWord.toLowerCase())) {
            Log.i(TAG, "Wake word detected: " + hypothesis);
            
            // 通知Flutter端检测到唤醒词
            mainHandler.post(() -> {
                if (methodChannel != null) {
                    methodChannel.invokeMethod("onWakeWordDetected", hypothesis);
                }
            });
        }
    }
    
    @Override
    public void onFinalResult(String hypothesis) {
        // 最终结果，也检查是否包含唤醒词
        if (hypothesis.toLowerCase().contains(currentWakeWord.toLowerCase())) {
            Log.i(TAG, "Wake word detected in final result: " + hypothesis);
            
            // 通知Flutter端检测到唤醒词
            mainHandler.post(() -> {
                if (methodChannel != null) {
                    methodChannel.invokeMethod("onWakeWordDetected", hypothesis);
                }
            });
        }
    }
    
    @Override
    public void onPartialResult(String hypothesis) {
        // 部分结果，不用于唤醒词检测
    }
    
    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Recognition error", e);
        
        // 通知Flutter端发生错误
        mainHandler.post(() -> {
            if (methodChannel != null) {
                methodChannel.invokeMethod("onError", e.getMessage());
            }
        });
    }
    
    @Override
    public void onTimeout() {
        Log.i(TAG, "Recognition timeout");
    }
    
    // MethodCallHandler 实现
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "initializeModel":
                initializeModel(result);
                break;
            case "startListening":
                startListening(result);
                break;
            case "stopListening":
                stopListening(result);
                break;
            case "setWakeWord":
                String wakeWord = call.argument("wakeWord");
                setWakeWord(wakeWord, result);
                break;
            case "getWakeWord":
                result.success(currentWakeWord);
                break;
            case "isListening":
                result.success(isListening);
                break;
            default:
                result.notImplemented();
                break;
        }
    }
}