import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:bloc/bloc.dart';
import 'package:meta/meta.dart';
import 'package:logger/logger.dart';
import 'package:opus_dart/opus_dart.dart';
import 'package:opus_flutter/opus_flutter.dart' as opus_flutter;
import 'package:permission_handler/permission_handler.dart';
import 'package:record/record.dart';
import 'package:taudio/public/fs/flutter_sound.dart';
import 'package:uuid/uuid.dart';
import 'package:web_socket_channel/io.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:xiaozhi/common/x_const.dart';
import 'package:xiaozhi/model/storage_message.dart';
import 'package:xiaozhi/model/websocket_message.dart';
import 'package:xiaozhi/util/common_utils.dart';
import 'package:xiaozhi/util/shared_preferences_util.dart';
import 'package:xiaozhi/util/storage_util.dart';
import 'package:xiaozhi/bloc/ota/ota_bloc.dart';

part 'chat_event.dart';
part 'chat_state.dart';

class ChatBloc extends Bloc<ChatEvent, ChatState> {
  late Logger _logger;

  WebSocketChannel? _websocketChannel;

  StreamSubscription? _websocketStreamSubscription;

  AudioRecorder? _audioRecorder;

  FlutterSoundPlayer? _audioPlayer;

  Stream<Uint8List>? _audioRecorderStream;

  StreamSubscription<Uint8List>? _audioRecorderSubscription;

  String? _sessionId;

  int _audioSampleRate = AudioParams.sampleRate16000;

  int _audioChannels = AudioParams.channels1;

  int _audioFrameDuration = AudioParams.frameDuration60;

  final int _messageListPaginatedLimit = 20;

  int _messageListPaginatedOffset = 0;

  bool _isOnCall = false;
  
  // 添加重连机制相关变量
  Timer? _reconnectTimer;
  int _reconnectAttempts = 0;
  static const int _maxReconnectAttempts = 5;
  static const Duration _reconnectDelay = Duration(seconds: 3);

  @override
  Future<void> close() {
    if (null != _websocketStreamSubscription) {
      _websocketStreamSubscription!.cancel();
      _websocketStreamSubscription = null;
    }
    if (null != _audioRecorderSubscription) {
      _audioRecorderSubscription!.cancel();
      _audioRecorderSubscription = null;
    }
    if (null != _reconnectTimer) {
      _reconnectTimer!.cancel();
      _reconnectTimer = null;
    }
    if (null != _websocketChannel) {
      _websocketChannel!.sink.close();
      _websocketChannel = null;
    }
    return super.close();
  }

  void _initWebsocketListener() {
    // 确保 _websocketChannel 不为 null
    if (_websocketChannel == null) {
      _logger.e('___ERROR _websocketChannel is null');
      return;
    }
    
    _websocketStreamSubscription = _websocketChannel!.stream.listen(
      (data) async {
        try {
          if (data is String) {
            final WebsocketMessage message = WebsocketMessage.fromJson(
              jsonDecode(data),
            );

            if (null != message.sessionId) {
              _sessionId = message.sessionId;
            }
            if (null != message.audioParams) {
              _audioSampleRate = message.audioParams!.sampleRate;
              _audioChannels = message.audioParams!.channels;
              _audioFrameDuration = message.audioParams!.frameDuration;
            }

            if (message.type == WebsocketMessage.typeSpeechToText) {
              if (null != _audioRecorder &&
                  await _audioRecorder!.isRecording()) {
                await _audioRecorder!.stop();
              }

              if (null != message.text) {
                add(
                  ChatOnMessageEvent(
                    message: StorageMessage(
                      id: Uuid().v4(),
                      text: message.text!,
                      sendByMe: true,
                      createdAt: DateTime.now(),
                    ),
                  ),
                );
              }
            } else if (message.type == WebsocketMessage.typeTextToSpeech &&
                message.state == WebsocketMessage.stateSentenceStart &&
                null != message.text) {
              add(
                ChatOnMessageEvent(
                  message: StorageMessage(
                    id: Uuid().v4(),
                    text: message.text!,
                    sendByMe: false,
                    createdAt: DateTime.now(),
                  ),
                ),
              );
            } else if (message.type == WebsocketMessage.typeTextToSpeech &&
                message.state == WebsocketMessage.stateStop &&
                _isOnCall) {
              add(ChatStartListenEvent());
            }
          } else if (data is Uint8List) {
            // 确保 _audioPlayer 不为 null
            if (_audioPlayer == null) {
              _logger.e('___ERROR _audioPlayer is null');
              return;
            }
            
            // 检查播放器是否已打开
            if (!_audioPlayer!.isOpen()) {
              await _audioPlayer!.openPlayer();
            }

            // 解码 Opus 数据
            final pcmData = await CommonUtils.opusToPcm(
              opusData: data,
              sampleRate: _audioSampleRate,
              channels: _audioChannels,
            );
            
            // 确保解码成功
            if (pcmData == null) {
              _logger.e('___ERROR Failed to decode Opus data');
              return;
            }

            if (_audioPlayer!.isPlaying) {
              // 确保 uint8ListSink 不为 null
              if (_audioPlayer!.uint8ListSink == null) {
                _logger.e('___ERROR uint8ListSink is null');
                return;
              }
              _audioPlayer!.uint8ListSink!.add(pcmData);
            } else {
              await _audioPlayer!.startPlayerFromStream(
                codec: Codec.pcm16,
                interleaved: false,
                numChannels: _audioChannels,
                sampleRate: _audioSampleRate,
                bufferSize: 1024,
              );
            }
          }
        } catch (e, s) {
          _logger.e('___ERROR Listen $s $e');
        }
      },
      onError: (e) {
        _logger.e('___ERROR Websocket $e');
        // 连接错误，更新状态并尝试重连
        add(ChatConnectionErrorEvent());
        _scheduleReconnect();
      },
      onDone: () {
        _logger.i('___INFO Websocket Closed');
        if (null != _websocketStreamSubscription) {
          _websocketStreamSubscription!.cancel();
          _websocketStreamSubscription = null;
        }
        // 连接关闭，更新状态并尝试重连
        add(ChatConnectionDisconnectedEvent());
        _scheduleReconnect();
      },
    );
  }
  
