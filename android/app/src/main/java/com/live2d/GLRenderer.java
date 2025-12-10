package com.live2d;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGL渲染器类
 * 实现GLSurfaceView.Renderer接口，负责OpenGL渲染相关操作
 */
public class GLRenderer implements GLSurfaceView.Renderer {
    // LAppDelegate引用
    private final LAppDelegate appDelegate;
    
    /**
     * 构造函数
     * @param appDelegate LAppDelegate实例
     */
    public GLRenderer(LAppDelegate appDelegate) {
        this.appDelegate = appDelegate;
    }
    
    /**
     * Surface创建时调用
     * @param gl GL10接口
     * @param config EGL配置
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("GLRenderer: onSurfaceCreated");
        }
        
        // 调用AppDelegate的对应方法
        appDelegate.onSurfaceCreated();
    }
    
    /**
     * Surface尺寸改变时调用
     * @param gl GL10接口
     * @param width 新宽度
     * @param height 新高度
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("GLRenderer: onSurfaceChanged(" + width + ", " + height + ")");
        }
        
        // 调用AppDelegate的对应方法
        appDelegate.onSurfaceChanged(width, height);
    }
    
    /**
     * 绘制每一帧时调用
     * @param gl GL10接口
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        // 调用AppDelegate的运行方法
        appDelegate.run();
    }
}