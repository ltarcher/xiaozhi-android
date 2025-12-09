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
        texInfo.fileName = fileName;
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

    private final Context context;
    private final List<TextureInfo> textures = new ArrayList<TextureInfo>();        // 图像信息列表
}