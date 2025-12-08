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

/**
 * 精灵着色器类
 */
public class LAppSpriteShader {
    /**
     * 属性位置：位置
     */
    public static final int ATTR_POSITION_LOCATION = 0;
    /**
     * 属性位置：UV
     */
    public static final int ATTR_UV_LOCATION = 1;

    /**
     * 生成着色器程序
     *
     * @param vertShaderSrc 顶点着色器源码
     * @param fragShaderSrc 片段着色器源码
     * @return 程序ID
     */
    public static int generateShaderProgram(String vertShaderSrc, String fragShaderSrc) {
        // 编译顶点着色器
        int vertShaderId = compileShader(GLES20.GL_VERTEX_SHADER, vertShaderSrc);
        if (vertShaderId == 0) {
            CubismDebug.cubismLogError("Vertex shader compile error!");
            return 0;
        }

        // 编译片段着色器
        int fragShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragShaderSrc);
        if (fragShaderId == 0) {
            CubismDebug.cubismLogError("Fragment shader compile error!");
            return 0;
        }

        // 链接着色器程序
        int programId = linkProgram(vertShaderId, fragShaderId);
        if (programId == 0) {
            CubismDebug.cubismLogError("Shader program link error!");
            return 0;
        }

        return programId;
    }

    /**
     * 编译着色器
     *
     * @param shaderType 着色器类型 (GL_VERTEX_SHADER 或 GL_FRAGMENT_SHADER)
     * @param shaderSource 着色器源码
     * @return 着色器ID
     */
    private static int compileShader(int shaderType, String shaderSource) {
        // 创建着色器
        int shaderId = GLES20.glCreateShader(shaderType);
        if (shaderId == 0) {
            return 0;
        }

        // 设置着色器源码
        GLES20.glShaderSource(shaderId, shaderSource);

        // 编译着色器
        GLES20.glCompileShader(shaderId);

        // 检查编译结果
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            // 编译失败
            String infoLog = GLES20.glGetShaderInfoLog(shaderId);
            GLES20.glDeleteShader(shaderId);
            return 0;
        }

        return shaderId;
    }

    /**
     * 链接着色器程序
     *
     * @param vertexShaderId 顶点着色器ID
     * @param fragmentShaderId 片段着色器ID
     * @return 程序ID
     */
    private static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        // 创建程序
        int programId = GLES20.glCreateProgram();
        if (programId == 0) {
            return 0;
        }

        // 绑定着色器
        GLES20.glAttachShader(programId, vertexShaderId);
        GLES20.glAttachShader(programId, fragmentShaderId);

        // 链接程序
        GLES20.glLinkProgram(programId);

        // 检查链接结果
        int[] linked = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            // 链接失败
            String infoLog = GLES20.glGetProgramInfoLog(programId);
            GLES20.glDeleteProgram(programId);
            return 0;
        }

        return programId;
    }

    /**
     * 构造函数
     */
    public LAppSpriteShader() {
        String vertShaderSrc =
                "#version 100\n"
                        + "attribute vec3 position;"
                        + "attribute vec2 uv;"
                        + "varying vec2 vuv;"
                        + "void main(void)"
                        + "{"
                        + "   gl_Position = vec4(position, 1.0);"
                        + "   vuv = uv;"
                        + "}";
        String fragShaderSrc =
                "#version 100\n"
                        + "precision mediump float;"
                        + "varying vec2 vuv;"
                        + "uniform sampler2D texture;"
                        + "uniform vec4 baseColor;"
                        + "void main(void)"
                        + "{"
                        + "   gl_FragColor = texture2D(texture, vuv) * baseColor;"
                        + "}";

        shaderId = generateShaderProgram(vertShaderSrc, fragShaderSrc);
    }

    /**
     * 绑定着色器程序
     */
    public void bindShaderProgram() {
        if (shaderId == 0) {
            CubismDebug.cubismLogError("Shader program is not set.");
            return;
        }

        GLES20.glUseProgram(shaderId);
    }

    /**
     * 获取着色器程序ID
     *
     * @return 程序ID
     */
    public int getShaderId() {
        return shaderId;
    }

    /**
     * 着色器程序ID
     */
    private int shaderId;
}