package com.live2d;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 纹理管理类
 * 负责加载、管理和释放纹理资源
 */
public class LAppTextureManager {
    /**
     * 纹理信息类
     */
    public static class TextureInfo {
        // 纹理ID
        public int id;
        // 纹理宽度
        public int width;
        // 纹理高度
        public int height;
        // 文件名
        public String fileName;
        
        /**
         * 构造函数
         */
        public TextureInfo() {
            id = 0;
            width = 0;
            height = 0;
            fileName = "";
        }
    }
    
    // AssetManager引用
    private AssetManager assetManager;
    
    // 纹理信息列表
    private final List<TextureInfo> textures = new ArrayList<>();
    
    /**
     * 构造函数
     * @param assetManager AssetManager实例
     */
    public LAppTextureManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }
    
    /**
     * 从PNG文件创建纹理
     * @param imagePath 图片路径
     * @return 纹理信息
     */
    public TextureInfo createTextureFromPngFile(String imagePath) {
        // 先检查是否已经加载过该纹理
        for (TextureInfo info : textures) {
            if (info.fileName.equals(imagePath)) {
                return info;
            }
        }
        
        // 创建新的纹理信息
        TextureInfo textureInfo = new TextureInfo();
        textureInfo.fileName = imagePath;
        
        // 加载图片
        Bitmap bitmap = loadBitmapFromFile(imagePath);
        if (bitmap == null) {
            LAppPal.printErrorLog("Failed to load bitmap: " + imagePath);
            return textureInfo;
        }
        
        // 创建纹理
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        textureInfo.id = textureIds[0];
        
        if (textureInfo.id == 0) {
            LAppPal.printErrorLog("Failed to generate texture: " + imagePath);
            bitmap.recycle();
            return textureInfo;
        }
        
        // 绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureInfo.id);
        
        // 设置纹理参数
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            bitmap.getWidth(),
            bitmap.getHeight(),
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        );
        
        // 加载纹理数据
        GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap);
        
        // 设置纹理过滤参数
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        
        // 生成MipMap
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        
        // 解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        
        // 保存纹理信息
        textureInfo.width = bitmap.getWidth();
        textureInfo.height = bitmap.getHeight();
        textures.add(textureInfo);
        
        // 释放Bitmap资源
        bitmap.recycle();
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("Loaded texture: " + imagePath + " (" + textureInfo.width + "x" + textureInfo.height + ")");
        }
        
        return textureInfo;
    }
    
    /**
     * 从文件加载Bitmap
     * @param imagePath 图片路径
     * @return Bitmap对象
     */
    private Bitmap loadBitmapFromFile(String imagePath) {
        if (assetManager == null) {
            LAppPal.printErrorLog("AssetManager is not set");
            return null;
        }
        
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(imagePath);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            if (bitmap == null) {
                LAppPal.printErrorLog("Failed to decode bitmap: " + imagePath);
                return null;
            }
            
            return bitmap;
        } catch (IOException e) {
            LAppPal.printErrorLog("Failed to load bitmap: " + imagePath + ", error: " + e.getMessage());
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LAppPal.printErrorLog("Failed to close input stream: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 释放指定纹理
     * @param textureId 纹理ID
     */
    public void releaseTexture(int textureId) {
        for (int i = 0; i < textures.size(); i++) {
            if (textures.get(i).id == textureId) {
                TextureInfo info = textures.get(i);
                
                // 删除OpenGL纹理
                int[] textureIds = {info.id};
                GLES20.glDeleteTextures(1, textureIds, 0);
                
                // 从列表中移除
                textures.remove(i);
                
                if (LAppDefine.DEBUG_LOG_ENABLE) {
                    LAppPal.printLog("Released texture: " + info.fileName);
                }
                break;
            }
        }
    }
    
    /**
     * 释放所有纹理
     */
    public void releaseAllTextures() {
        for (TextureInfo info : textures) {
            int[] textureIds = {info.id};
            GLES20.glDeleteTextures(1, textureIds, 0);
            
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                LAppPal.printLog("Released texture: " + info.fileName);
            }
        }
        textures.clear();
    }
    
    /**
     * 根据纹理ID获取纹理信息
     * @param textureId 纹理ID
     * @return 纹理信息，如果找不到则返回null
     */
    public TextureInfo getTextureInfoById(int textureId) {
        for (TextureInfo info : textures) {
            if (info.id == textureId) {
                return info;
            }
        }
        return null;
    }
}