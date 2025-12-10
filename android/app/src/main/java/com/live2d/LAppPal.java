package com.live2d;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 平台相关功能处理类
 */
public class LAppPal {
    private static final String TAG = "LAppPal";
    
    // 上一帧的时间戳（毫秒）
    private static double s_lastFrameTime = 0.0;
    // 帧间时间差（秒）
    private static double s_deltaTime = 0.0;
    
    // AssetManager引用
    private static AssetManager s_assetManager;
    
    /**
     * 获取帧间时间差
     * @return 帧间时间差（秒）
     */
    public static double getDeltaTime() {
        return s_deltaTime;
    }
    
    /**
     * 更新时间信息
     */
    public static void updateTime() {
        final double currentTime = System.currentTimeMillis() / 1000.0;
        s_deltaTime = s_lastFrameTime == 0.0 ? 0.0 : currentTime - s_lastFrameTime;
        s_lastFrameTime = currentTime;
    }
    
    /**
     * 打印日志
     * @param logText 日志内容
     */
    public static void printLog(String logText) {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            Log.d(TAG, logText);
        }
    }
    
    /**
     * 设置AssetManager
     * @param assetManager AssetManager实例
     */
    public static void setAssetManager(AssetManager assetManager) {
        s_assetManager = assetManager;
    }
    
    /**
     * 打印错误日志
     * @param logText 错误日志内容
     */
    public static void printErrorLog(String logText) {
        Log.e(TAG, logText);
    }
    
    /**
     * 从assets目录读取文件到字节数组
     * @param filePath 文件路径
     * @return 文件内容字节数组
     */
    public static byte[] loadFileAsBytes(String filePath) {
        if (s_assetManager == null) {
            printErrorLog("AssetManager is not set");
            return new byte[0];
        }
        
        InputStream inputStream = null;
        try {
            inputStream = s_assetManager.open(filePath);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            printErrorLog("Failed to load file: " + filePath + ", error: " + e.getMessage());
            return new byte[0];
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    printErrorLog("Failed to close input stream: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 从assets目录读取文件为字符串
     * @param filePath 文件路径
     * @return 文件内容字符串
     */
    public static String loadFileAsString(String filePath) {
        byte[] bytes = loadFileAsBytes(filePath);
        if (bytes.length == 0) {
            return "";
        }
        return new String(bytes);
    }
}