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
    private int frameCount = 0;
    
    // Called at initialization (when the drawing context is lost and recreated).
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated: OpenGL surface created, config=" + config);
        
        if (gl != null) {
            Log.d(TAG, "onSurfaceCreated: OpenGL context information:");
            Log.d(TAG, "  Vendor: " + gl.glGetString(GL10.GL_VENDOR));
            Log.d(TAG, "  Renderer: " + gl.glGetString(GL10.GL_RENDERER));
            Log.d(TAG, "  Version: " + gl.glGetString(GL10.GL_VERSION));
        }
        
        LAppDelegate.getInstance().onSurfaceCreated();
        Log.d(TAG, "onSurfaceCreated: LAppDelegate.onSurfaceCreated() completed");
    }

    // Mainly called when switching between landscape and portrait.
    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.d(TAG, "onSurfaceChanged: Surface size changed to " + width + "x" + height);
        LAppDelegate.getInstance().onSurfaceChanged(width, height);
        Log.d(TAG, "onSurfaceChanged: LAppDelegate.onSurfaceChanged() completed");
    }

    // Called repeatedly for drawing.
    @Override
    public void onDrawFrame(GL10 unused) {
        frameCount++;
        
        // 每60帧输出一次调试信息
        if (frameCount % 60 == 0) {
            Log.d(TAG, "onDrawFrame: Rendering frame #" + frameCount);
        }
        
        try {
            LAppDelegate appDelegate = LAppDelegate.getInstance();
            if (appDelegate != null) {
                if (frameCount % 60 == 0) {
                    Log.d(TAG, "onDrawFrame: Calling LAppDelegate.run()");
                }
                appDelegate.run();
                if (frameCount % 60 == 0) {
                    Log.d(TAG, "onDrawFrame: LAppDelegate.run() completed");
                }
            } else {
                Log.e(TAG, "onDrawFrame: LAppDelegate.getInstance() returned null");
            }
        } catch (Exception e) {
            Log.e(TAG, "onDrawFrame: Error during LAppDelegate.run(): " + e.getMessage(), e);
        } catch (Error e) {
            Log.e(TAG, "onDrawFrame: Error during LAppDelegate.run(): " + e.getMessage(), e);
        }
    }
}
