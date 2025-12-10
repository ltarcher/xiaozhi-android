package com.live2d;

import android.opengl.GLES20;

import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.math.CubismViewMatrix;

/**
 * Live2D视图类
 * 负责渲染和用户交互处理
 */
public class LAppView {
    /**
     * LAppModel的渲染目标
     */
    public enum RenderingTarget {
        NONE,                   // 默认的帧缓冲区渲染
        MODEL_FRAME_BUFFER,     // 模型各自的帧缓冲区渲染
        VIEW_FRAME_BUFFER       // 视图的帧缓冲区渲染
    }
    
    // 投影矩阵
    protected final CubismMatrix44 projection = CubismMatrix44.create();
    
    // 视图矩阵
    protected final CubismViewMatrix viewMatrix = new CubismViewMatrix();
    
    // 设备到屏幕的变换矩阵
    protected final CubismMatrix44 deviceToScreen = CubismMatrix44.create();
    
    // 当前渲染目标
    protected RenderingTarget renderingTarget = RenderingTarget.NONE;
    
    // 背景颜色 [R, G, B, A]
    protected final float[] clearColor = new float[4];
    
    // 视图宽度和高度
    protected int width;
    protected int height;
    
    // 触摸管理器
    protected TouchManager touchManager;
    
    /**
     * 构造函数
     */
    public LAppView() {
        // 初始化背景颜色为白色半透明
        clearColor[0] = 1.0f;  // R
        clearColor[1] = 1.0f;  // G
        clearColor[2] = 1.0f;  // B
        clearColor[3] = 0.0f;  // A
        
        // 创建触摸管理器
        touchManager = TouchManager.getInstance();
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppView: Created");
        }
    }
    
    /**
     * 初始化视图
     * @param width 宽度
     * @param height 高度
     */
    public void initialize(int width, int height) {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppView: initialize 被调用，尺寸: " + width + "x" + height);
        }
        
        this.width = width;
        this.height = height;
        
        // 初始化投影矩阵
        projection.loadIdentity();
        
        // 初始化设备到屏幕矩阵
        deviceToScreen.loadIdentity();
        // 将设备坐标系转换为屏幕坐标系
        // 竖屏情况下，将宽高比调整为height/width
        if (width > height) {
            deviceToScreen.scale(1.0f, (float) width / (float) height);
        } else {
            deviceToScreen.scale((float) height / (float) width, 1.0f);
        }
        
        // 设置屏幕矩形范围
        viewMatrix.setScreenRect(-1.0f, 1.0f, -1.0f, 1.0f);
        // 设置最大屏幕矩形范围
        viewMatrix.setMaxScreenRect(-2.0f, 2.0f, -2.0f, 2.0f);
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppView: 初始化完成");
        }
    }
    
    /**
     * 渲染处理
     */
    public void render() {
        // 清除屏幕
        GLES20.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }
    
    /**
     * 处理触摸开始事件
     * @param x 触摸点x坐标
     * @param y 触摸点y坐标
     */
    public void onTouchesBegan(float x, float y) {
        if (LAppDefine.DEBUG_TOUCH_LOG_ENABLE) {
            LAppPal.printLog("LAppView: onTouchesBegan(" + x + ", " + y + ")");
        }
        
        touchManager.touchesBegan(x, y, -1, -1);
    }
    
    /**
     * 处理触摸移动事件
     * @param x 触摸点x坐标
     * @param y 触摸点y坐标
     */
    public void onTouchesMoved(float x, float y) {
        if (LAppDefine.DEBUG_TOUCH_LOG_ENABLE) {
            LAppPal.printLog("LAppView: onTouchesMoved(" + x + ", " + y + ")");
        }
        
        touchManager.touchesMoved(x, y, -1, -1);
    }
    
    /**
     * 处理触摸结束事件
     * @param x 触摸点x坐标
     * @param y 触摸点y坐标
     */
    public void onTouchesEnded(float x, float y) {
        if (LAppDefine.DEBUG_TOUCH_LOG_ENABLE) {
            LAppPal.printLog("LAppView: onTouchesEnded(" + x + ", " + y + ")");
        }
        
        touchManager.touchesEnded();
    }
    
    /**
     * 获取视图矩阵
     * @return 视图矩阵
     */
    public CubismViewMatrix getViewMatrix() {
        return viewMatrix;
    }
    
    /**
     * 获取设备到屏幕的变换矩阵
     * @return 变换矩阵
     */
    public CubismMatrix44 getDeviceToScreen() {
        return deviceToScreen;
    }
    
    /**
     * 设置渲染目标
     * @param target 渲染目标
     */
    public void setRenderingTarget(RenderingTarget target) {
        renderingTarget = target;
    }
    
    /**
     * 获取渲染目标
     * @return 渲染目标
     */
    public RenderingTarget getRenderingTarget() {
        return renderingTarget;
    }
}