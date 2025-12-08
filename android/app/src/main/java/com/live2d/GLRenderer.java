/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {
    // 在初始化时调用（在绘制上下文丢失并重新创建时调用）。
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        LAppDelegate.getInstance().onSurfaceCreated();
    }

    // 主要在横竖屏切换时调用。
    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        LAppDelegate.getInstance().onSurfaceChanged(width, height);
    }

    // 重复调用进行绘制。
    @Override
    public void onDrawFrame(GL10 unused) {
        LAppDelegate.getInstance().run();
    }
}