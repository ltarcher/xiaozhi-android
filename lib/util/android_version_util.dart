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
      if (kDebugMode) {
        print('AndroidVersionUtil: API level from native: $apiLevel');
      }
      if (apiLevel > 0) {
        return apiLevel;
      }
      
      // 回退到使用dart:io获取系统信息
      final version = Platform.operatingSystemVersion;
      if (kDebugMode) {
        print('AndroidVersionUtil: OS version from Platform: $version');
      }
      
      // 尝试从版本字符串中提取Android版本号，然后转换为API级别
      final match = RegExp(r'Android (\d+)').firstMatch(version);
      if (match != null) {
        final androidVersion = int.tryParse(match.group(1)!) ?? -1;
        // 将Android版本号转换为API级别
        return _versionToApiLevel(androidVersion);
      }
      
      // 如果无法提取，尝试直接解析整个字符串
      final directVersion = int.tryParse(version) ?? -1;
      if (directVersion > 0) {
        return _versionToApiLevel(directVersion);
      }
      
      return -1;
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
      // 对于同步版本，我们也尝试使用原生方法，但作为最后的回退方案
      // 注意：这里不能等待异步方法，所以只能尝试直接获取
      
      // 使用dart:io获取系统信息
      final version = Platform.operatingSystemVersion;
      if (kDebugMode) {
        print('AndroidVersionUtil: OS version from Platform: $version');
      }
      
      // 尝试从版本字符串中提取Android版本号，然后转换为API级别
      final match = RegExp(r'Android (\d+)').firstMatch(version);
      if (match != null) {
        final androidVersion = int.tryParse(match.group(1)!) ?? -1;
        // 将Android版本号转换为API级别
        final apiLevel = _versionToApiLevel(androidVersion);
        if (kDebugMode) {
          print('AndroidVersionUtil: Extracted Android version: $androidVersion, converted to API level: $apiLevel');
        }
        return apiLevel;
      }
      
      // 如果无法提取，尝试直接解析整个字符串
      final directVersion = int.tryParse(version) ?? -1;
      if (directVersion > 0) {
        final apiLevel = _versionToApiLevel(directVersion);
        if (kDebugMode) {
          print('AndroidVersionUtil: Parsed version: $directVersion, converted to API level: $apiLevel');
        }
        return apiLevel;
      }
      
      // 如果所有方法都失败，默认假设是Android 10+（因为现在大部分设备都是新版本）
      if (kDebugMode) {
        print('AndroidVersionUtil: Could not determine version, defaulting to API level 29 (Android 10+)');
      }
      return 29; // Android 10的API级别
    } catch (e) {
      if (kDebugMode) {
        print('AndroidVersionUtil: Error getting API level: $e, defaulting to API level 29');
      }
      return 29; // Android 10的API级别
    }
  }
  
  /// 将Android版本号转换为API级别
  static int _versionToApiLevel(int androidVersion) {
    switch (androidVersion) {
      case 9:
        return ANDROID_9_API_LEVEL; // 28
      case 10:
        return ANDROID_10_API_LEVEL; // 29
      case 11:
        return 30;
      case 12:
        return 31;
      case 13:
        return 33;
      case 14:
        return 34;
      case 15:
        return 35;
      default:
        // 对于未知的版本，假设是较新的版本（>= Android 10）
        if (androidVersion >= 10) {
          // 估算API级别：Android 10 (29) + (版本号 - 10)
          return ANDROID_10_API_LEVEL + (androidVersion - 10);
        }
        // 对于较低的版本，假设是Android 9或更早
        return ANDROID_9_API_LEVEL;
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
    final isAndroid10Plus = apiLevel >= ANDROID_10_API_LEVEL;
    
    if (kDebugMode) {
      print('AndroidVersionUtil: isAndroid10OrHigherSync - API Level: $apiLevel, Is Android 10+: $isAndroid10Plus');
    }
    
    return isAndroid10Plus;
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