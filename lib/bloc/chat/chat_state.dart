part of 'chat_bloc.dart';

// WebSocket连接状态枚举
enum WebSocketConnectionStatus {
  disconnected, // 未连接
  connecting,   // 连接中
  connected,    // 已连接
  reconnecting, // 重连中
  error,        // 连接错误
}

@immutable
sealed class ChatState {
  final List<StorageMessage> messageList;
  final WebSocketConnectionStatus connectionStatus;

  const ChatState({
    this.messageList = const [],
    this.connectionStatus = WebSocketConnectionStatus.disconnected,
  });
  
  ChatState copyWith({
    List<StorageMessage>? messageList,
    WebSocketConnectionStatus? connectionStatus,
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
  });
  
  @override
  ChatInitialState copyWith({
    List<StorageMessage>? messageList,
    WebSocketConnectionStatus? connectionStatus,
    bool? hasMore,
  }) {
    return ChatInitialState(
      messageList: messageList ?? this.messageList,
      connectionStatus: connectionStatus ?? this.connectionStatus,
      hasMore: hasMore ?? this.hasMore,
    );
  }
}

final class ChatNoMicrophonePermissionState extends ChatState {
  const ChatNoMicrophonePermissionState({
    super.messageList,
    super.connectionStatus,
  });
  
  @override
  ChatNoMicrophonePermissionState copyWith({
    List<StorageMessage>? messageList,
    WebSocketConnectionStatus? connectionStatus,
  }) {
    return ChatNoMicrophonePermissionState(
      messageList: messageList ?? this.messageList,
      connectionStatus: connectionStatus ?? this.connectionStatus,
    );
  }
}
