part of 'chat_bloc.dart';

@immutable
sealed class ChatEvent {}

class ChatInitialEvent extends ChatEvent {}

class ChatStartListenEvent extends ChatEvent {
  final bool isRequestMicrophonePermission;

  ChatStartListenEvent({this.isRequestMicrophonePermission = false});
}

class ChatOnMessageEvent extends ChatEvent {
  final StorageMessage message;

  ChatOnMessageEvent({required this.message});
}

class ChatLoadMoreEvent extends ChatEvent {}

class ChatStopListenEvent extends ChatEvent {}

class ChatStartCallEvent extends ChatEvent {}

class ChatStopCallEvent extends ChatEvent {}

// 添加退出唤醒模式事件
class ChatExitWakeModeEvent extends ChatEvent {}

// WebSocket连接状态事件
class ChatConnectionConnectingEvent extends ChatEvent {}

class ChatConnectionConnectedEvent extends ChatEvent {}

class ChatConnectionDisconnectedEvent extends ChatEvent {}

class ChatConnectionReconnectingEvent extends ChatEvent {}

class ChatConnectionErrorEvent extends ChatEvent {}

// 授权状态事件
class ChatUnauthorizedEvent extends ChatEvent {}

class ChatAuthorizedEvent extends ChatEvent {}

// 录音管理状态事件
class ChatRecordingInitializedEvent extends ChatEvent {}

class ChatRecordingStartedEvent extends ChatEvent {}

class ChatRecordingErrorEvent extends ChatEvent {}

// 对话状态事件
class ChatConversationIdleEvent extends ChatEvent {}

class ChatConversationRecordingEvent extends ChatEvent {}

class ChatConversationPlayingEvent extends ChatEvent {}

class ChatConversationWaitingEvent extends ChatEvent {}

// 口型同步事件
class ChatLipSyncUpdateEvent extends ChatEvent {
  final double lipSyncValue;

  ChatLipSyncUpdateEvent({required this.lipSyncValue});
}
