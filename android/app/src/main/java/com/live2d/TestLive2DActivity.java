package com.live2d;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;

/**
 * 测试Live2D模型加载的Activity
 */
public class TestLive2DActivity extends Activity {
    private static final String TAG = "TestLive2DActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: TestLive2DActivity started");

        // 初始化GLSurfaceView
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);

        // 创建渲染器
        glRenderer = new GLRenderer();
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        setContentView(glSurfaceView);

        // 设置全屏沉浸式模式
        setupFullScreenMode();

        // 显示提示信息
        Toast.makeText(this, "正在加载Live2D模型...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onCreate: GLSurfaceView initialized");
    }

    private void setupFullScreenMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            getWindow().getInsetsController().hide(WindowInsets.Type.navigationBars() | WindowInsets.Type.statusBars());
            getWindow().getInsetsController().setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
        Log.d(TAG, "setupFullScreenMode: Full screen mode set");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Activity starting");
        LAppDelegate.getInstance().onStart(this);
        Log.d(TAG, "onStart: LAppDelegate started");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity resuming");
        glSurfaceView.onResume();

        View decor = this.getWindow().getDecorView();
        decor.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        Log.d(TAG, "onResume: Activity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity pausing");
        glSurfaceView.onPause();
        LAppDelegate.getInstance().onPause();
        Log.d(TAG, "onPause: Activity paused");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Activity stopping");
        LAppDelegate.getInstance().onStop();
        Log.d(TAG, "onStop: Activity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity destroying");
        LAppDelegate.getInstance().onDestroy();
        Log.d(TAG, "onDestroy: Activity destroyed");
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final float pointX = event.getX();
        final float pointY = event.getY();
        Log.d(TAG, "onTouchEvent: Action=" + event.getAction() + ", x=" + pointX + ", y=" + pointY);

        // 将触摸事件添加到GL线程队列中
        glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "onTouchEvent: ACTION_DOWN");
                        LAppDelegate.getInstance().onTouchBegan(pointX, pointY);
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "onTouchEvent: ACTION_UP");
                        LAppDelegate.getInstance().onTouchEnd(pointX, pointY);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d(TAG, "onTouchEvent: ACTION_MOVE");
                        LAppDelegate.getInstance().onTouchMoved(pointX, pointY);
                        break;
                }
            }
        });
        return super.onTouchEvent(event);
    }

    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
}