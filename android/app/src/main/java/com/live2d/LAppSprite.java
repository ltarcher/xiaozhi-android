/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * 用于绘制精灵的类
 */
public class LAppSprite {
    /**
     * 返回纹理ID
     *
     * @return 纹理ID
     */
    public int getTextureId() {
        return textureId;
    }

    /**
     * 返回当前对象的放大率
     *
     * @return 精灵的放大倍数
     */
    public float[] getScale() {
        return new float[]{width, height};
    }

    /**
     * 返回当前对象的位置
     *
     * @return 精灵的位置
     */
    public float[] getPosition() {
        return new float[]{positionX, positionY};
    }

    /**
     * 返回当前对象的旋转弧度
     *
     * @return 精灵的旋转弧度
     */
    public float getRotate() {
        return rotate;
    }

    /**
     * 检查给定点是否位于精灵区域内
     *
     * @param pointX x坐标
     * @param pointY y坐标
     * @return 如果点在精灵区域则返回true，否则返回false
     */
    public boolean isHit(float pointX, float pointY) {
        // 获取屏幕坐标系中的精灵区域
        float left = positionX - width * 0.5f;
        float right = positionX + width * 0.5f;
        float top = positionY + height * 0.5f;
        float bottom = positionY - height * 0.5f;

        return (left <= pointX && pointX <= right && top >= pointY && pointY >= bottom);
    }

    /**
     * 调整精灵大小
     *
     * @param x      x坐标
     * @param y      y坐标
     * @param width  宽度
     * @param height 高度
     */
    public void resize(float x, float y, float width, float height) {
        rect.left = (x - width * 0.5f);
        rect.right = (x + width * 0.5f);
        rect.up = (y + height * 0.5f);
        rect.down = (y - height * 0.5f);

        // 顶点坐标
        final float[] positionVertex = {
                (rect.left - x) / width, (rect.up - y) / height,    // 左上
                (rect.left - x) / width, (rect.down - y) / height,  // 左下
                (rect.right - x) / width, (rect.up - y) / height,   // 右上
                (rect.right - x) / width, (rect.down - y) / height  // 右下
        };

        // 绑定顶点坐标VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffer);
        // 为VBO分配空间并注册数据
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(positionVertex.length * Float.SIZE / Byte.SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer positionBuffer = byteBuffer.asFloatBuffer();
        positionBuffer.put(positionVertex);
        positionBuffer.position(0);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, positionVertex.length * (Float.SIZE / Byte.SIZE), positionBuffer, GLES20.GL_STATIC_DRAW);
        // 解绑顶点坐标VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    /**
     * 立即绘制精灵（不使用VAO/VBO）
     *
     * @param textureId 纹理ID
     * @param uvArray   UV数组
     */
    public void renderImmediate(int textureId, float[] uvArray) {
        if (programId == 0) {
            return;
        }

        GLES20.glUseProgram(programId);

        // 获取统一变量的位置
        int baseColorLocation = GLES20.glGetUniformLocation(programId, "baseColor");
        int textureLocation = GLES20.glGetUniformLocation(programId, "texture");

        // 设置统一变量
        GLES20.glUniform4f(baseColorLocation, colorR, colorG, colorB, colorA);
        GLES20.glUniform1i(textureLocation, 0);

        // 创建顶点缓冲区
        FloatBuffer positionBuf = ByteBuffer.allocateDirect(positionVertex.length * Float.SIZE / Byte.SIZE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        positionBuf.put(positionVertex);
        positionBuf.position(0);

        FloatBuffer uvBuf = ByteBuffer.allocateDirect(uvArray.length * Float.SIZE / Byte.SIZE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        uvBuf.put(uvArray);
        uvBuf.position(0);

        ShortBuffer indexBuf = ByteBuffer.allocateDirect(indexArray.length * Short.SIZE / Byte.SIZE)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        indexBuf.put(indexArray);
        indexBuf.position(0);

        // 设置顶点属性
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, positionBuf);

        GLES20.glEnableVertexAttribArray(1);
        GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, uvBuf);

        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        // 绘制
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexArray.length, GLES20.GL_UNSIGNED_SHORT, indexBuf);

        // 解绑
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisableVertexAttribArray(0);
        GLES20.glDisableVertexAttribArray(1);
        GLES20.glUseProgram(0);
    }

    /**
     * 设置精灵的颜色
     *
     * @param r 红色分量 (0.0f - 1.0f)
     * @param g 绿色分量 (0.0f - 1.0f)
     * @param b 蓝色分量 (0.0f - 1.0f)
     * @param a alpha分量 (0.0f - 1.0f)
     */
    public void setColor(float r, float g, float b, float a) {
        colorR = r;
        colorG = g;
        colorB = b;
        colorA = a;
    }

    /**
     * 设置窗口大小
     *
     * @param width  窗口宽度
     * @param height 窗口高度
     */
    public void setWindowSize(int width, int height) {
        maxWidth = width;
        maxHeight = height;
    }

    /**
     * 释放纹理
     */
    public void deleteTexture() {
        if (textureId != 0) {
            int[] textureIds = new int[]{textureId};
            GLES20.glDeleteTextures(1, textureIds, 0);
            textureId = 0;
        }
    }

