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
        if (context == null) {
            printLog("ERROR: Context is null when trying to load file: " + path);
            return new byte[0];
        }
        
        if (path == null || path.trim().isEmpty()) {
            printLog("ERROR: Invalid file path (null or empty)");
            return new byte[0];
        }
        
        InputStream fileData = null;
        try {
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                printLog("Trying to load file: " + path);
            }
            
            fileData = context.getAssets().open(path);

            int fileSize = fileData.available();
            if (fileSize < 0) {
                printLog("ERROR: Cannot determine file size: " + path);
                return new byte[0];
            }
            
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                printLog("File size: " + fileSize + " bytes");
            }
            
            if (fileSize == 0) {
                printLog("WARNING: File is empty: " + path);
                return new byte[0];
            }
            
            byte[] fileBuffer = new byte[fileSize];
            int bytesRead = fileData.read(fileBuffer, 0, fileSize);
            
            if (bytesRead != fileSize) {
                printLog("WARNING: Incomplete read for file: " + path + 
                        ", expected: " + fileSize + ", actual: " + bytesRead);
            }
            
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                printLog("Successfully loaded file: " + path + ", size: " + fileSize + " bytes");
            }
            return fileBuffer;
        } catch (NullPointerException e) {
            printLog("CRITICAL ERROR: Null pointer when loading file: " + path + 
                    ", Error: " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        } catch (SecurityException e) {
            printLog("SECURITY ERROR: Permission denied when accessing file: " + path + 
                    ", Error: " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        } catch (IOException e) {
            // 区分不同类型的IO异常
            if (e.getMessage() != null && 
                (e.getMessage().contains("Permission") || e.getMessage().contains("permission"))) {
                printLog("PERMISSION ERROR: Cannot access file: " + path + 
                        ", Check asset permissions. Error: " + e.getMessage());
            } else if (e.getMessage() != null && 
                      (e.getMessage().contains("No such file") || e.getMessage().contains("not found"))) {
                printLog("NOT FOUND ERROR: File does not exist: " + path + 
                        ", Please check file path and ensure file is in assets folder.");
            } else {
                printLog("FILE IO ERROR: Failed to load file: " + path + 
                        ", Error: " + e.getMessage());
            }
            e.printStackTrace();
            return new byte[0];
        } catch (OutOfMemoryError e) {
            printLog("MEMORY ERROR: Out of memory when loading file: " + path + 
                    ", File may be too large. Error: " + e.getMessage());
            return new byte[0];
        } catch (Exception e) {
            printLog("UNKNOWN ERROR: Unexpected error when loading file: " + path + 
                    ", Error: " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        } finally {
            try {
                if (fileData != null) {
                    fileData.close();
                }
            } catch (IOException e) {
                printLog("File close error for: " + path + ", Error: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                printLog("Unexpected error during file close for: " + path + 
                        ", Error: " + e.getMessage());
                e.printStackTrace();
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