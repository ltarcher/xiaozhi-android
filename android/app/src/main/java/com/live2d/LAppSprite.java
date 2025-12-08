/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.opengl.GLES20;
import com.live2d.sdk.cubism.framework.utils.CubismDebug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.*;

public class LAppSprite implements AutoCloseable {
    /**
     * 创建Sprite类的实例
     *
     * @param x             x坐标
     * @param y             y坐标
     * @param width         宽度
     * @param height        高度
     * @param textureId     纹理ID
     * @param programId     着色器程序ID
     */
    public LAppSprite(float x, float y, float width, float height, int textureId, int programId) {
        rect.left = (x - width * 0.5f);
        rect.right = (x + width * 0.5f);
        rect.up = (y + height * 0.5f);
        rect.down = (y - height * 0.5f);

        this.textureId = textureId;
        this.programId = programId;

        // 顶点坐标
        final float[] positionVertex = {
            (rect.left - x) / width, (rect.up - y) / height,    // 左上
            (rect.left - x) / width, (rect.down - y) / height,  // 左下
            (rect.right - x) / width, (rect.up - y) / height,   // 右上
            (rect.right - x) / width, (rect.down - y) / height  // 右下
        };

        // UV坐标
        final float[] uvVertex = {
            0.0f, 0.0f, // 左上
            0.0f, 1.0f, // 左下
            1.0f, 0.0f, // 右上
            1.0f, 1.0f  // 右下
        };

        // 顶点索引
        final short[] indexArray = {
            0, 1, 2,
            1, 3, 2
        };

        // 创建VBO
        if (vertexBuffer == 0) {
            // 创建顶点坐标VBO
            int[] vbo = new int[1];
            glGenBuffers(1, vbo, 0);
            vertexBuffer = vbo[0];
        }

        // 绑定顶点坐标VBO
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        // 为VBO分配空间并注册数据
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(positionVertex.length * Float.SIZE / Byte.SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer positionBuffer = byteBuffer.asFloatBuffer();
        positionBuffer.put(positionVertex);
        positionBuffer.position(0);
        glBufferData(GL_ARRAY_BUFFER, positionVertex.length * (Float.SIZE / Byte.SIZE), positionBuffer, GL_STATIC_DRAW);
        // 解绑顶点坐标VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // 创建UV坐标VBO
        if (uvBuffer == 0) {
            int[] vbo = new int[1];
            glGenBuffers(1, vbo, 0);
            uvBuffer = vbo[0];
        }

        // 绑定UV坐标VBO
        glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
        // 为VBO分配空间并注册数据
        byteBuffer = ByteBuffer.allocateDirect(uvVertex.length * Float.SIZE / Byte.SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer uvBuffer = byteBuffer.asFloatBuffer();
        uvBuffer.put(uvVertex);
        uvBuffer.position(0);
        glBufferData(GL_ARRAY_BUFFER, uvVertex.length * (Float.SIZE / Byte.SIZE), uvBuffer, GL_STATIC_DRAW);
        // 解绑UV坐标VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // 创建IBO
        if (indexBuffer == 0) {
            int[] ibo = new int[1];
            glGenBuffers(1, ibo, 0);
            indexBuffer = ibo[0];
        }

        // 绑定IBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        // 为IBO分配空间并注册数据
        ByteBuffer indexByteBuffer = ByteBuffer.allocateDirect(indexArray.length * Short.SIZE / Byte.SIZE);
        indexByteBuffer.order(ByteOrder.nativeOrder());
        indexBufferObject = indexByteBuffer.asShortBuffer();
        indexBufferObject.put(indexArray);
        indexBufferObject.position(0);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexArray.length * (Short.SIZE / Byte.SIZE), indexBufferObject, GL_STATIC_DRAW);
        // 解绑IBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        // 启用属性
        glEnableVertexAttribArray(LAppSpriteShader.ATTR_POSITION_LOCATION);
        glEnableVertexAttribArray(LAppSpriteShader.ATTR_UV_LOCATION);
    }

    @Override
    public void close() {
        // 释放VBO
        if (vertexBuffer != 0) {
            int[] vbo = {vertexBuffer};
            glDeleteBuffers(1, vbo, 0);
            vertexBuffer = 0;
        }

        if (uvBuffer != 0) {
            int[] vbo = {uvBuffer};
            glDeleteBuffers(1, vbo, 0);
            uvBuffer = 0;
        }

        // 释放IBO
        if (indexBuffer != 0) {
            int[] ibo = {indexBuffer};
            glDeleteBuffers(1, ibo, 0);
            indexBuffer = 0;
        }
    }

    // 绘制精灵
    public void render() {
        render(textureId);
    }

    // 绘制精灵
    public void renderImmediate(int textureId, float[] uvVertex) {
        // 注册UV坐标
        // 绑定UV坐标VBO
        glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
        // 为VBO分配空间并注册数据
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(uvVertex.length * Float.SIZE / Byte.SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer uvBufferObject = byteBuffer.asFloatBuffer();
        uvBufferObject.put(uvVertex);
        uvBufferObject.position(0);
        glBufferData(GL_ARRAY_BUFFER, uvVertex.length * (Float.SIZE / Byte.SIZE), uvBufferObject, GL_STATIC_DRAW);
        // 解绑UV坐标VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        render(textureId);
    }

    // 绘制精灵
    public void render(int textureId) {
        if (programId == 0) {
            CubismDebug.cubismLogError("Shader program is not set.");
            return;
        }

        glUseProgram(programId);

        // 获取统一变量的位置
        int baseColorLocation = glGetUniformLocation(programId, "baseColor");
        int textureLocation = glGetUniformLocation(programId, "texture");

        // 设置统一变量
        glUniform4f(baseColorLocation, colorR, colorG, colorB, colorA);
        glUniform1i(textureLocation, 0);

        // 绑定顶点坐标VBO
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glVertexAttribPointer(LAppSpriteShader.ATTR_POSITION_LOCATION, 2, GL_FLOAT, false, 0, 0);

        // 绑定UV坐标VBO
        glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
        glVertexAttribPointer(LAppSpriteShader.ATTR_UV_LOCATION, 2, GL_FLOAT, false, 0, 0);

        // 绑定IBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);

        // 绑定纹理
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);

        // 绘制
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);

        // 解绑纹理
        glBindTexture(GL_TEXTURE_2D, 0);

        // 解绑IBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        // 解绑VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glUseProgram(0);
    }

