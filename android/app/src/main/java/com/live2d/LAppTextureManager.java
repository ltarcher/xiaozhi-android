/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import com.live2d.LAppDefine;
import com.live2d.sdk.cubism.framework.CubismFramework;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

// 纹理管理类
public class LAppTextureManager {
    private static final String TAG = "LAppTextureManager";
    
    // 实例ID，用于区分不同的Live2D实例
    private String instanceId;
    
    public LAppTextureManager() {
        this(null);
    }
    
    public LAppTextureManager(String instanceId) {
        this.instanceId = instanceId;
        Log.d(TAG, "LAppTextureManager created with instanceId: " + instanceId);
    }
    
    /**
     * 设置实例ID
     * @param instanceId 实例ID
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
    
    // 图像信息数据类
    public static class TextureInfo {
        public int id;  // 纹理ID
        public int width;   // 宽度
        public int height;  // 高度
        public String filePath; // 文件名
    }

    // 图像读取
    // imageFileOffset: 通过glGenTextures创建的纹理的保存位置
    public TextureInfo createTextureFromPngFile(String filePath) {
        Log.d(TAG, "createTextureFromPngFile: Loading texture from " + filePath);
        
        // search loaded texture already
        for (TextureInfo textureInfo : textures) {
            if (textureInfo.filePath.equals(filePath)) {
                Log.d(TAG, "createTextureFromPngFile: Texture already loaded, returning cached version");
                return textureInfo;
            }
        }

        // 从assets文件夹的图像创建位图
        AssetManager assetManager;
        if (instanceId != null && !instanceId.isEmpty()) {
            LAppDelegate appDelegate = LAppDelegate.getInstance(instanceId);
            if (appDelegate != null) {
                assetManager = appDelegate.getActivity().getAssets();
            } else {
                // 回退到默认实例
                assetManager = LAppDelegate.getInstance().getActivity().getAssets();
            }
        } else {
            assetManager = LAppDelegate.getInstance().getActivity().getAssets();
        }
        InputStream stream = null;
        try {
            Log.d(TAG, "createTextureFromPngFile: Opening asset file");
            stream = assetManager.open(filePath);
        } catch (IOException e) {
            Log.e(TAG, "createTextureFromPngFile: Failed to open asset file: " + filePath, e);
            e.printStackTrace();
        }
        
        if (stream == null) {
            Log.e(TAG, "createTextureFromPngFile: InputStream is null for file: " + filePath);
            return null;
        }
        
        // decodeStream似乎以预乘alpha的方式读取图像
        Bitmap bitmap = BitmapFactory.decodeStream(stream);
        Log.d(TAG, "createTextureFromPngFile: Bitmap decoded, null=" + (bitmap == null));
        
        if (bitmap == null) {
            Log.e(TAG, "createTextureFromPngFile: Failed to decode bitmap from stream for file: " + filePath);
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(TAG, "createTextureFromPngFile: Failed to close stream", e);
            }
            return null;
        }

        // 激活Texture0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // 在OpenGL中生成纹理
        int[] textureId = new int[1];
        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

        // 将内存上的2D图像分配给纹理
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // 生成多级渐远纹理
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
        
        Log.d(TAG, "createTextureFromPngFile: Texture info - width=" + textureInfo.width + 
              ", height=" + textureInfo.height + ", id=" + textureInfo.id);

        textures.add(textureInfo);

        // 释放bitmap
        bitmap.recycle();
        bitmap = null;

        if (LAppDefine.DEBUG_LOG_ENABLE) {
            CubismFramework.coreLogFunction("Create texture: " + filePath);
        }
        
        try {
            stream.close();
        } catch (IOException e) {
            Log.e(TAG, "createTextureFromPngFile: Failed to close stream", e);
        }
        
        Log.d(TAG, "createTextureFromPngFile: Texture created successfully");
        return textureInfo;
    }

    private final List<TextureInfo> textures = new ArrayList<TextureInfo>();        // 图像信息的列表
}