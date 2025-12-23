# WebSocket 协议调试指南

## 概述

本文档说明如何使用新添加的调试输出功能来分析和调试 WebSocket 对话协议。

## 对话协议结构

当前 WebSocket 协议使用 JSON 格式的消息，包含以下主要字段：

```dart
class WebsocketMessage {
  final String type;          // 消息类型: 'hello', 'stt', 'tts', 'llm', 'iot', 'listen'
  final String? transport;    // 传输方式: 'websocket'
  final int? version;         // 协议版本
  final AudioParams? audioParams; // 音频参数
  final String? sessionId;    // 会话ID
  final String? state;        // 状态: 'start', 'stop', 'sentence_start', 'sentence_end'
  final String? emotion;      // 情感: 'happy'
  final String? text;         // 文本内容
  final String? mode;         // 模式: 'auto', 'manual', 'realtime'
}
```

## 调试输出功能

### 接收消息调试

当应用接收到 WebSocket 消息时，会输出以下调试信息：

1. **原始消息**:
   ```
   ___DEBUG Received WebSocket message: {"type": "stt", "text": "你好", ...}
   ```

2. **解析后的消息**:
   ```
   ___DEBUG Parsed message - Type: stt, State: null, Text: 你好, SessionId: abc123
   ```

3. **会话ID更新**:
   ```
   ___DEBUG Updated session ID: abc123
   ```

4. **音频参数更新**:
   ```
   ___DEBUG Updated audio params - SampleRate: 16000, Channels: 1, FrameDuration: 60
   ```

5. **STT消息处理**:
   ```
   ___DEBUG Processing STT message: 你好
   ___DEBUG Adding user message to chat: 你好
   ```

6. **TTS消息处理**:
   ```
   ___DEBUG Processing TTS sentence start: 你好，我是小清
   ___DEBUG Processing TTS stop message, restarting listen
   ```

### 发送消息调试

当应用发送 WebSocket 消息时，会输出以下调试信息：

1. **Hello消息**:
   ```
   ___DEBUG Sending hello message: {"type": "hello", "transport": "websocket", "audio_params": {...}}
   ```

2. **Listen消息**:
   ```
   ___DEBUG Sending listen message: {"type": "listen", "session_id": "abc123", "state": "start", "mode": "auto"}
   ```

3. **音频数据**:
   ```
   ___DEBUG Sending audio data: 1024 bytes
   ```

## 如何查看调试输出

### 在 Android Studio 中

1. 运行应用
2. 打开 Android Studio 的 **Logcat** 窗口
3. 在过滤器中输入 `___DEBUG` 来筛选调试日志
4. 观察 WebSocket 消息的发送和接收情况

### 在命令行中

```bash
# 使用 adb logcat 查看调试输出
adb logcat | grep "___DEBUG"
```

## 调试流程示例

以下是一个典型的对话流程及对应的调试输出：

1. **连接建立**:
   ```
   ___DEBUG Sending hello message: {"type": "hello", "transport": "websocket", "audio_params": {"sample_rate": 16000, "channels": 1, "frame_duration": 60, "format": "opus"}}
   ```

2. **开始录音**:
   ```
   ___DEBUG Sending listen message: {"type": "listen", "session_id": null, "state": "start", "mode": "auto"}
   ```

3. **发送音频数据**:
   ```
   ___DEBUG Sending audio data: 1024 bytes
   ___DEBUG Sending audio data: 1024 bytes
   ...
   ```

4. **接收STT结果**:
   ```
   ___DEBUG Received WebSocket message: {"type": "stt", "text": "今天天气怎么样"}
   ___DEBUG Parsed message - Type: stt, State: null, Text: 今天天气怎么样, SessionId: abc123
   ___DEBUG Processing STT message: 今天天气怎么样
   ___DEBUG Adding user message to chat: 今天天气怎么样
   ```

5. **接收TTS响应**:
   ```
   ___DEBUG Received WebSocket message: {"type": "tts", "state": "sentence_start", "text": "今天天气晴朗"}
   ___DEBUG Parsed message - Type: tts, State: sentence_start, Text: 今天天气晴朗, SessionId: abc123
   ___DEBUG Processing TTS sentence start: 今天天气晴朗
   ```

6. **TTS结束**:
   ```
   ___DEBUG Received WebSocket message: {"type": "tts", "state": "stop"}
   ___DEBUG Parsed message - Type: tts, State: stop, Text: null, SessionId: abc123
   ___DEBUG Processing TTS stop message, restarting listen
   ```

## 注意事项

1. 调试输出仅在调试模式 (`kDebugMode`) 下启用，不会影响发布版本的性能
2. 所有调试日志都以 `___DEBUG` 为前缀，便于筛选
3. 敏感信息（如认证令牌）不会在调试输出中显示
4. 音频数据只显示大小，不显示实际内容，以避免日志过大

## 常见问题排查

1. **连接失败**: 检查是否发送了 hello 消息以及服务器响应
2. **录音无响应**: 检查是否发送了 listen 消息
3. **语音识别无结果**: 检查是否收到了 STT 消息
4. **无语音播放**: 检查是否收到了 TTS 消息及其内容