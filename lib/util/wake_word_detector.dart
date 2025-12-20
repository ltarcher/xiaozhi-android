import 'dart:async';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:record/record.dart';
import 'package:opus_flutter/opus_flutter.dart' as opus_flutter;
import 'package:xiaozhi/util/shared_preferences_util.dart';
import 'package:xiaozhi/model/websocket_message.dart';
import 'package:path_provider/path_provider.dart';
import 'package:intl/intl.dart';

/// 唤醒词检测器
class WakeWordDetector {
  static const String TAG = 'WakeWordDetector';
  
  /// 录音器
  AudioRecorder? _audioRecorder;
  
  /// 录音流
  Stream<Uint8List>? _audioRecorderStream;
  
  /// 录音订阅
  StreamSubscription<Uint8List>? _audioRecorderSubscription;
  
  /// 唤醒词
  String _wakeWord = '你好，小清';
  
  /// 是否正在检测
  bool _isDetecting = false;
  
  /// 唤醒回调
  final Function()? onWakeWordDetected;
  
  /// 音频数据回调
  final Function(Uint8List)? onAudioDataReady;
  
  /// 音频参数
  final int _audioSampleRate = AudioParams.sampleRate16000;
  final int _audioChannels = AudioParams.channels1;
  final int _audioFrameDuration = AudioParams.frameDuration60;
  
  /// 缓存的音频数据
  final List<double> _audioBuffer = [];
  
  /// 最大缓存时长（秒）
  final int _maxBufferDuration = 5;
  
  /// 计算最大缓存样本数
  int get _maxBufferSamples => _maxBufferDuration * _audioSampleRate;
  
  /// 检测间隔（毫秒）
  final int _detectionInterval = 1000;
  
  /// 定时器
  Timer? _detectionTimer;
  
  /// 日志输出
  void _log(String message) {
    if (kDebugMode) {
      print('$TAG: $message');
    }
  }
  
  WakeWordDetector({
    required this.onWakeWordDetected,
    this.onAudioDataReady,
  });
  
  /// 初始化
  Future<void> initialize() async {
    try {
      // 从持久化存储中获取唤醒词
      String? wakeWord = await SharedPreferencesUtil().getWakeWord();
      if (wakeWord != null) {
        _wakeWord = wakeWord;
      }
      
      // 初始化录音器
      _audioRecorder = AudioRecorder();
      
      // 初始化Opus
      opus_flutter.load();
      
      _log('Initialized with wake word: $_wakeWord');
    } catch (e) {
      _log('Failed to initialize: $e');
      rethrow;
    }
  }
  
  /// 开始检测
  Future<void> startDetection() async {
    if (_isDetecting) {
      _log('Already detecting');
      return;
    }
    
    try {
      _isDetecting = true;
      _audioBuffer.clear();
      
      // 开始录音
      _audioRecorderStream = (await _audioRecorder!.startStream(
        RecordConfig(
          encoder: AudioEncoder.pcm16bits,
          echoCancel: true,
          noiseSuppress: true,
          numChannels: _audioChannels,
          sampleRate: _audioSampleRate,
        ),
      ));
      
      // 监听音频数据
      if (_audioRecorderSubscription != null) {
        await _audioRecorderSubscription!.cancel();
      }
      
      _audioRecorderSubscription = _audioRecorderStream!.listen((data) {
        if (data.isNotEmpty && data.length % 2 == 0) {
          // 转换PCM数据为double列表
          List<double> samples = _pcmToDoubleList(data);
          
          // 添加到缓冲区
          _audioBuffer.addAll(samples);
          
          // 限制缓冲区大小
          if (_audioBuffer.length > _maxBufferSamples) {
            _audioBuffer.removeRange(0, _audioBuffer.length - _maxBufferSamples);
          }
        }
      });
      
      // 启动定时检测
      _startDetectionTimer();
      
      _log('Started wake word detection');
    } catch (e) {
      _log('Failed to start detection: $e');
      _isDetecting = false;
      rethrow;
    }
  }
  
  /// 停止检测
  Future<void> stopDetection() async {
    if (!_isDetecting) {
      _log('Not detecting');
      return;
    }
    
    try {
      _isDetecting = false;
      
      // 停止录音
      if (_audioRecorder != null && await _audioRecorder!.isRecording()) {
        await _audioRecorder!.stop();
      }
      
      // 取消订阅
      if (_audioRecorderSubscription != null) {
        await _audioRecorderSubscription!.cancel();
        _audioRecorderSubscription = null;
      }
      
      // 停止定时器
      if (_detectionTimer != null) {
        _detectionTimer!.cancel();
        _detectionTimer = null;
      }
      
      _log('Stopped wake word detection');
    } catch (e) {
      _log('Failed to stop detection: $e');
    }
  }
  
