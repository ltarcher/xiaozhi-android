/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.app.Activity;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;
import com.live2d.LAppDefine;
import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.Live2DInstanceManager;
import com.live2d.Live2DInstanceManager.Live2DInstanceData;

import static android.opengl.GLES20.*;

public class LAppDelegate {
    private static final String TAG = "LAppDelegate";
    
    /**
     * 实例ID，用于多实例支持
     */
    private String instanceId;
    
    /**
     * 获取指定实例的LAppDelegate
     *
     * @param instanceId 实例ID，如果为null则返回默认实例
     * @return LAppDelegate实例
     */
    public static LAppDelegate getInstance(String instanceId) {
        //Log.d(TAG, "getInstance: Requested instance: " + instanceId);
        
        // 首先尝试从Live2DInstanceManager获取
        Live2DInstanceData instanceData =
            Live2DInstanceManager.getInstance().getInstance(instanceId);
            
        if (instanceData != null) {
            Log.d(TAG, "getInstance: Found instance in manager: " + instanceId);
            return instanceData.appDelegate;
        }
        
        // 如果没有指定ID或找不到实例，返回默认实例（向后兼容）
        if (instanceId == null) {
            Live2DInstanceData defaultData =
                    Live2DInstanceManager.getInstance().getInstance(
                        Live2DInstanceManager.getInstance().getDefaultInstanceId());
            if (defaultData != null) {
                Log.d(TAG, "getInstance: Returning default instance from manager");
                return defaultData.appDelegate;
            }
        }
        
        // 如果仍然找不到，创建一个新的单例（向后兼容）
        if (s_instance == null) {
            Log.d(TAG, "getInstance: Creating fallback singleton instance");
            s_instance = new LAppDelegate();
        }
        
        if (instanceId != null) {
            s_instance.setInstanceId(instanceId);
        }
        
        return s_instance;
    }
    
    /**
     * 获取默认实例（向后兼容）
     */
    public static LAppDelegate getInstance() {
        return getInstance(null);
    }
    
    /**
     * 释放类的实例（单例）。
     */
    public static void releaseInstance() {
        Log.d(TAG, "releaseInstance: Releasing LAppDelegate instance");
        if (s_instance != null) {
            s_instance = null;
        }
    }
    
    /**
     * 设置实例ID
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
        //Log.d(TAG, "setInstanceId: " + instanceId);
    }
    
    /**
     * 获取实例ID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 将应用程序设为非活动状态
     */
    public void deactivateApp() {
        Log.d(TAG, "deactivateApp: Deactivating application");
        isActive = false;
    }

    public void onStart(Activity activity) {
        Log.d(TAG, "onStart: Initializing LAppDelegate with activity: " + activity);
        // 更新Activity引用
        if (activity != null) {
            this.activity = activity;
            Log.d(TAG, "onStart: Activity reference updated");
        }
        
        textureManager = new LAppTextureManager(instanceId);
        Log.d(TAG, "onStart: Created LAppTextureManager for instance: " + instanceId);
        view = new LAppView(instanceId);
        Log.d(TAG, "onStart: Created LAppView");

        LAppPal.updateTime();
        Log.d(TAG, "onStart: Updated time");
    }

    public void onPause() {
        Log.d(TAG, "onPause: Pausing LAppDelegate for instance: " + instanceId);
        LAppLive2DManager manager = LAppLive2DManager.getInstance(instanceId);
        if (manager != null) {
            currentModel = manager.getCurrentModel();
        }
    }

    public void onStop() {
        Log.d(TAG, "onStop: Stopping LAppDelegate for instance: " + instanceId);
        if (view != null) {
            view.close();
        }
        textureManager = null;

        // 释放特定实例的管理器
        LAppLive2DManager.releaseInstance(instanceId);
        
        // 只在没有其他实例时才释放框架
        if (Live2DInstanceManager.getInstance().getInstanceCount() <= 1) {
            CubismFramework.dispose();
            Log.d(TAG, "onStop: Released CubismFramework - no more instances");
        }
        
        Log.d(TAG, "onStop: Released resources for instance: " + instanceId);
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy: Destroying LAppDelegate");
        releaseInstance();
    }

