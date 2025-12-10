package com.live2d;

import android.app.Activity;
import android.content.res.AssetManager;
import android.opengl.GLES20;

import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.CubismFrameworkConfig;

import com.live2d.LAppPal; // 显式导入LAppPal类

/**
 * Live2D应用程序委托类
 * 作为整个Live2D系统的入口点和协调者
 */
public class LAppDelegate {
    // 单例实例
    private static LAppDelegate s_instance;
    
    /**
     * 获取单例实例
     * @return LAppDelegate实例
     */
    public static LAppDelegate getInstance() {
        if (s_instance == null) {
            s_instance = new LAppDelegate();
        }
        return s_instance;
    }
    
    /**
     * 释放单例实例
     */
    public static void releaseInstance() {
        if (s_instance != null) {
            s_instance = null;
        }
    }
    
    // Activity引用
    private Activity activity;
    
    // AssetManager引用
    private AssetManager assetManager;
    
    // 纹理管理器
    private LAppTextureManager textureManager;
    
    // 视图
    private LAppView view;
    
    // Live2D管理器
    private LAppLive2DManager live2DManager;
    
    // 窗口宽度和高度
    private int windowWidth;
    private int windowHeight;
    
    // 是否处于活动状态
    private boolean isActive = true;
    
    // 鼠标坐标
    private float mouseX;
    private float mouseY;
    
    // 是否被捕获（触摸中）
    private boolean isCaptured = false;
    
    // Cubism框架选项
    private final CubismFramework.Option cubismOption = new CubismFramework.Option();
    
    /**
     * 私有构造函数
     */
    private LAppDelegate() {
        // 设置日志级别
        cubismOption.loggingLevel = LAppDefine.cubismLoggingLevel;
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: Created");
        }
    }
    
    /**
     * Activity启动时调用
     * @param activity Activity实例
     */
    public void onStart(Activity activity) {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: onStart 被调用");
        }
        
        this.activity = activity;
        
        // 设置AssetManager
        assetManager = activity.getResources().getAssets();
        LAppPal.setAssetManager(assetManager);
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: AssetManager 已设置");
        }
        
        // 创建纹理管理器
        textureManager = new LAppTextureManager(assetManager);
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: 纹理管理器已创建");
        }
        
        // 创建视图
        view = new LAppView();
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: 视图已创建");
        }
        
        // 创建Live2D管理器
        live2DManager = LAppLive2DManager.getInstance();
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: Live2D管理器已创建");
        }
        
        // 初始化时间系统
        LAppPal.updateTime();
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: 时间系统已初始化");
        }
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: onStart 完成");
        }
    }
    
    /**
     * Activity暂停时调用
     */
    public void onPause() {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: onPause");
        }
    }
    
    /**
     * Activity停止时调用
     */
    public void onStop() {
        if (view != null) {
            // view.close();
        }
        textureManager = null;
        
        // 释放Live2D管理器
        LAppLive2DManager.releaseInstance();
        
        // 释放Cubism框架
        CubismFramework.dispose();
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: onStop");
        }
    }
    
    /**
     * Activity销毁时调用
     */
    public void onDestroy() {
        releaseInstance();
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: onDestroy");
        }
    }
    
    /**
     * Surface创建时调用
     */
    public void onSurfaceCreated() {
        // 设置纹理采样参数
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        
        // 启用透明度设置
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        
        // 清理并启动Cubism SDK框架
        CubismFramework.cleanUp();
        CubismFramework.startUp(cubismOption);
        
        // 初始化Cubism SDK框架
        CubismFramework.initialize();
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: onSurfaceCreated");
        }
    }
    
    /**
     * Surface尺寸改变时调用
     * @param width 新宽度
     * @param height 新高度
     */
    public void onSurfaceChanged(int width, int height) {
        // 设置视口
        GLES20.glViewport(0, 0, width, height);
        windowWidth = width;
        windowHeight = height;
        
        // 初始化视图
        view.initialize(width, height);
        
        // TODO: 初始化精灵等UI元素
        
        // 加载模型
        live2DManager.setupModels();
        if (live2DManager.getModelCount() > 0) {
            // 默认加载第一个模型
            live2DManager.changeScene(0);
        }
        
        isActive = true;
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: onSurfaceChanged(" + width + ", " + height + ")");
        }
    }
    
    /**
     * 主循环运行时调用
     */
    public void run() {
        // 更新时间
        LAppPal.updateTime();
        
        // 清屏
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearDepthf(1.0f);
        
        // 渲染
        if (view != null) {
            view.render();
        }
        
        // 更新和绘制模型
        if (live2DManager != null) {
            live2DManager.onUpdate();
            live2DManager.onDraw();
        }
        
        // 如果不处于活动状态，结束Activity
        if (!isActive) {
            if (activity != null) {
                activity.finish();
            }
        }
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            // LAppPal.printLog("LAppDelegate: run");  // 这个日志太频繁，一般不开启
        }
    }
    
    /**
     * 处理触摸开始事件
     * @param x 触摸点x坐标
     * @param y 触摸点y坐标
     */
    public void onTouchBegan(float x, float y) {
        mouseX = x;
        mouseY = y;
        
        if (view != null) {
            isCaptured = true;
            view.onTouchesBegan(mouseX, mouseY);
        }
        
        if (LAppDefine.DEBUG_TOUCH_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: onTouchBegan(" + x + ", " + y + ")");
        }
    }
    
    /**
     * 处理触摸结束事件
     * @param x 触摸点x坐标
     * @param y 触摸点y坐标
     */
    public void onTouchEnd(float x, float y) {
        mouseX = x;
        mouseY = y;
        
        if (view != null) {
            isCaptured = false;
            view.onTouchesEnded(mouseX, mouseY);
        }
        
        if (LAppDefine.DEBUG_TOUCH_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: onTouchEnd(" + x + ", " + y + ")");
        }
    }
    
    /**
     * 处理触摸移动事件
     * @param x 触摸点x坐标
     * @param y 触摸点y坐标
     */
    public void onTouchMoved(float x, float y) {
        mouseX = x;
        mouseY = y;
        
        if (isCaptured && view != null) {
            view.onTouchesMoved(mouseX, mouseY);
        }
        
        if (LAppDefine.DEBUG_TOUCH_LOG_ENABLE) {
            LAppPal.printLog("LAppDelegate: onTouchMoved(" + x + ", " + y + ")");
        }
    }
    
    /**
     * 获取Activity
     * @return Activity实例
     */
    public Activity getActivity() {
        return activity;
    }
    
    /**
     * 获取AssetManager
     * @return AssetManager实例
     */
    public AssetManager getAssetManager() {
        return assetManager;
    }
    
    /**
     * 获取纹理管理器
     * @return 纹理管理器实例
     */
    public LAppTextureManager getTextureManager() {
        return textureManager;
    }
    
    /**
     * 获取视图
     * @return 视图实例
     */
    public LAppView getView() {
        return view;
    }
    
    /**
     * 获取窗口宽度
     * @return 窗口宽度
     */
    public int getWindowWidth() {
        return windowWidth;
    }
    
    /**
     * 获取窗口高度
     * @return 窗口高度
     */
    public int getWindowHeight() {
        return windowHeight;
    }
    
    /**
     * 设置唇形同步值
     * @param value 唇形同步值
     */
    public void setLipSyncValue(float value) {
        if (live2DManager != null) {
            live2DManager.setLipSyncValue(value);
        }
    }
    
    /**
     * 设置活动状态
     * @param active 是否活动
     */
    public void setActive(boolean active) {
        isActive = active;
    }
}