package com.live2d;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.flutter.plugin.platform.PlatformView;

public class Live2DPlatformView implements PlatformView {
    private static final String TAG = "Live2DPlatformView";
    private final GLSurfaceView glSurfaceView;
    private final GLRenderer glRenderer;
    private Activity activity;

    public Live2DPlatformView(@NonNull Context context, @Nullable Map<String, Object> creationParams) {
        Log.d(TAG, "Live2DPlatformView constructor called");
        
        // 优先通过LAppDelegate获取Activity
        LAppDelegate appDelegate = LAppDelegate.getInstance();
        this.activity = appDelegate.getActivity();
        Log.d(TAG, "Activity from LAppDelegate: " + (this.activity != null ? "Available" : "Null"));

        // 如果从LAppDelegate获取不到Activity，则尝试从context获取
        if (this.activity == null) {
            if (context instanceof Activity) {
                this.activity = (Activity) context;
                Log.d(TAG, "Activity from context: Direct Activity instance");
            } else if (context instanceof android.content.MutableContextWrapper) {
                android.content.MutableContextWrapper wrapper = (android.content.MutableContextWrapper) context;
                Context baseContext = wrapper.getBaseContext();
                Log.d(TAG, "Base context type: " + (baseContext != null ? baseContext.getClass().getName() : "null"));
                if (baseContext instanceof Activity) {
                    this.activity = (Activity) baseContext;
                    Log.d(TAG, "Activity from context: Base context is Activity");
                }
            } else {
                Log.d(TAG, "Context is not an Activity instance, type: " + context.getClass().getName());
            }
        }
        
        // 特殊处理：如果仍然无法获取Activity，尝试通过反射或其他方式获取
        if (this.activity == null) {
            Log.w(TAG, "Warning: Could not get Activity from standard methods. Live2D may not work correctly.");
            // 在这种情况下，我们仍然尝试使用context，尽管可能会有一些限制
        }

        // 使用可用的context创建GLSurfaceView
        glSurfaceView = new GLSurfaceView(context);
        glSurfaceView.setEGLContextClientVersion(2);

        // 创建渲染器
        glRenderer = new GLRenderer();
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // 初始化Live2D（如果有activity的话）
        if (this.activity != null) {
            appDelegate.onStart(this.activity);
            Log.d(TAG, "Live2D initialized with Activity context");
        } else {
            Log.w(TAG, "Live2D initialized without Activity context. Some features may not work.");
        }
        
        // 设置触摸事件处理
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final float pointX = event.getX();
                final float pointY = event.getY();
                Log.d(TAG, "Touch event received: action=" + event.getAction() + ", x=" + pointX + ", y=" + pointY);

                // 将触摸事件添加到GL线程队列中
                glSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                Log.d(TAG, "Processing ACTION_DOWN event");
                                LAppDelegate.getInstance().onTouchBegan(pointX, pointY);
                                break;
                            case MotionEvent.ACTION_UP:
                                Log.d(TAG, "Processing ACTION_UP event");
                                LAppDelegate.getInstance().onTouchEnd(pointX, pointY);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                Log.d(TAG, "Processing ACTION_MOVE event");
                                LAppDelegate.getInstance().onTouchMoved(pointX, pointY);
                                break;
                        }
                    }
                });
                return true;
            }
        });
        Log.d(TAG, "Live2DPlatformView construction completed");
    }

    @NonNull
    @Override
    public View getView() {
        Log.d(TAG, "getView() called");
        return glSurfaceView;
    }

    @Override
    public void dispose() {
        Log.d(TAG, "dispose() called");
        // 清理资源
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
            Log.d(TAG, "GLSurfaceView paused");
        }
        if (LAppDelegate.getInstance() != null) {
            LAppDelegate.getInstance().onPause();
            LAppDelegate.getInstance().onStop();
            Log.d(TAG, "LAppDelegate paused and stopped");
        }
        Log.d(TAG, "dispose() completed");
    }
}