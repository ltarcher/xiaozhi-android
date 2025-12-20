import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';

/// Android服务管理器，用于与Android后台服务通信
class AndroidServiceManager {
  static const MethodChannel _channel = MethodChannel('xiaozhi/wake_word_service');
  
  /// 启动唤醒词服务
  static Future<bool> startWakeWordService() async {
    try {
      final bool result = await _channel.invokeMethod('startWakeWordService');
      return result;
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print('AndroidServiceManager: Failed to start wake word service: ${e.message}');
      }
      return false;
    }
  }
  
  /// 停止唤醒词服务
  static Future<bool> stopWakeWordService() async {
    try {
      final bool result = await _channel.invokeMethod('stopWakeWordService');
      return result;
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print('AndroidServiceManager: Failed to stop wake word service: ${e.message}');
      }
      return false;
    }
  }
  
  /// 检查唤醒词服务是否正在运行
  static Future<bool> isWakeWordServiceRunning() async {
    try {
      final bool result = await _channel.invokeMethod('isWakeWordServiceRunning');
      return result;
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print('AndroidServiceManager: Failed to check wake word service status: ${e.message}');
      }
      return false;
    }
  }
  
  /// 更新唤醒词
  static Future<bool> updateWakeWord(String wakeWord) async {
    try {
      final bool result = await _channel.invokeMethod('updateWakeWord', {
        'wakeWord': wakeWord,
      });
      return result;
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print('AndroidServiceManager: Failed to update wake word: ${e.message}');
      }
      return false;
    }
  }
}