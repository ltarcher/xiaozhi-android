import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_live2d/flutter_live2d.dart';

class Live2DManager {
  static final Live2DManager _instance = Live2DManager._internal();
  factory Live2DManager() => _instance;
  Live2DManager._internal();

  bool _isInitialized = false;
  bool _isModelLoaded = false;

  /// 初始化Live2D引擎
  Future<bool> initialize() async {
    if (_isInitialized) return true;

    try {
      await FlutterLive2d.initLive2d();
      _isInitialized = true;
      debugPrint('Live2D engine initialized successfully');
      return true;
    } catch (e) {
      debugPrint('Failed to initialize Live2D engine: $e');
      return false;
    }
  }

  /// 加载指定模型
  Future<bool> loadModel(String modelPath) async {
    if (!_isInitialized) {
      final initSuccess = await initialize();
      if (!initSuccess) return false;
    }

    if (_isModelLoaded) return true;

    try {
      await FlutterLive2d.loadModel(modelPath);
      _isModelLoaded = true;
      debugPrint('Live2D model loaded successfully: $modelPath');
      return true;
    } catch (e) {
      debugPrint('Failed to load Live2D model: $e');
      return false;
    }
  }

  /// 设置模型缩放
  Future<void> setScale(double scale) async {
    if (!_isInitialized) return;
    try {
      await FlutterLive2d.setScale(scale);
    } catch (e) {
      debugPrint('Failed to set Live2D scale: $e');
    }
  }

  /// 设置模型位置
  Future<void> setPosition(double x, double y) async {
    if (!_isInitialized) return;
    try {
      await FlutterLive2d.setPosition(x, y);
    } catch (e) {
      debugPrint('Failed to set Live2D position: $e');
    }
  }

  /// 播放指定动作
  Future<void> startMotion(String group, int index) async {
    if (!_isInitialized || !_isModelLoaded) return;
    try {
      await FlutterLive2d.startMotion(group, index);
    } catch (e) {
      debugPrint('Failed to start Live2D motion: $e');
    }
  }

  /// 设置表情
  Future<void> setExpression(String expression) async {
    if (!_isInitialized || !_isModelLoaded) return;
    try {
      await FlutterLive2d.setExpression(expression);
    } catch (e) {
      debugPrint('Failed to set Live2D expression: $e');
    }
  }

  /// 检查是否已初始化
  bool get isInitialized => _isInitialized;

  /// 检查模型是否已加载
  bool get isModelLoaded => _isModelLoaded;
  
  /// 重置状态
  void reset() {
    _isInitialized = false;
    _isModelLoaded = false;
  }
}