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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 纹理管理类
public class LAppTextureManager {
    // 图像信息数据类
    public static class TextureInfo {
        public int id;  // 纹理ID
        public int width;   // 宽度
        public int height;  // 高度
        public String filePath; // 文件路径
    }

    public LAppTextureManager(Context context) {
        this.context = context;
    }

    // 加载图像
    // imageFileOffset: glGenTextures创建的纹理保存位置
    public TextureInfo createTextureFromPngFile(final String fileName) {
        LAppPal.printLog("开始创建纹理: " + fileName);
        // 查找已加载的纹理
        TextureInfo texInfo = textures.get(fileName);
        if (texInfo != null) {
            LAppPal.printLog("纹理已在缓存中找到: " + fileName);
            return texInfo;
        }

        // png文件的读取
        byte[] pngData = LAppPal.loadFileAsBytes(fileName, context);
        if (pngData.length == 0) {
            LAppPal.printLog("ERROR: 纹理文件读取失败或为空: " + fileName);
            return null;
        }
        
        LAppPal.printLog("纹理文件读取成功: " + fileName + ", 大小: " + pngData.length + " 字节");

        Bitmap bitmap = loadPngAsBitmap(pngData);
        if (bitmap == null) {
            LAppPal.printLog("ERROR: 位图加载失败: " + fileName);
            return null;
        }
        
        LAppPal.printLog("位图加载成功: " + fileName + ", 宽度: " + bitmap.getWidth() + ", 高度: " + bitmap.getHeight());

        texInfo = new TextureInfo();
        texInfo.filePath = fileName;
        texInfo.width = bitmap.getWidth();
        texInfo.height = bitmap.getHeight();
        texInfo.id = generateTexture(bitmap);

        if (texInfo.id == 0) {
            LAppPal.printLog("ERROR: 纹理生成失败: " + fileName);
            return null;
        }
        
        LAppPal.printLog("纹理生成成功: " + fileName + ", OpenGL ID: " + texInfo.id);

        textures.put(fileName, texInfo);

        // 释放不再需要的bitmap
        bitmap.recycle();

        return texInfo;
    }

    /**
     * 从PNG数据创建Bitmap
     * @param data PNG数据
     * @return Bitmap对象
     */
    private Bitmap loadPngAsBitmap(byte[] data) {
        // 从PNG数据创建Bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        return bitmap;
    }

    /**
     * 从Bitmap生成OpenGL纹理
     * @param bitmap Bitmap对象
     * @return 纹理ID
     */
    private int generateTexture(Bitmap bitmap) {
        // 创建纹理对象
        int[] textureId = new int[1];
        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap.getWidth(), bitmap.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        
        // 设置纹理参数
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        
        return textureId[0];
    }

    private final Context context;
    private final Map<String, TextureInfo> textures = new HashMap<String, TextureInfo>();        // 图像信息列表
}