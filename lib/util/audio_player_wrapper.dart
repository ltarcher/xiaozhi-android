import 'dart:async';
import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:taudio/public/fs/flutter_sound.dart' as taudio;
import 'package:xiaozhi/util/android_version_util.dart';

/// 音频播放器包装类，根据Android版本选择合适的播放器实现
abstract class AudioPlayerWrapper {
  /// 打开播放器
  Future<void> openPlayer();
  
  /// 检查播放器是否已打开
  bool isOpen();
  
  /// 检查播放器是否正在播放
  bool get isPlaying;
  
  /// 从流开始播放
  Future<void> startPlayerFromStream({
    required taudio.Codec codec,
    required bool interleaved,
    required int numChannels,
    required int sampleRate,
    required int bufferSize,
  });
  
  /// 获取数据流接收器
  StreamSink<Uint8List>? get uint8ListSink;
  
  /// 停止播放器
  Future<void> stopPlayer();
  
  /// 关闭播放器
  Future<void> closePlayer();
  
  /// 释放资源
  Future<void> release();
}

/// Android 10+版本的taudio实现
class TaudioPlayerWrapper extends AudioPlayerWrapper {
  taudio.FlutterSoundPlayer? _player;
  
  TaudioPlayerWrapper() {
    _player = taudio.FlutterSoundPlayer();
  }
  
  @override
  Future<void> openPlayer() async {
    if (_player != null && !_player!.isOpen()) {
      await _player!.openPlayer();
    }
  }
  
  @override
  bool isOpen() {
    return _player?.isOpen() ?? false;
  }
  
  @override
  bool get isPlaying {
    return _player?.isPlaying ?? false;
  }
  
  @override
  Future<void> startPlayerFromStream({
    required taudio.Codec codec,
    required bool interleaved,
    required int numChannels,
    required int sampleRate,
    required int bufferSize,
  }) async {
    if (_player != null) {
      await _player!.startPlayerFromStream(
        codec: codec,
        interleaved: interleaved,
        numChannels: numChannels,
        sampleRate: sampleRate,
        bufferSize: bufferSize,
      );
    }
  }
  
  @override
  StreamSink<Uint8List>? get uint8ListSink {
    return _player?.uint8ListSink;
  }
  
  @override
  Future<void> stopPlayer() async {
    if (_player != null) {
      await _player!.stopPlayer();
    }
  }
  
  @override
  Future<void> closePlayer() async {
    if (_player != null) {
      await _player!.closePlayer();
    }
  }
  
  @override
  Future<void> release() async {
    _player = null;
  }
}

/// Android 9兼容版本的音频播放器实现
/// 使用更基础的音频API来确保兼容性
class CompatibleAudioPlayerWrapper extends AudioPlayerWrapper {
  bool _isOpen = false;
  bool _isPlaying = false;
  StreamController<Uint8List>? _streamController;
  String? _tempFilePath;
  Timer? _playbackTimer;
  List<Uint8List> _audioBuffer = [];
  static const MethodChannel _channel = MethodChannel('xiaozhi/audio_player');
  
  @override
  Future<void> openPlayer() async {
    if (kDebugMode) {
      print('CompatibleAudioPlayer: Opening player (Android 9 compatibility mode)');
    }
    _isOpen = true;
    _streamController = StreamController<Uint8List>.broadcast();
    _audioBuffer.clear();
    
    // 初始化原生音频播放器
    try {
      await _channel.invokeMethod('initialize');
    } catch (e) {
      if (kDebugMode) {
        print('CompatibleAudioPlayer: Failed to initialize native player: $e');
      }
    }
  }
  
  @override
  bool isOpen() {
    return _isOpen;
  }
  
  @override
  bool get isPlaying {
    return _isPlaying;
  }
  
