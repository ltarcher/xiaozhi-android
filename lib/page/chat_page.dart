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
import 'package:xiaozhi/util/shared_preferences_util.dart';
import 'package:xiaozhi/widget/hold_to_talk_widget.dart';
import 'package:xiaozhi/widget/live2d_widget.dart';
import 'package:xiaozhi/util/audio_processor.dart'; // 添加音频处理导入

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
  final GlobalKey _live2DKey = GlobalKey(); // 使用动态类型访问State方法
  
  // 添加控制按钮可见性的状态变量
  bool _isGearVisible = false;
  bool _isPowerVisible = false;
  
  // 移除本地口型同步控制器，使用ChatBloc中的
  // late LipSyncController _lipSyncController;
  
  // 添加控制授权对话框显示的状态变量
  bool _showActivationDialog = false;
  String? _activationCode;
  String? _activationUrl;
  
  // 添加控制权限对话框显示的状态变量
  bool _showingPermissionDialog = false;

  @override
  void initState() {
    if (kDebugMode) {
      print('ChatPage: initState called');
    }
    _refreshController = RefreshController();
    WidgetsBinding.instance.addObserver(this); // 添加观察者以监听页面可见性变化
    
    // 从持久化存储恢复按钮可见性状态
    _restoreButtonStates();
    
    // 不再需要初始化本地口型同步控制器
    // _lipSyncController = LipSyncController(
    //   onLipSyncUpdate: _onLipSyncUpdate,
    // );
    
    super.initState();
  }

  // 口型同步更新回调
  void _onLipSyncUpdate(double lipSyncValue) {
    if (kDebugMode) {
      print('ChatPage: LipSync value updated: $lipSyncValue');
    }
    
    // 更新Live2D模型的口型同步值
    if (_live2DKey.currentState != null) {
      try {
        (_live2DKey.currentState as dynamic).setLipSyncValue(lipSyncValue);
      } catch (e) {
        if (kDebugMode) {
          print('ChatPage: Error setting lip sync value: $e');
        }
      }
    }
  }
  
  // 处理服务器端音频播放时的口型同步
  void _handleServerAudioLipSync(double lipSyncValue) {
    if (kDebugMode) {
      print('ChatPage: Server audio lip sync value updated: $lipSyncValue');
    }
    
    // 更新Live2D模型的口型同步值
    if (_live2DKey.currentState != null) {
      try {
        (_live2DKey.currentState as dynamic).setLipSyncValue(lipSyncValue);
      } catch (e) {
        if (kDebugMode) {
          print('ChatPage: Error setting server audio lip sync value: $e');
        }
      }
    }
  }

  // 从持久化存储恢复按钮状态
  Future<void> _restoreButtonStates() async {
    try {
      SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
      
      bool? gearVisible = await prefsUtil.getLive2DGearVisible();
      bool? powerVisible = await prefsUtil.getLive2DPowerVisible();
      
      if (kDebugMode) {
        print('ChatPage: Restored button states - gearVisible: $gearVisible, powerVisible: $powerVisible');
      }
      
      if (mounted) {
        setState(() {
          _isGearVisible = gearVisible ?? true; // 默认可见
          _isPowerVisible = powerVisible ?? false; // 默认不可见
        });
        
        // 延迟设置Live2D按钮状态，确保Widget已完全初始化
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted && _live2DKey.currentState != null) {
            (_live2DKey.currentState as dynamic).setGearVisible(_isGearVisible);
            (_live2DKey.currentState as dynamic).setPowerVisible(_isPowerVisible);
            if (kDebugMode) {
              print('ChatPage: Applied restored states to Live2D widget');
            }
          }
        });
      }
    } catch (e) {
      if (kDebugMode) {
        print('ChatPage: Error restoring button states: $e');
      }
    }
  }

  @override
  void dispose() {
    if (kDebugMode) {
      print('ChatPage: dispose called');
    }
    WidgetsBinding.instance.removeObserver(this); // 移除观察者
    _refreshController.dispose();
    // 不再需要停止本地口型同步控制器
    // _lipSyncController.stop(); // 停止口型同步控制器
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
        // 直接通过GlobalKey访问State方法
        (_live2DKey.currentState as dynamic).activate();
        
        // 不再需要启动本地口型同步控制器
        // 恢复口型同步（如果正在录音）
        // if (_isPressing) {
        //   _lipSyncController.start();
        // }
      } else {
        if (kDebugMode) {
          print('ChatPage: Deactivating Live2D widget');
        }
        // 直接通过GlobalKey访问State方法
        (_live2DKey.currentState as dynamic).deactivate();
        
        // 不再需要暂停本地口型同步控制器
        // 暂停口型同步
        // _lipSyncController.stop();
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
    
    // 不再需要停止本地口型同步控制器
    // 停止口型同步
    // _lipSyncController.stop();
  }
  
  // 控制齿轮按钮可见性的方法
  void _toggleGearVisible() async {
    if (kDebugMode) {
      print('ChatPage: _toggleGearVisible called, current value: $_isGearVisible');
    }
    
    setState(() {
      _isGearVisible = !_isGearVisible;
      if (kDebugMode) {
        print('ChatPage: _isGearVisible updated to: $_isGearVisible');
      }
    });
    
    // 保存状态到持久化存储
    try {
      SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
      await prefsUtil.setLive2DGearVisible(_isGearVisible);
      if (kDebugMode) {
        print('ChatPage: Saved gear visibility to persistent storage: $_isGearVisible');
      }
    } catch (e) {
      if (kDebugMode) {
        print('ChatPage: Error saving gear visibility: $e');
      }
    }
    
    // 更新Live2D中的齿轮按钮可见性
    if (_live2DKey.currentState != null) {
      if (kDebugMode) {
        print('ChatPage: Calling Live2D setGearVisible with value: $_isGearVisible');
      }
      // 直接通过GlobalKey访问State方法
      (_live2DKey.currentState as dynamic).setGearVisible(_isGearVisible);
      if (kDebugMode) {
        print('ChatPage: Live2D setGearVisible call completed');
      }
    } else {
      if (kDebugMode) {
        print('ChatPage: Live2D widget state is null, cannot set gear visible');
      }
    }
    
    if (kDebugMode) {
      print('ChatPage: _toggleGearVisible function completed');
    }
  }
  
  // 控制电源按钮可见性的方法
  void _togglePowerVisible() async {
    if (kDebugMode) {
      print('ChatPage: _togglePowerVisible called, current value: $_isPowerVisible');
    }
    
    setState(() {
      _isPowerVisible = !_isPowerVisible;
      if (kDebugMode) {
        print('ChatPage: _isPowerVisible updated to: $_isPowerVisible');
      }
    });
    
    // 保存状态到持久化存储
    try {
      SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
      await prefsUtil.setLive2DPowerVisible(_isPowerVisible);
      if (kDebugMode) {
        print('ChatPage: Saved power visibility to persistent storage: $_isPowerVisible');
      }
    } catch (e) {
      if (kDebugMode) {
        print('ChatPage: Error saving power visibility: $e');
      }
    }
    
    // 更新Live2D中的电源按钮可见性
    if (_live2DKey.currentState != null) {
      if (kDebugMode) {
        print('ChatPage: Calling Live2D setPowerVisible with value: $_isPowerVisible');
      }
      // 直接通过GlobalKey访问State方法
      (_live2DKey.currentState as dynamic).setPowerVisible(_isPowerVisible);
      if (kDebugMode) {
        print('ChatPage: Live2D setPowerVisible call completed');
      }
    } else {
      if (kDebugMode) {
        print('ChatPage: Live2D widget state is null, cannot set power visible');
      }
    }
    
    if (kDebugMode) {
      print('ChatPage: _togglePowerVisible function completed');
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
        
        // 处理授权状态变化
        if (otaState is OtaNotActivatedState) {
          // 设备未授权
          if (kDebugMode) {
            print('ChatPage: OtaNotActivatedState detected, sending ChatUnauthorizedEvent');
          }
          chatBloc.add(ChatUnauthorizedEvent());
          
          // 更新状态变量以显示授权对话框，而不是直接调用showDialog
          if (mounted) {
            setState(() {
              _showActivationDialog = true;
              _activationCode = otaState.code;
              _activationUrl = otaState.url;
            });
            
            if (kDebugMode) {
              print('ChatPage: Updated state to show activation dialog with code: ${otaState.code}');
            }
          }
        } else if (otaState is OtaActivatedState) {
          // 设备已授权
          if (kDebugMode) {
            print('ChatPage: OtaActivatedState detected, sending ChatAuthorizedEvent');
          }
          chatBloc.add(ChatAuthorizedEvent());
          
          // 隐藏授权对话框
          if (mounted && _showActivationDialog) {
            setState(() {
              _showActivationDialog = false;
              _activationCode = null;
              _activationUrl = null;
            });
            
            if (kDebugMode) {
              print('ChatPage: Updated state to hide activation dialog');
            }
          }
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
              print('ChatPage: No microphone permission, checking if dialog should show');
            }
            clearUp();

            // 添加一个标志来防止重复显示对话框
            if (mounted && !_showingPermissionDialog) {
              setState(() {
                _showingPermissionDialog = true;
              });
              
              showDialog(
                context: context,
                barrierDismissible: false, // 防止点击外部关闭对话框
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
                              // 对话框关闭后重置标志
                              if (mounted) {
                                setState(() {
                                  _showingPermissionDialog = false;
                                });
                              }
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
                              // 对话框关闭后重置标志
                              if (mounted) {
                                setState(() {
                                  _showingPermissionDialog = false;
                                });
                              }
                              // 延迟一下再发送事件，确保状态已更新
                              Future.delayed(Duration(milliseconds: 100), () {
                                chatBloc.add(
                                  ChatStartListenEvent(
                                    isRequestMicrophonePermission: true,
                                  ),
                                );
                              });
                            },
                            child: Text(AppLocalizations.of(context)!.agree),
                          ),
                        ],
                      ),
                    ],
                  );
                },
              ).then((_) {
                // 对话框关闭后重置标志
                if (mounted) {
                  setState(() {
                    _showingPermissionDialog = false;
                  });
                }
              });
            } else {
              if (kDebugMode) {
                print('ChatPage: Permission dialog already showing or widget not mounted, skipping');
              }
            }
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
            
            // 当收到新消息时，触发Live2D模型的随机表情（限制频率避免性能问题）
            if (chatState.messageList.isNotEmpty) {
              // 使用GlobalKey直接访问State方法触发表情
              if (_live2DKey.currentState != null) {
                if (kDebugMode) {
                  print('ChatPage: Triggering Live2D expression');
                }
                // 使用更安全的表情触发方法
                _triggerExpressionSafely('Happy'); // 使用语义化的表情名称
              } else {
                if (kDebugMode) {
                  print('ChatPage: Live2D widget state not available for triggering expression');
                }
              }
            }
          }
        },
        builder: (context, ChatState chatState) {
          if (kDebugMode) {
            print('ChatPage: Building UI with ${chatState.messageList.length} messages');
            print('ChatPage: Current state - connectionStatus: ${chatState.connectionStatus}, authorizationStatus: ${chatState.authorizationStatus}');
          }
          
          // 监听口型同步值变化（添加阈值避免频繁更新）
          if (chatState.lipSyncValue > 0.01) {
            _handleServerAudioLipSync(chatState.lipSyncValue);
          }
          return Scaffold(
            appBar: AppBar(
              title: Row(
                children: [
                  Text(AppLocalizations.of(context)!.xiaozhi),
                  SizedBox(width: 8),
                  // 添加连接状态和授权状态指示器
                  _buildStatusIndicators(chatState.connectionStatus, chatState.authorizationStatus, chatState.recordingStatus, chatState.conversationStatus),
                ],
              ),
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
                        if (kDebugMode) {
                          print('ChatPage: toggle_gear menu item tapped');
                        }
                        _toggleGearVisible();
                        break;
                      case 'toggle_power':
                        if (kDebugMode) {
                          print('ChatPage: toggle_power menu item tapped');
                        }
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
                                    gearVisible: _isGearVisible,    // 传递齿轮按钮可见性状态
                                    powerVisible: _isPowerVisible,  // 传递电源按钮可见性状态
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
                          
                          // 不再需要启动本地口型同步控制器
                          // 开始口型同步
                          // _lipSyncController.start();
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
                          
                          // 不再需要启动本地口型同步控制器
                          // 开始口型同步
                          // _lipSyncController.start();
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
                
                // 授权对话框作为页面的一部分，而不是弹窗
                if (_showActivationDialog && _activationCode != null)
                  Positioned.fill(
                    child: Container(
                      color: Colors.black54,
                      child: Center(
                        child: AlertDialog(
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
                                    _activationCode!,
                                    style: TextStyle(
                                      fontSize: XConst.spacer * 4,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  if (null != _activationUrl)
                                    Text(
                                      _activationUrl!,
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
                                    setState(() {
                                      _showActivationDialog = false;
                                      _activationCode = null;
                                      _activationUrl = null;
                                    });
                                    // 用户拒绝激活后，确保ChatBloc知道设备未授权
                                    if (kDebugMode) {
                                      print('ChatPage: User rejected activation, sending ChatUnauthorizedEvent');
                                    }
                                    chatBloc.add(ChatUnauthorizedEvent());
                                  },
                                  child: Text(AppLocalizations.of(context)!.reject),
                                ),
                                SizedBox(width: XConst.spacer),
                                FilledButton(
                                  onPressed: () async {
                                    if (kDebugMode) {
                                      print('ChatPage: User accepted activation, code copied and URL launched');
                                    }
                                    setState(() {
                                      _showActivationDialog = false;
                                      _activationCode = null;
                                      _activationUrl = null;
                                    });
                                    try {
                                      await Clipboard.setData(
                                        ClipboardData(text: _activationCode!),
                                      );
                                      if (null != _activationUrl) {
                                        await launchUrl(Uri.parse(_activationUrl!));
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
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
  
  // 构建状态指示器（连接状态、授权状态、录音管理状态和对话状态）
  Widget _buildStatusIndicators(dynamic connectionStatus, dynamic authorizationStatus, dynamic recordingStatus, dynamic conversationStatus) {
    // 使用字符串比较而不是枚举比较，以避免直接引用枚举类型
    String connectionString = connectionStatus.toString();
    String authorizationString = authorizationStatus.toString();
    String recordingString = recordingStatus.toString();
    String conversationString = conversationStatus.toString();
    
    // 添加调试日志
    if (kDebugMode) {
      print('ChatPage: _buildStatusIndicators - connectionStatus: $connectionString, authorizationStatus: $authorizationString, recordingStatus: $recordingString, conversationStatus: $conversationString');
    }
    
    // 构建状态指示器列表
    List<Widget> indicators = [];
    
    // 添加连接状态指示器
    IconData connectionIcon;
    Color connectionColor;
    String connectionTooltip;
    
    // 判断连接状态
    if (connectionString.contains('connected')) {
      connectionIcon = Icons.cloud_done;
      connectionColor = Colors.green;
      connectionTooltip = '已连接到服务器';
    } else if (connectionString.contains('connecting')) {
      connectionIcon = Icons.cloud_sync;
      connectionColor = Colors.orange;
      connectionTooltip = '正在连接服务器...';
    } else if (connectionString.contains('reconnecting')) {
      connectionIcon = Icons.cloud_sync;
      connectionColor = Colors.orange;
      connectionTooltip = '正在重连服务器...';
    } else if (connectionString.contains('disconnected')) {
      connectionIcon = Icons.cloud_off;
      connectionColor = Colors.red;
      connectionTooltip = '未连接到服务器';
    } else {
      // error状态或其他未知状态
      connectionIcon = Icons.error_outline;
      connectionColor = Colors.red;
      connectionTooltip = '连接错误';
    }
    
    // 添加连接状态指示器
    indicators.add(
      Tooltip(
        message: connectionTooltip,
        child: Icon(
          connectionIcon,
          color: connectionColor,
          size: 16,
        ),
      ),
    );
    
    // 如果正在重连，添加进度指示器
    if (connectionString.contains('reconnecting')) {
      indicators.add(
        Padding(
          padding: EdgeInsets.only(left: 4),
          child: SizedBox(
            width: 12,
            height: 12,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              valueColor: AlwaysStoppedAnimation<Color>(connectionColor),
            ),
          ),
        ),
      );
    }
    
    // 添加授权状态指示器
    // 使用更精确的字符串匹配，避免部分匹配问题
    if (authorizationString.contains('AuthorizationStatus.authorized')) {
      if (kDebugMode) {
        print('ChatPage: Adding authorized indicator (green security icon)');
      }
      indicators.add(
        Padding(
          padding: EdgeInsets.only(left: 4),
          child: Tooltip(
            message: '已授权',
            child: Icon(
              Icons.security,
              color: Colors.green,
              size: 16,
            ),
          ),
        ),
      );
    } else if (authorizationString.contains('AuthorizationStatus.unauthorized')) {
      if (kDebugMode) {
        print('ChatPage: Adding unauthorized indicator (orange security icon)');
      }
      indicators.add(
        Padding(
          padding: EdgeInsets.only(left: 4),
          child: Tooltip(
            message: '未授权',
            child: Icon(
              Icons.security,
              color: Colors.orange,
              size: 16,
            ),
          ),
        ),
      );
    } else {
      if (kDebugMode) {
        print('ChatPage: Unknown authorization status: $authorizationString');
      }
    }
    
    // 添加录音管理状态指示器
    if (recordingString.contains('RecordingStatus.initialized')) {
      indicators.add(
        Padding(
          padding: EdgeInsets.only(left: 4),
          child: Tooltip(
            message: '录音初始化',
            child: Icon(
              Icons.mic_none,
              color: Colors.blue,
              size: 16,
            ),
          ),
        ),
      );
    } else if (recordingString.contains('RecordingStatus.recording')) {
      indicators.add(
        Padding(
          padding: EdgeInsets.only(left: 4),
          child: Tooltip(
            message: '录音中',
            child: Icon(
              Icons.mic,
              color: Colors.red,
              size: 16,
            ),
          ),
        ),
      );
    } else if (recordingString.contains('RecordingStatus.error')) {
      indicators.add(
        Padding(
          padding: EdgeInsets.only(left: 4),
          child: Tooltip(
            message: '录音出错',
            child: Icon(
              Icons.mic_off,
              color: Colors.red,
              size: 16,
            ),
          ),
        ),
      );
    }
    
    // 添加对话状态指示器
    if (conversationString.contains('ConversationStatus.idle')) {
      indicators.add(
        Padding(
          padding: EdgeInsets.only(left: 4),
          child: Tooltip(
            message: '休闲中',
            child: Icon(
              Icons.coffee,
              color: Colors.grey,
              size: 16,
            ),
          ),
        ),
      );
    } else if (conversationString.contains('ConversationStatus.recording')) {
      indicators.add(
        Padding(
          padding: EdgeInsets.only(left: 4),
          child: Tooltip(
            message: '录音中',
            child: Icon(
              Icons.record_voice_over,
              color: Colors.red,
              size: 16,
            ),
          ),
        ),
      );
    } else if (conversationString.contains('ConversationStatus.playing')) {
      indicators.add(
        Padding(
          padding: EdgeInsets.only(left: 4),
          child: Tooltip(
            message: '播放中',
            child: Icon(
              Icons.volume_up,
              color: Colors.green,
              size: 16,
            ),
          ),
        ),
      );
    } else if (conversationString.contains('ConversationStatus.waiting')) {
      indicators.add(
        Padding(
          padding: EdgeInsets.only(left: 4),
          child: Tooltip(
            message: '等待中',
            child: Icon(
              Icons.hourglass_empty,
              color: Colors.orange,
              size: 16,
            ),
          ),
        ),
      );
    }
    
    return Row(
      children: indicators,
    );
  }
  
  /// 安全地触发表情，包含验证和错误处理
  void _triggerExpressionSafely(String expressionName) async {
    if (_live2DKey.currentState == null) {
      if (kDebugMode) {
        print('ChatPage: Live2D widget state is null, cannot trigger expression');
      }
      return;
    }
    
    try {
      // 直接使用语义化名称，让Android端处理映射
      if (kDebugMode) {
        print('ChatPage: Triggering expression: $expressionName');
      }
      
      // 调用原生方法触发表情，让Android端处理名称映射
      await (_live2DKey.currentState as dynamic).triggerExpression(expressionName);
      
      if (kDebugMode) {
        print('ChatPage: Expression triggered successfully: $expressionName');
      }
    } catch (e) {
      if (kDebugMode) {
        print('ChatPage: Error triggering expression: $expressionName, error: $e');
      }
      
      // 如果触发表情失败，尝试使用随机表情作为后备
      try {
        if (kDebugMode) {
          print('ChatPage: Falling back to random expression due to error');
        }
        await (_live2DKey.currentState as dynamic).triggerExpression('Random');
      } catch (fallbackError) {
        if (kDebugMode) {
          print('ChatPage: Even fallback to random expression failed: $fallbackError');
        }
      }
    }
  }
}