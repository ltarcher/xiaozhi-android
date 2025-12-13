import 'dart:math';
import 'package:flutter/foundation.dart';

/// 音频处理工具类，用于分析音频并提取口型同步参数
class AudioProcessor {
  static const String TAG = 'AudioProcessor';

  /// 音频能量阈值，低于此值认为是静音
  final double _energyThreshold;

  /// 平滑因子，用于平滑口型同步值的变化 (0.0 - 1.0)
  final double _smoothingFactor;

  /// 上一个口型同步值，用于平滑处理
  double _lastLipSyncValue = 0.0;

  /// 音频采样率
  final int _sampleRate;

  AudioProcessor({
    double energyThreshold = 0.01,
    double smoothingFactor = 0.3,
    int sampleRate = 16000,
  })  : _energyThreshold = energyThreshold,
        _smoothingFactor = smoothingFactor,
        _sampleRate = sampleRate;

  /// 处理音频数据并计算口型同步值
  ///
  /// [audioData] 音频数据，通常是从麦克风或其他音频源获取的PCM数据
  /// 返回值范围在 0.0 到 1.0 之间，表示口型开合程度
  double processAudio(List<double> audioData) {
    if (audioData.isEmpty) {
      return _smoothValue(0.0);
    }

    // 计算音频能量(RMS)
    double energy = _calculateEnergy(audioData);

    // 将能量转换为口型同步值
    double lipSyncValue = _energyToLipSyncValue(energy);

    // 应用平滑处理
    return _smoothValue(lipSyncValue);
  }

  /// 计算音频能量(RMS)
  double _calculateEnergy(List<double> audioData) {
    double sum = 0.0;
    for (double sample in audioData) {
      sum += sample * sample;
    }
    return sqrt(sum / audioData.length);
  }

  /// 将音频能量转换为口型同步值
  double _energyToLipSyncValue(double energy) {
    // 如果能量低于阈值，认为是静音
    if (energy < _energyThreshold) {
      return 0.0;
    }

    // 将能量映射到 0.0 - 1.0 范围
    // 使用对数函数使低音量变化更敏感
    double normalizedEnergy = min(1.0, energy / 0.5);
    double logEnergy = log(normalizedEnergy + 1) / log(2);
    return min(1.0, max(0.0, logEnergy));
  }

  /// 平滑口型同步值，避免剧烈变化
  double _smoothValue(double currentValue) {
    double smoothedValue = _lastLipSyncValue +
        _smoothingFactor * (currentValue - _lastLipSyncValue);
    _lastLipSyncValue = smoothedValue;
    return smoothedValue;
  }

  /// 重置处理器状态
  void reset() {
    _lastLipSyncValue = 0.0;
  }

  /// 处理短时傅里叶变换(STFT)数据并提取口型同步参数
  ///
  /// [frequencyData] 频率数据，通常是STFT的结果
  /// [formantFrequencies] 共振峰频率列表，例如 [800, 1200, 2800] Hz
  /// 返回值范围在 0.0 到 1.0 之间
  double processFrequencyData(List<double> frequencyData, List<int> formantFrequencies) {
    if (frequencyData.isEmpty || formantFrequencies.isEmpty) {
      return _smoothValue(0.0);
    }

    // 计算共振峰区域的能量
    double formantEnergy = _calculateFormantEnergy(frequencyData, formantFrequencies);

    // 将共振峰能量转换为口型同步值
    double lipSyncValue = _formantEnergyToLipSyncValue(formantEnergy);

    // 应用平滑处理
    return _smoothValue(lipSyncValue);
  }

  /// 计算共振峰区域的能量
  double _calculateFormantEnergy(List<double> frequencyData, List<int> formantFrequencies) {
    double totalEnergy = 0.0;
    int sampleCount = 0;

    // 对每个共振峰频率计算周围区域的能量
    for (int formantFreq in formantFrequencies) {
      // 计算频率索引（假设频率数据是均匀分布的）
      int centerIndex = ((formantFreq * frequencyData.length) ~/ (_sampleRate ~/ 2)).clamp(0, frequencyData.length - 1);
      
      // 计算周围几个频率点的能量
      int windowSize = 5; // 窗口大小
      for (int i = centerIndex - windowSize; i <= centerIndex + windowSize; i++) {
        if (i >= 0 && i < frequencyData.length) {
          totalEnergy += frequencyData[i] * frequencyData[i];
          sampleCount++;
        }
      }
    }

    if (sampleCount == 0) return 0.0;
    return sqrt(totalEnergy / sampleCount);
  }

  /// 将共振峰能量转换为口型同步值
  double _formantEnergyToLipSyncValue(double formantEnergy) {
    // 如果能量低于阈值，认为是静音
    if (formantEnergy < _energyThreshold) {
      return 0.0;
    }

    // 将能量映射到 0.0 - 1.0 范围
    double normalizedEnergy = min(1.0, formantEnergy / 0.3);
    return min(1.0, max(0.0, normalizedEnergy));
  }
}

/// 口型同步控制器，用于控制Live2D模型的口型
class LipSyncController {
  static const String TAG = 'LipSyncController';

  /// 音频处理器
  final AudioProcessor _audioProcessor;

  /// 口型同步更新回调
  final Function(double lipSyncValue)? _onLipSyncUpdate;

  /// 是否正在运行
  bool _isRunning = false;

  /// 更新间隔（毫秒）
  final int _updateInterval;

  LipSyncController({
    AudioProcessor? audioProcessor,
    Function(double lipSyncValue)? onLipSyncUpdate,
    int updateInterval = 50, // 默认每50毫秒更新一次
  })  : _audioProcessor = audioProcessor ?? AudioProcessor(),
        _onLipSyncUpdate = onLipSyncUpdate,
        _updateInterval = updateInterval;

  /// 开始口型同步
  void start() {
    if (_isRunning) return;
    _isRunning = true;
    if (kDebugMode) {
      print('$TAG: LipSyncController started');
    }
    _processLoop();
  }

  /// 停止口型同步
  void stop() {
    _isRunning = false;
    _audioProcessor.reset();
    if (kDebugMode) {
      print('$TAG: LipSyncController stopped');
    }
  }

  /// 模拟处理循环（实际应用中应该从音频源获取数据）
  void _processLoop() async {
    if (!_isRunning) return;

    // 模拟获取音频数据
    List<double> dummyAudioData = _generateDummyAudioData();

    // 处理音频数据
    double lipSyncValue = _audioProcessor.processAudio(dummyAudioData);

    // 触发更新回调
    _onLipSyncUpdate?.call(lipSyncValue);

    // 延迟后继续处理
    await Future.delayed(Duration(milliseconds: _updateInterval));
    _processLoop();
  }

  /// 生成模拟音频数据（实际应用中应从麦克风等源获取真实数据）
  List<double> _generateDummyAudioData() {
    Random random = Random();
    List<double> data = [];
    int sampleCount = 1024; // 模拟1024个采样点
    
    for (int i = 0; i < sampleCount; i++) {
      // 生成随机音频样本（-1.0 到 1.0）
      data.add((random.nextDouble() * 2.0) - 1.0);
    }
    
    return data;
  }

  /// 处理真实的音频数据
  void processAudioData(List<double> audioData) {
    if (!_isRunning) return;

    double lipSyncValue = _audioProcessor.processAudio(audioData);
    _onLipSyncUpdate?.call(lipSyncValue);
  }
}