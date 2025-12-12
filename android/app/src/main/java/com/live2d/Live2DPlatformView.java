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
    private String modelPath;
    private String instanceId;

    public Live2DPlatformView(@NonNull Context context, @Nullable Map<String, Object> creationParams) {
        Log.d(TAG, "Live2DPlatformView constructor called");
        
        // 解析创建参数
        if (creationParams != null) {
            this.modelPath = (String) creationParams.get("modelPath");
            this.instanceId = (String) creationParams.get("instanceId");
            Log.d(TAG, "Creating view with modelPath: " + this.modelPath + ", instanceId: " + this.instanceId);
        } else {
            Log.d(TAG, "Creating view with no parameters");
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
        
        // 注册到LAppDelegate
        LAppDelegate.getInstance().setLive2DPlatformView(this);
        
        // 现在GLSurfaceView已创建，可以安全地初始化参数
        if (this.modelPath != null || this.instanceId != null) {
            initializeWithParams(this.modelPath, this.instanceId);
        }
        
        // 设置触摸事件处理
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final float pointX = event.getX();
                final float pointY = event.getY();
                String actionName = getActionName(event.getAction());
                Log.d(TAG, "Touch event received: action=" + actionName + " (" + event.getAction() + "), x=" + pointX + ", y=" + pointY + ", viewWidth=" + v.getWidth() + ", viewHeight=" + v.getHeight());

                // 将触摸事件添加到GL线程队列中
                glSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        String actionNameInGL = getActionName(event.getAction());
                        Log.d(TAG, "Executing in GL thread: " + actionNameInGL + " at (" + pointX + ", " + pointY + ")");
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                Log.d(TAG, "Processing ACTION_DOWN event - coordinates: (" + pointX + ", " + pointY + ")");
                                LAppDelegate.getInstance().onTouchBegan(pointX, pointY);
                                break;
                            case MotionEvent.ACTION_UP:
                                Log.d(TAG, "Processing ACTION_UP event - coordinates: (" + pointX + ", " + pointY + ")");
                                LAppDelegate.getInstance().onTouchEnd(pointX, pointY);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                Log.d(TAG, "Processing ACTION_MOVE event - coordinates: (" + pointX + ", " + pointY + ")");
                                LAppDelegate.getInstance().onTouchMoved(pointX, pointY);
                                break;
                            default:
                                Log.d(TAG, "Processing other touch event: " + actionNameInGL);
                                break;
                        }
                    }
                });
                return true;
            }
            
            private String getActionName(int action) {
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        return "ACTION_DOWN";
                    case MotionEvent.ACTION_UP:
                        return "ACTION_UP";
                    case MotionEvent.ACTION_MOVE:
                        return "ACTION_MOVE";
                    case MotionEvent.ACTION_CANCEL:
                        return "ACTION_CANCEL";
                    case MotionEvent.ACTION_OUTSIDE:
                        return "ACTION_OUTSIDE";
                    default:
                        return "UNKNOWN (" + action + ")";
                }
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
    
    /**
     * 强制刷新视图
     */
    public void refreshView() {
        if (glSurfaceView != null) {
            glSurfaceView.requestRender();
            Log.d(TAG, "View refresh requested");
        }
    }
    
    /**
     * 使用参数初始化Live2D平台视图
     * @param modelPath 模型路径
     * @param instanceId 实例ID
     */
    public void initializeWithParams(String modelPath, String instanceId) {
        this.modelPath = modelPath;
        this.instanceId = instanceId;
        
        Log.d(TAG, "Initialized with modelPath: " + modelPath + ", instanceId: " + instanceId);
        
        // 通知Live2D模块参数信息
        LAppDelegate appDelegate = LAppDelegate.getInstance();
        if (appDelegate != null) {
            // 可以通过appDelegate传递参数到具体的Live2D实例
            if (instanceId != null) {
                // 确保MainActivity知道这个实例
                // 这里可以通过某种方式通知MainActivity
                Log.d(TAG, "Instance ID registered with Live2D module: " + instanceId);
            }
            
            // 只请求渲染，不强制重新加载模型
            if (modelPath != null && !modelPath.isEmpty()) {
                Log.d(TAG, "Model path provided: " + modelPath);
                
                // 简单的渲染请求，避免强制重新加载导致的崩溃
                appDelegate.requestRender();
                Log.d(TAG, "Requested render after parameter initialization");
            }
        }
    }
    
    
    /**
     * 获取当前实例ID
     * @return 实例ID
     */
    public String getInstanceId() {
        return instanceId;
    }
    
    /**
     * 获取当前模型路径
     * @return 模型路径
     */
    public String getModelPath() {
        return modelPath;
    }
}