import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:pull_to_refresh_flutter3/pull_to_refresh_flutter3.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:xiaozhi/bloc/chat/chat_bloc.dart';
import 'package:xiaozhi/bloc/ota/ota_bloc.dart';
import 'package:xiaozhi/common/x_const.dart';
import 'package:xiaozhi/l10n/generated/app_localizations.dart';
import 'package:xiaozhi/widget/hold_to_talk_widget.dart';
import 'package:xiaozhi/widget/live2d_widget.dart';

import 'call_page.dart';
import 'setting_page.dart';

class ChatPage extends StatefulWidget {
  const ChatPage({super.key});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> with WidgetsBindingObserver {
  late RefreshController _refreshController;

  final GlobalKey<HoldToTalkWidgetState> holdToTalkKey =
      GlobalKey<HoldToTalkWidgetState>();

  bool _isPressing = false;
  late ChatBloc chatBloc;
  late Live2DWidget live2DWidget;
  final GlobalKey _live2DKey = GlobalKey(); // 使用不带泛型参数的GlobalKey
  
  // 添加控制按钮可见性的状态变量
  bool _isGearVisible = true;
  bool _isPowerVisible = true;

  @override
  void initState() {
    if (kDebugMode) {
      print('ChatPage: initState called');
    }
    _refreshController = RefreshController();
    WidgetsBinding.instance.addObserver(this); // 添加观察者以监听页面可见性变化
    super.initState();
  }

  @override
  void dispose() {
    if (kDebugMode) {
      print('ChatPage: dispose called');
    }
    WidgetsBinding.instance.removeObserver(this); // 移除观察者
    _refreshController.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (kDebugMode) {
      print('ChatPage: didChangeAppLifecycleState called with state: $state');
    }
    // 应用生命周期变化时处理Live2D实例
    if (_live2DKey.currentState != null) {
      if (state == AppLifecycleState.resumed) {
        if (kDebugMode) {
          print('ChatPage: Activating Live2D widget');
        }
        // 使用新的公开方法
        (_live2DKey.currentWidget as Live2DWidget).activate();
      } else {
        if (kDebugMode) {
          print('ChatPage: Deactivating Live2D widget');
        }
        // 使用新的公开方法
        (_live2DKey.currentWidget as Live2DWidget).deactivate();
      }
    }
  }

  clearUp() async {
    if (kDebugMode) {
      print('ChatPage: clearUp called');
    }
    chatBloc.add(ChatStopListenEvent());
    holdToTalkKey.currentState!.setSpeaking(false);
    if (_isPressing && mounted) {
      setState(() {
        _isPressing = false;
      });
    }
  }
  
  // 控制齿轮按钮可见性的方法
  void _toggleGearVisible() {
    if (kDebugMode) {
      print('ChatPage: _toggleGearVisible called, current value: $_isGearVisible');
    }
    setState(() {
      _isGearVisible = !_isGearVisible;
      if (kDebugMode) {
        print('ChatPage: _isGearVisible updated to: $_isGearVisible');
      }
    });
    
    // 更新Live2D中的齿轮按钮可见性
    if (_live2DKey.currentWidget != null) {
      if (kDebugMode) {
        print('ChatPage: Calling Live2D setGearVisible with value: $_isGearVisible');
      }
      (_live2DKey.currentWidget as Live2DWidget).setGearVisible(_isGearVisible);
    } else {
      if (kDebugMode) {
        print('ChatPage: Live2D widget is null, cannot set gear visible');
      }
    }
  }
  
  // 控制电源按钮可见性的方法
  void _togglePowerVisible() {
    if (kDebugMode) {
      print('ChatPage: _togglePowerVisible called, current value: $_isPowerVisible');
    }
    setState(() {
      _isPowerVisible = !_isPowerVisible;
      if (kDebugMode) {
        print('ChatPage: _isPowerVisible updated to: $_isPowerVisible');
      }
    });
    
    // 更新Live2D中的电源按钮可见性
    if (_live2DKey.currentWidget != null) {
      if (kDebugMode) {
        print('ChatPage: Calling Live2D setPowerVisible with value: $_isPowerVisible');
      }
      (_live2DKey.currentWidget as Live2DWidget).setPowerVisible(_isPowerVisible);
    } else {
      if (kDebugMode) {
        print('ChatPage: Live2D widget is null, cannot set power visible');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (kDebugMode) {
      print('ChatPage: build called, gearVisible: $_isGearVisible, powerVisible: $_isPowerVisible');
    }
    final MediaQueryData mediaQuery = MediaQuery.of(context);

    OtaBloc otaBloc = BlocProvider.of<OtaBloc>(context);

    chatBloc = BlocProvider.of<ChatBloc>(context);

    return BlocListener(
      bloc: otaBloc,
      listener: (context, OtaState otaState) {
        if (kDebugMode) {
          print('ChatPage: OtaBloc state changed to: ${otaState.runtimeType}');
        }
        if (otaState is OtaNotActivatedState) {
          if (kDebugMode) {
            print('ChatPage: Showing activation dialog with code: ${otaState.code}');
          }
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
                          if (kDebugMode) {
                            print('ChatPage: User rejected activation');
                          }
                          Navigator.pop(context);
                        },
                        child: Text(AppLocalizations.of(context)!.reject),
                      ),
                      SizedBox(width: XConst.spacer),
                      FilledButton(
                        onPressed: () async {
                          if (kDebugMode) {
                            print('ChatPage: User accepted activation, code copied and URL launched');
                          }
                          Navigator.pop(context);
                          try {
                            await Clipboard.setData(
                              ClipboardData(text: otaState.code),
                            );
                            if (null != otaState.url) {
                              await launchUrl(Uri.parse(otaState.url!));
                            }
                          } catch (e) {
                            if (kDebugMode) {
                              print('ChatPage: Error during activation process: $e');
                            }
                          }
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
          if (kDebugMode) {
            print('ChatPage: ChatBloc state changed to: ${chatState.runtimeType}');
          }
          if (chatState is ChatNoMicrophonePermissionState) {
            if (kDebugMode) {
              print('ChatPage: No microphone permission, showing dialog');
            }
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
                            if (kDebugMode) {
                              print('ChatPage: User rejected microphone permission');
                            }
                            Navigator.pop(context);
                          },
                          child: Text(AppLocalizations.of(context)!.reject),
                        ),
                        SizedBox(width: XConst.spacer),
                        FilledButton(
                          onPressed: () async {
                            if (kDebugMode) {
                              print('ChatPage: User granted microphone permission');
                            }
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
            if (kDebugMode) {
              print('ChatPage: ChatInitialState - hasMore: ${chatState.hasMore}, messageList length: ${chatState.messageList.length}');
            }
            if (chatState.hasMore) {
              _refreshController.loadComplete();
            } else {
              _refreshController.loadNoData();
            }

            if (chatState.messageList.isNotEmpty &&
                chatState.messageList.first.sendByMe) {
              if (kDebugMode) {
                print('ChatPage: Message sent by me, calling clearUp');
              }
              clearUp();
            }
            
            // 当收到新消息时，触发Live2D模型的随机表情
            if (chatState.messageList.isNotEmpty) {
              // 使用新的公开方法触发表情
              if (_live2DKey.currentWidget != null) {
                if (kDebugMode) {
                  print('ChatPage: Triggering Live2D expression');
                }
                (_live2DKey.currentWidget as Live2DWidget).triggerExpression('Happy');
              } else {
                if (kDebugMode) {
                  print('ChatPage: Live2D widget not available for triggering expression');
                }
              }
            }
          }
        },
        builder: (context, ChatState chatState) {
          if (kDebugMode) {
            print('ChatPage: Building UI with ${chatState.messageList.length} messages');
          }
          return Scaffold(
            appBar: AppBar(
              title: Text(AppLocalizations.of(context)!.xiaozhi),
              leading: Row(
                children: [
                  IconButton(
                    onPressed: () {
                      if (kDebugMode) {
                        print('ChatPage: Navigating to SettingPage');
                      }
                      Navigator.of(context).push(
                        MaterialPageRoute(builder: (context) => SettingPage()),
                      );
                    },
                    icon: Icon(Icons.settings_rounded),
                  ),
                ],
              ),
              actions: [
                // 添加控制按钮可见性的开关
                PopupMenuButton<String>(
                  icon: Icon(Icons.visibility),
                  onSelected: (String result) {
                    if (kDebugMode) {
                      print('ChatPage: Popup menu item selected: $result');
                    }
                    switch (result) {
                      case 'toggle_gear':
                        _toggleGearVisible();
                        break;
                      case 'toggle_power':
                        _togglePowerVisible();
                        break;
                      default:
                        if (kDebugMode) {
                          print('ChatPage: Unknown menu item selected: $result');
                        }
                    }
                  },
                  itemBuilder: (BuildContext context) {
                    if (kDebugMode) {
                      print('ChatPage: Building popup menu items, gearVisible: $_isGearVisible, powerVisible: $_isPowerVisible');
                    }
                    return <PopupMenuEntry<String>>[
                      PopupMenuItem<String>(
                        value: 'toggle_gear',
                        child: Row(
                          children: [
                            Icon(_isGearVisible ? Icons.visibility : Icons.visibility_off),
                            SizedBox(width: 8),
                            Text(_isGearVisible 
                              ? '${AppLocalizations.of(context)!.setting}齿轮按钮' 
                              : '显示齿轮按钮'),
                          ],
                        ),
                      ),
                      PopupMenuItem<String>(
                        value: 'toggle_power',
                        child: Row(
                          children: [
                            Icon(_isPowerVisible ? Icons.visibility : Icons.visibility_off),
                            SizedBox(width: 8),
                            Text(_isPowerVisible 
                              ? '${AppLocalizations.of(context)!.setting}电源按钮' 
                              : '显示电源按钮'),
                          ],
                        ),
                      ),
                    ];
                  },
                ),
                IconButton(
                  onPressed: () {
                    if (kDebugMode) {
                      print('ChatPage: Navigating to CallPage');
                    }
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
                // 主要内容区域
                Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Expanded(
                      // 将Live2D模型作为消息列表区域的背景
                      child: LayoutBuilder(
                        builder: (context, constraints) {
                          return Stack(
                            children: [
                              // Live2D模型作为背景铺满整个区域
                              Positioned.fill(
                                child: Container(
                                  color: Colors.transparent,
                                  child: Live2DWidget(
                                    key: _live2DKey, // 添加key以便访问widget状态
                                    modelPath: "assets/live2d/Haru/Haru.model3.json",
                                    // 使用LayoutBuilder提供的约束来设置Live2DWidget的尺寸
                                    width: constraints.maxWidth,
                                    height: constraints.maxHeight,
                                    instanceId: 'chat_page_live2d', // 为这个实例指定特定ID
                                  ),
                                ),
                              ),
                              // SmartRefresher占据大部分区域，但为Live2D按钮留出空间
                              Positioned(
                                top: 80, // 为顶部按钮留出足够空间
                                left: 0,
                                right: 80, // 为右上角按钮留出足够空间
                                bottom: 0,
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
                                    if (kDebugMode) {
                                      print('ChatPage: Loading more messages');
                                    }
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
                            ],
                          );
                        }
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.all(XConst.spacer).copyWith(
                        bottom: 12 + MediaQuery.of(context).padding.bottom,
                      ),
                      child: GestureDetector(
                        onTapDown: (_) {
                          if (kDebugMode) {
                            print('ChatPage: Tap down on hold-to-talk button');
                          }
                          holdToTalkKey.currentState!.setCancelTapUp(false);
                          if (!_isPressing) {
                            setState(() {
                              _isPressing = true;
                            });
                          }
                        },
                        onTapUp: (_) {
                          if (kDebugMode) {
                            print('ChatPage: Tap up on hold-to-talk button');
                          }
                          holdToTalkKey.currentState!.setCancelTapUp(false);
                          clearUp();
                        },
                        onTapCancel: () {
                          if (kDebugMode) {
                            print('ChatPage: Tap cancel on hold-to-talk button');
                          }
                          holdToTalkKey.currentState!.setCancelTapUp(false);
                          clearUp();
                        },
                        onLongPressStart: (_) async {
                          if (kDebugMode) {
                            print('ChatPage: Long press started on hold-to-talk button');
                          }
                          holdToTalkKey.currentState!.setSpeaking(true);
                          if (!_isPressing) {
                            setState(() {
                              _isPressing = true;
                            });
                          }
                          chatBloc.add(ChatStartListenEvent());
                        },
                        onLongPressEnd: (detail) async {
                          if (kDebugMode) {
                            print('ChatPage: Long press ended on hold-to-talk button');
                          }
                          clearUp();
                        },
                        onLongPressCancel: () {
                          if (kDebugMode) {
                            print('ChatPage: Long press cancelled on hold-to-talk button');
                          }
                          holdToTalkKey.currentState!.setCancelTapUp(false);
                          clearUp();
                        },
                        onLongPressMoveUpdate: (detail) {
                          if (kDebugMode) {
                            print('ChatPage: Long press move update on hold-to-talk button');
                          }
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
                
                // HoldToTalkWidget覆盖在页面上层，确保它始终可见
                HoldToTalkWidget(key: holdToTalkKey),
              ],
            ),
          );
        },
      ),
    );
  }
}