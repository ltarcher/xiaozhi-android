import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';

/// Android音频播放器，兼容Android 9 (API 28)
/// 使用原生Android API实现，替代taudio库
class AndroidAudioPlayer {
  static const MethodChannel _channel = MethodChannel('com.thinkerror.xiaozhi/audio_player');
  
  bool _isInitialized = false;
  bool _isPlaying = false;
  
  /// 初始化音频播放器
  Future<void> initialize() async {
    try {
      await _channel.invokeMethod('initialize');
      _isInitialized = true;
      if (kDebugMode) {
        print('AndroidAudioPlayer: Initialized successfully');
      }
    } catch (e) {
      if (kDebugMode) {
        print('AndroidAudioPlayer: Failed to initialize - $e');
      }
      throw Exception('Failed to initialize audio player: $e');
    }
  }
  
  /// 播放assets中的音频文件
  /// 类似Live2D官方演示的功能
  Future<void> playAssetAudio(String assetPath) async {
    _ensureInitialized();
    
    try {
      await _channel.invokeMethod('playAssetAudio', {'assetPath': assetPath});
      _isPlaying = true;
      if (kDebugMode) {
        print('AndroidAudioPlayer: Playing asset audio: $assetPath');
      }
    } catch (e) {
      if (kDebugMode) {
        print('AndroidAudioPlayer: Failed to play asset audio - $e');
      }
      throw Exception('Failed to play asset audio: $e');
    }
  }
  
  /// 播放PCM音频数据流
  /// 这是替代taudio的核心功能，用于实时音频播放
  Future<void> playPcmStream(Uint8List pcmData) async {
    _ensureInitialized();
    
    try {
      await _channel.invokeMethod('playPcmStream', {'pcmData': pcmData});
      _isPlaying = true;
      if (kDebugMode) {
        print('AndroidAudioPlayer: Playing PCM stream, size: ${pcmData.length}');
      }
    } catch (e) {
      if (kDebugMode) {
        print('AndroidAudioPlayer: Failed to play PCM stream - $e');
      }
      throw Exception('Failed to play PCM stream: $e');
    }
  }
  
  /// 暂停播放
  Future<void> pause() async {
    _ensureInitialized();
    
    try {
      await _channel.invokeMethod('pause');
      if (kDebugMode) {
        print('AndroidAudioPlayer: Paused');
      }
    } catch (e) {
      if (kDebugMode) {
        print('AndroidAudioPlayer: Failed to pause - $e');
      }
    }
  }
  
  /// 恢复播放
  Future<void> resume() async {
    _ensureInitialized();
    
    try {
      await _channel.invokeMethod('resume');
      _isPlaying = true;
      if (kDebugMode) {
        print('AndroidAudioPlayer: Resumed');
      }
    } catch (e) {
      if (kDebugMode) {
        print('AndroidAudioPlayer: Failed to resume - $e');
      }
    }
  }
  
  /// 停止播放
  Future<void> stop() async {
    _ensureInitialized();
    
    try {
      await _channel.invokeMethod('stop');
      _isPlaying = false;
      if (kDebugMode) {
        print('AndroidAudioPlayer: Stopped');
      }
    } catch (e) {
      if (kDebugMode) {
        print('AndroidAudioPlayer: Failed to stop - $e');
      }
    }
  }
  
  /// 设置音量 (0.0 - 1.0)
  Future<void> setVolume(double volume) async {
    _ensureInitialized();
    
    try {
      await _channel.invokeMethod('setVolume', {'volume': volume});
      if (kDebugMode) {
        print('AndroidAudioPlayer: Volume set to $volume');
      }
    } catch (e) {
      if (kDebugMode) {
        print('AndroidAudioPlayer: Failed to set volume - $e');
      }
    }
  }
  
  /// 释放资源
  Future<void> release() async {
    if (!_isInitialized) return;
    
    try {
      await _channel.invokeMethod('release');
      _isInitialized = false;
      _isPlaying = false;
      if (kDebugMode) {
        print('AndroidAudioPlayer: Released');
      }
    } catch (e) {
      if (kDebugMode) {
        print('AndroidAudioPlayer: Failed to release - $e');
      }
    }
  }
  
  /// 检查是否正在播放
  bool get isPlaying => _isPlaying;
  
  /// 检查是否已初始化
  bool get isInitialized => _isInitialized;
  
  /// 确保播放器已初始化
  void _ensureInitialized() {
    if (!_isInitialized) {
      throw Exception('Audio player is not initialized. Call initialize() first.');
    }
  }
}

/// 兼容FlutterSoundPlayer的接口包装器
/// 用于无缝替换现有的taudio音频播放器
class CompatibleAudioPlayer {
  final AndroidAudioPlayer _androidPlayer = AndroidAudioPlayer();
  bool _isOpen = false;
  bool _isPlaying = false;
  
  /// 打开播放器（兼容FlutterSoundPlayer接口）
  Future<void> openPlayer() async {
    if (!_isOpen) {
      await _androidPlayer.initialize();
      _isOpen = true;
      if (kDebugMode) {
        print('CompatibleAudioPlayer: Player opened');
      }
    }
  }
  
  /// 开始从流播放（兼容FlutterSoundPlayer接口）
  Future<void> startPlayerFromStream({
    required int sampleRate,
    required int numChannels,
    required int bufferSize,
    required String codec,
    bool interleaved = true,
  }) async {
    await openPlayer();
    _isPlaying = true;
    if (kDebugMode) {
      print('CompatibleAudioPlayer: Started player from stream - '
            'sampleRate: $sampleRate, channels: $numChannels, bufferSize: $bufferSize');
    }
  }
  
  /// 向流中写入音频数据（兼容FlutterSoundPlayer接口）
  Future<void> write(Uint8List data) async {
    if (!_isOpen || !_isPlaying) {
      await startPlayerFromStream(
        sampleRate: 16000,
        numChannels: 1,
        bufferSize: 1024,
        codec: 'pcm16',
        interleaved: false,
      );
    }
    
    await _androidPlayer.playPcmStream(data);
  }
  
  /// 检查播放器是否打开（兼容FlutterSoundPlayer接口）
  bool get isOpen => _isOpen;
  
  /// 检查是否正在播放（兼容FlutterSoundPlayer接口）
  bool get isPlaying => _isPlaying;
  
  /// 停止播放器（兼容FlutterSoundPlayer接口）
  Future<void> stopPlayer() async {
    await _androidPlayer.stop();
    _isPlaying = false;
    if (kDebugMode) {
      print('CompatibleAudioPlayer: Player stopped');
    }
  }
  
  /// 释放播放器（兼容FlutterSoundPlayer接口）
  Future<void> closePlayer() async {
    await _androidPlayer.release();
    _isOpen = false;
    _isPlaying = false;
    if (kDebugMode) {
      print('CompatibleAudioPlayer: Player closed');
    }
  }
  
  /// 用于兼容FlutterSoundPlayer的uint8ListSink
  StreamController<Uint8List>? _sinkController;
  
  /// 获取uint8ListSink（兼容FlutterSoundPlayer接口）
  StreamController<Uint8List>? get uint8ListSink {
    if (_sinkController == null) {
      _sinkController = StreamController<Uint8List>();
      _sinkController!.stream.listen((data) async {
        await write(data);
      });
    }
    return _sinkController;
  }
}