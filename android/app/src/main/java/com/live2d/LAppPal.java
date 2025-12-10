package com.live2d;

import android.util.Log;

import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * 平台相关功能处理类
 */
public class LAppPal {
    private static final String TAG = "LAppPal";
    
    // 上一帧的时间戳（毫秒）
    private static double s_lastFrameTime = 0.0;
    // 帧间时间差（秒）
    private static double s_deltaTime = 0.0;
    
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
        // 此方法将在后续实现中补充具体内容
        // 因为需要访问AssetManager，所以会在具体使用时传入
        return new byte[0];
    }
    
    /**
     * 从assets目录读取文件为字符串
     * @param filePath 文件路径
     * @return 文件内容字符串
     */
    public static String loadFileAsString(String filePath) {
        // 此方法将在后续实现中补充具体内容
        return "";
    }
}