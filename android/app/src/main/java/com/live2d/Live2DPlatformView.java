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
    private boolean surfaceInitialized = false;

    public Live2DPlatformView(@NonNull Context context, @Nullable Map<String, Object> creationParams) {
        Log.d(TAG, "Live2DPlatformView constructor called");
        
        // 解析创建参数
        if (creationParams != null) {
            this.modelPath = (String) creationParams.get("modelPath");
            Log.d(TAG, "Creating view with modelPath: " + this.modelPath);
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
     * 标记surface已初始化，可以安全加载模型
     */
    public void setSurfaceInitialized() {
        Log.d(TAG, "setSurfaceInitialized: Surface is now initialized");
        surfaceInitialized = true;
        
        // 现在surface已初始化，可以安全地加载模型
        if (modelPath != null && !modelPath.isEmpty()) {
            Log.d(TAG, "setSurfaceInitialized: Loading model with path: " + modelPath);
            loadModelAfterSurfaceReady();
        }
    }
    
    /**
     * 在surface准备好后加载模型
     */
    private void loadModelAfterSurfaceReady() {
        // 在GL线程中确保模型加载
        glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                try {
                    // 确保Live2D管理器已初始化并至少有一个模型
                    LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
                    if (live2DManager.getModelNum() == 0) {
                        Log.d(TAG, "loadModelAfterSurfaceReady: No models loaded, loading first model");
                        live2DManager.nextScene();
                    }
                    
                    Log.d(TAG, "loadModelAfterSurfaceReady: Live2D models loaded, count: " + live2DManager.getModelNum());
                } catch (Exception e) {
                    Log.e(TAG, "loadModelAfterSurfaceReady: Error initializing Live2D models in GL thread", e);
                }
            }
        });
    }
    
    /**
     * 使用参数初始化Live2D平台视图
     * @param modelPath 模型路径
     */
    public void initializeWithParams(String modelPath) {
        this.modelPath = modelPath;
        
        Log.d(TAG, "initializeWithParams: Initialized with modelPath: " + modelPath);
        
        // 如果surface已经初始化，立即加载模型
        if (surfaceInitialized) {
            Log.d(TAG, "initializeWithParams: Surface already initialized, loading model now");
            loadModelAfterSurfaceReady();
        } else {
            Log.d(TAG, "initializeWithParams: Surface not yet initialized, will load after surface is ready");
        }
    }
    
    /**
     * 获取当前模型路径
     * @return 模型路径
     */
    public String getModelPath() {
        return modelPath;
    }
}