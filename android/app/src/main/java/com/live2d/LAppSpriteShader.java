package com.live2d;

import android.opengl.GLES20;

import com.live2d.LAppDefine;
import com.live2d.sdk.cubism.framework.utils.CubismDebug;

/**
 * 用于保存精灵用着色器设置的类
 */
public class LAppSpriteShader implements AutoCloseable {
    /**
     * 构造函数
     */
    public LAppSpriteShader() {
        programId = createShader();
    }

    @Override
    public void close() {
        GLES20.glDeleteShader(programId);
    }

    /**
     * 获取着色器ID。
     *
     * @return 着色器ID
     */
    public int getShaderId() {
        return programId;
    }

    /**
     * 创建着色器。
     *
     * @return 着色器ID。无法正常创建时返回0。
     */
    private int createShader() {
        // 创建着色器路径
        String vertShaderFile = LAppDefine.ResourcePath.SHADER_ROOT.getPath();
        vertShaderFile += ("/" + LAppDefine.ResourcePath.VERT_SHADER.getPath());

        String fragShaderFile = LAppDefine.ResourcePath.SHADER_ROOT.getPath();
        fragShaderFile += ("/" + LAppDefine.ResourcePath.FRAG_SHADER.getPath());

        // 编译着色器
        int vertexShaderId = compileShader(vertShaderFile, GLES20.GL_VERTEX_SHADER);
        int fragmentShaderId = compileShader(fragShaderFile, GLES20.GL_FRAGMENT_SHADER);

        if (vertexShaderId == 0 || fragmentShaderId == 0) {
            return 0;
        }

        // 创建程序对象
        int programId = GLES20.glCreateProgram();

        // 设置程序的着色器
        GLES20.glAttachShader(programId, vertexShaderId);
        GLES20.glAttachShader(programId, fragmentShaderId);

        GLES20.glLinkProgram(programId);
        GLES20.glUseProgram(programId);

        // 删除不再需要的着色器对象
        GLES20.glDeleteShader(vertexShaderId);
        GLES20.glDeleteShader(fragmentShaderId);

        return programId;
    }

    /**
     * CreateShader内部函数。进行错误检查。
     *
     * @param shaderId 着色器ID
     * @return 错误检查结果。true时表示没有错误。
     */
    private boolean checkShader(int shaderId) {
        int[] logLength = new int[1];
        GLES20.glGetShaderiv(shaderId, GLES20.GL_INFO_LOG_LENGTH, logLength, 0);

        if (logLength[0] > 0) {
            String log = GLES20.glGetShaderInfoLog(shaderId);
            CubismDebug.cubismLogError("Shader compile log: %s", log);
        }

        int[] status = new int[1];
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, status, 0);

        if (status[0] == GLES20.GL_FALSE) {
            GLES20.glDeleteShader(shaderId);
            return false;
        }

        return true;
    }


    /**
     * 编译着色器。
     * 编译成功时返回0。
     *
     * @param fileName 着色器文件名
     * @param shaderType 要创建的着色器类型
     * @return 着色器ID。无法正常创建时返回0。
     */
    private int compileShader(String fileName, int shaderType) {
        // 文件读取
        byte[] shaderBuffer = LAppPal.loadFileAsBytes(fileName);

        // 编译
        int shaderId = GLES20.glCreateShader(shaderType);
        GLES20.glShaderSource(shaderId, new String(shaderBuffer));
        GLES20.glCompileShader(shaderId);

        if (!checkShader(shaderId)) {
            return 0;
        }

        return shaderId;
    }

    private final int programId; // 着色器ID
}