  // 添加重连逻辑
  void _scheduleReconnect() {
    if (_reconnectAttempts >= _maxReconnectAttempts) {
      _logger.i('___INFO Max reconnect attempts reached, giving up');
      return;
    }
    
    _reconnectAttempts++;
    _logger.i('___INFO Scheduling reconnect attempt $_reconnectAttempts/$_maxReconnectAttempts');
    
    add(ChatConnectionReconnectingEvent());
    
    _reconnectTimer = Timer(_reconnectDelay, () {
      _connectWebSocket();
    });
  }
  
  // 添加WebSocket连接方法
  Future<void> _connectWebSocket() async {
    try {
      add(ChatConnectionConnectingEvent());
      
      _websocketChannel = IOWebSocketChannel.connect(
        Uri.parse(
          (await SharedPreferencesUtil().getWebsocketUrl()) ??
              XConst.defaultWebsocketUrl,
        ),
        headers: {
          "Protocol-Version": "1",
          "Device-Id": await SharedPreferencesUtil().getMacAddress(),
        },
      );

      _initWebsocketListener();

      _websocketChannel!.sink.add(
        jsonEncode(
          WebsocketMessage(
            type: WebsocketMessage.typeHello,
            transport: WebsocketMessage.transportWebSocket,
            audioParams: AudioParams(
              sampleRate: _audioSampleRate,
              channels: _audioChannels,
              frameDuration: _audioFrameDuration,
              format: AudioParams.formatOpus,
            ),
          ).toJson(),
        ),
      );
      
      // 连接成功，重置重连计数器并更新状态
      _reconnectAttempts = 0;
      add(ChatConnectionConnectedEvent());
      
      _logger.i('___INFO WebSocket connected successfully');
    } catch (e, s) {
      _logger.e('___ERROR Failed to connect WebSocket: $e $s');
      add(ChatConnectionErrorEvent());
      _scheduleReconnect();
    }
  }

