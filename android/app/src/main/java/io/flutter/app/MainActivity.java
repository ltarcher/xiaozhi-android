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
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "live2d_channel";
    private static final String TAG = "MainActivity";
    private Live2DViewFactory live2DViewFactory;

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
                                // TODO: 实现初始化Live2D模型的逻辑
                                result.success(null);
                            } else if (call.method.equals("onTap")) {
                                Double x = call.argument("x");
                                Double y = call.argument("y");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "onTap called: x=" + x + ", y=" + y + ", instanceId=" + instanceId);
                                // TODO: 实现点击事件处理逻辑
                                result.success(null);
                            } else if (call.method.equals("triggerExpression")) {
                                String expressionName = call.argument("expressionName");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "triggerExpression called: expressionName=" + expressionName + ", instanceId=" + instanceId);
                                
                                // 获取当前模型并触发表情
                                LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
                                if (live2DManager.getModel(0) != null) {
                                    live2DManager.getModel(0).setRandomExpression();
                                    result.success(null);
                                } else {
                                    result.error("MODEL_NOT_READY", "Live2D model is not ready", null);
                                }
                            } else if (call.method.equals("playMotion")) {
                                String motionGroup = call.argument("motionGroup");
                                Integer priority = call.argument("priority");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "playMotion called: motionGroup=" + motionGroup + ", priority=" + priority + ", instanceId=" + instanceId);
                                
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
                            } else if (call.method.equals("setGearVisible")) {
                                Boolean visible = call.argument("visible");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "setGearVisible called: visible=" + visible + ", instanceId=" + instanceId);
                                if (visible != null) {
                                    LAppDelegate appDelegate = LAppDelegate.getInstance();
                                    Log.d(TAG, "setGearVisible: appDelegate=" + (appDelegate != null ? "not null" : "null"));
                                    if (appDelegate != null) {
                                        LAppView appView = appDelegate.getView();
                                        Log.d(TAG, "setGearVisible: appView=" + (appView != null ? "not null" : "null"));
                                        if (appView != null) {
                                            appView.setGearVisible(visible);
                                            Log.d(TAG, "setGearVisible: Called appView.setGearVisible(" + visible + ")");
                                            // 强制刷新视图
                                            appDelegate.requestRender();
                                            Log.d(TAG, "setGearVisible: Called appDelegate.requestRender()");
                                            result.success(null);
                                        } else {
                                            Log.e(TAG, "setGearVisible: Live2D view is not ready");
                                            result.error("VIEW_NOT_READY", "Live2D view is not ready", null);
                                        }
                                    } else {
                                        Log.e(TAG, "setGearVisible: Live2D app delegate is not ready");
                                        result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                    }
                                } else {
                                    Log.e(TAG, "setGearVisible: Visible argument is null");
                                    result.error("INVALID_ARGUMENT", "Visible argument is null", null);
                                }
                            } else if (call.method.equals("setPowerVisible")) {
                                Boolean visible = call.argument("visible");
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "setPowerVisible called: visible=" + visible + ", instanceId=" + instanceId);
                                if (visible != null) {
                                    LAppDelegate appDelegate = LAppDelegate.getInstance();
                                    Log.d(TAG, "setPowerVisible: appDelegate=" + (appDelegate != null ? "not null" : "null"));
                                    if (appDelegate != null) {
                                        LAppView appView = appDelegate.getView();
                                        Log.d(TAG, "setPowerVisible: appView=" + (appView != null ? "not null" : "null"));
                                        if (appView != null) {
                                            appView.setPowerVisible(visible);
                                            Log.d(TAG, "setPowerVisible: Called appView.setPowerVisible(" + visible + ")");
                                            // 强制刷新视图
                                            appDelegate.requestRender();
                                            Log.d(TAG, "setPowerVisible: Called appDelegate.requestRender()");
                                            result.success(null);
                                        } else {
                                            Log.e(TAG, "setPowerVisible: Live2D view is not ready");
                                            result.error("VIEW_NOT_READY", "Live2D view is not ready", null);
                                        }
                                    } else {
                                        Log.e(TAG, "setPowerVisible: Live2D app delegate is not ready");
                                        result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                    }
                                } else {
                                    Log.e(TAG, "setPowerVisible: Visible argument is null");
                                    result.error("INVALID_ARGUMENT", "Visible argument is null", null);
                                }
                            } else if (call.method.equals("isGearVisible")) {
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "isGearVisible called: instanceId=" + instanceId);
                                LAppDelegate appDelegate = LAppDelegate.getInstance();
                                if (appDelegate != null) {
                                    LAppView appView = appDelegate.getView();
                                    if (appView != null) {
                                        boolean visible = appView.isGearVisible();
                                        Log.d(TAG, "isGearVisible returning: " + visible);
                                        result.success(visible);
                                    } else {
                                        Log.e(TAG, "isGearVisible: Live2D view is not ready");
                                        result.error("VIEW_NOT_READY", "Live2D view is not ready", null);
                                    }
                                } else {
                                    Log.e(TAG, "isGearVisible: Live2D app delegate is not ready");
                                    result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                }
                            } else if (call.method.equals("isPowerVisible")) {
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "isPowerVisible called: instanceId=" + instanceId);
                                LAppDelegate appDelegate = LAppDelegate.getInstance();
                                if (appDelegate != null) {
                                    LAppView appView = appDelegate.getView();
                                    if (appView != null) {
                                        boolean visible = appView.isPowerVisible();
                                        Log.d(TAG, "isPowerVisible returning: " + visible);
                                        result.success(visible);
                                    } else {
                                        Log.e(TAG, "isPowerVisible: Live2D view is not ready");
                                        result.error("VIEW_NOT_READY", "Live2D view is not ready", null);
                                    }
                                } else {
                                    Log.e(TAG, "isPowerVisible: Live2D app delegate is not ready");
                                    result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                }
                            } else if (call.method.equals("refreshView")) {
                                String instanceId = call.argument("instanceId");
                                Log.d(TAG, "refreshView called: instanceId=" + instanceId);
                                LAppDelegate appDelegate = LAppDelegate.getInstance();
                                Log.d(TAG, "refreshView: appDelegate=" + (appDelegate != null ? "not null" : "null"));
                                if (appDelegate != null) {
                                    // 请求重新渲染视图
                                    appDelegate.requestRender();
                                    Log.d(TAG, "refreshView: Called appDelegate.requestRender()");
                                    result.success(null);
                                } else {
                                    Log.e(TAG, "refreshView: Live2D app delegate is not ready");
                                    result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                }
                            } else {
                                Log.w(TAG, "Unknown method called: " + call.method);
                                result.notImplemented();
                            }
                        }
                );
        Log.d(TAG, "configureFlutterEngine: Configuration completed");
    }
}