    public void onSurfaceCreated() {
        Log.d(TAG, "onSurfaceCreated: Surface created");
        // 纹理采样设置
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        // 透明度设置
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        // Initialize Cubism SDK framework - 只在第一个实例创建时初始化
        if (!CubismFramework.isInitialized()) {
            CubismFramework.initialize();
            Log.d(TAG, "onSurfaceCreated: CubismFramework initialized for the first time");
        } else {
            Log.d(TAG, "onSurfaceCreated: CubismFramework already initialized, skipping");
        }
    }

    public void onSurfaceChanged(int width, int height) {
        Log.d(TAG, "onSurfaceChanged: width=" + width + ", height=" + height);
        // 绘制范围指定
        GLES20.glViewport(0, 0, width, height);
        windowWidth = width;
        windowHeight = height;

        // AppView的初始化 - 添加空值检查
        if (view != null) {
            try {
                view.initialize();
                Log.d(TAG, "onSurfaceChanged: View initialized");
                view.initializeSprite();
                Log.d(TAG, "onSurfaceChanged: Sprites initialized");
            } catch (Exception e) {
                Log.e(TAG, "onSurfaceChanged: Error initializing view or sprites", e);
            }
        } else {
            Log.e(TAG, "onSurfaceChanged: View is null, cannot initialize");
        }

        // load models - 添加更安全的处理
        try {
            LAppLive2DManager manager = LAppLive2DManager.getInstance(instanceId);
            if (manager != null) {
                if (manager.getCurrentModel() != currentModel) {
                    manager.changeScene(currentModel);
                }
            } else {
                Log.e(TAG, "onSurfaceChanged: LAppLive2DManager is null for instance: " + instanceId);
            }
        } catch (Exception e) {
            Log.e(TAG, "onSurfaceChanged: Error loading models", e);
        }

        isActive = true;
        Log.d(TAG, "onSurfaceChanged: Surface changed completed");
    }

    public void run() {
        // 时间更新
        LAppPal.updateTime();

        // 画面初始化
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearDepthf(1.0f);

        if (view != null) {
            view.render();
        }

        // 使应用程序变为非活动状态
        if (!isActive) {
            if (activity != null) {
                activity.finishAndRemoveTask();
            }
        }

        // 处理模型更新 - 使用实例特定的管理器
        LAppLive2DManager live2DManager = LAppLive2DManager.getInstance(instanceId);
        if (live2DManager != null) {
            live2DManager.onUpdate();
        }
    }


    public void onTouchBegan(float x, float y) {
        Log.d(TAG, "onTouchBegan: x=" + x + ", y=" + y + " for instance: " + instanceId);
        mouseX = x;
        mouseY = y;

        // 添加额外的调试信息
        Log.d(TAG, "onTouchBegan: isCaptured=" + isCaptured + ", view=" + (view != null ? "not null" : "null"));

        if (view != null) {
            isCaptured = true;
            view.onTouchesBegan(mouseX, mouseY);
            Log.d(TAG, "onTouchBegan: Successfully called view.onTouchesBegan");
        } else {
            Log.e(TAG, "onTouchBegan: View is null, cannot process touch event");
        }
    }

    public void onTouchEnd(float x, float y) {
        Log.d(TAG, "onTouchEnd: x=" + x + ", y=" + y + " for instance: " + instanceId);
        mouseX = x;
        mouseY = y;

        // 添加额外的调试信息
        Log.d(TAG, "onTouchEnd: isCaptured=" + isCaptured + ", view=" + (view != null ? "not null" : "null"));

        if (view != null) {
            isCaptured = false;
            view.onTouchesEnded(mouseX, mouseY);
            Log.d(TAG, "onTouchEnd: Successfully called view.onTouchesEnded");
        } else {
            Log.e(TAG, "onTouchEnd: View is null, cannot process touch event");
        }

        // 处理点击事件 - 调用Live2DManager的onTap
        LAppLive2DManager live2DManager = LAppLive2DManager.getInstance(instanceId);
        if (live2DManager != null) {
            live2DManager.onTap(x, y);
        }
    }

