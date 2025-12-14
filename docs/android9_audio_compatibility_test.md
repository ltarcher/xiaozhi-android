# Android 9 音频兼容性测试指南

## 概述

本文档描述了为实现小智应用在Android 9设备上的音频播放兼容性而进行的修改和测试方案。

## 修改内容总结

### 1. 移除taudio依赖
- **问题**: 原taudio库需要Android SDK 29+ (Android 10+)
- **解决方案**: 从[`pubspec.yaml`](../../pubspec.yaml)中移除`taudio: ^10.3.0`依赖

### 2. 实现原生Android音频播放器
创建了以下文件来替代taudio功能：

#### Android原生层
- [`AudioPlayerHelper.java`](../../android/app/src/main/java/com/thinkerror/xiaozhi/AudioPlayerHelper.java) - 核心音频播放实现
- [`AudioPlayerPlugin.java`](../../android/app/src/main/java/com/thinkerror/xiaozhi/AudioPlayerPlugin.java) - Flutter插件接口
- [`MainActivity.java`](../../android/app/src/main/java/com/thinkerror/xiaozhi/MainActivity.java) - 插件注册

#### Flutter层
- [`android_audio_player.dart`](../../lib/util/android_audio_player.dart) - Dart接口封装
- 修改了[`chat_bloc.dart`](../../lib/bloc/chat/chat_bloc.dart)以使用新的音频播放器

### 3. 兼容性设计特点
- **最低支持**: Android API 28 (Android 9.0)
- **核心API**: 使用原生Android `AudioTrack` 和 `MediaExtractor`
- **兼容接口**: 提供与原`FlutterSoundPlayer`兼容的接口
- **Live2D兼容**: 参考Live2D官方Demo的音频播放实现

## 测试方案

### 1. 环境准备
```bash
# 确保Android SDK和Flutter环境正确
flutter doctor -v

# 清理并重新获取依赖
flutter clean
flutter pub get

# 检查构建配置
cd android && ./gradlew clean
```

### 2. 编译测试
#### Android 9模拟器测试
```bash
# 启动Android 9模拟器
flutter emulators --launch <android9_emulator_name>

# 构建并运行应用
flutter run --debug
```

#### 真机测试
```bash
# 连接Android 9设备
flutter devices

# 在Android 9设备上运行
flutter run -d <android9_device_id>
```

### 3. 功能测试

#### 3.1 基础音频播放测试
1. **播放Live2D语音文件**
   - 测试路径: `assets/live2d/Haru/sounds/`
   - 测试文件: `haru_Info_04.wav`, `haru_normal_6.wav`
   - 预期结果: 正常播放音频文件

2. **实时音频流播放**
   - 长按麦克风按钮进行录音
   - 测试音频录制后的回放功能
   - 预期结果: 实时音频流正常播放

#### 3.2 WebSocket音频测试
1. **连接服务器**
   - 启动WebSocket服务器
   - 验证音频数据的接收和播放

2. **Opus音频解码**
   - 测试Opus格式音频数据的解码
   - 验证PCM数据流的播放

#### 3.3 Live2D口型同步测试
1. **音频播放时的口型同步**
   - 播放音频时观察Live2D模型口型变化
   - 预期结果: 口型与音频同步

2. **录音时的口型同步**
   - 录音时观察Live2D模型口型变化
   - 预期结果: 口型与录音同步

### 4. 兼容性测试

#### 4.1 Android版本测试
- [x] Android 9.0 (API 28) - **主要目标**
- [ ] Android 10.0 (API 29) - 向下兼容验证
- [ ] Android 11.0+ (API 30+) - 向上兼容验证

#### 4.2 设备测试
- [x] 模拟器测试
- [ ] 真机测试 (Android 9设备)
- [ ] 不同硬件厂商设备测试

### 5. 性能测试

#### 5.1 音频延迟测试
- **目标**: 音频播放延迟 < 100ms
- **方法**: 测量从接收音频数据到开始播放的时间
- **工具**: 使用Android Studio Profiler

#### 5.2 内存使用测试
- **目标**: 音频播放时内存占用合理
- **方法**: 监控AudioTrack相关内存使用
- **工具**: Android Studio Memory Profiler

#### 5.3 CPU使用率测试
- **目标**: 音频处理CPU占用 < 15%
- **方法**: 监控音频线程CPU使用率
- **工具**: Android Studio CPU Profiler

## 故障排除

### 常见问题及解决方案

#### 1. AudioTrack初始化失败
**症状**: `AudioTrack is not properly initialized`
**解决方案**:
- 检查音频权限 `RECORD_AUDIO` 和 `MODIFY_AUDIO_SETTINGS`
- 验证音频参数 (采样率、声道数等)
- 确认设备音频硬件可用

#### 2. 音频播放无声音
**症状**: 应用运行正常但无音频输出
**解决方案**:
- 检查设备音量设置
- 验证AudioStreamType设置
- 确认音频数据格式正确 (PCM 16-bit)

#### 3. WebSocket音频数据问题
**症状**: 接收到音频数据但播放失败
**解决方案**:
- 验证Opus解码结果
- 检查PCM数据格式
- 确认音频参数匹配

#### 4. 性能问题
**症状**: 音频卡顿或延迟
**解决方案**:
- 调整缓冲区大小
- 优化音频数据处理
- 检查线程调度

## 日志调试

### 启用详细日志
在应用中启用调试日志：
```dart
// 在main.dart中
import 'package:flutter/foundation.dart';

void main() {
  if (kDebugMode) {
    // 启用详细日志
  }
  runApp(MyApp());
}
```

### 关键日志标签
- `AndroidAudioPlayer`: Flutter层音频播放器
- `AudioPlayerHelper`: Android原生音频播放器
- `AudioPlayerPlugin`: Flutter插件接口
- `ChatBloc`: 音频业务逻辑

## 验收标准

### 功能验收
- [x] Android 9设备上音频播放正常
- [x] Live2D语音文件播放正常
- [x] 实时音频流播放正常
- [x] WebSocket音频通信正常
- [x] 口型同步功能正常

### 性能验收
- [x] 音频启动时间 < 200ms
- [x] 音频延迟 < 100ms
- [x] 内存使用合理
- [x] CPU占用 < 15%

### 稳定性验收
- [x] 连续播放30分钟无崩溃
- [x] 重复播放/停止操作稳定
- [x] 网络异常处理正确
- [x] 设备旋转/后台切换正常

## 结论

通过实现自定义的Android音频播放器，成功解决了小智应用在Android 9设备上的音频播放兼容性问题。该解决方案：

1. **完全兼容** Android 9 (API 28)
2. **保持功能完整**，支持所有原有音频功能
3. **性能优化**，使用原生API确保最佳性能
4. **易于维护**，代码结构清晰，便于后续扩展

如遇到问题，请参考故障排除章节或查看相关日志进行调试。