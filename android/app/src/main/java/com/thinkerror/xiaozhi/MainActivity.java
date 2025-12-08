package com.thinkerror.xiaozhi;

import android.os.Bundle;
import android.view.SurfaceView;
import android.opengl.GLSurfaceView;
import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import com.live2d.LAppDelegate;
import com.live2d.GLRenderer;

public class MainActivity extends FlutterActivity {
    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 首先初始化OpenGL ES支持
        initOpenGL();
        
        // 设置内容视图为GLSurfaceView
        setContentView(glSurfaceView);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // 然后初始化Live2D
        LAppDelegate.getInstance().onStart(this);
    }
    
    private void initOpenGL() {
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // 使用OpenGL ES 2.0
        
        glRenderer = new GLRenderer();
        glSurfaceView.setRenderer(glRenderer);
        
        // 设置渲染模式为持续渲染
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LAppDelegate.getInstance().onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LAppDelegate.getInstance().onStop();
    }
    
    // getter方法，供其他类访问GLSurfaceView和GLRenderer
    public GLSurfaceView getGLSurfaceView() {
        return glSurfaceView;
    }
    
    public GLRenderer getGLRenderer() {
        return glRenderer;
    }
}