    // 调整精灵大小
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
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        // 为VBO分配空间并注册数据
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(positionVertex.length * Float.SIZE / Byte.SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer positionBuffer = byteBuffer.asFloatBuffer();
        positionBuffer.put(positionVertex);
        positionBuffer.position(0);
        glBufferData(GL_ARRAY_BUFFER, positionVertex.length * (Float.SIZE / Byte.SIZE), positionBuffer, GL_STATIC_DRAW);
        // 解绑顶点坐标VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    // 检测点击
    public boolean isHit(float pointX, float pointY) {
        // 获取屏幕尺寸
        int maxWidth = windowWidth;
        int maxHeight = windowHeight;

        // Y轴上下颠倒
        float y = maxHeight - pointY;

        return (pointX >= rect.left && pointX <= rect.right && y <= rect.up && y >= rect.down);
    }

    // 设置颜色
    public void setColor(float r, float g, float b, float a) {
        colorR = r;
        colorG = g;
        colorB = b;
        colorA = a;
    }

    // 设置窗口大小
    public void setWindowSize(int width, int height) {
        windowWidth = width;
        windowHeight = height;
    }

    // 矩形
    public static class Rect {
        // 左端
        public float left;
        // 右端
        public float right;
        // 上端
        public float up;
        // 下端
        public float down;
    }

    private final Rect rect = new Rect();          // 矩形
    private int textureId;                         // 纹理ID
    private final int programId;                   // 着色器程序ID

    private int vertexBuffer;                      // 顶点坐标VBO
    private int uvBuffer;                          // UV坐标VBO
    private int indexBuffer;                       // IBO
    private ShortBuffer indexBufferObject;         // IBO对象

    private float colorR = 1.0f;                   // 颜色(R)
    private float colorG = 1.0f;                   // 颜色(G)
    private float colorB = 1.0f;                   // 颜色(B)
    private float colorA = 1.0f;                   // 颜色(A)

    private int windowWidth;                       // 窗口宽度
    private int windowHeight;                      // 窗口高度
}