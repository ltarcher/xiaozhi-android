/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import com.live2d.sdk.cubism.framework.CubismFramework;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

// 纹理管理类
public class LAppTextureManager {
    // 图像信息数据类
    public static class TextureInfo {
        public int id;  // 纹理ID
        public int width;   // 宽度
        public int height;  // 高度
        public String filePath; // 文件名
    }

    public LAppTextureManager(Context context) {
        this.context = context;
    }

    // 加载图像
    // imageFileOffset: glGenTextures创建的纹理保存位置
    public TextureInfo createTextureFromPngFile(String filePath) {
        // 搜索已加载的纹理
        for (TextureInfo textureInfo : textures) {
            if (textureInfo.filePath.equals(filePath)) {
                return textureInfo;
            }
        }

        // 从assets文件夹的图像创建位图
        InputStream stream = null;
        try {
            stream = context.getAssets().open(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            // 如果无法打开文件，返回null或默认纹理
            return null;
        }
        
        // decodeStream似乎会将图像作为预乘alpha加载
        Bitmap bitmap = BitmapFactory.decodeStream(stream);
        
        // 添加空指针检查
        if (bitmap == null) {
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                CubismFramework.coreLogFunction("Failed to decode bitmap from file: " + filePath);
            }
            return null;
        }

        // 激活Texture0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // 在OpenGL中生成纹理
        int[] textureId = new int[1];
        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

        // 将内存中的2D图像分配给纹理
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // 生成mipmap
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        // 缩小时的插值设置
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        // 放大时的插值设置
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        TextureInfo textureInfo = new TextureInfo();
        textureInfo.filePath = filePath;
        textureInfo.width = bitmap.getWidth();
        textureInfo.height = bitmap.getHeight();
        textureInfo.id = textureId[0];

        textures.add(textureInfo);

        // 释放bitmap
        bitmap.recycle();
        bitmap = null;

        if (LAppDefine.DEBUG_LOG_ENABLE) {
            CubismFramework.coreLogFunction("Create texture: " + filePath);
        }

        return textureInfo;
    }

    private final Context context;
    private final List<TextureInfo> textures = new ArrayList<TextureInfo>();        // 图像信息列表
}