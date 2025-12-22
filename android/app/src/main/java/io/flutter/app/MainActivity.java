package io.flutter.app;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.live2d.LAppLive2DManager;
import com.live2d.LAppModel;
import com.live2d.LAppDefine.MotionGroup;
import com.live2d.LAppDefine;
import com.live2d.LAppView;
import com.live2d.Live2DViewFactory;
import com.live2d.LAppDelegate;
import com.thinkerror.xiaozhi.AudioPlayerCompat;
import com.thinkerror.xiaozhi.DeviceInfoCompat;
import com.thinkerror.xiaozhi.VoiceWakeUpService;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "live2d_channel";
    private static final String TAG = "MainActivity";
    private Live2DViewFactory live2DViewFactory;
    private VoiceWakeUpService voiceWakeUpService;
    private MethodChannel voiceWakeUpChannel;
    
    // 单实例支持，不需要实例映射管理
    
    // 表情管理器
    // private ExpressionManager expressionManager = new ExpressionManager();
    
    // 防重复触发表情的机制
    private String lastExpression = null;
    private static final long EXPRESSION_COOLDOWN_MS = 500; // 500ms冷却时间
    
    // 防重复设置口型同步值的机制
    private float lastLipSyncValue = -1.0f;
    private static final float LIPSYNC_THRESHOLD = 0.01f; // 口型同步值变化阈值


    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        Log.d(TAG, "configureFlutterEngine: Starting configuration");
        // 先注册PlatformView工厂
        live2DViewFactory = new Live2DViewFactory(this);
        flutterEngine
                .getPlatformViewsController()
                .getRegistry()
                .registerViewFactory("live2d_view", live2DViewFactory);
        
        // 然后调用父类方法
        super.configureFlutterEngine(flutterEngine);

        // 注册Android 9兼容音频播放器
        AudioPlayerCompat.registerWith(flutterEngine, getApplicationContext());
        // 注册设备信息兼容性工具
        DeviceInfoCompat.registerWith(flutterEngine);
        
        // 注册语音唤醒服务
        voiceWakeUpChannel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), "voice_wakeup_channel");
        voiceWakeUpService = new VoiceWakeUpService(getApplicationContext(), voiceWakeUpChannel);
        voiceWakeUpChannel.setMethodCallHandler(voiceWakeUpService);
        
        // 注册MethodChannel用于与Flutter通信
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (call, result) -> {
                            Log.d(TAG, "MethodChannel call received: " + call.method);
                            if (call.method.equals("initLive2D")) {
                                String modelPath = call.argument("modelPath");
                                Log.d(TAG, "initLive2D called: modelPath=" + modelPath);
                                
                                try {
                                    // 单实例模式，不需要模型索引
                                    Log.d(TAG, "initLive2D: initializing single instance");
                                   
                                    // TODO: 实现具体的模型初始化逻辑
                                    // 这里可以根据modelPath加载特定的模型
                                    result.success(0);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in initLive2D", e);
                                    result.error("INIT_ERROR", "Failed to initialize Live2D: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("onTap")) {
                                Double x = call.argument("x");
                                Double y = call.argument("y");
                                Log.d(TAG, "onTap called: x=" + x + ", y=" + y);
                                
                                try {
                                    // 将触摸事件传递给Live2D模块处理
                                    LAppDelegate appDelegate = LAppDelegate.getInstance();
                                    if (appDelegate != null) {
                                        // 直接调用触摸处理方法
                                        float touchX = x != null ? x.floatValue() : 0.0f;
                                        float touchY = y != null ? y.floatValue() : 0.0f;
                                        
                                        // 模拟触摸事件序列
                                        appDelegate.onTouchBegan(touchX, touchY);
                                        appDelegate.onTouchEnd(touchX, touchY);
                                        
                                        Log.d(TAG, "onTap: Successfully processed touch event");
                                        result.success(null);
                                    } else {
                                        Log.e(TAG, "onTap: LAppDelegate is not ready");
                                        result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in onTap", e);
                                    result.error("TAP_ERROR", "Failed to process tap: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("triggerExpression")) {
                                String expressionName = call.argument("expressionName");
                                Log.d(TAG, "triggerExpression called: expressionName=" + expressionName);
                                
                                // 验证表达式名称
                                if (expressionName == null || expressionName.isEmpty()) {
                                    Log.w(TAG, "triggerExpression: Expression name is null or empty, skipping");
                                    result.success(null); // 不报错，只是跳过
                                    return;
                                }
                                
                                // 检查冷却时间，避免重复触发
                                long currentTime = System.currentTimeMillis();
                                if (lastExpression != null && lastExpression.equals(expressionName)) {
                                    Log.d(TAG, "triggerExpression: Same expression triggered too recently, skipping: " + expressionName);
                                    result.success(null);
                                    return;
                                }
                                
                                try {
                                    LAppLive2DManager live2DManager;
                                    try {
                                        live2DManager = LAppLive2DManager.getInstance();
                                        if (live2DManager == null) {
                                            Log.e(TAG, "triggerExpression: LAppLive2DManager.getInstance() returned null");
                                            result.error("MANAGER_NOT_INITIALIZED", "Live2D manager is not initialized", null);
                                            return;
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "triggerExpression: Error getting LAppLive2DManager instance", e);
                                        result.error("MANAGER_ERROR", "Failed to get Live2D manager: " + e.getMessage(), null);
                                        return;
                                    }
                                   
                                    // 检查是否有模型加载
                                    if (live2DManager.getModelNum() == 0) {
                                        Log.w(TAG, "triggerExpression: No models loaded, forcing scene change to index 0");
                                        live2DManager.nextScene();
                                    }
                                    
                                    // 使用当前活动的模型索引
                                    int currentModelIndex = live2DManager.getCurrentModel();
                                    if (currentModelIndex >= 0 && currentModelIndex < live2DManager.getModelNum()) {
                                        LAppModel model = live2DManager.getModel(currentModelIndex);
                                        if (model != null) {
                                            // 检查表达式是否在模型中可用
                                            if (isExpressionAvailable(model, expressionName)) {
                                                // 获取表达式ID
                                                String expressionId = getExpressionId(expressionName);
                                                Log.d(TAG, "triggerExpression: Using expression ID: " + expressionId + " for expression: " + expressionName);
                                                
                                                model.setExpression(expressionId);
                                                lastExpression = expressionName; // 记录最后触发的表达式
                                                Log.d(TAG, "triggerExpression: Successfully triggered expression: " + expressionName + " with ID: " + expressionId + " for model index " + currentModelIndex);
                                                result.success(null);
                                            } else {
                                                Log.w(TAG, "triggerExpression: Expression '" + expressionName + "' not available in model, trying random expression");
                                                model.setRandomExpression(); // 回退到随机表情
                                                lastExpression = "Random";
                                                result.success(null);
                                            }
                                        } else {
                                            Log.e(TAG, "triggerExpression: Model is null for index: " + currentModelIndex);
                                            result.error("MODEL_NULL", "Model is null", null);
                                        }
                                    } else {
                                        Log.e(TAG, "triggerExpression: Invalid model index: " + currentModelIndex);
                                        result.error("INVALID_MODEL_INDEX", "Invalid model index", null);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in triggerExpression for: " + expressionName, e);
                                    result.error("EXPRESSION_ERROR", "Failed to trigger expression: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("playMotion")) {
                                String motionGroup = call.argument("motionGroup");
                                Integer priority = call.argument("priority");
                                Log.d(TAG, "playMotion called: motionGroup=" + motionGroup + ", priority=" + priority);
                                
                                if (motionGroup == null) {
                                    result.error("INVALID_ARGUMENT", "Motion group is null", null);
                                    return;
                                }
                                
                                int motionPriority = (priority != null) ? priority : 1; // 默认优先级为1
                                
                                try {
                                    LAppLive2DManager live2DManager;
                                    try {
                                        live2DManager = LAppLive2DManager.getInstance();
                                        if (live2DManager == null) {
                                            Log.e(TAG, "playMotion: LAppLive2DManager.getInstance() returned null");
                                            result.error("MANAGER_NOT_INITIALIZED", "Live2D manager is not initialized", null);
                                            return;
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "playMotion: Error getting LAppLive2DManager instance", e);
                                        result.error("MANAGER_ERROR", "Failed to get Live2D manager: " + e.getMessage(), null);
                                        return;
                                    }
                                   
                                    // 检查是否有模型加载
                                    if (live2DManager.getModelNum() == 0) {
                                        Log.w(TAG, "playMotion: No models loaded, forcing scene change to index 0");
                                        live2DManager.nextScene();
                                    }
                                    
                                    // 使用当前活动的模型索引
                                    int currentModelIndex = live2DManager.getCurrentModel();
                                    if (currentModelIndex >= 0 && currentModelIndex < live2DManager.getModelNum()) {
                                        LAppModel model = live2DManager.getModel(currentModelIndex);
                                        if (model != null) {
                                            // 触发动作
                                            Log.d(TAG, "playMotion: Playing motion group: " + motionGroup + " with priority: " + motionPriority);
                                            
                                            // 将motionGroup转换为MotionGroup枚举，添加错误处理
                                            MotionGroup motionGroupEnum;
                                            try {
                                                motionGroupEnum = MotionGroup.valueOf(motionGroup.toUpperCase());
                                            } catch (IllegalArgumentException e) {
                                                Log.w(TAG, "Unknown motion group: " + motionGroup + ", using IDLE");
                                                motionGroupEnum = MotionGroup.IDLE;
                                            }
                                            // 将优先级整数转换为枚举
                                            LAppDefine.Priority priorityEnum = motionPriority >= 3 ? LAppDefine.Priority.FORCE :
                                                                   motionPriority >= 2 ? LAppDefine.Priority.NORMAL :
                                                                   LAppDefine.Priority.IDLE;
                                            
                                            // 使用motionGroupEnum的ID作为动作组名称，number设为0
                                            model.startMotion(motionGroupEnum.getId(), 0, priorityEnum.getPriority());
                                            Log.d(TAG, "playMotion: Successfully started motion: " + motionGroup + " with priority: " + motionPriority + " for model index " + currentModelIndex);
                                            result.success(null);
                                        } else {
                                            Log.e(TAG, "playMotion: Model is null for index: " + currentModelIndex);
                                            result.error("MODEL_NULL", "Model is null", null);
                                        }
                                    } else {
                                        Log.e(TAG, "playMotion: Invalid model index: " + currentModelIndex);
                                        result.error("INVALID_MODEL_INDEX", "Invalid model index", null);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in playMotion for motionGroup: " + motionGroup, e);
                                    result.error("MOTION_ERROR", "Failed to play motion: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("setGearVisible")) {
                                Boolean visible = call.argument("visible");
                                Log.d(TAG, "setGearVisible called: visible=" + visible);
                                
                                if (visible == null) {
                                    Log.e(TAG, "setGearVisible: Visible argument is null");
                                    result.error("INVALID_ARGUMENT", "Visible argument is null", null);
                                    return;
                                }
                                
                                try {
                                    LAppDelegate appDelegate = LAppDelegate.getInstance();
                                    Log.d(TAG, "setGearVisible: appDelegate=" + (appDelegate != null ? "not null" : "null"));
                                    if (appDelegate == null) {
                                        Log.e(TAG, "setGearVisible: Live2D app delegate is not ready");
                                        result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                        return;
                                    }
                                   
                                    LAppView appView = appDelegate.getView();
                                    Log.d(TAG, "setGearVisible: appView=" + (appView != null ? "not null" : "null"));
                                    if (appView == null) {
                                        Log.e(TAG, "setGearVisible: Live2D view is not ready");
                                        result.error("VIEW_NOT_READY", "Live2D view is not ready", null);
                                        return;
                                    }
                                   
                                    // 设置齿轮按钮可见性
                                    appView.setGearVisible(visible);
                                    Log.d(TAG, "setGearVisible: Called appView.setGearVisible(" + visible + ")");
                                   
                                    // 强制刷新视图
                                    appDelegate.requestRender();
                                    Log.d(TAG, "setGearVisible: Called appDelegate.requestRender()");
                                   
                                    result.success(null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in setGearVisible", e);
                                    result.error("GEAR_VISIBLE_ERROR", "Failed to set gear visibility: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("setPowerVisible")) {
                                Boolean visible = call.argument("visible");
                                Log.d(TAG, "setPowerVisible called: visible=" + visible);
                                
                                if (visible == null) {
                                    Log.e(TAG, "setPowerVisible: Visible argument is null");
                                    result.error("INVALID_ARGUMENT", "Visible argument is null", null);
                                    return;
                                }
                                
                                try {
                                    LAppDelegate appDelegate = LAppDelegate.getInstance();
                                    Log.d(TAG, "setPowerVisible: appDelegate=" + (appDelegate != null ? "not null" : "null"));
                                    if (appDelegate == null) {
                                        Log.e(TAG, "setPowerVisible: Live2D app delegate is not ready");
                                        result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                        return;
                                    }
                                   
                                    LAppView appView = appDelegate.getView();
                                    Log.d(TAG, "setPowerVisible: appView=" + (appView != null ? "not null" : "null"));
                                    if (appView == null) {
                                        Log.e(TAG, "setPowerVisible: Live2D view is not ready");
                                        result.error("VIEW_NOT_READY", "Live2D view is not ready", null);
                                        return;
                                    }
                                   
                                    // 设置电源按钮可见性
                                    appView.setPowerVisible(visible);
                                    Log.d(TAG, "setPowerVisible: Called appView.setPowerVisible(" + visible + ")");
                                   
                                    // 强制刷新视图
                                    appDelegate.requestRender();
                                    Log.d(TAG, "setPowerVisible: Called appDelegate.requestRender()");
                                   
                                    result.success(null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in setPowerVisible", e);
                                    result.error("POWER_VISIBLE_ERROR", "Failed to set power visibility: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("isGearVisible")) {
                                Log.d(TAG, "isGearVisible called");
                                
                                try {
                                    LAppDelegate appDelegate = LAppDelegate.getInstance();
                                    if (appDelegate == null) {
                                        Log.e(TAG, "isGearVisible: Live2D app delegate is not ready");
                                        result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                        return;
                                    }
                                   
                                    LAppView appView = appDelegate.getView();
                                    if (appView == null) {
                                        Log.e(TAG, "isGearVisible: Live2D view is not ready");
                                        result.error("VIEW_NOT_READY", "Live2D view is not ready", null);
                                        return;
                                    }
                                   
                                    boolean visible = appView.isGearVisible();
                                    Log.d(TAG, "isGearVisible returning: " + visible);
                                    result.success(visible);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in isGearVisible", e);
                                    result.error("GEAR_VISIBLE_QUERY_ERROR", "Failed to query gear visibility: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("isPowerVisible")) {
                                Log.d(TAG, "isPowerVisible called");
                                
                                try {
                                    LAppDelegate appDelegate = LAppDelegate.getInstance();
                                    if (appDelegate == null) {
                                        Log.e(TAG, "isPowerVisible: Live2D app delegate is not ready");
                                        result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                        return;
                                    }
                                   
                                    LAppView appView = appDelegate.getView();
                                    if (appView == null) {
                                        Log.e(TAG, "isPowerVisible: Live2D view is not ready");
                                        result.error("VIEW_NOT_READY", "Live2D view is not ready", null);
                                        return;
                                    }
                                   
                                    boolean visible = appView.isPowerVisible();
                                    Log.d(TAG, "isPowerVisible returning: " + visible);
                                    result.success(visible);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in isPowerVisible", e);
                                    result.error("POWER_VISIBLE_QUERY_ERROR", "Failed to query power visibility: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("refreshView")) {
                                Log.d(TAG, "refreshView called");
                                
                                try {
                                    LAppDelegate appDelegate = LAppDelegate.getInstance();
                                    Log.d(TAG, "refreshView: appDelegate=" + (appDelegate != null ? "not null" : "null"));
                                    if (appDelegate == null) {
                                        Log.e(TAG, "refreshView: Live2D app delegate is not ready");
                                        result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                        return;
                                    }
                                   
                                    // 请求重新渲染视图
                                    appDelegate.requestRender();
                                    Log.d(TAG, "refreshView: Called appDelegate.requestRender()");
                                    result.success(null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in refreshView", e);
                                    result.error("REFRESH_ERROR", "Failed to refresh view: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("setLipSyncValue")) {
                                Double value = call.argument("value");
                                Log.d(TAG, "setLipSyncValue called: value=" + value);
                                
                                if (value == null) {
                                    Log.e(TAG, "setLipSyncValue: Value argument is null");
                                    result.error("INVALID_ARGUMENT", "Value argument is null", null);
                                    return;
                                }
                                
                                // 限制value在0.0-1.0范围内
                                float lipSyncValue = Math.max(0.0f, Math.min(1.0f, value.floatValue()));
                                
                                // 检查值是否与上次设置的值有显著变化
                                if (Math.abs(lipSyncValue - lastLipSyncValue) < LIPSYNC_THRESHOLD) {
                                    // 值没有显著变化，跳过更新
                                    Log.d(TAG, "setLipSyncValue: Skipping update - value (" + lipSyncValue + ") unchanged from last value (" + lastLipSyncValue + ")");
                                    result.success(null);
                                    return;
                                }
                                
                                try {
                                    LAppLive2DManager live2DManager;
                                    try {
                                        live2DManager = LAppLive2DManager.getInstance();
                                        if (live2DManager == null) {
                                            Log.e(TAG, "setLipSyncValue: LAppLive2DManager.getInstance() returned null");
                                            result.error("MANAGER_NOT_INITIALIZED", "Live2D manager is not initialized", null);
                                            return;
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "setLipSyncValue: Error getting LAppLive2DManager instance", e);
                                        result.error("MANAGER_ERROR", "Failed to get Live2D manager: " + e.getMessage(), null);
                                        return;
                                    }
                                   
                                    // 检查是否有模型加载
                                    if (live2DManager.getModelNum() == 0) {
                                        Log.w(TAG, "setLipSyncValue: No models loaded, forcing scene change to index 0");
                                        live2DManager.nextScene();
                                    }
                                    
                                    // 在单实例模式下，直接使用当前活动模型
                                    // 不依赖索引，而是直接获取当前活动的模型
                                    LAppModel model = null;
                                    try {
                                        // 尝试获取第一个可用的模型
                                        if (live2DManager.getModelNum() > 0) {
                                            model = live2DManager.getModel(0);
                                        }
                                        
                                        if (model != null) {
                                            model.setLipSyncValue(lipSyncValue);
                                            lastLipSyncValue = lipSyncValue; // 更新最后设置的值
                                            Log.d(TAG, "setLipSyncValue: Successfully set lip sync value " + lipSyncValue);
                                            result.success(null);
                                        } else {
                                            Log.e(TAG, "setLipSyncValue: No available model found");
                                            result.error("MODEL_NULL", "No available model", null);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "setLipSyncValue: Error accessing model", e);
                                        result.error("MODEL_ACCESS_ERROR", "Error accessing model: " + e.getMessage(), null);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in setLipSyncValue", e);
                                    result.error("LIPSYNC_ERROR", "Failed to set lip sync value: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("updateModel")) {
                                String modelPath = call.argument("modelPath");
                                Log.d(TAG, "updateModel called: modelPath=" + modelPath);
                                
                                if (modelPath == null || modelPath.isEmpty()) {
                                    Log.e(TAG, "updateModel: Model path is null or empty");
                                    result.error("INVALID_ARGUMENT", "Model path is null or empty", null);
                                    return;
                                }
                                
                                try {
                                    // 获取Live2DPlatformView实例并更新模型
                                    if (live2DViewFactory != null) {
                                        // 直接通过Live2DViewFactory的updateModel方法更新模型
                                        live2DViewFactory.updateModel(modelPath);
                                        Log.d(TAG, "updateModel: Model updated successfully to: " + modelPath);
                                        result.success(null);
                                    } else {
                                        Log.e(TAG, "updateModel: Live2DViewFactory is null");
                                        result.error("FACTORY_NULL", "Live2D view factory is null", null);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in updateModel", e);
                                    result.error("UPDATE_MODEL_ERROR", "Failed to update model: " + e.getMessage(), null);
                                }
                            } else {
                                Log.w(TAG, "Unknown method called: " + call.method);
                                result.notImplemented();
                            }
                            
                        }
                );
        Log.d(TAG, "configureFlutterEngine: Configuration completed");
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

    /**
     * 根据模型名称切换到指定模型
     * @param modelPath 模型路径（通常是模型名称）
     */
    private void changeToModelByName(String modelPath) {
        try {
            LAppLive2DManager live2DManager;
            try {
                live2DManager = LAppLive2DManager.getInstance();
                if (live2DManager == null) {
                    Log.e(TAG, "changeToModelByName: LAppLive2DManager.getInstance() returned null");
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "changeToModelByName: Error getting LAppLive2DManager instance", e);
                return;
            }
            
            // 从modelPath中提取模型名称（去掉路径前缀）
            String modelName = modelPath;
            if (modelPath.contains("/")) {
                String[] parts = modelPath.split("/");
                modelName = parts[parts.length - 1];
            }
            
            Log.d(TAG, "changeToModelByName: Extracted model name: " + modelName + " from path: " + modelPath);
            
            // 尝试通过LAppLive2DManager的私有方法或反射来切换模型
            // 由于没有直接的API，我们使用nextScene()来循环切换直到找到目标模型
            // 这里简化处理，确保至少有一个模型被加载
            if (live2DManager.getModelNum() == 0) {
                Log.d(TAG, "changeToModelByName: No models loaded, calling nextScene to load first model");
                live2DManager.nextScene();
            }
            
            Log.d(TAG, "changeToModelByName: Model loading completed");
            
        } catch (Exception e) {
            Log.e(TAG, "changeToModelByName: Error changing model", e);
        }
    }
    
    /**
     * 检查指定的表达式是否在模型中可用
     * @param model Live2D模型
     * @param expressionName 表达式名称
     * @return 如果表达式存在返回true，否则返回false
     */
    private boolean isExpressionAvailable(com.live2d.LAppModel model, String expressionName) {
        if (model == null || expressionName == null || expressionName.isEmpty()) {
            return false;
        }
        
        try {
            // 通过反射获取expressions字段
            java.lang.reflect.Field expressionsField = model.getClass().getDeclaredField("expressions");
            expressionsField.setAccessible(true);
            java.util.Map<String, ?> expressions = (java.util.Map<String, ?>) expressionsField.get(model);
            
            if (expressions != null) {
                // 首先检查直接的表达式名称
                if (expressions.containsKey(expressionName)) {
                    Log.d(TAG, "Expression '" + expressionName + "' found directly in model");
                    return true;
                }
                
                // 检查常见的表情映射
                String mappedId = getExpressionId(expressionName);
                if (expressions.containsKey(mappedId)) {
                    Log.d(TAG, "Expression '" + expressionName + "' -> '" + mappedId + "' found in model");
                    return true;
                }
                
                Log.d(TAG, "Expression '" + expressionName + "' not found in model");
                return false;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking expression availability: " + e.getMessage());
        }
        
        // 如果无法检查表达式列表，假设表达式存在（向后兼容）
        Log.d(TAG, "Cannot verify expression '" + expressionName + "', assuming it exists");
        return true;
    }
    
    /**
     * 获取表达式对应的实际ID
     * @param expressionName 表达式名称
     * @return 表达式ID，如果映射不存在则返回原始名称
     */
    private String getExpressionId(String expressionName) {
        if (expressionName == null || expressionName.isEmpty()) {
            return expressionName;
        }
        
        // 常见的表情名称到ID的映射
        if (expressionName.equals("Happy")) return "F02";
        if (expressionName.equals("Sad")) return "F07";
        if (expressionName.equals("Surprised")) return "F03";
        if (expressionName.equals("Normal")) return "F01";
        if (expressionName.equals("Blushing")) return "F08";
        if (expressionName.equals("Angry")) return "F05";
        if (expressionName.equals("Sleepy")) return "F06";
        
        // 如果不在常见映射中，返回原始名称
        return expressionName;
    }

}