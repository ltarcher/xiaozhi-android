part of 'chat_bloc.dart';

// WebSocket连接状态枚举
enum WebSocketConnectionStatus {
  disconnected,   // 未连接
  connecting,     // 连接中
  connected,      // 已连接
  reconnecting,   // 重连中
  error,          // 连接错误
}

// 授权状态枚举
enum AuthorizationStatus {
  unknown,        // 未知状态
  unauthorized,   // 未授权
  authorized,     // 已授权
}

// 录音管理状态枚举
enum RecordingStatus {
  initialized,    // 录音初始化
  recording,      // 录音中
  error,          // 录音出错
}

// 对话状态枚举
enum ConversationStatus {
  idle,           // 休闲中(未开始对话)
  recording,      // 录音中
  playing,        // 播放中
  waiting,        // 等待中(开始对话过程中等待用户录音)
}

@immutable
sealed class ChatState {
  final List<StorageMessage> messageList;
  final WebSocketConnectionStatus connectionStatus;
  final AuthorizationStatus authorizationStatus;
  final RecordingStatus recordingStatus;
  final ConversationStatus conversationStatus;

  const ChatState({
    this.messageList = const [],
    this.connectionStatus = WebSocketConnectionStatus.disconnected,
    this.authorizationStatus = AuthorizationStatus.unknown,
    this.recordingStatus = RecordingStatus.initialized,
    this.conversationStatus = ConversationStatus.idle,
  });
  
  ChatState copyWith({
    List<StorageMessage>? messageList,
    WebSocketConnectionStatus? connectionStatus,
    AuthorizationStatus? authorizationStatus,
    RecordingStatus? recordingStatus,
    ConversationStatus? conversationStatus,
  }) {
    // 由于ChatState是抽象类，这个方法由子类实现
    throw UnimplementedError('copyWith must be implemented by subclasses');
  }
}

final class ChatInitialState extends ChatState {
  final bool hasMore;

  const ChatInitialState({
    this.hasMore = true,
    super.messageList,
    super.connectionStatus,
    super.authorizationStatus,
    super.recordingStatus,
    super.conversationStatus,
  });
  
  @override
  ChatInitialState copyWith({
    List<StorageMessage>? messageList,
    WebSocketConnectionStatus? connectionStatus,
    AuthorizationStatus? authorizationStatus,
    RecordingStatus? recordingStatus,
    ConversationStatus? conversationStatus,
    bool? hasMore,
  }) {
    return ChatInitialState(
      messageList: messageList ?? this.messageList,
      connectionStatus: connectionStatus ?? this.connectionStatus,
      authorizationStatus: authorizationStatus ?? this.authorizationStatus,
      recordingStatus: recordingStatus ?? this.recordingStatus,
      conversationStatus: conversationStatus ?? this.conversationStatus,
      hasMore: hasMore ?? this.hasMore,
    );
  }
}

final class ChatNoMicrophonePermissionState extends ChatState {
  const ChatNoMicrophonePermissionState({
    super.messageList,
    super.connectionStatus,
    super.authorizationStatus,
    super.recordingStatus,
    super.conversationStatus,
  });
  
  @override
  ChatNoMicrophonePermissionState copyWith({
    List<StorageMessage>? messageList,
    WebSocketConnectionStatus? connectionStatus,
    AuthorizationStatus? authorizationStatus,
    RecordingStatus? recordingStatus,
    ConversationStatus? conversationStatus,
  }) {
    return ChatNoMicrophonePermissionState(
      messageList: messageList ?? this.messageList,
      connectionStatus: connectionStatus ?? this.connectionStatus,
      authorizationStatus: authorizationStatus ?? this.authorizationStatus,
      recordingStatus: recordingStatus ?? this.recordingStatus,
      conversationStatus: conversationStatus ?? this.conversationStatus,
    );
  }
}
