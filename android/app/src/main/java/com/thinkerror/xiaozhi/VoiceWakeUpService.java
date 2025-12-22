package com.thinkerror.xiaozhi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    // 添加多唤醒词支持
    private List<String> wakeWords = new ArrayList<>();
    private Map<String, String> wakeWordPinyins = new HashMap<>();
    
    // 添加退出唤醒词支持
    private List<String> exitWakeWords = new ArrayList<>();
    private Map<String, String> exitWakeWordPinyins = new HashMap<>();
    
    private double similarityThreshold = 0.8;
    private long lastDetectionTime = 0;
    private double detectionCooldown = 2.0; // 防重复触发的冷却时间(秒)
    
    public VoiceWakeUpService(Context context, MethodChannel methodChannel) {
        this.context = context;
        this.methodChannel = methodChannel;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 初始化Vosk日志级别
        LibVosk.setLogLevel(LogLevel.INFO);
        
        // 初始化唤醒词列表
        initializeWakeWords();
        
        // 加载保存的唤醒词
        loadWakeWord();
    }
    
    /**
     * 初始化唤醒词列表
     */
    private void initializeWakeWords() {
        // 默认唤醒词列表，参考py-xiaozhi的实现
        wakeWords.add("你好小清");
        wakeWords.add("你好小明");
        wakeWords.add("你好小智");
        wakeWords.add("你好小天");
        wakeWords.add("小爱同学");
        wakeWords.add("贾维斯");
        
        // 预计算拼音(简单实现，不使用外部库)
        for (String wakeWord : wakeWords) {
            String pinyin = convertToPinyin(wakeWord);
            wakeWordPinyins.put(wakeWord, pinyin);
            Log.i(TAG, "Added wake word: " + wakeWord + " (pinyin: " + pinyin + ")");
        }
        
        // 初始化退出唤醒词列表
        initializeExitWakeWords();
        
        Log.i(TAG, "Initialized " + wakeWords.size() + " wake words and " + exitWakeWords.size() + " exit wake words");
    }
    
    /**
     * 初始化退出唤醒词列表
     */
    private void initializeExitWakeWords() {
        // 添加常见的退出唤醒词
        exitWakeWords.add("再见");
        exitWakeWords.add("拜拜");
        exitWakeWords.add("睡觉啦");
        exitWakeWords.add("晚安");
        exitWakeWords.add("下次再聊");
        exitWakeWords.add("不聊了");
        exitWakeWords.add("挂断");
        exitWakeWords.add("结束对话");
        
        // 预计算拼音
        for (String exitWakeWord : exitWakeWords) {
            String pinyin = convertToPinyin(exitWakeWord);
            exitWakeWordPinyins.put(exitWakeWord, pinyin);
            Log.i(TAG, "Added exit wake word: " + exitWakeWord + " (pinyin: " + pinyin + ")");
        }
    }
    
    /**
     * 简单的汉字转拼音实现
     * 这是一个简化的实现，主要用于匹配唤醒词
     */
    private String convertToPinyin(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // 简单的拼音映射表(只包含常用字)
        Map<Character, String> pinyinMap = new HashMap<>();
        pinyinMap.put('你', "ni");
        pinyinMap.put('好', "hao");
        pinyinMap.put('小', "xiao");
        pinyinMap.put('清', "qing");
        pinyinMap.put('明', "ming");
        pinyinMap.put('智', "zhi");
        pinyinMap.put('天', "tian");
        pinyinMap.put('爱', "ai");
        pinyinMap.put('同', "tong");
        pinyinMap.put('学', "xue");
        pinyinMap.put('贾', "jia");
        pinyinMap.put('维', "wei");
        pinyinMap.put('斯', "si");
        
        // 添加退出唤醒词的拼音映射
        pinyinMap.put('再', "zai");
        pinyinMap.put('见', "jian");
        pinyinMap.put('拜', "bai");
        pinyinMap.put('睡', "shui");
        pinyinMap.put('觉', "jiao");
        pinyinMap.put('啦', "la");
        pinyinMap.put('晚', "wan");
        pinyinMap.put('安', "an");
        pinyinMap.put('下', "xia");
        pinyinMap.put('次', "ci");
        pinyinMap.put('聊', "liao");
        pinyinMap.put('不', "bu");
        pinyinMap.put('挂', "gua");
        pinyinMap.put('断', "duan");
        pinyinMap.put('结', "jie");
        pinyinMap.put('束', "shu");
        pinyinMap.put('对', "dui");
        pinyinMap.put('话', "hua");
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String pinyin = pinyinMap.get(c);
            if (pinyin != null) {
                result.append(pinyin);
            } else if (c != '，' && c != ',') { // 跳过标点符号
                result.append(c); // 保留未映射的字符
            }
        }
        
        return result.toString().toLowerCase();
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
            // 不使用语法限制，创建自由识别器
            // 这样可以识别模型词汇表中的任何词，然后在结果中进行关键词匹配
            Log.i(TAG, "Using free-form recognition without grammar restrictions");
            
            // 创建不带语法的识别器，这样可以识别模型支持的所有词汇
            Recognizer recognizer = new Recognizer(model, 16000.0f);
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
        
        // 更新唤醒词列表，添加新的唤醒词
        if (!wakeWords.contains(currentWakeWord)) {
            wakeWords.add(currentWakeWord);
            String pinyin = convertToPinyin(currentWakeWord);
            wakeWordPinyins.put(currentWakeWord, pinyin);
            Log.i(TAG, "Added new wake word: " + currentWakeWord + " (pinyin: " + pinyin + ")");
        }
        
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
        // 添加调试日志，打印所有识别结果
        Log.i(TAG, "Received recognition result: " + hypothesis);
        
        // 使用更灵活的关键词检测
        if (containsWakeWord(hypothesis)) {
            Log.i(TAG, "Wake word detected: " + hypothesis);
            
            // 重置识别器，类似py-xiaozhi中的实现
            if (speechService != null) {
                try {
                    // 创建新的识别器
                    Recognizer recognizer = new Recognizer(model, 16000.0f);
                    speechService.stop();
                    speechService.shutdown();
                    
                    // 重新启动识别服务
                    speechService = new SpeechService(recognizer, 16000.0f);
                    speechService.startListening(this);
                    Log.i(TAG, "Recognizer reset after wake word detection");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to reset recognizer", e);
                }
            }
            
            // 创建final变量用于lambda表达式
            final String finalHypothesis = hypothesis;
            
            // 通知Flutter端检测到唤醒词
            mainHandler.post(() -> {
                if (methodChannel != null) {
                    methodChannel.invokeMethod("onWakeWordDetected", finalHypothesis);
                }
            });
        }
    }
    
    @Override
    public void onFinalResult(String hypothesis) {
        // 添加调试日志，打印所有最终识别结果
        Log.i(TAG, "Received final recognition result: " + hypothesis);
        
        // 最终结果，也使用灵活的关键词检测
        if (containsWakeWord(hypothesis)) {
            Log.i(TAG, "Wake word detected in final result: " + hypothesis);
            
            // 重置识别器，类似py-xiaozhi中的实现
            if (speechService != null) {
                try {
                    // 创建新的识别器
                    Recognizer recognizer = new Recognizer(model, 16000.0f);
                    speechService.stop();
                    speechService.shutdown();
                    
                    // 重新启动识别服务
                    speechService = new SpeechService(recognizer, 16000.0f);
                    speechService.startListening(this);
                    Log.i(TAG, "Recognizer reset after wake word detection in final result");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to reset recognizer", e);
                }
            }
            
            // 创建final变量用于lambda表达式
            final String finalHypothesis = hypothesis;
            
            // 通知Flutter端检测到唤醒词
            mainHandler.post(() -> {
                if (methodChannel != null) {
                    methodChannel.invokeMethod("onWakeWordDetected", finalHypothesis);
                }
            });
        }
    }
    
    /**
     * 检测文本中是否包含唤醒词
     * 参考py-xiaozhi的实现，使用拼音相似度计算
     * 增加了更模糊的匹配策略，解决同音词识别错误问题
     */
    private boolean containsWakeWord(String hypothesis) {
        // 添加调试日志
        //Log.d(TAG, "Checking for wake word in hypothesis: " + hypothesis);
        
        if (hypothesis == null || hypothesis.trim().isEmpty()) {
            Log.d(TAG, "Empty hypothesis, no wake word found");
            return false;
        }
        
        // 防重复触发
        long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime - lastDetectionTime < detectionCooldown) {
            //Log.d(TAG, "Detection cooldown active, ignoring");
            return false;
        }
        
        // 解析JSON格式的识别结果
        String recognizedText = extractTextFromJson(hypothesis);
        //Log.d(TAG, "Extracted text from hypothesis: " + recognizedText);
        
        if (recognizedText == null || recognizedText.trim().isEmpty()) {
            //Log.d(TAG, "No text extracted from hypothesis");
            return false;
        }
        
        // 只处理长度>=2的文本，因为退出唤醒词可能较短
        if (recognizedText.length() < 2) {
            //Log.d(TAG, "Text too short, ignoring: " + recognizedText);
            return false;
        }
        
        String normalizedHypothesis = recognizedText.toLowerCase().trim();
        String hypothesisPinyin = convertToPinyin(normalizedHypothesis);
        
        //Log.d(TAG, "Normalized hypothesis: " + normalizedHypothesis);
        //Log.d(TAG, "Hypothesis pinyin: " + hypothesisPinyin);
        
        // 先检查是否是退出唤醒词
        String bestExitMatch = null;
        double bestExitSimilarity = 0.0;
        
        for (String exitWakeWord : exitWakeWords) {
            // 1. 原文匹配
            double textSimilarity = calculateSimilarity(normalizedHypothesis, exitWakeWord);
            if (textSimilarity >= similarityThreshold && textSimilarity > bestExitSimilarity) {
                bestExitSimilarity = textSimilarity;
                bestExitMatch = exitWakeWord;
                Log.d(TAG, "Exit text match found: " + exitWakeWord + " with similarity " + textSimilarity);
            }
            
            // 2. 拼音匹配
            String exitWakeWordPinyin = exitWakeWordPinyins.get(exitWakeWord);
            if (exitWakeWordPinyin != null) {
                double pinyinSimilarity = calculateSimilarity(hypothesisPinyin, exitWakeWordPinyin);
                if (pinyinSimilarity >= similarityThreshold && pinyinSimilarity > bestExitSimilarity) {
                    bestExitSimilarity = pinyinSimilarity;
                    bestExitMatch = exitWakeWord;
                    Log.d(TAG, "Exit pinyin match found: " + exitWakeWord + " with similarity " + pinyinSimilarity);
                }
            }
        }
        
        // 如果检测到退出唤醒词，优先处理
        if (bestExitMatch != null) {
            lastDetectionTime = currentTime;
            Log.i(TAG, "Exit wake word detected: '" + bestExitMatch + "' (similarity: " + String.format("%.3f", bestExitSimilarity) + ")");
            Log.i(TAG, "Original text: " + recognizedText);
            
            // 创建final变量用于lambda表达式
            final String finalBestExitMatch = bestExitMatch;
            
            // 通知Flutter端检测到退出唤醒词
            mainHandler.post(() -> {
                if (methodChannel != null) {
                    methodChannel.invokeMethod("onExitWakeWordDetected", finalBestExitMatch);
                }
            });
            return true;
        }
        
        // 寻找普通唤醒词的最佳匹配
        String bestMatch = null;
        double bestSimilarity = 0.0;
        
        for (String wakeWord : wakeWords) {
            // 1. 原文匹配
            double textSimilarity = calculateSimilarity(normalizedHypothesis, wakeWord);
            if (textSimilarity >= similarityThreshold && textSimilarity > bestSimilarity) {
                bestSimilarity = textSimilarity;
                bestMatch = wakeWord;
                Log.d(TAG, "Text match found: " + wakeWord + " with similarity " + textSimilarity);
            }
            
            // 2. 拼音匹配
            String wakeWordPinyin = wakeWordPinyins.get(wakeWord);
            if (wakeWordPinyin != null) {
                double pinyinSimilarity = calculateSimilarity(hypothesisPinyin, wakeWordPinyin);
                if (pinyinSimilarity >= similarityThreshold && pinyinSimilarity > bestSimilarity) {
                    bestSimilarity = pinyinSimilarity;
                    bestMatch = wakeWord;
                    Log.d(TAG, "Pinyin match found: " + wakeWord + " with similarity " + pinyinSimilarity);
                }
            }
            
            // 3. 模糊匹配 - 针对"你好 小心"这样的错误识别
            if (isFuzzyMatch(normalizedHypothesis, wakeWord)) {
                // 模糊匹配成功，给予较高权重
                double fuzzySimilarity = 0.9; // 模糊匹配给予高分
                if (fuzzySimilarity > bestSimilarity) {
                    bestSimilarity = fuzzySimilarity;
                    bestMatch = wakeWord;
                    Log.d(TAG, "Fuzzy match found: " + wakeWord + " with fuzzy similarity");
                }
            }
        }
        
        // 触发检测
        if (bestMatch != null) {
            lastDetectionTime = currentTime;
            Log.i(TAG, "Wake word detected: '" + bestMatch + "' (similarity: " + String.format("%.3f", bestSimilarity) + ")");
            Log.i(TAG, "Original text: " + recognizedText);
            
            // 重置识别器，类似py-xiaozhi中的实现
            if (speechService != null) {
                try {
                    // 创建新的识别器
                    Recognizer recognizer = new Recognizer(model, 16000.0f);
                    speechService.stop();
                    speechService.shutdown();
                    
                    // 重新启动识别服务
                    speechService = new SpeechService(recognizer, 16000.0f);
                    speechService.startListening(this);
                    Log.i(TAG, "Recognizer reset after wake word detection");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to reset recognizer", e);
                }
            }
            
            // 创建final变量用于lambda表达式
            final String finalBestMatch = bestMatch;
            
            // 通知Flutter端检测到唤醒词
            mainHandler.post(() -> {
                if (methodChannel != null) {
                    methodChannel.invokeMethod("onWakeWordDetected", finalBestMatch);
                }
            });
            return true;
        }
        
        Log.d(TAG, "No wake word found in hypothesis: " + recognizedText);
        return false;
    }
    
    /**
     * 模糊匹配检测
     * 专门处理同音字、近音字的情况
     */
    private boolean isFuzzyMatch(String hypothesis, String wakeWord) {
        // 针对"你好 小清"这个唤醒词的特殊处理
        if (wakeWord.contains("你好小清")) {
            // 检查是否包含"你好"和"小清"的近音字
            boolean hasNiHao = hypothesis.contains("你好") ||
                             hypothesis.contains("您好") ||
                             hypothesis.contains("你好啊") ||
                             // 处理误识别的情况
                             hypothesis.contains("小心"); // "小心"可能被误识别为"小清"
                             
            boolean hasXiaoQing = hypothesis.contains("小清") ||
                                hypothesis.contains("小晴") ||  // "晴"是"清"的近音字
                                hypothesis.contains("小情") ||  // "情"是"清"的近音字
                                hypothesis.contains("小请") ||  // "请"是"清"的近音字
                                hypothesis.contains("小庆") ||  // "庆"是"清"的近音字
                                hypothesis.contains("小静") ||  // "静"是"清"的近音字
                                hypothesis.contains("小菁") ||  // "菁"是"清"的近音字
                                hypothesis.contains("小晴") ||  // "晴"是"清"的近音字
                                hypothesis.contains("小净") ||  // "净"是"清"的近音字
                                hypothesis.contains("小境") ||  // "境"是"清"的近音字
                                hypothesis.contains("小青");   // 直接匹配"小清"
            
            // 特殊处理：如果检测到"你好 小心"，认为是"你好 小清"的误识别
            if (hypothesis.contains("你好 小心") || hypothesis.contains("你好小心")) {
                Log.d(TAG, "Fuzzy match: '你好 小心' considered as '你好 小清'");
                return true;
            }
            
            // 特殊处理：如果检测到"小心"且前面有"你好"，认为是"你好 小清"的误识别
            if (hasNiHao && hypothesis.contains("小清")) {
                Log.d(TAG, "Fuzzy match: '你好' + '小清' combination found");
                return true;
            }
            
            // 如果同时包含"你好"和"小清"的近音字，认为匹配
            if (hasNiHao && hasXiaoQing) {
                Log.d(TAG, "Fuzzy match: ni hao + xiao qing near-homophone found");
                return true;
            }
        }
        
        // 对其他唤醒词的模糊匹配
        if (wakeWord.contains("你好小明")) {
            return hypothesis.contains("你好") && (
                   hypothesis.contains("小明") ||
                   hypothesis.contains("小名") ||  // "名"是"明"的近音字
                   hypothesis.contains("小民") ||  // "民"是"明"的近音字
                   hypothesis.contains("小鸣"));   // "鸣"是"明"的近音字
        }
        
        // 对"你好小智"的模糊匹配
        if (wakeWord.contains("你好小智")) {
            return hypothesis.contains("你好") && (
                   hypothesis.contains("小智") ||
                   hypothesis.contains("小志") ||  // "志"是"智"的近音字
                   hypothesis.contains("小至"));   // "至"是"智"的近音字
        }
        
        // 对"你好小天"的模糊匹配
        if (wakeWord.contains("你好小天")) {
            return hypothesis.contains("你好") && (
                   hypothesis.contains("小天") ||
                   hypothesis.contains("小添") ||  // "添"是"天"的近音字
                   hypothesis.contains("小田"));   // "田"是"天"的近音字
        }
        
        return false;
    }
    
    /**
     * 计算两个文本的相似度
     * 参考py-xiaozhi的实现
     */
    private double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        text1 = text1.toLowerCase();
        text2 = text2.toLowerCase();
        
        // 精确匹配
        if (text1.contains(text2)) {
            return 1.0;
        }
        
        // 字符重叠度
        java.util.HashSet<Character> set1 = new java.util.HashSet<>();
        java.util.HashSet<Character> set2 = new java.util.HashSet<>();
        
        for (char c : text1.toCharArray()) {
            set1.add(c);
        }
        
        for (char c : text2.toCharArray()) {
            set2.add(c);
        }
        
        java.util.HashSet<Character> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);
        
        java.util.HashSet<Character> union = new java.util.HashSet<>(set1);
        union.addAll(set2);
        
        if (union.size() == 0) {
            return 0.0;
        }
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * 将异常堆栈跟踪转换为字符串，便于日志记录
     */
    private String getStackTraceString(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * 从Vosk识别结果中提取文本
     * 参考py-xiaozhi的实现，直接从JSON中提取text字段
     */
    private String extractTextFromJson(String jsonResult) {
        try {
            if (jsonResult == null || jsonResult.trim().isEmpty()) {
                return null;
            }
            
            // 移除换行符和多余空格，确保JSON在一行上，便于解析
            String normalizedJson = jsonResult.replaceAll("\\s+", " ").trim();
            //Log.d(TAG, "Normalized JSON: " + normalizedJson);
            
            // 尝试使用JSON对象解析
            try {
                JSONObject jsonObject = new JSONObject(normalizedJson);
                
                // 首先检查是否有text字段
                if (jsonObject.has("text")) {
                    String text = jsonObject.getString("text");
                    if (text != null) {
                        //Log.d(TAG, "Successfully extracted JSON text: '" + text + "'");
                        return text.trim();
                    }
                }
                
                // 然后检查是否有partial字段
                if (jsonObject.has("partial")) {
                    String partial = jsonObject.getString("partial");
                    // 即使是空字符串也要返回，这是有效的识别结果
                    if (partial != null) {
                        //Log.d(TAG, "Successfully extracted JSON partial: '" + partial + "'");
                        return partial.trim();
                    }
                }
                
                // 检查其他可能的字段
                if (jsonObject.has("result")) {
                    JSONArray resultArray = jsonObject.getJSONArray("result");
                    if (resultArray.length() > 0) {
                        JSONObject firstResult = resultArray.getJSONObject(0);
                        if (firstResult.has("word")) {
                            String word = firstResult.getString("word");
                            if (word != null) {
                                //Log.d(TAG, "Successfully extracted JSON word: '" + word + "'");
                                return word.trim();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // JSON解析失败，尝试手动解析
                Log.d(TAG, "JSON parsing failed, trying manual extraction: " + e.getMessage());
                Log.d(TAG, "JSON parsing failed, stack trace: " + getStackTraceString(e));
                Log.d(TAG, "Original JSON was: " + jsonResult);
                
                // 如果JSON格式看起来正常但解析失败，可能是由于特殊字符或其他格式问题
                // 检查是否是简单的空partial字段
                if (normalizedJson.contains("\"partial\"") && normalizedJson.contains("\"\"")) {
                    Log.d(TAG, "Detected empty partial field, returning empty string");
                    return "";
                }
                
                // 检查是否是有效的JSON但包含空字段
                if (normalizedJson.contains("\"partial\"") && normalizedJson.contains(":\"\"")) {
                    Log.d(TAG, "Detected empty partial field with explicit empty quotes, returning empty string");
                    return "";
                }
            }
            
            // 手动解析作为备选方案
            // 使用标准化后的JSON字符串
            String jsonForManualParse = normalizedJson;
            
            // 检查text字段
            if (jsonForManualParse.contains("\"text\"")) {
                int textIndex = jsonForManualParse.indexOf("\"text\"");
                int colonIndex = jsonForManualParse.indexOf(":", textIndex);
                if (colonIndex != -1) {
                    int start = colonIndex + 1;
                    
                    // 跳过空格和引号
                    while (start < jsonForManualParse.length() && (jsonForManualParse.charAt(start) == ' ' || jsonForManualParse.charAt(start) == '\"')) {
                        start++;
                    }
                    
                    if (start >= jsonForManualParse.length()) return null;
                    
                    // 查找结束引号
                    int end = start;
                    while (end < jsonForManualParse.length() && jsonForManualParse.charAt(end) != '\"') {
                        end++;
                    }
                    
                    // 处理空字符串的情况
                    if (end == start) {
                        Log.d(TAG, "Empty text field detected");
                        return "";
                    }
                    
                    if (end > start) {
                        String extractedText = jsonForManualParse.substring(start, end);
                        if (extractedText != null && !extractedText.trim().isEmpty()) {
                            Log.d(TAG, "Successfully extracted manual text: '" + extractedText + "'");
                            return extractedText.trim();
                        }
                    }
                }
            }
            
            // 检查partial字段
            if (jsonForManualParse.contains("\"partial\"")) {
                int partialIndex = jsonForManualParse.indexOf("\"partial\"");
                int colonIndex = jsonForManualParse.indexOf(":", partialIndex);
                if (colonIndex != -1) {
                    int start = colonIndex + 1;
                    
                    // 跳过空格和引号
                    while (start < jsonForManualParse.length() && (jsonForManualParse.charAt(start) == ' ' || jsonForManualParse.charAt(start) == '\"')) {
                        start++;
                    }
                    
                    if (start >= jsonForManualParse.length()) return null;
                    
                    // 查找结束引号
                    int end = start;
                    while (end < jsonForManualParse.length() && jsonForManualParse.charAt(end) != '\"') {
                        end++;
                    }
                    
                    // 检查提取的内容是否只是JSON的结束符号或其他无效内容
                    if (end > start) {
                        String extractedText = jsonForManualParse.substring(start, end);
                        // 如果提取的内容是JSON结构的一部分（如}或类似的符号），则忽略
                        if (extractedText.equals("}") || extractedText.equals("{") ||
                            extractedText.equals("]") || extractedText.equals("[") ||
                            extractedText.trim().isEmpty()) {
                            Log.d(TAG, "Extracted text is JSON structure symbol or empty, ignoring: '" + extractedText + "'");
                            return null;
                        }
                    }
                    
                    // 处理空字符串的情况 - 这是关键修复
                    if (end == start) {
                        Log.d(TAG, "Empty partial field detected");
                        return "";
                    }
                    
                    // 检查提取的内容是否在引号外，如果是JSON结构符号则忽略
                    if (end < jsonForManualParse.length() && end > start) {
                        String extractedText = jsonForManualParse.substring(start, end);
                        // 如果提取的内容是JSON结构的一部分（如}或类似的符号），则忽略
                        if (extractedText.equals("}") || extractedText.equals("{") ||
                            extractedText.equals("]") || extractedText.equals("[") ||
                            extractedText.trim().isEmpty()) {
                            Log.d(TAG, "Extracted text is JSON structure symbol or empty, ignoring: '" + extractedText + "'");
                            return null;
                        }
                        
                        if (extractedText != null) {
                            Log.d(TAG, "Successfully extracted manual partial: '" + extractedText + "'");
                            return extractedText.trim();
                        }
                    }
                }
            }
            
            // 如果不是JSON格式，但包含其他识别结果格式，尝试直接使用
            if (jsonResult != null && !jsonResult.trim().isEmpty() && !jsonResult.contains("{")) {
                Log.d(TAG, "Not JSON format, using raw text: " + jsonResult);
                return jsonResult.trim();
            }
            
            Log.d(TAG, "Unable to extract text from JSON, returning null");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting text from JSON: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public void onPartialResult(String hypothesis) {
        // 添加调试日志，打印部分识别结果
        Log.d(TAG, "Received partial recognition result: " + hypothesis);
        
        // 部分结果也用于唤醒词检测，提高响应速度
        if (containsWakeWord(hypothesis)) {
            Log.i(TAG, "Wake word detected in partial result: " + hypothesis);
            
            // 重置识别器，类似py-xiaozhi中的实现
            if (speechService != null) {
                try {
                    // 创建新的识别器
                    Recognizer recognizer = new Recognizer(model, 16000.0f);
                    speechService.stop();
                    speechService.shutdown();
                    
                    // 重新启动识别服务
                    speechService = new SpeechService(recognizer, 16000.0f);
                    speechService.startListening(this);
                    Log.i(TAG, "Recognizer reset after wake word detection in partial result");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to reset recognizer", e);
                }
            }
            
            // 创建final变量用于lambda表达式
            final String finalHypothesis = hypothesis;
            
            // 通知Flutter端检测到唤醒词
            mainHandler.post(() -> {
                if (methodChannel != null) {
                    methodChannel.invokeMethod("onWakeWordDetected", finalHypothesis);
                }
            });
        }
    }
    
    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Recognition error", e);
        
        // 创建final变量用于lambda表达式
        final String errorMessage = e.getMessage();
        
        // 通知Flutter端发生错误
        mainHandler.post(() -> {
            if (methodChannel != null) {
                methodChannel.invokeMethod("onError", errorMessage);
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
            case "testRecognition":
                testRecognition(result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }
    
    // 测试语音识别功能
    private void testRecognition(Result result) {
        Log.i(TAG, "Testing voice recognition...");
        
        if (model == null) {
            Log.e(TAG, "Model not initialized, cannot test recognition");
            if (result != null) {
                result.error("MODEL_NOT_INITIALIZED", "Model is not initialized", null);
            }
            return;
        }
        
        if (!isListening) {
            Log.e(TAG, "Not listening, cannot test recognition");
            if (result != null) {
                result.error("NOT_LISTENING", "Speech service is not listening", null);
            }
            return;
        }
        
        Log.i(TAG, "Recognition test: Model initialized and listening");
        if (result != null) {
            result.success(true);
        }
    }
}