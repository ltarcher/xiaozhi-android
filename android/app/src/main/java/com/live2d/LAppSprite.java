/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.*;

public class LAppSprite {
    private static final String TAG = "LAppSprite";
    
    /**
     * 精灵可见性标志，默认为true（可见）
     */
    private boolean isVisible = true;

    public LAppSprite(
        float x,
        float y,
        float width,
        float height,
        int textureId,
        int programId
    ) {
        Log.d(TAG, "LAppSprite constructor: x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);
        rect.left = x - width * 0.5f;
        rect.right = x + width * 0.5f;
        rect.up = y + height * 0.5f;
        rect.down = y - height * 0.5f;
        Log.d(TAG, "LAppSprite rect: left=" + rect.left + ", right=" + rect.right + ", up=" + rect.up + ", down=" + rect.down);

        this.textureId = textureId;

        // 何番目のattribute変数か
        positionLocation = GLES20.glGetAttribLocation(programId, "position");
        uvLocation = GLES20.glGetAttribLocation(programId, "uv");
        textureLocation = GLES20.glGetUniformLocation(programId, "texture");
        colorLocation = GLES20.glGetUniformLocation(programId, "baseColor");

        spriteColor[0] = 1.0f;
        spriteColor[1] = 1.0f;
        spriteColor[2] = 1.0f;
        spriteColor[3] = 1.0f;
    }

    public void render() {
        // 检查精灵是否可见，如果不可见则直接返回
        if (!isVisible) {
            Log.v(TAG, "render: Sprite is not visible, skipping render");
            return;
        }
        
        // Log.v(TAG, "render: Rendering sprite");
        // Set the camera position (View matrix)
        uvVertex[0] = 1.0f;
        uvVertex[1] = 0.0f;
        uvVertex[2] = 0.0f;
        uvVertex[3] = 0.0f;
        uvVertex[4] = 0.0f;
        uvVertex[5] = 1.0f;
        uvVertex[6] = 1.0f;
        uvVertex[7] = 1.0f;

        // 透過設定 - 与模型渲染保持一致的混合模式
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glEnableVertexAttribArray(positionLocation);
        GLES20.glEnableVertexAttribArray(uvLocation);

        GLES20.glUniform1i(textureLocation, 0);

        // 頂点データ
        positionVertex[0] = (rect.right - maxWidth * 0.5f) / (maxWidth * 0.5f);
        positionVertex[1] = (rect.up - maxHeight * 0.5f) / (maxHeight * 0.5f);
        positionVertex[2] = (rect.left - maxWidth * 0.5f) / (maxWidth * 0.5f);
        positionVertex[3] = (rect.up - maxHeight * 0.5f) / (maxHeight * 0.5f);
        positionVertex[4] = (rect.left - maxWidth * 0.5f) / (maxWidth * 0.5f);
        positionVertex[5] = (rect.down - maxHeight * 0.5f) / (maxHeight * 0.5f);
        positionVertex[6] = (rect.right - maxWidth * 0.5f) / (maxWidth * 0.5f);
        positionVertex[7] = (rect.down - maxHeight * 0.5f) / (maxHeight * 0.5f);

        if (posVertexFloatBuffer == null) {
            ByteBuffer posVertexByteBuffer = ByteBuffer.allocateDirect(positionVertex.length * 4);
            posVertexByteBuffer.order(ByteOrder.nativeOrder());
            posVertexFloatBuffer = posVertexByteBuffer.asFloatBuffer();
        }
        if (uvVertexFloatBuffer == null) {
            ByteBuffer uvVertexByteBuffer = ByteBuffer.allocateDirect(uvVertex.length * 4);
            uvVertexByteBuffer.order(ByteOrder.nativeOrder());
            uvVertexFloatBuffer = uvVertexByteBuffer.asFloatBuffer();
        }
        posVertexFloatBuffer.put(positionVertex).position(0);
        uvVertexFloatBuffer.put(uvVertex).position(0);

        glVertexAttribPointer(positionLocation, 2, GL_FLOAT, false, 0, posVertexFloatBuffer);
        glVertexAttribPointer(uvLocation, 2, GL_FLOAT, false, 0, uvVertexFloatBuffer);

        GLES20.glUniform4f(colorLocation, spriteColor[0], spriteColor[1], spriteColor[2], spriteColor[3]);

        GLES20.glBindTexture(GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
    }

    private final float[] uvVertex = new float[8];
    private final float[] positionVertex = new float[8];

    private FloatBuffer posVertexFloatBuffer;
    private FloatBuffer uvVertexFloatBuffer;

    /**
     * 指定纹理ID进行绘制
     *
     * @param textureId 纹理ID
     * @param uvVertex uv顶点坐标
     */
    public void renderImmediate(int textureId, final float[] uvVertex) {
        // 检查精灵是否可见，如果不可见则直接返回
        if (!isVisible) {
            Log.v(TAG, "renderImmediate: Sprite is not visible, skipping render");
            return;
        }
        
        Log.v(TAG, "renderImmediate: Rendering sprite with textureId=" + textureId);
        // 启用attribute属性
        GLES20.glEnableVertexAttribArray(positionLocation);
        GLES20.glEnableVertexAttribArray(uvLocation);

        // 注册uniform属性
        GLES20.glUniform1i(textureLocation, 0);

        // 顶点数据
        float[] positionVertex = {
            (rect.right - maxWidth * 0.5f) / (maxWidth * 0.5f), (rect.up - maxHeight * 0.5f) / (maxHeight * 0.5f),
            (rect.left - maxWidth * 0.5f) / (maxWidth * 0.5f), (rect.up - maxHeight * 0.5f) / (maxHeight * 0.5f),
            (rect.left - maxWidth * 0.5f) / (maxWidth * 0.5f), (rect.down - maxHeight * 0.5f) / (maxHeight * 0.5f),
            (rect.right - maxWidth * 0.5f) / (maxWidth * 0.5f), (rect.down - maxHeight * 0.5f) / (maxHeight * 0.5f)
        };

        // 注册attribute属性
        {
            ByteBuffer bb = ByteBuffer.allocateDirect(positionVertex.length * 4);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer buffer = bb.asFloatBuffer();
            buffer.put(positionVertex);
            buffer.position(0);

            GLES20.glVertexAttribPointer(positionLocation, 2, GL_FLOAT, false, 0, buffer);
        }
        {
            ByteBuffer bb = ByteBuffer.allocateDirect(uvVertex.length * 4);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer buffer = bb.asFloatBuffer();
            buffer.put(uvVertex);
            buffer.position(0);

            GLES20.glVertexAttribPointer(uvLocation, 2, GL_FLOAT, false, 0, buffer);
        }

        GLES20.glUniform4f(colorLocation, spriteColor[0], spriteColor[1], spriteColor[2], spriteColor[3]);

        // 模型绘制
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
    }

    // 调整大小
    public void resize(float x, float y, float width, float height) {
        Log.d(TAG, "resize: x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);
        rect.left = x - width * 0.5f;
        rect.right = x + width * 0.5f;
        rect.up = y + height * 0.5f;
        rect.down = y - height * 0.5f;
        Log.d(TAG, "resize: New rect: left=" + rect.left + ", right=" + rect.right + ", up=" + rect.up + ", down=" + rect.down);
    }

    /**
     * 画像との当たり判定を行う
     *
     * @param pointX タッチした点のx座標
     * @param pointY タッチした点のy座標
     * @return 当たっていればtrue
     */
    public boolean isHit(float pointX, float pointY) {
        // y座標は変換する必要あり
        float y = maxHeight - pointY;
        Log.v(TAG, "isHit: pointX=" + pointX + ", pointY=" + pointY + ", convertedY=" + y + 
              ", rect.left=" + rect.left + ", rect.right=" + rect.right + 
              ", rect.up=" + rect.up + ", rect.down=" + rect.down);
        
        boolean hit = (pointX >= rect.left && pointX <= rect.right && y <= rect.up && y >= rect.down);
        Log.v(TAG, "isHit: result=" + hit);
        return hit;
    }

    public void setColor(float r, float g, float b, float a) {
        Log.v(TAG, "setColor: r=" + r + ", g=" + g + ", b=" + b + ", a=" + a);
        spriteColor[0] = r;
        spriteColor[1] = g;
        spriteColor[2] = b;
        spriteColor[3] = a;
    }
    
    /**
     * 设置精灵的可见性
     *
     * @param visible true表示显示，false表示隐藏
     */
    public void setIsVisible(boolean visible) {
        Log.d(TAG, "setIsVisible: Setting sprite visible to " + visible);
        this.isVisible = visible;
    }
    
    /**
     * 获取精灵的可见性状态
     *
     * @return true表示显示，false表示隐藏
     */
    public boolean getIsVisible() {
        Log.v(TAG, "getIsVisible: Returning " + isVisible);
        return isVisible;
    }

    /**
     * ウィンドウサイズを設定する。
     *
     * @param width 横幅
     * @param height 高さ
     */
    public void setWindowSize(int width, int height) {
        Log.v(TAG, "setWindowSize: width=" + width + ", height=" + height);
        maxWidth = width;
        maxHeight = height;
    }

    /**
     * Rectクラス
     */
    private static class Rect {
        /**
         * 左辺
         */
        public float left;
        /**
         * 右辺
         */
        public float right;
        /**
         * 上辺
         */
        public float up;
        /**
         * 下辺
         */
        public float down;
    }


    private final Rect rect = new Rect();
    private final int textureId;

    private final int positionLocation;  // 位置アトリビュート
    private final int uvLocation; // UVアトリビュート
    private final int textureLocation;   // テクスチャアトリビュート
    private final int colorLocation;     // カラーアトリビュート
    private final float[] spriteColor = new float[4];   // 表示カラー

    private int maxWidth;   // ウィンドウ幅
    private int maxHeight;  // ウィンドウ高さ
}