import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:xiaozhi/bloc/chat/chat_bloc.dart';
import 'package:xiaozhi/common/x_const.dart';
import 'package:xiaozhi/l10n/generated/app_localizations.dart';
import 'package:xiaozhi/model/storage_message.dart';
import 'package:xiaozhi/widget/fade_text_widget.dart';
import 'package:xiaozhi/widget/live2d_widget.dart';

class CallPage extends StatefulWidget {
  const CallPage({super.key});

  @override
  State<CallPage> createState() => _CallPageState();
}

class _CallPageState extends State<CallPage> with WidgetsBindingObserver {
  late ChatBloc chatBloc;
  final GlobalKey _live2DKey = GlobalKey(); // 使用不带泛型参数的GlobalKey

  StorageMessage? _message;

  @override
  void initState() {
    chatBloc = BlocProvider.of<ChatBloc>(context);
    chatBloc.add(ChatStartCallEvent());
    WidgetsBinding.instance.addObserver(this); // 添加观察者以监听页面可见性变化
    super.initState();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this); // 移除观察者
    chatBloc.add(ChatStopCallEvent());
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // 应用生命周期变化时处理Live2D实例
    if (_live2DKey.currentState != null) {
      if (state == AppLifecycleState.resumed) {
        // 使用新的公开方法
        (_live2DKey.currentWidget as Live2DWidget).activate();
      } else {
        // 使用新的公开方法
        (_live2DKey.currentWidget as Live2DWidget).deactivate();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final MediaQueryData mediaQuery = MediaQuery.of(context);

    return BlocListener(
      bloc: chatBloc,
      listener: (context, ChatState chatState) {
        if (chatState.messageList.isNotEmpty &&
            _message != chatState.messageList.first) {
          setState(() {
            _message = chatState.messageList.first;
          });
        }
      },
      child: Scaffold(
        appBar: AppBar(title: Text(AppLocalizations.of(context)!.call)),
        body: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              SizedBox(height: XConst.spacer),
              // 使用Live2DWidget替换原来的AnimatedMeshGradient
              Center(
                child: Live2DWidget(
                  key: _live2DKey, // 添加key以便访问widget状态
                  modelPath: "assets/live2d/Haru/Haru.model3.json",
                  width: mediaQuery.size.width * 0.4,
                  height: mediaQuery.size.width * 0.4,
                  instanceId: 'call_page_live2d', // 为这个实例指定特定ID
                ),
              ),
              Container(
                margin: EdgeInsets.symmetric(horizontal: XConst.spacer),
                alignment: Alignment.center,
                height: mediaQuery.size.width * 0.3,
                child:
                    null == _message
                        ? SizedBox.shrink()
                        : Container(
                          padding: EdgeInsets.symmetric(
                            vertical: XConst.spacer * 0.8,
                            horizontal: XConst.spacer,
                          ),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(
                              XConst.spacer * 1.5,
                            ),
                            color:
                                _message!.sendByMe
                                    ? Theme.of(
                                      context,
                                    ).colorScheme.primaryContainer
                                    : Theme.of(
                                      context,
                                    ).colorScheme.tertiaryContainer,
                          ),
                          child: FadeTextWidget(
                            text: _message!.text,
                            style: TextStyle(
                              color: Theme.of(context).primaryColor,
                            ),
                          ),
                        ),
              ),
              SizedBox(height: XConst.spacer),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  IconButton(
                    onPressed: () {
                      Navigator.of(context).pop();
                    },
                    icon: Container(
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: Theme.of(context).colorScheme.errorContainer,
                      ),
                      padding: const EdgeInsets.all(16),
                      child: Icon(
                        Icons.call_end,
                        color: Theme.of(context).colorScheme.error,
                        size: 40,
                      ),
                    ),
                  ),
                ],
              ),
              SizedBox(height: mediaQuery.padding.bottom + XConst.spacer),
            ],
          ),
        ),
      ),
    );
  }
}