/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.*;

public class GLRenderer implements GLSurfaceView.Renderer {
    /**
     * 自身实例
     */
    private static GLRenderer instance;

    public static GLRenderer getInstance() {
        if (instance == null) {
            instance = new GLRenderer();
        }
        return instance;
    }

    /**
     * 构造函数
     */
    private GLRenderer() {}

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        LAppPal.printLog("GLRenderer onSurfaceCreated 开始执行");
        // 背景设置为白色
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        LAppDelegate.getInstance().onSurfaceCreated();
        LAppPal.printLog("GLRenderer onSurfaceCreated 执行完成");
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        LAppPal.printLog("GLRenderer onSurfaceChanged 开始执行, 宽度: " + width + ", 高度: " + height);
        LAppDelegate.getInstance().onSurfaceChanged(width, height);
        LAppPal.printLog("GLRenderer onSurfaceChanged 执行完成");
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        LAppPal.printLog("GLRenderer onDrawFrame 开始执行");
        LAppDelegate.getInstance().run();
        LAppPal.printLog("GLRenderer onDrawFrame 执行完成");
    }
}