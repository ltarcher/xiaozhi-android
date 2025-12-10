package com.thinkerror.xiaozhi;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.live2d.LAppDelegate;
import com.live2d.GLRenderer;

/**
 * Live2D待机页面Activity
 * 用于显示Live2D模型的启动页面
 */
public class Live2dSplashActivity extends Activity {
    private static final String TAG = "Live2dSplashActivity";
    
    // GLSurfaceView用于渲染Live2D模型
    private GLSurfaceView glSurfaceView;
    
    // Live2D应用委托类
    private LAppDelegate live2DDelegate;
    
    // 渲染器
    private GLRenderer glRenderer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "Live2dSplashActivity onCreate");
        
        // 初始化Live2D系统
        initLive2D();
        
        // 初始化GLSurfaceView
        initGLSurfaceView();
        
        // 设置内容视图
        setContentView(glSurfaceView);
    }
    
    /**
     * 初始化Live2D系统
     */
    private void initLive2D() {
        Log.d(TAG, "Initializing Live2D system");
        
        // 获取LAppDelegate单例实例
        live2DDelegate = LAppDelegate.getInstance();
        
        // 在Activity启动时初始化
        live2DDelegate.onStart(this);
    }
    
    /**
     * 初始化GLSurfaceView
     */
    private void initGLSurfaceView() {
        Log.d(TAG, "Initializing GLSurfaceView");
        
        // 创建GLSurfaceView
        glSurfaceView = new GLSurfaceView(this);
        
        // 设置OpenGL ES版本为2.0
        glSurfaceView.setEGLContextClientVersion(2);
        
        // 创建渲染器
        glRenderer = new GLRenderer(live2DDelegate);
        
        // 设置渲染器
        glSurfaceView.setRenderer(glRenderer);
        
        // 设置渲染模式为连续渲染
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "Live2dSplashActivity onStart");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Live2dSplashActivity onResume");
        
        // 恢复GLSurfaceView
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Live2dSplashActivity onPause");
        
        // 暂停GLSurfaceView
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
        
        // 调用Live2D代理的暂停方法
        if (live2DDelegate != null) {
            live2DDelegate.onPause();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Live2dSplashActivity onStop");
        
        // 调用Live2D代理的停止方法
        if (live2DDelegate != null) {
            live2DDelegate.onStop();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Live2dSplashActivity onDestroy");
        
        // 调用Live2D代理的销毁方法
        if (live2DDelegate != null) {
            live2DDelegate.onDestroy();
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 将触摸事件传递给Live2D系统
        if (live2DDelegate != null) {
            float x = event.getX();
            float y = event.getY();
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    live2DDelegate.onTouchBegan(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    live2DDelegate.onTouchEnd(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    live2DDelegate.onTouchMoved(x, y);
                    break;
            }
        }
        
        return super.onTouchEvent(event);
    }
}