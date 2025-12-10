package com.thinkerror.xiaozhi;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import com.live2d.LAppDelegate;
import com.live2d.GLRenderer;
import com.live2d.LAppLive2DManager;
import com.live2d.LAppPal;

import java.io.IOException;

public class WelcomeActivity extends Activity {
    private static final String TAG = "WelcomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");

        // 打印资源目录列表
        printAssetsDirectoryList();

        // 初始化OpenGL ES环境
        Log.d(TAG, "开始初始化OpenGL环境");
        initOpenGL();
        Log.d(TAG, "OpenGL环境初始化完成");

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
        Log.d(TAG, "onCreate() 执行完成");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");

        // 初始化Live2D
        Log.d(TAG, "开始初始化Live2D");
        LAppDelegate.getInstance().onStart(this);
        Log.d(TAG, "Live2D初始化完成");
        
        // 设置模型
        Log.d(TAG, "开始设置模型");
        LAppLive2DManager.getInstance().setUpModel();
        Log.d(TAG, "模型设置完成");
        
        // 立即加载Haru模型
        Log.d(TAG, "开始加载Haru模型");
        LAppLive2DManager.getInstance().loadModel("Haru");
        Log.d(TAG, "Haru模型加载完成");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");

        glSurfaceView.onResume();

        // 恢复隐藏系统UI
        hideSystemUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called");

        glSurfaceView.onPause();
        LAppDelegate.getInstance().onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
        LAppDelegate.getInstance().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");

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
        Log.d(TAG, "initOpenGL() called");
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // 使用OpenGL ES 2.0

        glRenderer = GLRenderer.getInstance();

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
        Log.d(TAG, "startMainActivity() called");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // 关闭当前Activity
    }

    /**
     * 打印assets目录下的所有文件和文件夹列表
     */
    private void printAssetsDirectoryList() {
        Log.d(TAG, "=== Assets Directory List ===");
        try {
            Log.d(TAG, "Root assets:");
            String[] rootAssets = getAssets().list("");
            if (rootAssets != null) {
                for (String asset : rootAssets) {
                    Log.d(TAG, "  /" + asset);
                }
            }
            
            Log.d(TAG, "Flutter assets:");
            try {
                String[] flutterAssets = getAssets().list("/flutter_assets");
                if (flutterAssets != null) {
                    for (String asset : flutterAssets) {
                        Log.d(TAG, "  /flutter_assets/" + asset);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error listing /flutter_assets", e);
            }
            
            Log.d(TAG, "Live2D assets:");
            try {
                String[] live2dAssets = getAssets().list("live2d");
                if (live2dAssets != null) {
                    Log.d(TAG, "Found live2d directory with " + live2dAssets.length + " items");
                    for (String asset : live2dAssets) {
                        Log.d(TAG, "  live2d/" + asset);
                        
                        // Check if it's a directory by trying to list its contents
                        try {
                            String[] subAssets = getAssets().list("live2d/" + asset);
                            if (subAssets != null && subAssets.length > 0) {
                                Log.d(TAG, "    (directory with " + subAssets.length + " items)");
                                // Print first few items for verification
                                for (int i = 0; i < Math.min(3, subAssets.length); i++) {
                                    Log.d(TAG, "      live2d/" + asset + "/" + subAssets[i]);
                                }
                                if (subAssets.length > 3) {
                                    Log.d(TAG, "      ... and " + (subAssets.length - 3) + " more items");
                                }
                                
                                // Check if model file exists
                                String modelFile = asset + ".model3.json";
                                boolean foundModel = false;
                                for (String file : subAssets) {
                                    if (file.equals(modelFile)) {
                                        foundModel = true;
                                        break;
                                    }
                                }
                                if (foundModel) {
                                    Log.d(TAG, "      ✓ 包含模型文件: " + modelFile);
                                } else {
                                    Log.d(TAG, "      ✗ 缺少模型文件: " + modelFile);
                                }
                            } else {
                                Log.d(TAG, "    (file or empty directory)");
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "    (likely a file)");
                        }
                    }
                } else {
                    Log.d(TAG, "live2d directory not found");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error listing live2d assets", e);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to list assets", e);
        }
        Log.d(TAG, "=== End of Assets Directory List ===");
    }

    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
}