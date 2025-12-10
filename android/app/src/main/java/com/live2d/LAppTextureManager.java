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

// テクスチャの管理を行うクラス
public class LAppTextureManager {
    private static final String TAG = "LAppTextureManager";
    
    // 画像情報データクラス
    public static class TextureInfo {
        public int id;  // テクスチャID
        public int width;   // 横幅
        public int height;  // 高さ
        public String filePath; // ファイル名
    }

    // 画像読み込み
    // imageFileOffset: glGenTexturesで作成したテクスチャの保存場所
    public TextureInfo createTextureFromPngFile(String filePath) {
        Log.d(TAG, "createTextureFromPngFile: Loading texture from " + filePath);
        
        // search loaded texture already
        for (TextureInfo textureInfo : textures) {
            if (textureInfo.filePath.equals(filePath)) {
                Log.d(TAG, "createTextureFromPngFile: Texture already loaded, returning cached version");
                return textureInfo;
            }
        }

        // assetsフォルダの画像からビットマップを作成する
        AssetManager assetManager = LAppDelegate.getInstance().getActivity().getAssets();
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
        
        // decodeStreamは乗算済みアルファとして画像を読み込むようである
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

        // Texture0をアクティブにする
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // OpenGLにテクスチャを生成
        int[] textureId = new int[1];
        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

        // メモリ上の2D画像をテクスチャに割り当てる
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // ミップマップを生成する
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        // 縮小時の補間設定
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        // 拡大時の補間設定
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        TextureInfo textureInfo = new TextureInfo();
        textureInfo.filePath = filePath;
        textureInfo.width = bitmap.getWidth();
        textureInfo.height = bitmap.getHeight();
        textureInfo.id = textureId[0];
        
        Log.d(TAG, "createTextureFromPngFile: Texture info - width=" + textureInfo.width + 
              ", height=" + textureInfo.height + ", id=" + textureInfo.id);

        textures.add(textureInfo);

        // bitmap解放
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

    private final List<TextureInfo> textures = new ArrayList<TextureInfo>();        // 画像情報のリスト
}