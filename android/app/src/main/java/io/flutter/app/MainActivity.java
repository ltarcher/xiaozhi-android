package io.flutter.app;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.live2d.LAppLive2DManager;
import com.live2d.LAppDefine.MotionGroup;
import com.live2d.LAppDefine.Priority;
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
    
    // 表情管理器
    // private ExpressionManager expressionManager = new ExpressionManager();
    
    // 防重复触发表情的机制（简化为不使用实例ID）
    private String lastExpression = null;
    private static final long EXPRESSION_COOLDOWN_MS = 500; // 500ms冷却时间

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
                            //Log.d(TAG, "MethodChannel call received: " + call.method);
                            if (call.method.equals("initLive2D")) {
                                String modelPath = call.argument("modelPath");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "initLive2D called: modelPath=" + modelPath + ", instanceId=" + instanceId);
                                // TODO: 实现初始化Live2D模型的逻辑
                                result.success(null);
                            } else if (call.method.equals("onTap")) {
                                Double x = call.argument("x");
                                Double y = call.argument("y");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "onTap called: x=" + x + ", y=" + y + ", instanceId=" + instanceId);
                                
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
                                        
                                        Log.d(TAG, "onTap: Successfully processed touch event for instance: " + instanceId);
                                        result.success(null);
                                    } else {
                                        Log.e(TAG, "onTap: LAppDelegate is not ready");
                                        result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in onTap for instance: " + instanceId, e);
                                    result.error("TAP_ERROR", "Failed to process tap: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("triggerExpression")) {
                                String expressionName = call.argument("expressionName");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "triggerExpression called: expressionName=" + expressionName + ", instanceId=" + instanceId);
                                
                                // 验证表达式名称
                                if (expressionName == null || expressionName.isEmpty()) {
                                    Log.w(TAG, "triggerExpression: Expression name is null or empty, skipping");
                                    result.success(null); // 不报错，只是跳过
                                    return;
                                }
                                
                                try {
                                    // 获取当前模型并触发表情（简化为单实例）
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
                                   
                                    // 获取当前活动模型索引（简化为单实例）
                                    int modelIndex = live2DManager.getCurrentModel();
                                    if (modelIndex < 0 || live2DManager.getModel(modelIndex) == null) {
                                        // 如果当前没有活动模型，尝试加载第一个模型
                                        if (live2DManager.getModelNum() > 0) {
                                            modelIndex = 0;
                                        } else {
                                            result.error("MODEL_NOT_AVAILABLE", "No Live2D model available", null);
                                            return;
                                        }
                                    }
                                   
                                    Log.d(TAG, "triggerExpression: using current modelIndex=" + modelIndex);
                                   
                                    // 获取映射后的表达式ID
                                    String mappedExpressionId = getExpressionId(expressionName);
                                    Log.d(TAG, "triggerExpression: Mapping '" + expressionName + "' to '" + mappedExpressionId + "'");
                                   
                                    // 防重复触发检查（简化版）
                                    long currentTime = System.currentTimeMillis();
                                    if (lastExpression != null && lastExpression.equals(mappedExpressionId)) {
                                        // 这里可以添加时间检查，但暂时简化处理
                                        Log.d(TAG, "triggerExpression: Same expression as last one, proceeding anyway");
                                    }
                                   
                                    // 检查表达式是否存在
                                    if (isExpressionAvailable(live2DManager.getModel(modelIndex), mappedExpressionId)) {
                                        try {
                                            // 使用映射后的表达式ID
                                            live2DManager.getModel(modelIndex).setExpression(mappedExpressionId);
                                           
                                            // 更新最后触发的表情
                                            lastExpression = mappedExpressionId;
                                           
                                            Log.d(TAG, "triggerExpression: Successfully set expression '" + mappedExpressionId + "'");
                                            result.success(null);
                                        } catch (Exception e) {
                                            Log.e(TAG, "triggerExpression: Error setting expression '" + mappedExpressionId + "'", e);
                                            // 清除记录，允许下次重试
                                            lastExpression = null;
                                            result.error("EXCEPTION_SET_ERROR", "Failed to set expression: " + e.getMessage(), null);
                                        }
                                    } else {
                                        Log.w(TAG, "triggerExpression: Expression '" + expressionName + "' (mapped to '" + mappedExpressionId + "') not found in model, using random expression instead");
                                        // 如果指定的表达式不存在，使用随机表情作为后备
                                        try {
                                            live2DManager.getModel(modelIndex).setRandomExpression();
                                            result.success(null);
                                        } catch (Exception e) {
                                            Log.e(TAG, "triggerExpression: Error setting random expression", e);
                                            result.error("RANDOM_EXPRESSION_ERROR", "Failed to set random expression: " + e.getMessage(), null);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in triggerExpression for instance: " + instanceId, e);
                                    result.error("EXPRESSION_ERROR", "Failed to trigger expression: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("playMotion")) {
                                String motionGroup = call.argument("motionGroup");
                                Integer priority = call.argument("priority");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "playMotion called: motionGroup=" + motionGroup + ", priority=" + priority + ", instanceId=" + instanceId);
                                
                                if (motionGroup == null) {
                                    result.error("INVALID_ARGUMENT", "Motion group is null", null);
                                    return;
                                }
                                
                                try {
                                    // 获取当前模型并播放动作（简化为单实例）
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
                                   
                                    // 获取当前活动模型索引（简化为单实例）
                                    int modelIndex = live2DManager.getCurrentModel();
                                    if (modelIndex < 0 || live2DManager.getModel(modelIndex) == null) {
                                        // 如果当前没有活动模型，尝试加载第一个模型
                                        if (live2DManager.getModelNum() > 0) {
                                            modelIndex = 0;
                                        } else {
                                            result.error("MODEL_NOT_AVAILABLE", "No Live2D model available", null);
                                            return;
                                        }
                                    }
                                   
                                    Log.d(TAG, "playMotion: using current modelIndex=" + modelIndex);
                                   
                                    if (live2DManager.getModel(modelIndex) != null) {
                                        int prio = priority != null ? priority : Priority.NORMAL.getPriority();
                                        live2DManager.getModel(modelIndex).startRandomMotion(motionGroup, prio);
                                        result.success(null);
                                    } else {
                                        result.error("MODEL_NOT_READY", "Live2D model is not ready", null);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in playMotion for instance: " + instanceId, e);
                                    result.error("MOTION_ERROR", "Failed to play motion: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("initLive2D")) {
                                String modelPath = call.argument("modelPath");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "initLive2D called: modelPath=" + modelPath + ", instanceId=" + instanceId);
                                
                                try {
                                    // 简化初始化逻辑，不使用实例映射
                                    Log.d(TAG, "initLive2D: initializing Live2D for instanceId=" + instanceId);
                                   
                                    // TODO: 实现具体的模型初始化逻辑
                                    // 这里可以根据modelPath加载特定的模型
                                    result.success(0); // 总是返回模型索引0
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in initLive2D for instance: " + instanceId, e);
                                    result.error("INIT_ERROR", "Failed to initialize Live2D: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("activateInstance")) {
                                handleActivateInstance(call, result);
                            } else if (call.method.equals("deactivateInstance")) {
                                handleDeactivateInstance(call, result);
                            } else if (call.method.equals("setGearVisible")) {
                                Boolean visible = call.argument("visible");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "setGearVisible called: visible=" + visible + ", instanceId=" + instanceId);
                                
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
                                   
                                    // 设置指定实例的齿轮按钮可见性
                                    appView.setGearVisible(visible);
                                    Log.d(TAG, "setGearVisible: Called appView.setGearVisible(" + visible + ") for instance: " + instanceId);
                                   
                                    // 强制刷新视图
                                    appDelegate.requestRender();
                                    Log.d(TAG, "setGearVisible: Called appDelegate.requestRender() for instance: " + instanceId);
                                   
                                    result.success(null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in setGearVisible for instance: " + instanceId, e);
                                    result.error("GEAR_VISIBLE_ERROR", "Failed to set gear visibility: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("setPowerVisible")) {
                                Boolean visible = call.argument("visible");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "setPowerVisible called: visible=" + visible + ", instanceId=" + instanceId);
                                
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
                                   
                                    // 设置指定实例的电源按钮可见性
                                    appView.setPowerVisible(visible);
                                    Log.d(TAG, "setPowerVisible: Called appView.setPowerVisible(" + visible + ") for instance: " + instanceId);
                                   
                                    // 强制刷新视图
                                    appDelegate.requestRender();
                                    Log.d(TAG, "setPowerVisible: Called appDelegate.requestRender() for instance: " + instanceId);
                                   
                                    result.success(null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in setPowerVisible for instance: " + instanceId, e);
                                    result.error("POWER_VISIBLE_ERROR", "Failed to set power visibility: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("isGearVisible")) {
                                String instanceId = call.argument("instanceId");
                                //Log.d(TAG, "isGearVisible called: instanceId=" + instanceId);
                                
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
                                    Log.d(TAG, "isGearVisible returning: " + visible + " for instance: " + instanceId);
                                    result.success(visible);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in isGearVisible for instance: " + instanceId, e);
                                    result.error("GEAR_VISIBLE_QUERY_ERROR", "Failed to query gear visibility: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("isPowerVisible")) {
                                String instanceId = call.argument("instanceId");
                                //Log.d(TAG, "isPowerVisible called: instanceId=" + instanceId);
                                
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
                                    Log.d(TAG, "isPowerVisible returning: " + visible + " for instance: " + instanceId);
                                    result.success(visible);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in isPowerVisible for instance: " + instanceId, e);
                                    result.error("POWER_VISIBLE_QUERY_ERROR", "Failed to query power visibility: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("refreshView")) {
                                String instanceId = call.argument("instanceId");
                                //Log.d(TAG, "refreshView called: instanceId=" + instanceId);
                                
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
                                    Log.d(TAG, "refreshView: Called appDelegate.requestRender() for instance: " + instanceId);
                                    result.success(null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in refreshView for instance: " + instanceId, e);
                                    result.error("REFRESH_ERROR", "Failed to refresh view: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("setLipSyncValue")) {
                                Double value = call.argument("value");
                                String instanceId = call.argument("instanceId");
                                //Log.d(TAG, "setLipSyncValue called: value=" + value + ", instanceId=" + instanceId);
                                
                                if (value == null) {
                                    Log.e(TAG, "setLipSyncValue: Value argument is null");
                                    result.error("INVALID_ARGUMENT", "Value argument is null", null);
                                    return;
                                }
                                
                                try {
                                    // 获取Live2D管理器
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
                                   
                                    // 简化处理：直接使用当前活动模型，不使用实例映射
                                    int modelIndex = live2DManager.getCurrentModel();
                                    if (modelIndex < 0 || live2DManager.getModel(modelIndex) == null) {
                                        // 如果当前没有活动模型，尝试使用第一个模型
                                        if (live2DManager.getModelNum() > 0) {
                                            modelIndex = 0;
                                        } else {
                                            Log.w(TAG, "setLipSyncValue: No Live2D model available");
                                            result.error("MODEL_NOT_AVAILABLE", "No Live2D model available", null);
                                            return;
                                        }
                                    }
                                   
                                    //Log.d(TAG, "setLipSyncValue: using current modelIndex=" + modelIndex);
                                   
                                    // 限制value在0.0-1.0范围内
                                    float lipSyncValue = Math.max(0.0f, Math.min(1.0f, value.floatValue()));
                                    live2DManager.getModel(modelIndex).setLipSyncValue(lipSyncValue);
                                    //Log.d(TAG, "setLipSyncValue: Successfully set lip sync value " + lipSyncValue + " for model " + modelIndex);
                                    result.success(null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in setLipSyncValue", e);
                                    result.error("LIPSYNC_ERROR", "Failed to set lip sync value: " + e.getMessage(), null);
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
     * @param instanceId 实例ID
     */
    private void changeToModelByName(String modelPath, String instanceId) {
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
            
            Log.d(TAG, "changeToModelByName: Model loading completed for instance: " + instanceId);
            
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

    /**
     * 处理实例激活请求
     *
     * @param call MethodCall对象
     * @param result 结果回调
     */
    private void handleActivateInstance(MethodCall call, MethodChannel.Result result) {
        String instanceId = call.argument("instanceId");
        String modelPath = call.argument("modelPath");
        
        Log.d(TAG, "activateInstance called: instanceId=" + instanceId + ", modelPath=" + modelPath);
        
        // 简化处理：不需要实例ID，只要确保有模型加载
        try {
            LAppLive2DManager live2DManager;
            try {
                live2DManager = LAppLive2DManager.getInstance();
                if (live2DManager == null) {
                    Log.e(TAG, "activateInstance: LAppLive2DManager.getInstance() returned null");
                    result.error("MANAGER_NOT_INITIALIZED", "Live2D manager is not initialized", null);
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "activateInstance: Error getting LAppLive2DManager instance", e);
                result.error("MANAGER_ERROR", "Failed to get Live2D manager: " + e.getMessage(), null);
                return;
            }
            
            // 确保至少有一个模型加载
            if (live2DManager.getModelNum() == 0) {
                Log.d(TAG, "activateInstance: No models loaded, forcing scene change to index 0");
                live2DManager.nextScene();
            }
            
            // 如果提供了模型路径，尝试切换到指定模型
            if (modelPath != null && !modelPath.isEmpty()) {
                Log.d(TAG, "activateInstance: Attempting to load model from path: " + modelPath);
                changeToModelByName(modelPath, instanceId);
            }
            
            // 获取当前活动模型索引
            int currentModelIndex = live2DManager.getCurrentModel();
            if (currentModelIndex < 0) {
                currentModelIndex = 0; // 默认使用第一个模型
            }
            
            Log.d(TAG, "Instance activated: " + instanceId + " -> using modelIndex: " + currentModelIndex);
            result.success(currentModelIndex);
        } catch (Exception e) {
            Log.e(TAG, "Error in activateInstance for: " + instanceId, e);
            result.error("ACTIVATE_INSTANCE_ERROR", "Failed to activate instance: " + e.getMessage(), null);
        }
    }

    /**
     * 处理实例停用请求
     *
     * @param call MethodCall对象
     * @param result 结果回调
     */
    private void handleDeactivateInstance(MethodCall call, MethodChannel.Result result) {
        String instanceId = call.argument("instanceId");
        
        Log.d(TAG, "deactivateInstance called: instanceId=" + instanceId);
        
        // 简化处理：不使用实例映射，不做特殊处理
        Log.d(TAG, "Instance deactivation not needed for single instance model: " + instanceId);
        result.success(null);
    }
}