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
     * @param deviceX 触摸屏幕的x值
     * @param deviceY 触摸屏幕的y值
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
     * 拖动时的事件
     *
     * @param deviceX 触摸屏幕的x值
     * @param deviceY 触摸屏幕的y值
     */
    public void touchesMoved(float deviceX, float deviceY) {
        lastX = deviceX;
        lastY = deviceY;
        lastTouchDistance = -1.0f;
        isTouchSingle = true;
    }

    /**
     * 拖动时的事件
     *
     * @param deviceX1 1个触摸屏幕的x值
     * @param deviceY1 1个触摸屏幕的y値
     * @param deviceX2 2个触摸屏幕的x値
     * @param deviceY2 2个触摸屏幕的y値
     */
    public void touchesMoved(float deviceX1, float deviceY1, float deviceX2, float deviceY2) {
        float distance = calculateDistance(deviceX1, deviceY1, deviceX2, deviceY2);
        float centerX = (deviceX1 + deviceX2) * 0.5f;
        float centerY = (deviceY1 + deviceY2) * 0.5f;

        if (lastTouchDistance > 0.0f) {
            scale = (float) Math.pow(distance / lastTouchDistance, 0.75f);
            deltaX = calculateMovingAmount(deviceX1 - lastX1, deviceX2 - lastX2);
            deltaX = calculateMovingAmount(deviceY1 - lastY1, deviceY2 - lastY2);
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
     * 测定的滑动距离
     *
     * @return 滑动距离
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
     * 从点1到点2的距离
     *
     * @param x1 1个触摸屏幕的x值
     * @param y1 1个触摸屏幕的y値
     * @param x2 1个触摸屏幕的x値
     * @param y2 1个触摸屏幕のy値
     * @return 2点の距離
     */
    private float calculateDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    /**
     * 从2个值中，计算移动量
     * 如果方向不同，则移动量为0。如果方向相同，则参考绝对值较小的值
     *
     * @param v1 1个移动量
     * @param v2 2个移动量
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
     * 触摸开始时的x值
     */
    private float startX;
    /**
     * 触摸开始时的y値
     */
    private float startY;
    /**
     * 单点触摸时的x値
     */
    private float lastX;
    /**
     * 単点触摸時のy値
     */
    private float lastY;
    /**
     * 双点触摸时的1个x値
     */
    private float lastX1;
    /**
     * 双点触摸时的1个y値
     */
    private float lastY1;
    /**
     * 双点触摸时的2个x値
     */
    private float lastX2;
    /**
     * 双点触摸时的2个y値
     */
    private float lastY2;
    /**
     * 2本以上でタッチしたときの指の距離
     */
    private float lastTouchDistance;
    /**
     * 前回の値から今回の値へのxの移動距離
     */
    private float deltaX;
    /**
     * 前回の値から今回の値へのyの移動距離
     */
    private float deltaY;
    /**
     * このフレームで掛け合わせる拡大率。拡大操作中以外は1
     */
    private float scale;
    /**
     * 単点触摸時はtrue
     */
    private boolean isTouchSingle;
    /**
     * フリップが有効かどうか
     */
    private boolean isFlipAvailable;
}