  @override
  Future<void> startPlayerFromStream({
    required taudio.Codec codec,
    required bool interleaved,
    required int numChannels,
    required int sampleRate,
    required int bufferSize,
  }) async {
    if (kDebugMode) {
      print('CompatibleAudioPlayer: Starting player from stream (Android 9 compatibility mode)');
      print('  - Codec: ${codec.toString()}');
      print('  - Channels: $numChannels');
      print('  - Sample Rate: $sampleRate');
      print('  - Buffer Size: $bufferSize');
    }
    
    _isPlaying = true;
    
    // 在Android 9上，我们需要使用更基础的音频播放方法
    // 将PCM数据收集到缓冲区，然后通过原生方法播放
    _streamController?.stream.listen((data) {
      _audioBuffer.add(data);
      _processAudioBuffer();
    });
    
    // 尝试使用原生方法播放
    try {
      await _channel.invokeMethod('start', {
        'sampleRate': sampleRate,
        'channels': numChannels,
        'bufferSize': bufferSize,
      });
    } catch (e) {
      if (kDebugMode) {
        print('CompatibleAudioPlayer: Failed to start native player: $e');
        // 回退到临时文件方案
        _startFilePlayback(sampleRate, numChannels);
      }
    }
  }
  
  /// 处理音频缓冲区
  void _processAudioBuffer() {
    if (_audioBuffer.isEmpty || !_isPlaying) return;
    
    // 将缓冲区数据发送到原生播放器
    try {
      final data = _audioBuffer.removeAt(0);
      _channel.invokeMethod('writeAudio', {'data': data});
    } catch (e) {
      if (kDebugMode) {
        print('CompatibleAudioPlayer: Error writing audio data: $e');
      }
    }
  }
  
  /// 回退方案：使用临时文件播放
  Future<void> _startFilePlayback(int sampleRate, int channels) async {
    if (kDebugMode) {
      print('CompatibleAudioPlayer: Using file playback fallback');
    }
    
    try {
      // 获取临时目录
      final directory = await getTemporaryDirectory();
      _tempFilePath = '${directory.path}/temp_audio_${DateTime.now().millisecondsSinceEpoch}.wav';
      
      // 创建WAV文件头
      final header = _createWavHeader(sampleRate, channels, 16);
      
      // 将缓冲区数据写入文件
      final file = File(_tempFilePath!);
      final sink = file.openWrite();
      
      // 写入WAV头
      sink.add(header);
      
      // 写入音频数据
      for (final data in _audioBuffer) {
        sink.add(data);
      }
      
      await sink.close();
      
      // 使用平台方法播放文件
      await _channel.invokeMethod('playFile', {'path': _tempFilePath});
      
      // 设置定时器检查播放状态
      _playbackTimer = Timer.periodic(Duration(milliseconds: 100), (timer) async {
        try {
          final isPlaying = await _channel.invokeMethod('isPlaying');
          if (!isPlaying) {
            timer.cancel();
            _isPlaying = false;
            await _cleanupTempFile();
          }
        } catch (e) {
          if (kDebugMode) {
            print('CompatibleAudioPlayer: Error checking playback status: $e');
          }
        }
      });
    } catch (e) {
      if (kDebugMode) {
        print('CompatibleAudioPlayer: Error in file playback: $e');
      }
    }
  }
  
  /// 创建WAV文件头
  Uint8List _createWavHeader(int sampleRate, int channels, int bitsPerSample) {
    final byteData = ByteData(44);
    
    // RIFF header
    byteData.setUint8(0, 0x52); // 'R'
    byteData.setUint8(1, 0x49); // 'I'
    byteData.setUint8(2, 0x46); // 'F'
    byteData.setUint8(3, 0x46); // 'F'
    
    // File size (will be updated later)
    byteData.setUint32(4, 0, Endian.little);
    
    // WAVE header
    byteData.setUint8(8, 0x57); // 'W'
    byteData.setUint8(9, 0x41); // 'A'
    byteData.setUint8(10, 0x56); // 'V'
    byteData.setUint8(11, 0x45); // 'E'
    
    // fmt chunk
    byteData.setUint8(12, 0x66); // 'f'
    byteData.setUint8(13, 0x6D); // 'm'
    byteData.setUint8(14, 0x74); // 't'
    byteData.setUint8(15, 0x20); // ' '
    
    // Chunk size
    byteData.setUint32(16, 16, Endian.little);
    
    // Audio format (PCM)
    byteData.setUint16(20, 1, Endian.little);
    
    // Number of channels
    byteData.setUint16(22, channels, Endian.little);
    
    // Sample rate
    byteData.setUint32(24, sampleRate, Endian.little);
    
    // Byte rate
    byteData.setUint32(28, sampleRate * channels * bitsPerSample ~/ 8, Endian.little);
    
    // Block align
    byteData.setUint16(32, channels * bitsPerSample ~/ 8, Endian.little);
    
    // Bits per sample
    byteData.setUint16(34, bitsPerSample, Endian.little);
    
    // data chunk
    byteData.setUint8(36, 0x64); // 'd'
    byteData.setUint8(37, 0x61); // 'a'
    byteData.setUint8(38, 0x74); // 't'
    byteData.setUint8(39, 0x61); // 'a'
    
    // Data size (will be updated later)
    byteData.setUint32(40, 0, Endian.little);
    
    return byteData.buffer.asUint8List();
  }
  
