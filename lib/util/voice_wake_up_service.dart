import 'package:flutter/services.dart';
import 'package:xiaozhi/util/shared_preferences_util.dart';

/// 语音唤醒服务，用于处理基于Vosk的语音唤醒功能
class VoiceWakeUpService {
  static const MethodChannel _channel = MethodChannel('voice_wakeup_channel');
  static VoiceWakeUpService? _instance;
  
  // 事件回调
  Function(String)? onWakeWordDetected;
  Function(String)? onError;
  
  VoiceWakeUpService._internal();
  
  factory VoiceWakeUpService() {
    _instance ??= VoiceWakeUpService._internal();
    return _instance!;
  }
  
  /// 初始化语音唤醒服务
  Future<bool> initialize() async {
    try {
      // 设置方法调用处理器
      _channel.setMethodCallHandler(_handleMethodCall);
      
      // 初始化模型
      final bool result = await _channel.invokeMethod('initializeModel');
      
      // 加载保存的唤醒词
      await loadSavedWakeWord();
      
      return result;
    } catch (e) {
      print('VoiceWakeUpService: Failed to initialize: $e');
      return false;
    }
  }
  
  /// 开始监听唤醒词
  Future<bool> startListening() async {
    try {
      final bool result = await _channel.invokeMethod('startListening');
      print('VoiceWakeUpService: Start listening result: $result');
      return result;
    } catch (e) {
      print('VoiceWakeUpService: Failed to start listening: $e');
      return false;
    }
  }
  
  /// 停止监听唤醒词
  Future<bool> stopListening() async {
    try {
      final bool result = await _channel.invokeMethod('stopListening');
      print('VoiceWakeUpService: Stop listening result: $result');
      return result;
    } catch (e) {
      print('VoiceWakeUpService: Failed to stop listening: $e');
      return false;
    }
  }
  
  /// 设置唤醒词
  Future<bool> setWakeWord(String wakeWord) async {
    try {
      if (wakeWord.trim().isEmpty) {
        print('VoiceWakeUpService: Wake word cannot be empty');
        return false;
      }
      
      final bool result = await _channel.invokeMethod('setWakeWord', {
        'wakeWord': wakeWord.trim(),
      });
      
      if (result) {
        // 同时保存到SharedPreferences
        await SharedPreferencesUtil().setWakeWord(wakeWord.trim());
        print('VoiceWakeUpService: Wake word set to: $wakeWord');
      }
      
      return result;
    } catch (e) {
      print('VoiceWakeUpService: Failed to set wake word: $e');
      return false;
    }
  }
  
  /// 获取当前唤醒词
  Future<String> getWakeWord() async {
    try {
      final String? result = await _channel.invokeMethod('getWakeWord');
      return result ?? '你好，小清';
    } catch (e) {
      print('VoiceWakeUpService: Failed to get wake word: $e');
      return '你好，小清';
    }
  }
  
  /// 检查是否正在监听
  Future<bool> isListening() async {
    try {
      final bool? result = await _channel.invokeMethod('isListening');
      return result ?? false;
    } catch (e) {
      print('VoiceWakeUpService: Failed to check listening status: $e');
      return false;
    }
  }
  
  /// 从SharedPreferences加载保存的唤醒词
  Future<void> loadSavedWakeWord() async {
    try {
      final String? savedWakeWord = await SharedPreferencesUtil().getWakeWord();
      if (savedWakeWord != null && savedWakeWord.trim().isNotEmpty) {
        await setWakeWord(savedWakeWord);
        print('VoiceWakeUpService: Loaded saved wake word: $savedWakeWord');
      }
    } catch (e) {
      print('VoiceWakeUpService: Failed to load saved wake word: $e');
    }
  }
  
  /// 处理来自原生端的方法调用
  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onWakeWordDetected':
        final String hypothesis = call.arguments as String;
        print('VoiceWakeUpService: Wake word detected: $hypothesis');
        onWakeWordDetected?.call(hypothesis);
        break;
        
      case 'onError':
        final String error = call.arguments as String;
        print('VoiceWakeUpService: Error: $error');
        onError?.call(error);
        break;
        
      default:
        print('VoiceWakeUpService: Unknown method call: ${call.method}');
        break;
    }
  }
  
  /// 释放资源
  void dispose() {
    onWakeWordDetected = null;
    onError = null;
    _channel.setMethodCallHandler(null);
  }
}