    public void onTouchMoved(float x, float y) {
        Log.d(TAG, "onTouchMoved: x=" + x + ", y=" + y + " for instance: " + instanceId);
        mouseX = x;
        mouseY = y;

        // 添加额外的调试信息
        Log.d(TAG, "onTouchMoved: isCaptured=" + isCaptured + ", view=" + (view != null ? "not null" : "null"));

        if (isCaptured && view != null) {
            view.onTouchesMoved(mouseX, mouseY);
            Log.d(TAG, "onTouchMoved: Successfully called view.onTouchesMoved");
        } else {
            if (!isCaptured) {
                Log.w(TAG, "onTouchMoved: Not captured, ignoring touch move event");
            }
            if (view == null) {
                Log.e(TAG, "onTouchMoved: View is null, cannot process touch event");
            }
        }

        // 处理拖拽事件 - 调用Live2DManager的onDrag
        LAppLive2DManager live2DManager = LAppLive2DManager.getInstance(instanceId);
        if (live2DManager != null) {
            live2DManager.onDrag(x, y);
        }
    }

    /**
     * 请求重新渲染
     */
    public void requestRender() {
        Log.d(TAG, "requestRender: Render requested");
        // 通知Live2DPlatformView需要重新渲染
        if (live2DPlatformView != null) {
            live2DPlatformView.refreshView();
        }
    }

    /**
     * 设置Live2DPlatformView实例引用
     * @param platformView
     */
    public void setLive2DPlatformView(Live2DPlatformView platformView) {
        this.live2DPlatformView = platformView;
    }

    // getter, setter群
    public Activity getActivity() {
        Log.d(TAG, "getActivity: Returning " + (activity != null ? "valid activity" : "null activity"));
        return activity;
    }

    public LAppTextureManager getTextureManager() {
        return textureManager;
    }

    public LAppView getView() {
        return view;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    private static LAppDelegate s_instance;

    public LAppDelegate() {
        Log.d(TAG, "LAppDelegate constructor called");
        currentModel = 0;

        // Set up Cubism SDK framework.
        cubismOption.logFunction = new LAppPal.PrintLogFunction();
        cubismOption.loggingLevel = LAppDefine.cubismLoggingLevel;

        // 只在第一次创建时执行清理和启动
        if (!CubismFramework.isInitialized()) {
            CubismFramework.cleanUp();
            CubismFramework.startUp(cubismOption);
            Log.d(TAG, "LAppDelegate constructor: CubismFramework started up for the first time");
        } else {
            Log.d(TAG, "LAppDelegate constructor: CubismFramework already started up, skipping");
        }
    }

    /**
     * 带实例ID的构造函数
     *
     * @param instanceId 实例ID
     */
    public LAppDelegate(String instanceId) {
        this();
        this.instanceId = instanceId;
        Log.d(TAG, "LAppDelegate constructor with instanceId: " + instanceId);
    }

    private Activity activity;

    private Live2DPlatformView live2DPlatformView; // 添加对Live2DPlatformView的引用

    private final CubismFramework.Option cubismOption = new CubismFramework.Option();

    private LAppTextureManager textureManager;
    private LAppView view;
    private int windowWidth;
    private int windowHeight;
    private boolean isActive = true;

    // 添加初始化检查方法
    public boolean isInitialized() {
        boolean initialized = (view != null && textureManager != null && activity != null);
        Log.d(TAG, "isInitialized: view=" + (view != null) + 
              ", textureManager=" + (textureManager != null) + 
              ", activity=" + (activity != null) + 
              ", result=" + initialized);
        return initialized;
    }

    /**
     * 模型场景索引
     */
    private int currentModel;

    /**
     * 是否正在点击
     */
    private boolean isCaptured;

    /**
     * 鼠标的X坐标
     */
    private float mouseX;

    /**
     * 鼠标的Y坐标
     */
    private float mouseY;
}