    /**
     * 构造函数
     *
     * @param x         x坐标
     * @param y         y坐标
     * @param width     宽度
     * @param height    高度
     * @param textureId 纹理ID
     * @param programId 着色器程序ID
     */
    public LAppSprite(
            float x,
            float y,
            float width,
            float height,
            int textureId,
            int programId
    ) {
        rect = new Rect();
        rect.left = (x - width * 0.5f);
        rect.right = (x + width * 0.5f);
        rect.up = (y + height * 0.5f);
        rect.down = (y - height * 0.5f);

        this.textureId = textureId;
        this.programId = programId;

        // 顶点坐标
        positionVertex = new float[]{
                (rect.left - x) / width, (rect.up - y) / height,    // 左上
                (rect.left - x) / width, (rect.down - y) / height,  // 左下
                (rect.right - x) / width, (rect.up - y) / height,   // 右上
                (rect.right - x) / width, (rect.down - y) / height  // 右下
        };

        // UV坐标
        uvVertex = new float[]{
                0.0f, 0.0f, // 左上
                0.0f, 1.0f, // 左下
                1.0f, 0.0f, // 右上
                1.0f, 1.0f  // 右下
        };

        // 顶点索引
        indexArray = new short[]{
                0, 1, 2,
                1, 3, 2
        };

        // 创建VBO
        if (vertexBuffer == 0) {
            // 创建顶点坐标VBO
            int[] vbo = new int[1];
            GLES20.glGenBuffers(1, vbo, 0);
            vertexBuffer = vbo[0];
        }

        // 绑定顶点坐标VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffer);
        // 为VBO分配空间并注册数据
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(positionVertex.length * Float.SIZE / Byte.SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer positionBuffer = byteBuffer.asFloatBuffer();
        positionBuffer.put(positionVertex);
        positionBuffer.position(0);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, positionVertex.length * (Float.SIZE / Byte.SIZE), positionBuffer, GLES20.GL_STATIC_DRAW);
        // 解绑顶点坐标VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // 创建UV坐标VBO
        if (uvBuffer == 0) {
            int[] vbo = new int[1];
            GLES20.glGenBuffers(1, vbo, 0);
            uvBuffer = vbo[0];
        }

        // 绑定UV坐标VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, uvBuffer);
        // 为VBO分配空间并注册数据
        byteBuffer = ByteBuffer.allocateDirect(uvVertex.length * Float.SIZE / Byte.SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer uvBufferObject = byteBuffer.asFloatBuffer();
        uvBufferObject.put(uvVertex);
        uvBufferObject.position(0);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, uvVertex.length * (Float.SIZE / Byte.SIZE), uvBufferObject, GLES20.GL_STATIC_DRAW);
        // 解绑UV坐标VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // 创建IBO
        if (indexBuffer == 0) {
            int[] ibo = new int[1];
            GLES20.glGenBuffers(1, ibo, 0);
            indexBuffer = ibo[0];
        }

        // 绑定IBO
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        // 为IBO分配空间并注册数据
        ByteBuffer indexByteBuffer = ByteBuffer.allocateDirect(indexArray.length * Short.SIZE / Byte.SIZE);
        indexByteBuffer.order(ByteOrder.nativeOrder());
        indexBufferObject = indexByteBuffer.asShortBuffer();
        indexBufferObject.put(indexArray);
        indexBufferObject.position(0);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexArray.length * (Short.SIZE / Byte.SIZE), indexBufferObject, GLES20.GL_STATIC_DRAW);
        // 解绑IBO
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        // 启用属性
        GLES20.glEnableVertexAttribArray(LAppSpriteShader.ATTR_POSITION_LOCATION);
        GLES20.glEnableVertexAttribArray(LAppSpriteShader.ATTR_UV_LOCATION);
    }

    public void close() {
        // 释放VBO
        if (vertexBuffer != 0) {
            int[] vbo = {vertexBuffer};
            GLES20.glDeleteBuffers(1, vbo, 0);
            vertexBuffer = 0;
        }

        if (uvBuffer != 0) {
            int[] vbo = {uvBuffer};
            GLES20.glDeleteBuffers(1, vbo, 0);
            uvBuffer = 0;
        }

        // 释放IBO
        if (indexBuffer != 0) {
            int[] ibo = {indexBuffer};
            GLES20.glDeleteBuffers(1, ibo, 0);
            indexBuffer = 0;
        }
    }

    // 矩形
    private static class Rect {
        // 左端
        public float left;
        // 右端
        public float right;
        // 上端
        public float up;
        // 下端
        public float down;
    }

    private Rect rect;

    /**
     * X轴位置
     */
    private float positionX;
    /**
     * Y轴位置
     */
    private float positionY;
    /**
     * X轴方向的缩放因子
     */
    private float width;
    /**
     * Y轴方向的缩放因子
     */
    private float height;
    /**
     * 旋转弧度
     */
    private float rotate;
    /**
     * 纹理ID
     */
    private int textureId;
    /**
     * 着色器程序ID
     */
    private int programId;
    /**
     * 顶点坐标数组
     */
    private final float[] positionVertex;
    /**
     * UV坐标数组
     */
    private final float[] uvVertex;
    /**
     * 索引数组
     */
    private final short[] indexArray;
    /**
     * 颜色(RGBA)
     */
    private float colorR = 1.0f;
    private float colorG = 1.0f;
    private float colorB = 1.0f;
    private float colorA = 1.0f;
    /**
     * 窗口最大尺寸
     */
    private int maxWidth;
    private int maxHeight;
    /**
     * 顶点缓冲区对象
     */
    private int vertexBuffer;
    /**
     * UV缓冲区对象
     */
    private int uvBuffer;
    /**
     * 索引缓冲区对象
     */
    private int indexBuffer;
    /**
     * 索引缓冲区对象（ShortBuffer）
     */
    private ShortBuffer indexBufferObject;
}