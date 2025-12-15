import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Android版本工具类，用于检测当前Android版本并提供兼容性支持
class AndroidVersionUtil {
  static const int ANDROID_9_API_LEVEL = 28;
  static const int ANDROID_10_API_LEVEL = 29;
  
  static const MethodChannel _channel = MethodChannel('xiaozhi/device_info');

  /// 获取当前Android API级别
  static Future<int> getCurrentApiLevel() async {
    if (!Platform.isAndroid) {
      return -1; // 非Android平台
    }
    
    try {
      // 首先尝试使用原生方法获取API级别
      final apiLevel = await _getApiLevelFromNative();
      if (apiLevel > 0) {
        return apiLevel;
      }
      
      // 回退到使用dart:io获取系统信息
      final version = Platform.operatingSystemVersion;
      if (kDebugMode) {
        print('AndroidVersionUtil: OS version from Platform: $version');
      }
      
      // 尝试从版本字符串中提取API级别
      final match = RegExp(r'Android (\d+)').firstMatch(version);
      if (match != null) {
        return int.tryParse(match.group(1)!) ?? -1;
      }
      
      // 如果无法提取，尝试直接解析整个字符串
      return int.tryParse(version) ?? -1;
    } catch (e) {
      if (kDebugMode) {
        print('AndroidVersionUtil: Error getting API level: $e');
      }
      return -1;
    }
  }
  
  /// 从原生方法获取API级别
  static Future<int> _getApiLevelFromNative() async {
    try {
      final result = await _channel.invokeMethod('getApiLevel');
      if (result is int) {
        return result;
      }
      return -1;
    } catch (e) {
      if (kDebugMode) {
        print('AndroidVersionUtil: Error getting API level from native: $e');
      }
      return -1;
    }
  }
  
  /// 同步获取当前Android API级别（用于非异步上下文）
  static int getCurrentApiLevelSync() {
    if (!Platform.isAndroid) {
      return -1; // 非Android平台
    }
    
    try {
      // 使用dart:io获取系统信息
      final version = Platform.operatingSystemVersion;
      if (kDebugMode) {
        print('AndroidVersionUtil: OS version from Platform: $version');
      }
      
      // 尝试从版本字符串中提取API级别
      final match = RegExp(r'Android (\d+)').firstMatch(version);
      if (match != null) {
        return int.tryParse(match.group(1)!) ?? -1;
      }
      
      // 如果无法提取，尝试直接解析整个字符串
      return int.tryParse(version) ?? -1;
    } catch (e) {
      if (kDebugMode) {
        print('AndroidVersionUtil: Error getting API level: $e');
      }
      return -1;
    }
  }

  /// 检查是否为Android 9或更低版本
  static Future<bool> isAndroid9OrLower() async {
    final apiLevel = await getCurrentApiLevel();
    return apiLevel > 0 && apiLevel <= ANDROID_9_API_LEVEL;
  }
  
  /// 同步检查是否为Android 9或更低版本
  static bool isAndroid9OrLowerSync() {
    final apiLevel = getCurrentApiLevelSync();
    return apiLevel > 0 && apiLevel <= ANDROID_9_API_LEVEL;
  }

  /// 检查是否为Android 10或更高版本
  static Future<bool> isAndroid10OrHigher() async {
    final apiLevel = await getCurrentApiLevel();
    return apiLevel >= ANDROID_10_API_LEVEL;
  }
  
  /// 同步检查是否为Android 10或更高版本
  static bool isAndroid10OrHigherSync() {
    final apiLevel = getCurrentApiLevelSync();
    return apiLevel >= ANDROID_10_API_LEVEL;
  }

  /// 获取Android版本名称
  static Future<String> getAndroidVersionName() async {
    final apiLevel = await getCurrentApiLevel();
    if (apiLevel < 0) return 'Unknown';
    
    switch (apiLevel) {
      case 28:
        return 'Android 9 (Pie)';
      case 29:
        return 'Android 10 (Q)';
      case 30:
        return 'Android 11 (R)';
      case 31:
        return 'Android 12 (S)';
      case 32:
        return 'Android 12L (SV2)';
      case 33:
        return 'Android 13 (Tiramisu)';
      case 34:
        return 'Android 14 (Upside Down Cake)';
      case 35:
        return 'Android 15 (Vanilla Ice Cream)';
      default:
        return apiLevel > 28 ? 'Android $apiLevel (10+)' : 'Android $apiLevel (9-)';
    }
  }
  
  /// 同步获取Android版本名称
  static String getAndroidVersionNameSync() {
    final apiLevel = getCurrentApiLevelSync();
    if (apiLevel < 0) return 'Unknown';
    
    switch (apiLevel) {
      case 28:
        return 'Android 9 (Pie)';
      case 29:
        return 'Android 10 (Q)';
      case 30:
        return 'Android 11 (R)';
      case 31:
        return 'Android 12 (S)';
      case 32:
        return 'Android 12L (SV2)';
      case 33:
        return 'Android 13 (Tiramisu)';
      case 34:
        return 'Android 14 (Upside Down Cake)';
      case 35:
        return 'Android 15 (Vanilla Ice Cream)';
      default:
        return apiLevel > 28 ? 'Android $apiLevel (10+)' : 'Android $apiLevel (9-)';
    }
  }

  /// 检查是否需要使用兼容模式
  static Future<bool> needsCompatibilityMode() async {
    return await isAndroid9OrLower();
  }
  
  /// 同步检查是否需要使用兼容模式
  static bool needsCompatibilityModeSync() {
    return isAndroid9OrLowerSync();
  }
}