package com.live2d;

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
    
    // 视图矩阵
    protected final CubismViewMatrix viewMatrix = new CubismViewMatrix();
    
    // 设备到屏幕的变换矩阵
    protected final CubismMatrix44 deviceToScreen = CubismMatrix44.create();
    
    // 当前渲染目标
    protected RenderingTarget renderingTarget = RenderingTarget.NONE;
    
    // 背景颜色 [R, G, B, A]
    protected final float[] clearColor = new float[4];
    
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
     */
    public void initialize() {
        final int width = LAppDelegate.getInstance().getWindowWidth();
        final int height = LAppDelegate.getInstance().getWindowHeight();
        
        // 计算屏幕比例
        float ratio = (float) width / (float) height;
        float left = -ratio;
        float right = ratio;
        float bottom = LAppDefine.LogicalView.LEFT.getValue();
        float top = LAppDefine.LogicalView.RIGHT.getValue();
        
        // 设置屏幕矩形范围
        viewMatrix.setScreenRect(left, right, bottom, top);
        // 设置默认缩放
        viewMatrix.scale(LAppDefine.Scale.DEFAULT.getValue(), LAppDefine.Scale.DEFAULT.getValue());
        
        // 初始化设备到屏幕的变换矩阵
        deviceToScreen.loadIdentity();
        
        if (width > height) {
            // 横屏
            float screenW = Math.abs(right - left);
            deviceToScreen.scaleRelative(screenW / width, -screenW / width);
        } else {
            // 竖屏
            float screenH = Math.abs(top - bottom);
            deviceToScreen.scaleRelative(screenH / height, -screenH / height);
        }
        
        // 平移变换
        deviceToScreen.translateRelative(-width * 0.5f, -height * 0.5f);
        
        // 设置缩放限制
        viewMatrix.setMaxScale(LAppDefine.Scale.MAX.getValue());   // 最大放大率
        viewMatrix.setMinScale(LAppDefine.Scale.MIN.getValue());   // 最小缩小率
        
        // 设置最大显示范围
        viewMatrix.setMaxScreenRect(
            LAppDefine.MaxLogicalView.LEFT.getValue(),
            LAppDefine.MaxLogicalView.RIGHT.getValue(),
            LAppDefine.MaxLogicalView.BOTTOM.getValue(),
            LAppDefine.MaxLogicalView.TOP.getValue()
        );
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppView: Initialized (" + width + "x" + height + ")");
        }
    }
    
    /**
     * 渲染处理
     */
    public void render() {
        // TODO: 实现渲染逻辑
        // 这里会包含背景渲染、模型渲染等逻辑
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