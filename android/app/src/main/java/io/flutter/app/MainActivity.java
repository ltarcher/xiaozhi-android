package io.flutter.app;

import android.os.Bundle;

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
    private Live2DViewFactory live2DViewFactory;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
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
                            if (call.method.equals("initLive2D")) {
                                String modelPath = call.argument("modelPath");
                                // TODO: 实现初始化Live2D模型的逻辑
                                result.success(null);
                            } else if (call.method.equals("onTap")) {
                                Double x = call.argument("x");
                                Double y = call.argument("y");
                                // TODO: 实现处理点击事件的逻辑
                                result.success(null);
                            } else if (call.method.equals("triggerExpression")) {
                                String expressionName = call.argument("expressionName");
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
                                if (visible != null) {
                                    LAppDelegate appDelegate = LAppDelegate.getInstance();
                                    if (appDelegate != null) {
                                        LAppView appView = appDelegate.getView();
                                        if (appView != null) {
                                            appView.setGearVisible(visible);
                                            result.success(null);
                                        } else {
                                            result.error("VIEW_NOT_READY", "Live2D view is not ready", null);
                                        }
                                    } else {
                                        result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                    }
                                } else {
                                    result.error("INVALID_ARGUMENT", "Visible argument is null", null);
                                }
                            } else if (call.method.equals("setPowerVisible")) {
                                Boolean visible = call.argument("visible");
                                if (visible != null) {
                                    LAppDelegate appDelegate = LAppDelegate.getInstance();
                                    if (appDelegate != null) {
                                        LAppView appView = appDelegate.getView();
                                        if (appView != null) {
                                            appView.setPowerVisible(visible);
                                            result.success(null);
                                        } else {
                                            result.error("VIEW_NOT_READY", "Live2D view is not ready", null);
                                        }
                                    } else {
                                        result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                    }
                                } else {
                                    result.error("INVALID_ARGUMENT", "Visible argument is null", null);
                                }
                            } else if (call.method.equals("isGearVisible")) {
                                LAppDelegate appDelegate = LAppDelegate.getInstance();
                                if (appDelegate != null) {
                                    LAppView appView = appDelegate.getView();
                                    if (appView != null) {
                                        boolean visible = appView.isGearVisible();
                                        result.success(visible);
                                    } else {
                                        result.error("VIEW_NOT_READY", "Live2D view is not ready", null);
                                    }
                                } else {
                                    result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                }
                            } else if (call.method.equals("isPowerVisible")) {
                                LAppDelegate appDelegate = LAppDelegate.getInstance();
                                if (appDelegate != null) {
                                    LAppView appView = appDelegate.getView();
                                    if (appView != null) {
                                        boolean visible = appView.isPowerVisible();
                                        result.success(visible);
                                    } else {
                                        result.error("VIEW_NOT_READY", "Live2D view is not ready", null);
                                    }
                                } else {
                                    result.error("APP_DELEGATE_NOT_READY", "Live2D app delegate is not ready", null);
                                }
                            } else {
                                result.notImplemented();
                            }
                        }
                );
    }
}