package com.thinkerror.xiaozhi

import android.os.Build
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/**
 * 设备信息兼容性工具类
 * 提供Android API级别等设备信息
 */
class DeviceInfoCompat private constructor() : MethodCallHandler {

    companion object {
        private const val TAG = "DeviceInfoCompat"
        private const val CHANNEL_NAME = "xiaozhi/device_info"
        
        @JvmStatic
        fun registerWith(flutterEngine: FlutterEngine) {
            val channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME)
            channel.setMethodCallHandler(DeviceInfoCompat())
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getApiLevel" -> {
                result.success(Build.VERSION.SDK_INT)
            }
            "getAndroidVersion" -> {
                result.success(Build.VERSION.RELEASE)
            }
            "getDeviceModel" -> {
                result.success(Build.MODEL)
            }
            "getDeviceManufacturer" -> {
                result.success(Build.MANUFACTURER)
            }
            else -> {
                result.notImplemented()
            }
        }
    }
}