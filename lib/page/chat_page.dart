import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:pull_to_refresh_flutter3/pull_to_refresh_flutter3.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:xiaozhi/bloc/chat/chat_bloc.dart';
import 'package:xiaozhi/bloc/ota/ota_bloc.dart';
import 'package:xiaozhi/common/x_const.dart';
import 'package:xiaozhi/l10n/generated/app_localizations.dart';
import 'package:xiaozhi/widget/hold_to_talk_widget.dart';
import 'package:xiaozhi/util/live2d_manager.dart';

import 'call_page.dart';
import 'setting_page.dart';

class ChatPage extends StatefulWidget {
  const ChatPage({super.key});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  late RefreshController _refreshController;

  final GlobalKey<HoldToTalkWidgetState> holdToTalkKey =
      GlobalKey<HoldToTalkWidgetState>();

  bool _isPressing = false;

  late ChatBloc chatBloc;
  
  // 添加Live2D相关变量
  bool _live2dReady = false;
  bool _live2dInitializationAttempted = false;
  bool _live2dViewCreated = false;

  @override
  void initState() {
    _refreshController = RefreshController();
    super.initState();
  }
  
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // 在didChangeDependencies中初始化Live2D，确保context可用
    if (!_live2dInitializationAttempted) {
      _live2dInitializationAttempted = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _initializeLive2D();
      });
    }
  }

  /// 初始化Live2D
  Future<void> _initializeLive2D() async {
    try {
      // 初始化Live2D引擎
      bool initSuccess = await Live2DManager().initialize();
      if (!initSuccess) {
        debugPrint("Failed to initialize Live2D engine");
        return;
      }
      
      // 标记视图即将创建
      if (mounted) {
        setState(() {
          _live2dViewCreated = true;
        });
      }
      
      // 延迟加载模型，确保视图已创建
      await Future.delayed(Duration(milliseconds: 500));
      
      // 加载Haru模型
      bool loadSuccess = await Live2DManager().loadModel("assets/live2d/Haru/Haru.model3.json");
      if (!loadSuccess) {
        debugPrint("Failed to load Live2D model");
        return;
      }
      
      // 设置初始位置和缩放
      await Live2DManager().setPosition(0, 0);
      await Live2DManager().setScale(1.0);
      
      if (mounted) {
        setState(() {
          _live2dReady = true;
        });
      }
    } catch (e) {
      debugPrint("Live2D initialization error: $e");
    }
  }

  @override
  void dispose() {
    _refreshController.dispose();
    super.dispose();
  }

  clearUp() async {
    chatBloc.add(ChatStopListenEvent());
    holdToTalkKey.currentState!.setSpeaking(false);
    if (_isPressing && mounted) {
      setState(() {
        _isPressing = false;
      });
    }
    
    // 停止说话时恢复默认表情
    if (_live2dReady) {
      Live2DManager().setExpression("default");
    }
  }

  @override
  Widget build(BuildContext context) {
    final MediaQueryData mediaQuery = MediaQuery.of(context);

    OtaBloc otaBloc = BlocProvider.of<OtaBloc>(context);

    chatBloc = BlocProvider.of<ChatBloc>(context);

    return BlocListener(
      bloc: otaBloc,
      listener: (context, OtaState otaState) {
        if (otaState is OtaNotActivatedState) {
          showDialog(
            context: context,
            builder: (context) {
              return AlertDialog(
                title: Text(AppLocalizations.of(context)!.deviceActivation),
                content: Stack(
                  alignment: Alignment.center,
                  children: [
                    Icon(
                      Icons.pin_rounded,
                      color: Theme.of(context).colorScheme.primaryContainer,
                      size: 60,
                    ),
                    Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          AppLocalizations.of(
                            context,
                          )!.deviceActivationDescription,
                        ),
                        SizedBox(height: XConst.spacer),
                        Text(
                          otaState.code,
                          style: TextStyle(
                            fontSize: XConst.spacer * 4,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        if (null != otaState.url)
                          Text(
                            otaState.url!,
                            //text underline
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              decoration: TextDecoration.underline,
                            ),
                          ),
                      ],
                    ),
                  ],
                ),
                actions: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      TextButton(
                        onPressed: () async {
                          Navigator.pop(context);
                        },
                        child: Text(AppLocalizations.of(context)!.reject),
                      ),
                      SizedBox(width: XConst.spacer),
                      FilledButton(
                        onPressed: () async {
                          Navigator.pop(context);
                          try {
                            await Clipboard.setData(
                              ClipboardData(text: otaState.code),
                            );
                            if (null != otaState.url) {
                              await launchUrl(Uri.parse(otaState.url!));
                            }
                          } catch (_) {}
                        },
                        child: Text(AppLocalizations.of(context)!.ok),
                      ),
                    ],
                  ),
                ],
              );
            },
          );
        }
      },
      child: BlocConsumer(
        bloc: chatBloc,
        listener: (context, ChatState chatState) {
          if (chatState is ChatNoMicrophonePermissionState) {
            clearUp();

            showDialog(
              context: context,
              builder: (context) {
                return AlertDialog(
                  title: Text(AppLocalizations.of(context)!.requestPermission),
                  content: Stack(
                    alignment: Alignment.center,
                    children: [
                      Icon(
                        Icons.mic_rounded,
                        color: Theme.of(context).colorScheme.primaryContainer,
                        size: 60,
                      ),
                      Text(
                        AppLocalizations.of(
                          context,
                        )!.requestPermissionDescription,
                      ),
                    ],
                  ),
                  actions: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        TextButton(
                          onPressed: () async {
                            Navigator.pop(context);
                          },
                          child: Text(AppLocalizations.of(context)!.reject),
                        ),
                        SizedBox(width: XConst.spacer),
                        FilledButton(
                          onPressed: () async {
                            Navigator.pop(context);
                            chatBloc.add(
                              ChatStartListenEvent(
                                isRequestMicrophonePermission: true,
                              ),
                            );
                          },
                          child: Text(AppLocalizations.of(context)!.agree),
                        ),
                      ],
                    ),
                  ],
                );
              },
            );
          }

          if (chatState is ChatInitialState) {
            if (chatState.hasMore) {
              _refreshController.loadComplete();
            } else {
              _refreshController.loadNoData();
            }

            if (chatState.messageList.isNotEmpty &&
                chatState.messageList.first.sendByMe) {
              clearUp();
            }
            
            // 当收到回复时播放说话动作
            if (chatState.messageList.isNotEmpty && 
                !chatState.messageList.first.sendByMe) {
              if (_live2dReady) {
                // 播放说话动作
                Live2DManager().startMotion("talk", 0);
                // 设置说话表情
                Live2DManager().setExpression("happy");
              }
            }
          }
        },
        builder: (context, ChatState chatState) {
          return Scaffold(
            appBar: AppBar(
              title: Text(AppLocalizations.of(context)!.xiaozhi),
              leading: Row(
                children: [
                  IconButton(
                    onPressed: () {
                      Navigator.of(context).push(
                        MaterialPageRoute(builder: (context) => SettingPage()),
                      );
                    },
                    icon: Icon(Icons.settings_rounded),
                  ),
                ],
              ),
              actions: [
                IconButton(
                  onPressed: () {
                    Navigator.of(
                      context,
                    ).push(MaterialPageRoute(builder: (context) => CallPage()));
                  },
                  icon: Icon(Icons.call_rounded),
                ),
              ],
            ),
            body: Stack(
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    // 添加Live2D显示区域
                    if (_live2dReady)
                      SizedBox(
                        height: 300,
                        child: AndroidView(
                          viewType: 'live2d_view',
                          creationParams: <String, dynamic>{},
                          creationParamsCodec: const StandardMessageCodec(),
                        ),
                      )
                    else
                      SizedBox(
                        height: 300,
                        child: Center(
                          child: _live2dViewCreated 
                            ? Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  CircularProgressIndicator(),
                                  SizedBox(height: 10),
                                  Text("Loading Live2D model...")
                                ],
                              )
                            : Text("Live2D not available"),
                        ),
                      ),
                    Expanded(
                      child: SmartRefresher(
                        enablePullDown: false,
                        enablePullUp: true,
                        controller: _refreshController,
                        footer: CustomFooter(
                          builder: (BuildContext context, LoadStatus? mode) {
                            String text;
                            switch (mode) {
                              case LoadStatus.loading:
                                text = AppLocalizations.of(context)!.loading;
                                break;
                              default:
                                text = AppLocalizations.of(context)!.noMoreData;
                            }
                            return Center(
                              child: Text(
                                text,
                                style: TextStyle(
                                  color: Theme.of(context)
                                      .colorScheme
                                      .onPrimaryContainer
                                      .withValues(alpha: 0.5),
                                ),
                              ),
                            );
                          },
                        ),
                        onLoading: () {
                          chatBloc.add(ChatLoadMoreEvent());
                        },
                        child: ListView(
                          reverse: true,
                          padding: EdgeInsets.all(XConst.spacer),
                          children:
                              chatState.messageList
                                  .map(
                                    (e) => Row(
                                      mainAxisAlignment:
                                          e.sendByMe
                                              ? MainAxisAlignment.end
                                              : MainAxisAlignment.start,
                                      children: [
                                        Container(
                                          constraints: BoxConstraints(
                                            maxWidth:
                                                mediaQuery.size.width * 0.75,
                                          ),
                                          margin: EdgeInsets.only(
                                            top: XConst.spacer,
                                          ),
                                          padding: EdgeInsets.symmetric(
                                            vertical: XConst.spacer * 0.8,
                                            horizontal: XConst.spacer,
                                          ),
                                          decoration: BoxDecoration(
                                            borderRadius: BorderRadius.circular(
                                              XConst.spacer * 1.5,
                                            ),
                                            color:
                                                e.sendByMe
                                                    ? Theme.of(context)
                                                        .colorScheme
                                                        .primaryContainer
                                                    : Theme.of(context)
                                                        .colorScheme
                                                        .tertiaryContainer,
                                          ),
                                          child: Text(
                                            e.text,
                                            style: TextStyle(
                                              color:
                                                  e.sendByMe
                                                      ? Theme.of(context)
                                                          .colorScheme
                                                          .onPrimaryContainer
                                                      : Theme.of(context)
                                                          .colorScheme
                                                          .onTertiaryContainer,
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  )
                                  .toList(),
                        ),
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.all(XConst.spacer).copyWith(
                        bottom: 12 + MediaQuery.of(context).padding.bottom,
                      ),
                      child: GestureDetector(
                        onTapDown: (_) {
                          holdToTalkKey.currentState!.setCancelTapUp(false);
                          if (!_isPressing) {
                            setState(() {
                              _isPressing = true;
                            });
                          }
                          
                          // 按下说话按钮时触发表情变化
                          if (_live2dReady) {
                            Live2DManager().setExpression("surprise");
                          }
                        },
                        onTapUp: (_) {
                          holdToTalkKey.currentState!.setCancelTapUp(false);
                          clearUp();
                        },
                        onTapCancel: () {
                          holdToTalkKey.currentState!.setCancelTapUp(false);
                          clearUp();
                        },
                        onLongPressStart: (_) async {
                          holdToTalkKey.currentState!.setSpeaking(true);
                          if (!_isPressing) {
                            setState(() {
                              _isPressing = true;
                            });
                          }
                          chatBloc.add(ChatStartListenEvent());
                          
                          // 开始录音时触发动画
                          if (_live2dReady) {
                            Live2DManager().startMotion("tap_body", 0);
                          }
                        },
                        onLongPressEnd: (detail) async {
                          clearUp();
                        },
                        onLongPressCancel: () {
                          holdToTalkKey.currentState!.setCancelTapUp(false);
                          clearUp();
                        },
                        onLongPressMoveUpdate: (detail) {
                          if ((mediaQuery.size.height -
                                  detail.globalPosition.dy) <
                              (XConst.holdToTalkResponseAreaHeight +
                                  mediaQuery.padding.bottom)) {
                            holdToTalkKey.currentState!.setCancelTapUp(false);
                          } else {
                            holdToTalkKey.currentState!.setCancelTapUp(true);
                          }
                          if (mounted) setState(() {});
                        },
                        child: FilledButton(
                          onPressed: () {},
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.mic_rounded),
                              SizedBox(width: XConst.spacer),
                              Text(AppLocalizations.of(context)!.holdToTalk),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
                HoldToTalkWidget(key: holdToTalkKey),
              ],
            ),
          );
        },
      ),
    );
  }
}