  /// 启动检测定时器
  void _startDetectionTimer() {
    _detectionTimer = Timer.periodic(Duration(milliseconds: _detectionInterval), (_) {
      if (_isDetecting) {
        _detectWakeWord();
      }
    });
  }
  
  /// 检测唤醒词
  Future<void> _detectWakeWord() async {
    if (_audioBuffer.length < _audioSampleRate) {
      // 缓冲区数据不足1秒，跳过检测
      return;
    }
    
    try {
      // 计算最近1秒的音频能量
      int recentSampleCount = _audioSampleRate; // 最近1秒的数据
      List<double> recentSamples = _audioBuffer.length > recentSampleCount
          ? _audioBuffer.sublist(_audioBuffer.length - recentSampleCount)
          : _audioBuffer;
      
      // 计算音频能量
      double energy = _calculateAudioEnergy(recentSamples);
      
      // 设置能量阈值
      double energyThreshold = 0.01; // 可根据实际情况调整
      
      // 检测到有声音
      if (energy > energyThreshold) {
        _log('Detected voice activity, energy: $energy (threshold: $energyThreshold)');
        _log('Audio buffer size: ${_audioBuffer.length}, samples: $recentSampleCount');
        
        // 将PCM数据转换为字节数组
        Uint8List pcmBytes = _doubleListToPcmBytes(recentSamples);
        _log('PCM data size: ${pcmBytes.length} bytes');
        
        // 保存PCM数据为WAV文件
        await _savePcmAsWav(pcmBytes);
        
        // 编码为Opus格式
        Uint8List? opusData = await _pcmToOpus(pcmBytes);
        
        if (opusData != null) {
          _log('Opus data size: ${opusData.length} bytes');
          // 发送音频数据到WebSocket服务器进行语音识别
          sendAudioToServer(opusData);
          _log('Audio data sent for wake word detection');
        } else {
          _log('Failed to encode PCM to Opus');
        }
      } else {
        _log('No voice activity detected, energy: $energy (threshold: $energyThreshold)');
      }
    } catch (e) {
      _log('Error in wake word detection: $e');
    }
  }
  
  /// 保存PCM数据为WAV文件
  Future<void> _savePcmAsWav(Uint8List pcmData) async {
    try {
      // 直接使用外部存储目录
      Directory? externalDir = await getExternalStorageDirectory();
      if (externalDir == null) {
        _log('Failed to get external storage directory');
        return;
      }
      // 创建voice目录（如果不存在）
      Directory voiceDir = Directory('${externalDir.path}/voice');
      if (!await voiceDir.exists()) {
        await voiceDir.create(recursive: true);
        _log('Created voice directory in external storage: ${voiceDir.path}');
      }
      
      // 生成带时间戳的文件名
      DateTime now = DateTime.now();
      String timestamp = DateFormat('yyyyMMdd_HHmmss').format(now);
      String fileName = 'wake_word_audio_$timestamp.wav';
      String filePath = '${voiceDir.path}/$fileName';
      
      // 创建WAV文件头
      int sampleRate = _audioSampleRate;
      int channels = _audioChannels;
      int bitsPerSample = 16;
      int byteRate = sampleRate * channels * bitsPerSample ~/ 8;
      int blockAlign = channels * bitsPerSample ~/ 8;
      int dataSize = pcmData.length;
      int fileSize = 36 + dataSize;
      
      // 创建WAV文件
      File wavFile = File(filePath);
      RandomAccessFile raf = await wavFile.open(mode: FileMode.write);
      
      // 写入WAV头
      await raf.writeString('RIFF');
      final byteData = ByteData(4);
      byteData.setUint32(0, fileSize - 8); // 文件大小 - 8
      await raf.writeFrom(byteData.buffer.asUint8List());
      await raf.writeString('WAVE');
      await raf.writeString('fmt ');
      byteData.setUint32(0, 16); // fmt块大小
      await raf.writeFrom(byteData.buffer.asUint8List());
      byteData.setUint16(0, 1); // 音频格式（PCM）
      await raf.writeFrom(byteData.buffer.asUint8List());
      byteData.setUint16(0, channels); // 声道数
      await raf.writeFrom(byteData.buffer.asUint8List());
      byteData.setUint32(0, sampleRate); // 采样率
      await raf.writeFrom(byteData.buffer.asUint8List());
      byteData.setUint32(0, byteRate); // 字节率
      await raf.writeFrom(byteData.buffer.asUint8List());
      byteData.setUint16(0, blockAlign); // 块对齐
      await raf.writeFrom(byteData.buffer.asUint8List());
      byteData.setUint16(0, bitsPerSample); // 位深度
      await raf.writeFrom(byteData.buffer.asUint8List());
      await raf.writeString('data');
      byteData.setUint32(0, dataSize); // 数据大小
      await raf.writeFrom(byteData.buffer.asUint8List());
      
      // 写入PCM数据
      await raf.writeFrom(pcmData);
      
      await raf.close();
      
      _log('WAV file saved to: $filePath');
      _log('WAV file size: ${await wavFile.length()} bytes');
      
      // 额外记录尝试的所有目录路径
      _log('Storage paths attempted:');
      _log('  External directory: ${await getExternalStorageDirectory().then((dir) => dir?.path ?? "null")}');
    } catch (e) {
      _log('Error saving WAV file: $e');
    }
  }
  
