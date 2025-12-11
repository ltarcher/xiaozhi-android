package io.flutter.app;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.live2d.LAppLive2DManager;
import com.live2d.LAppDefine.MotionGroup;
import com.live2d.LAppDefine.Priority;
import com.live2d.Live2DViewFactory;
import com.live2d.LAppDelegate;
import com.live2d.sdk.cubism.framework.CubismFramework;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "live2d_channel";
    private static final String TAG = "MainActivity";
    private Live2DViewFactory live2DViewFactory;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        Log.d(TAG, "configureFlutterEngine called");
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
                            Log.d(TAG, "Method call received: " + call.method);
                            if (call.method.equals("initLive2D")) {
                                String modelPath = call.argument("modelPath");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "initLive2D called with modelPath: " + modelPath + ", instanceId: " + instanceId);
                                // TODO: 实现初始化Live2D模型的逻辑
                                result.success(null);
                            } else if (call.method.equals("onTap")) {
                                Double x = call.argument("x");
                                Double y = call.argument("y");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "onTap called with x: " + x + ", y: " + y + ", instanceId: " + instanceId);
                                // TODO: 实现处理点击事件的逻辑
                                result.success(null);
                            } else if (call.method.equals("triggerExpression")) {
                                String expressionName = call.argument("expressionName");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "triggerExpression called with expressionName: " + expressionName + ", instanceId: " + instanceId);
                                if (expressionName != null) {
                                    // 获取当前模型并设置表情
                                    LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
                                    if (live2DManager.getModel(0) != null) {
                                        live2DManager.getModel(0).setExpression(expressionName);
                                        result.success(null);
                                    } else {
                                        result.error("MODEL_NOT_READY", "Live2D model is not ready", null);
                                    }
                                } else {
                                    result.error("INVALID_ARGUMENT", "Expression name is null", null);
                                }
                            } else if (call.method.equals("playMotion")) {
                                String motionGroup = call.argument("motionGroup");
                                Integer priority = call.argument("priority");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "playMotion called with motionGroup: " + motionGroup + ", priority: " + priority + ", instanceId: " + instanceId);
                                
                                if (motionGroup != null) {
                                    // 获取当前模型并播放动作
                                    LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
                                    if (live2DManager.getModel(0) != null) {
                                        int prio = priority != null ? priority : Priority.NORMAL.getPriority();
                                        live2DManager.getModel(0).startRandomMotion(motionGroup, prio);
                                        result.success(null);
                                    } else {
                                        result.error("MODEL_NOT_READY", "Live2D model is not ready", null);
                                    }
                                } else {
                                    result.error("INVALID_ARGUMENT", "Motion group is null", null);
                                }
                            } else if (call.method.equals("activateInstance")) {
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "activateInstance called with instanceId: " + instanceId);
                                // 激活实例的逻辑
                                try {
                                    LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
                                    Log.d(TAG, "Current model count: " + live2DManager.getModelNum());
                                    // 确保模型已经加载，如果没有则重新加载
                                    if (live2DManager.getModelNum() <= 0) {
                                        Log.d(TAG, "No models loaded, setting up models");
                                        live2DManager.setUpModel();
                                        Log.d(TAG, "Model dir size after setup: " + live2DManager.getModelDirSize());
                                        if (live2DManager.getModelDirSize() > 0) {
                                            Log.d(TAG, "Changing scene to 0");
                                            live2DManager.changeScene(0);
                                        }
                                    }
                                    result.success(null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to activate instance: " + e.getMessage(), e);
                                    result.error("ACTIVATION_FAILED", "Failed to activate instance: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("deactivateInstance")) {
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "deactivateInstance called with instanceId: " + instanceId);
                                // 停用实例的逻辑 - 在这里我们可以暂停某些操作但不销毁模型
                                try {
                                    // 我们不实际销毁模型，只是标记为非活跃状态
                                    result.success(null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to deactivate instance: " + e.getMessage(), e);
                                    result.error("DEACTIVATION_FAILED", "Failed to deactivate instance: " + e.getMessage(), null);
                                }
                            } else if (call.method.equals("cleanupInstance")) {
                                String instanceId = call.argument("instanceId");
                                Integer viewId = call.argument("viewId");
                                Log.d(TAG, "cleanupInstance called with instanceId: " + instanceId + ", viewId: " + viewId);
                                // 清理实例的逻辑
                                try {
                                    // 实际清理模型资源
                                    LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
                                    live2DManager.releaseAllModel();
                                    result.success(null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to cleanup instance: " + e.getMessage(), e);
                                    result.error("CLEANUP_FAILED", "Failed to cleanup instance: " + e.getMessage(), null);
                                }
                            } else {
                                Log.d(TAG, "Method not implemented: " + call.method);
                                result.notImplemented();
                            }
                        }
                );
        Log.d(TAG, "configureFlutterEngine completed");
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate completed");
    }
    
    @Override
    protected void onStart() {
        Log.d(TAG, "onStart called");
        super.onStart();
        Log.d(TAG, "onStart completed");
    }
    
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume called");
        super.onResume();
        // 恢复Live2D应用程序
        LAppDelegate.getInstance().onStart(this);
        Log.d(TAG, "onResume completed");
    }
    
    @Override
    protected void onPause() {
        Log.d(TAG, "onPause called");
        super.onPause();
        // 暂停Live2D应用程序
        LAppDelegate.getInstance().onPause();
        Log.d(TAG, "onPause completed");
    }
    
    @Override
    protected void onStop() {
        Log.d(TAG, "onStop called");
        super.onStop();
        LAppDelegate.getInstance().onStop();
        Log.d(TAG, "onStop completed");
    }
    
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called");
        super.onDestroy();
        LAppDelegate.getInstance().onDestroy();
        Log.d(TAG, "onDestroy completed");
    }
}