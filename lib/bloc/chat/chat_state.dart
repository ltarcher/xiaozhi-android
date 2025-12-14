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

@immutable
sealed class ChatState {
  final List<StorageMessage> messageList;
  final WebSocketConnectionStatus connectionStatus;
  final AuthorizationStatus authorizationStatus;

  const ChatState({
    this.messageList = const [],
    this.connectionStatus = WebSocketConnectionStatus.disconnected,
    this.authorizationStatus = AuthorizationStatus.unknown,
  });
  
  ChatState copyWith({
    List<StorageMessage>? messageList,
    WebSocketConnectionStatus? connectionStatus,
    AuthorizationStatus? authorizationStatus,
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
  });
  
  @override
  ChatInitialState copyWith({
    List<StorageMessage>? messageList,
    WebSocketConnectionStatus? connectionStatus,
    AuthorizationStatus? authorizationStatus,
    bool? hasMore,
  }) {
    return ChatInitialState(
      messageList: messageList ?? this.messageList,
      connectionStatus: connectionStatus ?? this.connectionStatus,
      authorizationStatus: authorizationStatus ?? this.authorizationStatus,
      hasMore: hasMore ?? this.hasMore,
    );
  }
}

final class ChatNoMicrophonePermissionState extends ChatState {
  const ChatNoMicrophonePermissionState({
    super.messageList,
    super.connectionStatus,
    super.authorizationStatus,
  });
  
  @override
  ChatNoMicrophonePermissionState copyWith({
    List<StorageMessage>? messageList,
    WebSocketConnectionStatus? connectionStatus,
    AuthorizationStatus? authorizationStatus,
  }) {
    return ChatNoMicrophonePermissionState(
      messageList: messageList ?? this.messageList,
      connectionStatus: connectionStatus ?? this.connectionStatus,
      authorizationStatus: authorizationStatus ?? this.authorizationStatus,
    );
  }
}
