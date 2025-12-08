/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.opengl.GLES20;
import com.live2d.sdk.cubism.framework.utils.CubismDebug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.opengl.GLES20.*;
import static com.live2d.LAppDefine.ResourcePath.*;

public class LAppSpriteShader implements AutoCloseable {
    public static final int ATTR_POSITION_LOCATION = 0;
    public static final int ATTR_UV_LOCATION = 1;

    /**
     * 创建Sprite用着色器程序的实例
     */
    public LAppSpriteShader() {
        // 创建着色器程序
        programId = createShader();
    }

    @Override
    public void close() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId);
            programId = 0;
        }
    }

    public int getShaderId() {
        return programId;
    }

    /**
     * 创建着色器程序
     *
     * @return 着色器程序ID
     */
    private int createShader() {
        // 编译顶点着色器
        int vertexShaderId = compileShaderProgram(GLES20.GL_VERTEX_SHADER, SHADER_ROOT.getPath() + "/" + VERT_SHADER.getPath());

        // 编译片段着色器
        int fragmentShaderId = compileShaderProgram(GLES20.GL_FRAGMENT_SHADER, SHADER_ROOT.getPath() + "/" + FRAG_SHADER.getPath());

        // 创建着色器程序
        int programId = GLES20.glCreateProgram();
        if (programId == 0) {
            CubismDebug.cubismLogError("Failed to create shader program.");
            return 0;
        }

        // 附加顶点着色器
        GLES20.glAttachShader(programId, vertexShaderId);

        // 附加片段着色器
        GLES20.glAttachShader(programId, fragmentShaderId);

        // 绑定属性位置
        GLES20.glBindAttribLocation(programId, ATTR_POSITION_LOCATION, "position");
        GLES20.glBindAttribLocation(programId, ATTR_UV_LOCATION, "uv");

        // 链接着色器程序
        GLES20.glLinkProgram(programId);

        // 检查链接结果
        int[] linked = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            CubismDebug.cubismLogError("Failed to link program: " + GLES20.glGetProgramInfoLog(programId));
            GLES20.glDeleteProgram(programId);
            programId = 0;
        }

        // 释放着色器
        GLES20.glDeleteShader(vertexShaderId);
        GLES20.glDeleteShader(fragmentShaderId);

        return programId;
    }

    /**
     * 编译着色器程序
     *
     * @param shaderType 着色器类型
     * @param fileName   着色器文件名
     * @return 着色器ID
     */
    private int compileShaderProgram(int shaderType, String fileName) {
        // 读取着色器文件
        String shaderSource = loadShaderFile(fileName);

        // 创建着色器
        int shaderId = GLES20.glCreateShader(shaderType);
        if (shaderId == 0) {
            CubismDebug.cubismLogError("Failed to create shader.");
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
            CubismDebug.cubismLogError("Failed to compile shader: " + GLES20.glGetShaderInfoLog(shaderId));
            GLES20.glDeleteShader(shaderId);
            return 0;
        }

        return shaderId;
    }

    /**
     * 加载着色器文件
     *
     * @param fileName 着色器文件名
     * @return 着色器源码
     */
    private String loadShaderFile(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;

        try {
            inputStream = LAppDelegate.getInstance().getContext().getAssets().open(fileName);
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            CubismDebug.cubismLogError("Failed to load shader file: " + fileName);
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return stringBuilder.toString();
    }

    /**
     * 着色器程序ID
     */
    private int programId;
}