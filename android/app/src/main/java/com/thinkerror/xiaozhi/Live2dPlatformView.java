package com.thinkerror.xiaozhi;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.live2d.LAppDelegate;
import com.live2d.LAppLive2DManager;
import com.live2d.GLRenderer;

import java.util.Map;

import io.flutter.plugin.platform.PlatformView;

public class Live2dPlatformView implements PlatformView {
    private final GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
    private final Context context;

    public Live2dPlatformView(@NonNull Context context, int id, @Nullable Map<String, Object> creationParams) {
        this.context = context;
        
        // 初始化GLSurfaceView
        glSurfaceView = new GLSurfaceView(context);
        glSurfaceView.setEGLContextClientVersion(2);
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
    }
    
    @Override
    public void onFlutterViewAttached(@NonNull View flutterView) {
        // Flutter视图附加时的处理
        // 初始化渲染器
        glRenderer = new GLRenderer(LAppDelegate.getInstance());
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void onFlutterViewDetached() {
        // Flutter视图分离时的处理
    }

    @Override
    public void onInputConnectionLocked() {
        // 输入连接锁定时的处理
    }

    @Override
    public void onInputConnectionUnlocked() {
        // 输入连接解锁时的处理
    }
}