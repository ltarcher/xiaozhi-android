package com.thinkerror.xiaozhi;

import android.os.Bundle;
import android.view.SurfaceView;
import android.opengl.GLSurfaceView;
import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import com.live2d.LAppDelegate;
import com.live2d.GLRenderer;
import com.live2d.LAppLive2DManager;
import com.live2d.LAppModel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.thinkerror.xiaozhi/live2d";
    
    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
    private MethodChannel channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 首先初始化OpenGL ES支持
        initOpenGL();
        
        // 设置内容视图为GLSurfaceView
        setContentView(glSurfaceView);
    }
    
    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        
        // 创建MethodChannel
        channel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL);
        channel.setMethodCallHandler(
            (call, result) -> {
                switch (call.method) {
                    case "startMotion":
                        handleStartMotion(call, result);
                        break;
                    case "setExpression":
                        handleSetExpression(call, result);
                        break;
                    case "setLipSyncValue":
                        handleSetLipSyncValue(call, result);
                        break;
                    case "nextScene":
                        handleNextScene(call, result);
                        break;
                    case "changeScene":
                        handleChangeScene(call, result);
                        break;
                    default:
                        result.notImplemented();
                        break;
                }
            }
        );
    }
    
    // 处理开始动作的调用
    private void handleStartMotion(MethodCall call, MethodChannel.Result result) {
        String group = call.argument("group");
        Integer number = call.argument("number");
        Integer priority = call.argument("priority");
        
        if (group == null || number == null || priority == null) {
            result.error("INVALID_ARGUMENTS", "Missing arguments for startMotion", null);
            return;
        }
        
        // 在OpenGL线程中执行
        glSurfaceView.queueEvent(() -> {
            LAppLive2DManager manager = LAppLive2DManager.getInstance();
            int motionId = manager.startMotion(group, number, priority);
            // 通过主线程返回结果
            runOnUiThread(() -> result.success(motionId));
        });
    }
    
    // 处理设置表情的调用
    private void handleSetExpression(MethodCall call, MethodChannel.Result result) {
        String expressionId = call.argument("expressionId");
        
        if (expressionId == null) {
            result.error("INVALID_ARGUMENTS", "Missing expressionId for setExpression", null);
            return;
        }
        
        // 在OpenGL线程中执行
        glSurfaceView.queueEvent(() -> {
            LAppLive2DManager manager = LAppLive2DManager.getInstance();
            manager.setExpression(expressionId);
            // 通过主线程返回结果
            runOnUiThread(() -> result.success(true));
        });
    }
    
    // 处理设置口型同步值的调用
    private void handleSetLipSyncValue(MethodCall call, MethodChannel.Result result) {
        Double value = call.argument("value");
        
        if (value == null) {
            result.error("INVALID_ARGUMENTS", "Missing value for setLipSyncValue", null);
            return;
        }
        
        // 在OpenGL线程中执行
        glSurfaceView.queueEvent(() -> {
            LAppLive2DManager manager = LAppLive2DManager.getInstance();
            LAppModel model = manager.getModel();
            if (model != null) {
                model.setLipSyncValue(value.floatValue());
            }
            // 通过主线程返回结果
            runOnUiThread(() -> result.success(true));
        });
    }
    
    // 处理切换到下一个场景的调用
    private void handleNextScene(MethodCall call, MethodChannel.Result result) {
        // 在OpenGL线程中执行
        glSurfaceView.queueEvent(() -> {
            LAppLive2DManager manager = LAppLive2DManager.getInstance();
            manager.nextScene();
            // 通过主线程返回结果
            runOnUiThread(() -> result.success(true));
        });
    }
    
    // 处理切换到指定场景的调用
    private void handleChangeScene(MethodCall call, MethodChannel.Result result) {
        Integer index = call.argument("index");
        
        if (index == null) {
            result.error("INVALID_ARGUMENTS", "Missing index for changeScene", null);
            return;
        }
        
        // 在OpenGL线程中执行
        glSurfaceView.queueEvent(() -> {
            LAppLive2DManager manager = LAppLive2DManager.getInstance();
            manager.changeScene(index);
            // 通过主线程返回结果
            runOnUiThread(() -> result.success(true));
        });
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // 然后初始化Live2D
        LAppDelegate.getInstance().onStart(this);
        
        // 设置模型
        LAppLive2DManager.getInstance().setUpModel();
    }
    
    private void initOpenGL() {
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // 使用OpenGL ES 2.0
        
        glRenderer = GLRenderer.getInstance();
        glSurfaceView.setRenderer(glRenderer);
        
        // 设置渲染模式为持续渲染
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (channel != null) {
            channel.setMethodCallHandler(null);
        }
        LAppDelegate.getInstance().onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LAppDelegate.getInstance().onStop();
    }
    
    // getter方法，供其他类访问GLSurfaceView和GLRenderer
    public GLSurfaceView getGLSurfaceView() {
        return glSurfaceView;
    }
    
    public GLRenderer getGLRenderer() {
        return glRenderer;
    }
}