  /// 清理临时文件
  Future<void> _cleanupTempFile() async {
    if (_tempFilePath != null) {
      try {
        final file = File(_tempFilePath!);
        if (await file.exists()) {
          await file.delete();
        }
      } catch (e) {
        if (kDebugMode) {
          print('CompatibleAudioPlayer: Error cleaning up temp file: $e');
        }
      }
      _tempFilePath = null;
    }
  }
  
  @override
  StreamSink<Uint8List>? get uint8ListSink {
    return _streamController?.sink;
  }
  
  @override
  Future<void> stopPlayer() async {
    if (kDebugMode) {
      print('CompatibleAudioPlayer: Stopping player (Android 9 compatibility mode)');
    }
    _isPlaying = false;
    _playbackTimer?.cancel();
    
    try {
      await _channel.invokeMethod('stop');
    } catch (e) {
      if (kDebugMode) {
        print('CompatibleAudioPlayer: Error stopping player: $e');
      }
    }
  }
  
  @override
  Future<void> closePlayer() async {
    if (kDebugMode) {
      print('CompatibleAudioPlayer: Closing player (Android 9 compatibility mode)');
    }
    _isOpen = false;
    _isPlaying = false;
    _playbackTimer?.cancel();
    await _cleanupTempFile();
    await _streamController?.close();
    _streamController = null;
    _audioBuffer.clear();
    
    try {
      await _channel.invokeMethod('release');
    } catch (e) {
      if (kDebugMode) {
        print('CompatibleAudioPlayer: Error releasing player: $e');
      }
    }
  }
  
  @override
  Future<void> release() async {
    await closePlayer();
  }
}

/// 音频播放器工厂类
class AudioPlayerFactory {
  /// 创建适合当前Android版本的音频播放器
  static Future<AudioPlayerWrapper> createPlayer() async {
    if (await AndroidVersionUtil.isAndroid10OrHigher()) {
      if (kDebugMode) {
        print('AudioPlayerFactory: Creating TaudioPlayerWrapper for Android 10+');
      }
      return TaudioPlayerWrapper();
    } else {
      if (kDebugMode) {
        print('AudioPlayerFactory: Creating CompatibleAudioPlayerWrapper for Android 9');
      }
      return CompatibleAudioPlayerWrapper();
    }
  }
  
  /// 同步创建适合当前Android版本的音频播放器
  static AudioPlayerWrapper createPlayerSync() {
    if (AndroidVersionUtil.isAndroid10OrHigherSync()) {
      if (kDebugMode) {
        print('AudioPlayerFactory: Creating TaudioPlayerWrapper for Android 10+');
      }
      return TaudioPlayerWrapper();
    } else {
      if (kDebugMode) {
        print('AudioPlayerFactory: Creating CompatibleAudioPlayerWrapper for Android 9');
      }
      return CompatibleAudioPlayerWrapper();
    }
  }
  
  /// 获取当前使用的播放器类型描述
  static Future<String> getPlayerTypeDescription() async {
    if (await AndroidVersionUtil.isAndroid10OrHigher()) {
      return 'Taudio (FlutterSound) - Android 10+';
    } else {
      return 'Compatible Audio Player - Android 9';
    }
  }
  
  /// 同步获取当前使用的播放器类型描述
  static String getPlayerTypeDescriptionSync() {
    if (AndroidVersionUtil.isAndroid10OrHigherSync()) {
      return 'Taudio (FlutterSound) - Android 10+';
    } else {
      return 'Compatible Audio Player - Android 9';
    }
  }
}