  /// 发送音频数据到服务器
  /// 这个方法需要由调用者（ChatPage）实现，因为WakeWordDetector无法直接访问ChatBloc
  void sendAudioToServer(Uint8List opusData) {
    _log('Audio data ready to send to server for wake word detection');
    // 调用回调函数，让调用者处理音频数据发送
    onAudioDataReady?.call(opusData);
  }
  
  /// 将double列表转换为PCM字节数据
  Uint8List _doubleListToPcmBytes(List<double> samples) {
    Uint8List bytes = Uint8List(samples.length * 2);
    for (int i = 0; i < samples.length; i++) {
      int sample = (samples[i] * 32767.0).round();
      // 转换为有符号16位整数
      if (sample > 32767) sample = 32767;
      if (sample < -32768) sample = -32768;
      // 小端序转换
      bytes[i * 2] = sample & 0xff;
      bytes[i * 2 + 1] = (sample >> 8) & 0xff;
    }
    return bytes;
  }
  
  /// 将PCM数据转换为Opus格式
  Future<Uint8List?> _pcmToOpus(Uint8List pcmData) async {
    try {
      // 这里需要实现PCM到Opus的转换
      // 由于Opus编码可能需要特定的库，这里简化处理
      // 在实际实现中，您需要使用适当的Opus编码库
      
      // 临时返回原始PCM数据，实际应用中应该进行Opus编码
      return pcmData;
    } catch (e) {
      _log('Error converting PCM to Opus: $e');
      return null;
    }
  }
  
  /// 计算音频能量
  double _calculateAudioEnergy(List<double> samples) {
    if (samples.isEmpty) return 0.0;
    
    double sum = 0.0;
    for (double sample in samples) {
      sum += sample * sample;
    }
    return sum / samples.length;
  }
  
  /// 检查是否检测到唤醒词
  /// 这个方法应该由调用者根据服务器返回的语音识别结果调用
  void checkWakeWordFromServerResponse(String recognizedText) {
    _log('Checking wake word from server response: "$recognizedText"');
    _log('Current wake word: "$_wakeWord"');
    _log('Recognized text lower case: "${recognizedText.toLowerCase()}"');
    _log('Wake word lower case: "${_wakeWord.toLowerCase()}"');
    
    // 检查识别结果是否包含唤醒词
    if (recognizedText.toLowerCase().contains(_wakeWord.toLowerCase())) {
      _log('Wake word detected from server response: $_wakeWord');
      _log('Triggering wake word callback...');
      onWakeWordDetected?.call();
    } else {
      _log('Wake word not detected in server response');
    }
  }
  
  /// PCM字节数据转换为double列表
  List<double> _pcmToDoubleList(Uint8List pcmData) {
    List<double> samples = [];
    for (int i = 0; i < pcmData.length - 1; i += 2) {
      // 小端序转换
      int sample = (pcmData[i + 1] << 8) | (pcmData[i] & 0xff);
      // 转换为有符号16位整数
      if (sample > 32767) sample -= 65536;
      // 转换为-1.0到1.0范围的double
      samples.add(sample / 32768.0);
    }
    return samples;
  }
  
  /// 更新唤醒词
  Future<void> updateWakeWord(String newWakeWord) async {
    _wakeWord = newWakeWord;
    await SharedPreferencesUtil().setWakeWord(newWakeWord);
    _log('Updated wake word to: $_wakeWord');
  }
  
  /// 获取当前唤醒词
  String getWakeWord() {
    return _wakeWord;
  }
  
  /// 是否正在检测
  bool get isDetecting => _isDetecting;
  
  /// 释放资源
  void dispose() {
    stopDetection();
    _audioRecorder = null;
  }
}