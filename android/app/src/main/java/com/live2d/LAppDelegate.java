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

import static android.opengl.GLES20.*;

public class LAppDelegate {
    private static final String TAG = "LAppDelegate";
    
    public static LAppDelegate getInstance() {
        if (s_instance == null) {
            Log.d(TAG, "getInstance: Creating new LAppDelegate instance");
            s_instance = new LAppDelegate();
        } else {
            //Log.d(TAG, "getInstance: Returning existing LAppDelegate instance");
        }
        return s_instance;
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
        
        textureManager = new LAppTextureManager();
        Log.d(TAG, "onStart: Created LAppTextureManager");
        view = new LAppView();
        Log.d(TAG, "onStart: Created LAppView");

        LAppPal.updateTime();
        Log.d(TAG, "onStart: Updated time");
    }

    public void onPause() {
        Log.d(TAG, "onPause: Pausing LAppDelegate");
        currentModel = LAppLive2DManager.getInstance().getCurrentModel();
    }

    public void onStop() {
        Log.d(TAG, "onStop: Stopping LAppDelegate");
        if (view != null) {
            view.close();
        }
        textureManager = null;

        LAppLive2DManager.releaseInstance();
        CubismFramework.dispose();
        Log.d(TAG, "onStop: Released resources");
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy: Destroying LAppDelegate");
        releaseInstance();
    }

    public void onSurfaceCreated() {
        Log.d(TAG, "onSurfaceCreated: Surface created");
        // テクスチャサンプリング設定
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        // 透過設定
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        // Initialize Cubism SDK framework
        CubismFramework.initialize();
        Log.d(TAG, "onSurfaceCreated: CubismFramework initialized");
    }

    public void onSurfaceChanged(int width, int height) {
        Log.d(TAG, "onSurfaceChanged: width=" + width + ", height=" + height);
        // 描画範囲指定
        GLES20.glViewport(0, 0, width, height);
        windowWidth = width;
        windowHeight = height;

        // AppViewの初期化
        view.initialize();
        Log.d(TAG, "onSurfaceChanged: View initialized");
        view.initializeSprite();
        Log.d(TAG, "onSurfaceChanged: Sprites initialized");

        // load models
        if (LAppLive2DManager.getInstance().getCurrentModel() != currentModel) {
            LAppLive2DManager.getInstance().changeScene(currentModel);
        }

        isActive = true;
        Log.d(TAG, "onSurfaceChanged: Surface changed completed");
    }

    public void run() {
        // 時間更新
        LAppPal.updateTime();

        // 画面初期化
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearDepthf(1.0f);

        if (view != null) {
            view.render();
        }

        // アプリケーションを非アクティブにする
        if (!isActive) {
            if (activity != null) {
                activity.finishAndRemoveTask();
            }
        }
    }


    public void onTouchBegan(float x, float y) {
        Log.d(TAG, "onTouchBegan: x=" + x + ", y=" + y);
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
        Log.d(TAG, "onTouchEnd: x=" + x + ", y=" + y);
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
    }

    public void onTouchMoved(float x, float y) {
        Log.d(TAG, "onTouchMoved: x=" + x + ", y=" + y);
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
    }

    /**
     * 请求重新渲染
     */
    public void requestRender() {
        Log.d(TAG, "requestRender: Render requested");
        // 由于run()方法会在每一帧被调用，我们只需要确保它会被调用即可
        // 这里不需要额外的操作
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

    private LAppDelegate() {
        Log.d(TAG, "LAppDelegate constructor called");
        currentModel = 0;

        // Set up Cubism SDK framework.
        cubismOption.logFunction = new LAppPal.PrintLogFunction();
        cubismOption.loggingLevel = LAppDefine.cubismLoggingLevel;

        CubismFramework.cleanUp();
        CubismFramework.startUp(cubismOption);
        Log.d(TAG, "LAppDelegate constructor: CubismFramework started up");
    }

    private Activity activity;

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
     * 模型シーン索引
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