package com.live2d;

import android.app.Activity;
import android.content.res.AssetManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;

import java.io.IOException;

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

        // 创建渲染器，测试活动使用默认实例
        glRenderer = new GLRenderer("test_activity");
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        setContentView(glSurfaceView);

        // 设置全屏沉浸式模式
        setupFullScreenMode();

        // 显示提示信息
        Toast.makeText(this, "正在加载Live2D模型...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onCreate: GLSurfaceView initialized");
        
        // 打印资源目录结构
        printAssetsStructure();
    }

    /**
     * 递归打印assets目录结构
     */
    private void printAssetsStructure() {
        Log.d(TAG, "=== 开始递归打印Assets目录结构 ===");
        try {
            AssetManager assetManager = getAssets();
            printAssetsRecursively(assetManager, "", "");
        } catch (Exception e) {
            Log.e(TAG, "打印Assets目录结构时出错: " + e.getMessage(), e);
        }
        Log.d(TAG, "=== 结束递归打印Assets目录结构 ===");
    }

    /**
     * 递归遍历并打印assets目录内容
     * @param assetManager AssetManager实例
     * @param path 当前路径
     * @param indent 缩进字符串
     */
    private void printAssetsRecursively(AssetManager assetManager, String path, String indent) {
        try {
            String[] list = assetManager.list(path);
            if (list != null) {
                for (String file : list) {
                    Log.d(TAG, indent + "|-- " + file);
                    // 尝试列出子目录内容以判断是文件还是目录
                    String subPath = path.isEmpty() ? file : path + "/" + file;
                    try {
                        String[] subList = assetManager.list(subPath);
                        if (subList != null && subList.length > 0) {
                            // 是目录，递归遍历
                            printAssetsRecursively(assetManager, subPath, indent + "    ");
                        }
                    } catch (Exception e) {
                        // 无法列出内容，可能是文件
                        Log.d(TAG, indent + "    |-- (file)");
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "无法列出路径内容: " + path, e);
        }
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
        LAppDelegate delegate = LAppDelegate.getInstance("test_activity");
        if (delegate == null) {
            delegate = LAppDelegate.getInstance();
        }
        delegate.onStart(this);
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
        LAppDelegate delegate = LAppDelegate.getInstance("test_activity");
        if (delegate == null) {
            delegate = LAppDelegate.getInstance();
        }
        delegate.onPause();
        Log.d(TAG, "onPause: Activity paused");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Activity stopping");
        LAppDelegate delegate = LAppDelegate.getInstance("test_activity");
        if (delegate == null) {
            delegate = LAppDelegate.getInstance();
        }
        delegate.onStop();
        Log.d(TAG, "onStop: Activity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity destroying");
        LAppDelegate delegate = LAppDelegate.getInstance("test_activity");
        if (delegate == null) {
            delegate = LAppDelegate.getInstance();
        }
        delegate.onDestroy();
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
                LAppDelegate delegate = LAppDelegate.getInstance("test_activity");
                if (delegate == null) {
                    delegate = LAppDelegate.getInstance();
                }
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "onTouchEvent: ACTION_DOWN");
                        delegate.onTouchBegan(pointX, pointY);
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "onTouchEvent: ACTION_UP");
                        delegate.onTouchEnd(pointX, pointY);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d(TAG, "onTouchEvent: ACTION_MOVE");
                        delegate.onTouchMoved(pointX, pointY);
                        break;
                }
            }
        });
        return super.onTouchEvent(event);
    }

    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
}