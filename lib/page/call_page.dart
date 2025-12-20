import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:mesh_gradient/mesh_gradient.dart';
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

class _CallPageState extends State<CallPage> {
  late ChatBloc chatBloc;
  
  // 添加GlobalKey用于访问Live2D组件状态
  final GlobalKey _live2DKey = GlobalKey();

  StorageMessage? _message;

  @override
  void initState() {
    chatBloc = BlocProvider.of<ChatBloc>(context);
    chatBloc.add(ChatStartCallEvent());
    super.initState();
  }

  @override
  void dispose() {
    chatBloc.add(ChatStopCallEvent());
    super.dispose();
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
        body: Stack(
          children: [
            // Live2D模型作为背景
            Positioned.fill(
              child: Live2DWidget(
                key: _live2DKey,
                modelPath: "assets/live2d/Haru/Haru.model3.json",
                width: mediaQuery.size.width,
                height: mediaQuery.size.height - AppBar().preferredSize.height - mediaQuery.padding.top,
                instanceId: 'call_page_live2d',
              ),
            ),
            // 原有内容覆盖在Live2D模型上方
            Column(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Spacer(),
                // 移除圆形图形，让Live2D模型作为背景显示
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
                Spacer(),
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
          ],
        ),
      ),
    );
  }
}