  ChatBloc() : super(ChatInitialState()) {
    _logger = Logger();
    on<ChatEvent>((event, emit) async {
      if (event is ChatInitialEvent) {
        try {
          // 使用新的连接方法
          await _connectWebSocket();

          _audioRecorder = AudioRecorder();

          _audioPlayer = FlutterSoundPlayer();

          initOpus(await opus_flutter.load());

          List<StorageMessage> messageList = await StorageUtil()
              .getPaginatedMessages(
                limit: _messageListPaginatedLimit,
                offset: _messageListPaginatedOffset,
              );

          emit(ChatInitialState(
            messageList: messageList,
            connectionStatus: state.connectionStatus, // 保持当前连接状态
          ));
        } catch (e, s) {
          _logger.e('___ERROR ChatInitialEvent $e $s');
          emit(ChatInitialState(
            messageList: [],
            connectionStatus: WebSocketConnectionStatus.error,
          ));
        }
      }

      if (event is ChatStartListenEvent) {
        if (!await Permission.microphone.isGranted) {
          if (!event.isRequestMicrophonePermission ||
              !await Permission.microphone.request().isGranted) {
            emit(ChatNoMicrophonePermissionState());
          }
          if (!_isOnCall) {
            return;
          }
        }

        if (null == _websocketStreamSubscription) {
          _initWebsocketListener();
        }

        // 确保 _websocketChannel 不为 null
        if (_websocketChannel == null) {
          _logger.e('___ERROR _websocketChannel is null');
          return;
        }
        
        _websocketChannel!.sink.add(
          jsonEncode(
            WebsocketMessage(
              type: WebsocketMessage.typeListen,
              sessionId: _sessionId,
              state: WebsocketMessage.stateStart,
              mode: WebsocketMessage.modeAuto,
            ).toJson(),
          ),
        );

        // 确保 _audioRecorder 不为 null
        if (_audioRecorder == null) {
          _logger.e('___ERROR _audioRecorder is null');
          return;
        }
        
        _audioRecorderStream = (await _audioRecorder!.startStream(
          RecordConfig(
            encoder: AudioEncoder.pcm16bits,
            echoCancel: true,
            noiseSuppress: true,
            numChannels: _audioChannels,
            sampleRate: _audioSampleRate,
          ),
        ));

        if (null != _audioRecorderSubscription) {
          _audioRecorderSubscription!.cancel();
          _audioRecorderSubscription = null;
        }

        _audioRecorderSubscription = _audioRecorderStream!.listen((data) async {
          if (_websocketChannel != null &&
              data.isNotEmpty &&
              data.length % 2 == 0) {
            Uint8List? opusData = await CommonUtils.pcmToOpus(
              pcmData: data,
              sampleRate: _audioSampleRate,
              frameDuration: _audioFrameDuration,
            );
            if (null != opusData) {
              // 再次检查 _websocketChannel 不为 null
              if (_websocketChannel != null) {
                _websocketChannel!.sink.add(opusData);
              }
            }
          }
        });
      }

      if (event is ChatOnMessageEvent) {
        await StorageUtil().insertMessage(event.message);
        emit(
          ChatInitialState(
            messageList: [event.message, ...state.messageList],
            connectionStatus: state.connectionStatus, // 保持当前连接状态
          ),
        );
      }

      if (event is ChatLoadMoreEvent) {
        _messageListPaginatedOffset += _messageListPaginatedLimit;
        List<StorageMessage> messageList = await StorageUtil()
            .getPaginatedMessages(
              limit: 20,
              offset: _messageListPaginatedOffset,
            );
        emit(
          ChatInitialState(
            messageList: [...state.messageList, ...messageList],
            hasMore: messageList.length == _messageListPaginatedLimit,
            connectionStatus: state.connectionStatus, // 保持当前连接状态
          ),
        );
      }

      if (event is ChatStopListenEvent) {
        if (null != _audioRecorder && (await _audioRecorder!.isRecording())) {
          await _audioRecorder!.stop();
        }
      }

      if (event is ChatStartCallEvent) {
        _isOnCall = true;
        add(ChatStartListenEvent());
      }

      if (event is ChatStopCallEvent) {
        _isOnCall = false;
        add(ChatStopListenEvent());
      }
      
      // 处理WebSocket连接状态事件
      if (event is ChatConnectionConnectingEvent) {
        if (state is ChatInitialState) {
          emit((state as ChatInitialState).copyWith(connectionStatus: WebSocketConnectionStatus.connecting));
        } else if (state is ChatNoMicrophonePermissionState) {
          emit((state as ChatNoMicrophonePermissionState).copyWith(connectionStatus: WebSocketConnectionStatus.connecting));
        }
      }
      
      if (event is ChatConnectionConnectedEvent) {
        if (state is ChatInitialState) {
          emit((state as ChatInitialState).copyWith(connectionStatus: WebSocketConnectionStatus.connected));
        } else if (state is ChatNoMicrophonePermissionState) {
          emit((state as ChatNoMicrophonePermissionState).copyWith(connectionStatus: WebSocketConnectionStatus.connected));
        }
      }
      
      if (event is ChatConnectionDisconnectedEvent) {
        if (state is ChatInitialState) {
          emit((state as ChatInitialState).copyWith(connectionStatus: WebSocketConnectionStatus.disconnected));
        } else if (state is ChatNoMicrophonePermissionState) {
          emit((state as ChatNoMicrophonePermissionState).copyWith(connectionStatus: WebSocketConnectionStatus.disconnected));
        }
      }
      
      if (event is ChatConnectionReconnectingEvent) {
        if (state is ChatInitialState) {
          emit((state as ChatInitialState).copyWith(connectionStatus: WebSocketConnectionStatus.reconnecting));
        } else if (state is ChatNoMicrophonePermissionState) {
          emit((state as ChatNoMicrophonePermissionState).copyWith(connectionStatus: WebSocketConnectionStatus.reconnecting));
        }
      }
      
      if (event is ChatConnectionErrorEvent) {
        if (state is ChatInitialState) {
          emit((state as ChatInitialState).copyWith(connectionStatus: WebSocketConnectionStatus.error));
        } else if (state is ChatNoMicrophonePermissionState) {
          emit((state as ChatNoMicrophonePermissionState).copyWith(connectionStatus: WebSocketConnectionStatus.error));
        }
      }
      
      // 处理授权状态事件
      if (event is ChatUnauthorizedEvent) {
        if (state is ChatInitialState) {
          emit((state as ChatInitialState).copyWith(connectionStatus: WebSocketConnectionStatus.unauthorized));
        } else if (state is ChatNoMicrophonePermissionState) {
          emit((state as ChatNoMicrophonePermissionState).copyWith(connectionStatus: WebSocketConnectionStatus.unauthorized));
        }
      }
      
      if (event is ChatAuthorizedEvent) {
        if (state is ChatInitialState) {
          emit((state as ChatInitialState).copyWith(connectionStatus: WebSocketConnectionStatus.authorized));
        } else if (state is ChatNoMicrophonePermissionState) {
          emit((state as ChatNoMicrophonePermissionState).copyWith(connectionStatus: WebSocketConnectionStatus.authorized));
        }
      }
    });
  }
}