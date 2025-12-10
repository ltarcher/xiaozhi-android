package com.live2d;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.flutter.plugin.platform.PlatformView;

public class Live2DPlatformView implements PlatformView {
    private final GLSurfaceView glSurfaceView;
    private final GLRenderer glRenderer;
    private final Context context;

    public Live2DPlatformView(@NonNull Context context, @Nullable Map<String, Object> creationParams) {
        this.context = context;
        
        glSurfaceView = new GLSurfaceView(context);
        glSurfaceView.setEGLContextClientVersion(2);

        // 创建渲染器
        glRenderer = new GLRenderer();
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // 初始化Live2D
        LAppDelegate.getInstance().onStart((Activity) context);
        
        // 设置触摸事件处理
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final float pointX = event.getX();
                final float pointY = event.getY();

                // 将触摸事件添加到GL线程队列中
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
                return true;
            }
        });
    }

    @NonNull
    @Override
    public View getView() {
        return glSurfaceView;
    }

    @Override
    public void dispose() {
        // 清理资源
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
        if (LAppDelegate.getInstance() != null) {
            LAppDelegate.getInstance().onPause();
            LAppDelegate.getInstance().onStop();
        }
    }
}