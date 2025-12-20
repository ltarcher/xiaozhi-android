/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "GLRenderer";
    private String instanceId;
    
    public GLRenderer() {
        this(null);
    }
    
    public GLRenderer(String instanceId) {
        this.instanceId = instanceId;
        Log.d(TAG, "GLRenderer created with instanceId: " + instanceId);
    }
    
    /**
     * 设置实例ID
     * @param instanceId 实例ID
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
        Log.d(TAG, "GLRenderer instanceId set to: " + instanceId);
    }
    
    /**
     * 获取实例ID
     * @return 实例ID
     */
    public String getInstanceId() {
        return instanceId;
    }
    
    /**
     * 获取指定实例的LAppDelegate
     * @return LAppDelegate实例
     */
    private LAppDelegate getAppDelegate() {
        if (instanceId != null && !instanceId.isEmpty()) {
            LAppDelegate delegate = LAppDelegate.getInstance(instanceId);
            if (delegate != null) {
                return delegate;
            }
        }
        // 回退到默认实例
        return LAppDelegate.getInstance();
    }
    
    // Called at initialization (when the drawing context is lost and recreated).
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated called for instance: " + instanceId);
        getAppDelegate().onSurfaceCreated();
    }

    // Mainly called when switching between landscape and portrait.
    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.d(TAG, "onSurfaceChanged called for instance: " + instanceId + ", width=" + width + ", height=" + height);
        getAppDelegate().onSurfaceChanged(width, height);
    }

    // Called repeatedly for drawing.
    @Override
    public void onDrawFrame(GL10 unused) {
        try {
            getAppDelegate().run();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDrawFrame for instance: " + instanceId, e);
        }
    }
}
