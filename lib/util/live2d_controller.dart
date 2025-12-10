import 'dart:async';
import 'package:flutter/services.dart';

/// Live2D控制器，用于Flutter与Android原生代码通信
class Live2dController {
  /// 通信频道名称
  static const String CHANNEL = "com.thinkerror.xiaozhi/live2d";

  /// MethodChannel实例
  static const MethodChannel _channel = MethodChannel(CHANNEL);

  /// 初始化Live2D系统
  static Future<bool> initLive2D() async {
    try {
      final bool result = await _channel.invokeMethod('initLive2D');
      return result;
    } on PlatformException catch (e) {
      print("Failed to initialize Live2D: ${e.message}");
      return false;
    }
  }

  /// 加载模型
  /// [modelName] 模型名称，例如"Haru"
  static Future<bool> loadModel(String modelName) async {
    try {
      final bool result = await _channel.invokeMethod('loadModel', {
        'modelName': modelName,
      });
      return result;
    } on PlatformException catch (e) {
      print("Failed to load model: ${e.message}");
      return false;
    }
  }

  /// 播放动作
  /// [group] 动作组，例如"idle"、"tap_body"
  /// [no] 动作编号，默认为0
  /// [priority] 动作优先级，默认为3
  static Future<bool> startMotion(String group, {int no = 0, int priority = 3}) async {
    try {
      final bool result = await _channel.invokeMethod('startMotion', {
        'group': group,
        'no': no,
        'priority': priority,
      });
      return result;
    } on PlatformException catch (e) {
      print("Failed to start motion: ${e.message}");
      return false;
    }
  }

  /// 设置表情
  /// [expressionId] 表情ID
  static Future<bool> setExpression(String expressionId) async {
    try {
      final bool result = await _channel.invokeMethod('setExpression', {
        'expressionId': expressionId,
      });
      return result;
    } on PlatformException catch (e) {
      print("Failed to set expression: ${e.message}");
      return false;
    }
  }

  /// 设置口型同步参数
  /// [value] 口型参数值
  static Future<bool> setLipSyncValue(double value) async {
    try {
      final bool result = await _channel.invokeMethod('setLipSyncValue', {
        'value': value,
      });
      return result;
    } on PlatformException catch (e) {
      print("Failed to set lip sync value: ${e.message}");
      return false;
    }
  }

  /// 更新模型
  static Future<bool> updateModel() async {
    try {
      final bool result = await _channel.invokeMethod('updateModel');
      return result;
    } on PlatformException catch (e) {
      print("Failed to update model: ${e.message}");
      return false;
    }
  }

  /// 释放资源
  static Future<bool> dispose() async {
    try {
      final bool result = await _channel.invokeMethod('dispose');
      return result;
    } on PlatformException catch (e) {
      print("Failed to dispose Live2D: ${e.message}");
      return false;
    }
  }
}