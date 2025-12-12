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

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "live2d_channel";
    private static final String TAG = "MainActivity";
    private Live2DViewFactory live2DViewFactory;
    
    // 添加实例映射管理
    private java.util.Map<String, Integer> instanceMap = new java.util.HashMap<>();
    private int nextModelIndex = 0;

    /**
     * 获取模型索引的辅助方法
     * @param instanceId 实例ID
     * @return 模型索引
     */
    private int getModelIndex(String instanceId) {
        if (instanceId == null) {
            return 0; // 默认索引
        }
        
        if (!instanceMap.containsKey(instanceId)) {
            instanceMap.put(instanceId, nextModelIndex++);
        }
        
        return instanceMap.get(instanceId);
    }

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
        
        // 注册MethodChannel用于与Flutter通信
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (call, result) -> {
                            Log.d(TAG, "MethodChannel call received: " + call.method);
                            if (call.method.equals("initLive2D")) {
                                String modelPath = call.argument("modelPath");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "initLive2D called: modelPath=" + modelPath + ", instanceId=" + instanceId);
                                
                                try {
                                    // 确保实例映射存在
                                    int modelIndex = getModelIndex(instanceId);
                                    Log.d(TAG, "initLive2D: instanceId=" + instanceId + " -> modelIndex=" + modelIndex);
                                    
                                    // TODO: 实现具体的模型初始化逻辑
                                    // 这里可以根据modelPath加载特定的模型
                                    result.success(modelIndex);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in initLive2D for instance: " + instanceId, e);
                                    result.error("INIT_ERROR", "Failed to initialize Live2D: " + e.getMessage(), null);
                                }
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
                                
                                try {
                                    // 获取当前模型并触发表情，使用多实例管理
                                    LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
                                    int modelIndex = getModelIndex(instanceId);
                                    Log.d(TAG, "triggerExpression: instanceId=" + instanceId + " -> modelIndex=" + modelIndex);
                                    
                                    if (live2DManager.getModel(modelIndex) != null) {
                                        live2DManager.getModel(modelIndex).setRandomExpression();
                                        result.success(null);
                                    } else {
                                        result.error("MODEL_NOT_READY", "Live2D model is not ready for instance: " + instanceId, null);
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
                                    // 获取当前模型并播放动作，使用多实例管理
                                    LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
                                    int modelIndex = getModelIndex(instanceId);
                                    Log.d(TAG, "playMotion: instanceId=" + instanceId + " -> modelIndex=" + modelIndex);
                                    
                                    if (live2DManager.getModel(modelIndex) != null) {
                                        int prio = priority != null ? priority : Priority.NORMAL.getPriority();
                                        live2DManager.getModel(modelIndex).startRandomMotion(motionGroup, prio);
                                        result.success(null);
                                    } else {
                                        result.error("MODEL_NOT_READY", "Live2D model is not ready for instance: " + instanceId, null);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in playMotion for instance: " + instanceId, e);
                                    result.error("MOTION_ERROR", "Failed to play motion: " + e.getMessage(), null);
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
                                Log.d(TAG, "isGearVisible called: instanceId=" + instanceId);
                                
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
                                Log.d(TAG, "isPowerVisible called: instanceId=" + instanceId);
                                
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
                                Log.d(TAG, "refreshView called: instanceId=" + instanceId);
                                
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
                            } else {
                                Log.w(TAG, "Unknown method called: " + call.method);
                                result.notImplemented();
                            }
                            
                        }
                );
        Log.d(TAG, "configureFlutterEngine: Configuration completed");
    }

    /**
     * 根据模型名称切换到指定模型
     * @param modelPath 模型路径（通常是模型名称）
     * @param instanceId 实例ID
     */
    private void changeToModelByName(String modelPath, String instanceId) {
        try {
            LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
            
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
     * 处理实例激活请求
     *
     * @param call MethodCall对象
     * @param result 结果回调
     */
    private void handleActivateInstance(MethodCall call, MethodChannel.Result result) {
        String instanceId = call.argument("instanceId");
        String modelPath = call.argument("modelPath");
        
        Log.d(TAG, "activateInstance called: instanceId=" + instanceId + ", modelPath=" + modelPath);
        
        if (instanceId == null) {
            result.error("INVALID_ARGUMENT", "Instance ID is null", null);
            return;
        }
        
        try {
            // 确保实例映射存在
            if (!instanceMap.containsKey(instanceId)) {
                instanceMap.put(instanceId, nextModelIndex++);
            }
            
            // 平衡的激活逻辑：确保必要初始化但避免过度操作
            try {
                Log.d(TAG, "activateInstance: Activating instance with safe initialization: " + instanceId);
                
                LAppDelegate appDelegate = LAppDelegate.getInstance();
                if (appDelegate != null) {
                    // 首先确保Live2D系统已正确初始化
                    if (!appDelegate.isInitialized()) {
                        Log.d(TAG, "activateInstance: LAppDelegate not initialized, attempting to reinitialize");
                        // 尝试重新初始化Live2D系统（但不强制重新加载模型）
                        try {
                            if (appDelegate.getActivity() != null) {
                                appDelegate.onStart(appDelegate.getActivity());
                                Log.d(TAG, "activateInstance: LAppDelegate reinitialized successfully");
                            } else {
                                Log.w(TAG, "activateInstance: Activity is null, cannot reinitialize LAppDelegate");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "activateInstance: Error reinitializing LAppDelegate", e);
                        }
                    }
                    
                    // 检查是否有模型加载，如果没有则安全加载
                    LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
                    if (live2DManager != null) {
                        int modelCount = live2DManager.getModelNum();
                        Log.d(TAG, "activateInstance: Current model count: " + modelCount);
                        
                        if (modelCount == 0) {
                            Log.d(TAG, "activateInstance: No models loaded, safely loading models");
                            // 安全地加载模型（不强制释放现有资源）
                            try {
                                live2DManager.setUpModel();
                                Log.d(TAG, "activateInstance: Model directories set up");
                                
                                // 尝试加载第一个模型
                                live2DManager.nextScene();
                                Log.d(TAG, "activateInstance: First model loaded safely");
                            } catch (Exception e) {
                                Log.e(TAG, "activateInstance: Error during safe model loading", e);
                            }
                        }
                    }
                    
                    // 请求渲染以确保显示
                    appDelegate.requestRender();
                    Log.d(TAG, "activateInstance: Render requested for instance: " + instanceId);
                } else {
                    Log.w(TAG, "activateInstance: LAppDelegate is null, cannot initialize");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "activateInstance: Error during activation", e);
                // 不抛出异常，允许继续使用
            }
            
            Log.d(TAG, "Instance activated: " + instanceId + " -> modelIndex: " + instanceMap.get(instanceId));
            result.success(instanceMap.get(instanceId));
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
        
        if (instanceId == null) {
            result.error("INVALID_ARGUMENT", "Instance ID is null", null);
            return;
        }
        
        try {
            if (instanceMap.containsKey(instanceId)) {
                int modelIndex = instanceMap.get(instanceId);
                
                // 释放模型资源
                LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
                if (live2DManager.getModel(modelIndex) != null) {
                    live2DManager.getModel(modelIndex).deleteModel();
                }
                
                // 从映射中移除
                instanceMap.remove(instanceId);
                
                Log.d(TAG, "Instance deactivated: " + instanceId);
            }
            
            result.success(null);
        } catch (Exception e) {
            Log.e(TAG, "Error in deactivateInstance for: " + instanceId, e);
            result.error("DEACTIVATE_INSTANCE_ERROR", "Failed to deactivate instance: " + e.getMessage(), null);
        }
    }
}