/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.content.Context;
import android.opengl.GLES20;
import com.live2d.sdk.cubism.framework.CubismFramework;

import static android.opengl.GLES20.*;

public class LAppDelegate {
    public static LAppDelegate getInstance() {
        if (s_instance == null) {
            s_instance = new LAppDelegate();
        }
        return s_instance;
    }

    /**
     * 释放类的实例（单例）。
     */
    public static void releaseInstance() {
        if (s_instance != null) {
            s_instance = null;
        }
    }

    public void onStart(Context context) {
        LAppPal.printLog("LAppDelegate onStart 开始执行");
        this.context = context;
        textureManager = new LAppTextureManager(context);
        LAppPal.printLog("纹理管理器初始化完成");
        view = new LAppView(context);
        LAppPal.printLog("视图初始化完成");

        LAppPal.updateTime();
        LAppPal.printLog("LAppDelegate onStart 执行完成");
    }

    public void onPause() {
        // 暂停应用
    }

    public void onStop() {
        if (view != null) {
            view.close();
        }
        textureManager = null;

        LAppLive2DManager.releaseInstance();
        CubismFramework.dispose();
    }

    public void onDestroy() {
        releaseInstance();
    }

    public void onSurfaceCreated() {
        LAppPal.printLog("LAppDelegate onSurfaceCreated 开始执行");
        // 纹理采样设置
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        // 透明设置
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        // Initialize Cubism SDK framework
        CubismFramework.initialize();
        LAppPal.printLog("Cubism SDK框架初始化完成");
        LAppPal.printLog("LAppDelegate onSurfaceCreated 执行完成");
    }

    public void onSurfaceChanged(int width, int height) {
        LAppPal.printLog("LAppDelegate onSurfaceChanged 开始执行, 宽度: " + width + ", 高度: " + height);
        // 指定绘制范围
        GLES20.glViewport(0, 0, width, height);
        windowWidth = width;
        windowHeight = height;

        // 初始化AppView
        view.initialize();
        LAppPal.printLog("视图初始化完成");
        view.initializeSprite();
        LAppPal.printLog("精灵初始化完成");

        // 加载模型
        LAppPal.printLog("准备加载模型");
        LAppLive2DManager manager = LAppLive2DManager.getInstance();
        if (manager.getModel() == null) {
            LAppPal.printLog("当前没有模型，开始加载Haru模型");
            manager.loadModel("Haru");
            LAppPal.printLog("Haru模型加载完成");
        } else {
            LAppPal.printLog("已有模型，无需重复加载");
        }

        isActive = true;
        LAppPal.printLog("LAppDelegate onSurfaceChanged 执行完成");
    }

    public void run() {
        // 更新时间
        LAppPal.updateTime();

        // 画面初始化
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearDepthf(1.0f);

        if (view != null) {
            view.render();
        }
    }

    public void onTouchBegan(float x, float y) {
        mouseX = x;
        mouseY = y;

        if (view != null) {
            isCaptured = true;
            view.onTouchesBegan(mouseX, mouseY);
        }
    }

    public void onTouchEnd(float x, float y) {
        mouseX = x;
        mouseY = y;

        if (view != null) {
            isCaptured = false;
            view.onTouchesEnded(mouseX, mouseY);
        }
    }

    public void onTouchMoved(float x, float y) {
        mouseX = x;
        mouseY = y;

        if (isCaptured && view != null) {
            view.onTouchesMoved(mouseX, mouseY);
        }
    }

    // getter, setter群
    public Context getContext() {
        return context;
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
        // 设置 Cubism SDK framework
        cubismOption.logFunction = new LAppPal.PrintLogFunction();
        cubismOption.loggingLevel = LAppDefine.cubismLoggingLevel;

        CubismFramework.cleanUp();
        CubismFramework.startUp(cubismOption);
    }

    private Context context;

    private final CubismFramework.Option cubismOption = new CubismFramework.Option();

    private LAppTextureManager textureManager;
    private LAppView view;
    private int windowWidth;
    private int windowHeight;
    private boolean isActive = true;

    /**
     * 是否点击
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