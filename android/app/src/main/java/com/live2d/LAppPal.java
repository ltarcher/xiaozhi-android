/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.content.Context;
import android.util.Log;
import com.live2d.sdk.cubism.core.ICubismLogger;

import java.io.IOException;
import java.io.InputStream;

public class LAppPal {
    /**
     * Logging Function class to be registered in the CubismFramework's logging function.
     */
    public static class PrintLogFunction implements ICubismLogger {
        @Override
        public void print(String message) {
            Log.d(TAG, message);
        }
    }

    // 更新时间
    public static void updateTime() {
        s_currentFrame = getSystemNanoTime();
        _deltaNanoTime = s_currentFrame - _lastNanoTime;
        _lastNanoTime = s_currentFrame;
    }

    // 将文件作为字节数组加载
    public static byte[] loadFileAsBytes(final String path, Context context) {
        InputStream fileData = null;
        try {
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                printLog("Trying to load file: " + path);
            }
            
            fileData = context.getAssets().open(path);

            int fileSize = fileData.available();
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                printLog("File size: " + fileSize + " bytes");
            }
            
            if (fileSize == 0) {
                printLog("WARNING: File is empty: " + path);
            }
            
            byte[] fileBuffer = new byte[fileSize];
            fileData.read(fileBuffer, 0, fileSize);
            
            printLog("Successfully loaded file: " + path + ", size: " + fileSize + " bytes");
            return fileBuffer;
        } catch (IOException e) {
            e.printStackTrace();

            if (LAppDefine.DEBUG_LOG_ENABLE) {
                printLog("File open error: " + path + ", Error: " + e.getMessage());
            }

            return new byte[0];
        } finally {
            try {
                if (fileData != null) {
                    fileData.close();
                }
            } catch (IOException e) {
                e.printStackTrace();

                if (LAppDefine.DEBUG_LOG_ENABLE) {
                    printLog("File close error.");
                }
            }
        }
    }

    // 获取增量时间(与前一帧的时间差)
    public static float getDeltaTime() {
        // 将纳秒转换为秒
        return (float) (_deltaNanoTime / 1000000000.0f);
    }

    /**
     * 日志记录函数
     *
     * @param message 日志消息
     */
    public static void printLog(String message) {
        Log.d(TAG, message);
    }

    private static long getSystemNanoTime() {
        return System.nanoTime();
    }

    private static double s_currentFrame;
    private static double _lastNanoTime;
    private static double _deltaNanoTime;

    private static final String TAG = "[APP]";

    private LAppPal() {}
}