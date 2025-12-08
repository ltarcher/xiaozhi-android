import 'package:flutter/services.dart';

/// Live2D控制器类，用于从Flutter端控制Android上的Live2D模型
class Live2dController {
  static const MethodChannel _channel = MethodChannel('com.thinkerror.xiaozhi/live2d');

  /// 播放指定的动作
  ///
  /// [group] 动作组名
  /// [number] 组内编号
  /// [priority] 优先级
  ///
  /// 返回开始的动作的标识号
  static Future<int> startMotion({
    required String group,
    required int number,
    required int priority,
  }) async {
    try {
      final int motionId = await _channel.invokeMethod('startMotion', {
        'group': group,
        'number': number,
        'priority': priority,
      });
      return motionId;
    } on PlatformException catch (e) {
      throw Exception('Failed to start motion: ${e.message}');
    }
  }

  /// 设置指定的表情动作
  ///
  /// [expressionId] 表情动作的ID
  static Future<bool> setExpression(String expressionId) async {
    try {
      final bool result = await _channel.invokeMethod('setExpression', {
        'expressionId': expressionId,
      });
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to set expression: ${e.message}');
    }
  }

  /// 设置口型同步值
  ///
  /// [value] 口型同步值 (0.0 - 1.0)
  static Future<bool> setLipSyncValue(double value) async {
    try {
      final bool result = await _channel.invokeMethod('setLipSyncValue', {
        'value': value,
      });
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to set lip sync value: ${e.message}');
    }
  }

  /// 切换到下一个场景（模型）
  static Future<bool> nextScene() async {
    try {
      final bool result = await _channel.invokeMethod('nextScene');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to switch to next scene: ${e.message}');
    }
  }

  /// 切换到指定索引的场景（模型）
  ///
  /// [index] 场景索引
  static Future<bool> changeScene(int index) async {
    try {
      final bool result = await _channel.invokeMethod('changeScene', {
        'index': index,
      });
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to change scene: ${e.message}');
    }
  }
}