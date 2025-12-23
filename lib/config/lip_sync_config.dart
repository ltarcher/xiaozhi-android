/// 口型同步配置类
class LipSyncConfig {
  /// 单例实例
  static final LipSyncConfig _instance = LipSyncConfig._internal();
  factory LipSyncConfig() => _instance;
  LipSyncConfig._internal();

  /// 音频能量阈值，低于此值认为是静音
  double energyThreshold = 0.003;
  
  /// 平滑因子，用于平滑口型同步值的变化 (0.0 - 1.0)
  double smoothingFactor = 0.05;
  
  /// 口型夸张程度 (1.0-3.0, 越大越夸张)
  double exaggerationLevel = 2.0;
  
  /// 低音量敏感度 (0.1-1.0, 越大越敏感)
  double lowVolumeSensitivity = 0.3;
  
  /// 高音量敏感度 (1.0-5.0, 越大越敏感)
  double highVolumeSensitivity = 8.0;
  
  /// S型曲线陡峭度 (1.0-10.0, 越大越陡峭)
  double sigmoidSteepness = 8.0;
  
  /// 更新频率 (每秒更新次数)
  int updateFrequency = 90;
  
  /// 是否启用重叠窗口平滑
  bool enableOverlapSmoothing = true;
  
  /// 重置为默认值
  void resetToDefaults() {
    energyThreshold = 0.003;
    smoothingFactor = 0.05;
    exaggerationLevel = 2.0;
    lowVolumeSensitivity = 0.3;
    highVolumeSensitivity = 8.0;
    sigmoidSteepness = 8.0;
    updateFrequency = 90;
    enableOverlapSmoothing = true;
  }
  
  /// 设置为更加夸张的口型
  void setMoreExaggerated() {
    energyThreshold = 0.001;
    smoothingFactor = 0.03;
    exaggerationLevel = 3.0;
    lowVolumeSensitivity = 0.2;
    highVolumeSensitivity = 10.0;
    sigmoidSteepness = 10.0;
    updateFrequency = 120;
    enableOverlapSmoothing = true;
  }
  
  /// 设置为更加自然的口型
  void setMoreNatural() {
    energyThreshold = 0.01;
    smoothingFactor = 0.2;
    exaggerationLevel = 1.0;
    lowVolumeSensitivity = 0.5;
    highVolumeSensitivity = 4.0;
    sigmoidSteepness = 4.0;
    updateFrequency = 60;
    enableOverlapSmoothing = false;
  }
}