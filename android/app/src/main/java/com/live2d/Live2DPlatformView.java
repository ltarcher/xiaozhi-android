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
    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
    private Activity activity;
    private boolean isInitialized = false;
    private String modelPath;
    private String instanceId;
    private boolean isRunning = false;

    public Live2DPlatformView(@NonNull Context context, @Nullable Map<String, Object> creationParams) {
        Log.d(TAG, "Live2DPlatformView constructor called");
        
        if (creationParams != null) {
            modelPath = (String) creationParams.get("modelPath");
            instanceId = (String) creationParams.get("instanceId");
            Log.d(TAG, "Creation params - modelPath: " + modelPath + ", instanceId: " + instanceId);
        }
        
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

        initializeGLSurfaceView(context);
        
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

    private void initializeGLSurfaceView(@NonNull Context context) {
        Log.d(TAG, "initializeGLSurfaceView called");
        // 创建或重新创建GLSurfaceView
        glSurfaceView = new GLSurfaceView(context);
        glSurfaceView.setEGLContextClientVersion(2);

        // 创建渲染器
        glRenderer = new GLRenderer();
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        
        isInitialized = true;
        isRunning = true;

        // 初始化Live2D（如果有activity的话）
        if (this.activity != null) {
            LAppDelegate.getInstance().onStart(this.activity);
            Log.d(TAG, "Live2D initialized with Activity context");
        } else {
            Log.w(TAG, "Live2D initialized without Activity context. Some features may not work.");
        }
        Log.d(TAG, "initializeGLSurfaceView completed");
    }

    public void onResume() {
        Log.d(TAG, "onResume called, isRunning: " + isRunning);
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
            Log.d(TAG, "GLSurfaceView resumed");
        }
        
        // 确保Live2D系统也恢复运行
        LAppDelegate appDelegate = LAppDelegate.getInstance();
        if (appDelegate != null) {
            Log.d(TAG, "LAppDelegate instance available");
            if (activity != null) {
                Log.d(TAG, "Activity available, calling onStart");
                appDelegate.onStart(activity);
            } else {
                // 如果没有activity，至少确保系统运行
                Activity currentActivity = appDelegate.getActivity();
                Log.d(TAG, "Current activity from appDelegate: " + (currentActivity != null ? "Available" : "Null"));
                if (currentActivity != null) {
                    Log.d(TAG, "Calling onStart with current activity");
                    appDelegate.onStart(currentActivity);
                } else {
                    Log.d(TAG, "No activity available for onStart");
                }
            }
            Log.d(TAG, "LAppDelegate restarted");
        } else {
            Log.d(TAG, "LAppDelegate instance is null");
        }
        isRunning = true;
        Log.d(TAG, "onResume completed");
    }

    public void onPause() {
        Log.d(TAG, "onPause called, isRunning: " + isRunning);
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
            Log.d(TAG, "GLSurfaceView paused");
        }
        
        if (LAppDelegate.getInstance() != null) {
            LAppDelegate.getInstance().onPause();
            Log.d(TAG, "LAppDelegate paused");
        }
        isRunning = false;
        Log.d(TAG, "onPause completed");
    }

    @NonNull
    @Override
    public View getView() {
        Log.d(TAG, "getView() called, isRunning: " + isRunning);
        return glSurfaceView;
    }

    @Override
    public void dispose() {
        Log.d(TAG, "dispose() called, isRunning: " + isRunning);
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
        isInitialized = false;
        isRunning = false;
        Log.d(TAG, "dispose() completed");
    }
}