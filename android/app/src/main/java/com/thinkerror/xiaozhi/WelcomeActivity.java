package com.thinkerror.xiaozhi;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import com.live2d.LAppDelegate;
import com.live2d.GLRenderer;
import com.live2d.LAppLive2DManager;

public class WelcomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化OpenGL ES环境
        initOpenGL();

        // 创建一个容器布局
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.addView(glSurfaceView);

        // 创建进入主应用按钮
        Button enterButton = new Button(this);
        enterButton.setText("Enter App");
        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMainActivity();
            }
        });

        // 设置按钮布局参数
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.setMargins(0, 0, 32, 32); // 右下角边距
        buttonParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
        enterButton.setLayoutParams(buttonParams);

        frameLayout.addView(enterButton);
        setContentView(frameLayout);

        // 隐藏系统UI以获得更好的视觉效果
        hideSystemUI();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 初始化Live2D
        LAppDelegate.getInstance().onStart(this);
        
        // 设置模型
        LAppLive2DManager.getInstance().setUpModel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        glSurfaceView.onResume();

        // 恢复隐藏系统UI
        hideSystemUI();
    }

    @Override
    protected void onPause() {
        super.onPause();

        glSurfaceView.onPause();
        LAppDelegate.getInstance().onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LAppDelegate.getInstance().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LAppDelegate.getInstance().onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float pointX = event.getX();
        float pointY = event.getY();

        // 将触摸事件添加到GLSurfaceView的事件队列中
        glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        LAppDelegate.getInstance().onTouchBegan(pointX, pointY);
                        break;
                    case MotionEvent.ACTION_UP:
                        LAppDelegate.getInstance().onTouchEnd(pointX, pointY);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        LAppDelegate.getInstance().onTouchMoved(pointX, pointY);
                        break;
                }
            }
        });
        return super.onTouchEvent(event);
    }

    private void initOpenGL() {
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // 使用OpenGL ES 2.0

        glRenderer = new GLRenderer();

        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private void hideSystemUI() {
        // 隐藏系统UI以获得沉浸式体验
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // 关闭当前Activity
    }

    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
}