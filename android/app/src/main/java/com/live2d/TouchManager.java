package com.live2d;

/**
 * 触摸管理类
 * 管理触摸点、拖拽状态等触摸相关信息
 */
public class TouchManager {
    /**
     * 触摸状态枚举
     */
    public enum TouchState {
        NONE,           // 无触摸
        SINGLE_TOUCH,   // 单点触摸
        MULTI_TOUCH     // 多点触摸
    }
    
    // 单例实例
    private static TouchManager s_instance;
    
    /**
     * 获取单例实例
     * @return TouchManager实例
     */
    public static TouchManager getInstance() {
        if (s_instance == null) {
            s_instance = new TouchManager();
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
    
    // 当前触摸状态
    private TouchState touchState = TouchState.NONE;
    
    // 初始触摸点坐标
    private float startX;
    private float startY;
    
    // 当前触摸点坐标
    private float lastX;
    private float lastY;
    
    // 上一个触摸点坐标
    private float previousX;
    private float previousY;
    
    // 触摸距离
    private float lastTouchDistance;
    private float previousTouchDistance;
    
    // 是否处于拖拽状态
    private boolean isDragging = false;
    
    // 屏幕宽度和高度
    private int screenWidth;
    private int screenHeight;
    
    /**
     * 私有构造函数
     */
    private TouchManager() {
    }
    
    /**
     * 初始化触摸管理器
     * @param width 屏幕宽度
     * @param height 屏幕高度
     */
    public void initialize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
    }
    
    /**
     * 处理触摸开始事件
     * @param x1 第一个触摸点x坐标
     * @param y1 第一个触摸点y坐标
     * @param x2 第二个触摸点x坐标（多点触摸时）
     * @param y2 第二个触摸点y坐标（多点触摸时）
     */
    public void touchesBegan(float x1, float y1, float x2, float y2) {
        if (x2 < 0.0f && y2 < 0.0f) {
            // 单点触摸
            touchState = TouchState.SINGLE_TOUCH;
            
            startX = x1;
            startY = y1;
            lastX = x1;
            lastY = y1;
            previousX = x1;
            previousY = y1;
            
            isDragging = false;
        } else {
            // 多点触摸
            touchState = TouchState.MULTI_TOUCH;
            
            final float distance = calculateDistance(x1, y1, x2, y2);
            lastTouchDistance = distance;
            previousTouchDistance = distance;
            
            isDragging = false;
        }
    }
    
    /**
     * 处理触摸移动事件
     * @param x1 第一个触摸点x坐标
     * @param y1 第一个触摸点y坐标
     * @param x2 第二个触摸点x坐标（多点触摸时）
     * @param y2 第二个触摸点y坐标（多点触摸时）
     */
    public void touchesMoved(float x1, float y1, float x2, float y2) {
        previousX = lastX;
        previousY = lastY;
        
        if (touchState == TouchState.SINGLE_TOUCH) {
            // 单点触摸移动
            lastX = x1;
            lastY = y1;
            
            // 如果移动距离超过阈值，则认为是拖拽
            if (!isDragging) {
                final float dx = Math.abs(x1 - startX);
                final float dy = Math.abs(y1 - startY);
                const float threshold = 10.0f; // 拖拽判定阈值
                
                if (dx > threshold || dy > threshold) {
                    isDragging = true;
                }
            }
        } else if (touchState == TouchState.MULTI_TOUCH) {
            // 多点触摸移动
            previousTouchDistance = lastTouchDistance;
            lastTouchDistance = calculateDistance(x1, y1, x2, y2);
        }
    }
    
    /**
     * 处理触摸结束事件
     */
    public void touchesEnded() {
        touchState = TouchState.NONE;
        isDragging = false;
    }
    
    /**
     * 计算两点间距离
     * @param x1 第一个点x坐标
     * @param y1 第一个点y坐标
     * @param x2 第二个点x坐标
     * @param y2 第二个点y坐标
     * @return 两点间距离
     */
    private float calculateDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }
    
    /**
     * 获取X轴移动距离
     * @return X轴移动距离
     */
    public float getDeltaX() {
        return lastX - previousX;
    }
    
    /**
     * 获取Y轴移动距离
     * @return Y轴移动距离
     */
    public float getDeltaY() {
        return lastY - previousY;
    }
    
    /**
     * 获取缩放比例
     * @return 缩放比例
     */
    public float getScale() {
        if (previousTouchDistance == 0.0f || lastTouchDistance == 0.0f) {
            return 1.0f;
        }
        return lastTouchDistance / previousTouchDistance;
    }
    
    /**
     * 获取起始X坐标
     * @return 起始X坐标
     */
    public float getStartX() {
        return startX;
    }
    
    /**
     * 获取起始Y坐标
     * @return 起始Y坐标
     */
    public float getStartY() {
        return startY;
    }
    
    /**
     * 获取当前X坐标
     * @return 当前X坐标
     */
    public float getCurrentX() {
        return lastX;
    }
    
    /**
     * 获取当前Y坐标
     * @return 当前Y坐标
     */
    public float getCurrentY() {
        return lastY;
    }
    
    /**
     * 是否处于拖拽状态
     * @return true表示正在拖拽，false表示未拖拽
     */
    public boolean isDragging() {
        return isDragging;
    }
    
    /**
     * 是否为多点触摸
     * @return true表示多点触摸，false表示单点触摸
     */
    public boolean isMultiTouch() {
        return touchState == TouchState.MULTI_TOUCH;
    }
}