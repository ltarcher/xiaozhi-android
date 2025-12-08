/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

/**
 * 触摸管理器
 */
public class TouchManager {
    /**
     * 触摸开始时的事件
     *
     * @param deviceX 触摸屏幕的x坐标
     * @param deviceY 触摸屏幕的y坐标
     */
    public void touchesBegan(float deviceX, float deviceY) {
        lastX = deviceX;
        lastY = deviceY;

        startX = deviceX;
        startY = deviceY;

        lastTouchDistance = -1.0f;

        isFlipAvailable = true;
        isTouchSingle = true;
    }

    /**
     * 拖拽时的事件
     *
     * @param deviceX 触摸屏幕的x坐标
     * @param deviceY 触摸屏幕的y坐标
     */
    public void touchesMoved(float deviceX, float deviceY) {
        lastX = deviceX;
        lastY = deviceY;
        lastTouchDistance = -1.0f;
        isTouchSingle = true;
    }

    /**
     * 拖拽时的事件
     *
     * @param deviceX1 第一个触摸点的x坐标
     * @param deviceY1 第一个触摸点的y坐标
     * @param deviceX2 第二个触摸点的x坐标
     * @param deviceY2 第二个触摸点的y坐标
     */
    public void touchesMoved(float deviceX1, float deviceY1, float deviceX2, float deviceY2) {
        float distance = calculateDistance(deviceX1, deviceY1, deviceX2, deviceY2);
        float centerX = (deviceX1 + deviceX2) * 0.5f;
        float centerY = (deviceY1 + deviceY2) * 0.5f;

        if (lastTouchDistance > 0.0f) {
            scale = (float) Math.pow(distance / lastTouchDistance, 0.75f);
            deltaX = calculateMovingAmount(deviceX1 - lastX1, deviceX2 - lastX2);
            deltaY = calculateMovingAmount(deviceY1 - lastY1, deviceY2 - lastY2);
        } else {
            scale = 1.0f;
            deltaX = 0.0f;
            deltaY = 0.0f;
        }

        lastX = centerX;
        lastY = centerY;
        lastX1 = deviceX1;
        lastY1 = deviceY1;
        lastX2 = deviceX2;
        lastY2 = deviceY2;
        lastTouchDistance = distance;
        isTouchSingle = false;
    }

    /**
     * 测量轻扫距离
     *
     * @return 轻扫距离
     */
    public float calculateGetFlickDistance() {
        return calculateDistance(startX, startY, lastX, lastY);
    }

    // ----- getter methods -----
    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }

    public float getLastX() {
        return lastX;
    }

    public float getLastY() {
        return lastY;
    }

    public float getLastX1() {
        return lastX1;
    }

    public float getLastY1() {
        return lastY1;
    }

    public float getLastX2() {
        return lastX2;
    }

    public float getLastY2() {
        return lastY2;
    }

    public float getLastTouchDistance() {
        return lastTouchDistance;
    }

    public float getDeltaX() {
        return deltaX;
    }

    public float getDeltaY() {
        return deltaY;
    }

    public float getScale() {
        return scale;
    }

    public boolean isTouchSingle() {
        return isTouchSingle;
    }

    public boolean isFlipAvailable() {
        return isFlipAvailable;
    }

    /**
     * 计算两点间距离
     *
     * @param x1 第一个点的x坐标
     * @param y1 第一个点的y坐标
     * @param x2 第二个点的x坐标
     * @param y2 第二个点的y坐标
     * @return 两点间距离
     */
    private float calculateDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    /**
     * 从两个值计算移动量
     * 如果方向不同则移动量为0。如果方向相同，则取绝对值较小的值作为参考
     *
     * @param v1 第一个移动量
     * @param v2 第二个移动量
     * @return 较小的移动量
     */
    private float calculateMovingAmount(float v1, float v2) {
        if ((v1 > 0.0f) != (v2 > 0.0f)) {
            return 0.0f;
        }

        float sign = v1 > 0.0f ? 1.0f : -1.0f;
        float absoluteValue1 = Math.abs(v1);
        float absoluteValue2 = Math.abs(v2);

        return sign * (Math.min(absoluteValue1, absoluteValue2));
    }

    /**
     * 触摸开始时的x坐标
     */
    private float startX;
    /**
     * 触摸开始时的y坐标
     */
    private float startY;
    /**
     * 单点触摸时的x坐标
     */
    private float lastX;
    /**
     * 单点触摸时的y坐标
     */
    private float lastY;
    /**
     * 双点触摸时第一个点的x坐标
     */
    private float lastX1;
    /**
     * 双点触摸时第一个点的y坐标
     */
    private float lastY1;
    /**
     * 双点触摸时第二个点的x坐标
     */
    private float lastX2;
    /**
     * 双点触摸时第二个点的y坐标
     */
    private float lastY2;
    /**
     * 多点触摸时手指间的距离
     */
    private float lastTouchDistance;
    /**
     * 从前一个值到当前值的x轴移动距离
     */
    private float deltaX;
    /**
     * 从前一个值到当前值的y轴移动距离
     */
    private float deltaY;
    /**
     * 此帧要乘以的放大倍数。非放大操作时为1
     */
    private float scale;
    /**
     * 单点触摸时为true
     */
    private boolean isTouchSingle;
    /**
     * 翻转是否有效
     */
    private boolean isFlipAvailable;
}