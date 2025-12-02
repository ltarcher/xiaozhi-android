import 'dart:io' show Platform;

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'package:flutter_live2d/flutter_live2d.dart';

class Live2DManager {
  static final Live2DManager _instance = Live2DManager._internal();

  factory Live2DManager() => _instance;

  Live2DManager._internal();

  bool _initialized = false;
  bool _modelLoaded = false;

  /// 初始化Live2D系统
  Future<void> initialize() async {
    if (_initialized) return;
    
    try {
      // Live2D目前只支持Android平台
      if (!kIsWeb && Platform.isAndroid) {
        await FlutterLive2d.initLive2d();
        _initialized = true;
      }
    } catch (e) {
      debugPrint('Live2D initialization failed: $e');
    }
  }

  /// 加载默认的Haru模型
  Future<void> loadDefaultModel() async {
    if (!_initialized || _modelLoaded) return;
    
    try {
      if (!kIsWeb && Platform.isAndroid) {
        await FlutterLive2d.loadModel("assets/live2d/Haru/Haru.model3.json");
        _modelLoaded = true;
        
        // 设置初始缩放和位置
        await FlutterLive2d.setScale(1.0);
        await FlutterLive2d.setPosition(0.0, 0.0);
      }
    } catch (e) {
      debugPrint('Failed to load Live2D model: $e');
    }
  }

  /// 播放说话动画
  Future<void> playSpeakAnimation() async {
    if (!_modelLoaded) return;
    
    try {
      if (!kIsWeb && Platform.isAndroid) {
        // 播放随机说话动作
        final randomIndex = DateTime.now().millisecondsSinceEpoch % 3;
        await FlutterLive2d.startMotion("talk", randomIndex);
      }
    } catch (e) {
      debugPrint('Failed to play speak animation: $e');
    }
  }

  /// 播放待机动画
  Future<void> playIdleAnimation() async {
    if (!_modelLoaded) return;
    
    try {
      if (!kIsWeb && Platform.isAndroid) {
        // 播放待机动画
        await FlutterLive2d.startMotion("idle", 0);
      }
    } catch (e) {
      debugPrint('Failed to play idle animation: $e');
    }
  }

  /// 设置表情
  Future<void> setExpression(String expression) async {
    if (!_modelLoaded) return;
    
    try {
      if (!kIsWeb && Platform.isAndroid) {
        await FlutterLive2d.setExpression(expression);
      }
    } catch (e) {
      debugPrint('Failed to set expression: $e');
    }
  }

  /// 是否已经初始化
  bool get initialized => _initialized;

  /// 是否已加载模型
  bool get modelLoaded => _modelLoaded;
}