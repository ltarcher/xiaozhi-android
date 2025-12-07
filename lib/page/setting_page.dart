import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:xiaozhi/bloc/chat/chat_bloc.dart';
import 'package:xiaozhi/common/x_const.dart';
import 'package:xiaozhi/l10n/generated/app_localizations.dart';
import 'package:xiaozhi/util/shared_preferences_util.dart';

class SettingPage extends StatefulWidget {
  const SettingPage({super.key});

  @override
  State<SettingPage> createState() => _SettingPageState();
}

class _SettingPageState extends State<SettingPage> {
  late TextEditingController _otaUrlController;
  late TextEditingController _websocketUrlController;
  late TextEditingController _macAddressController;

  @override
  void initState() {
    _otaUrlController = TextEditingController();
    _websocketUrlController = TextEditingController();
    _macAddressController = TextEditingController();
    SharedPreferencesUtil().getOtaUrl().then((v) {
      if (null != v) {
        _otaUrlController.text = v;
      }
    });
    SharedPreferencesUtil().getWebsocketUrl().then((v) {
      if (null != v) {
        _websocketUrlController.text = v;
      }
    });
    SharedPreferencesUtil().getMacAddress().then((v) {
      if (null != v) {
        _macAddressController.text = v;
      }
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.setting),
        actions: [
          IconButton(
            onPressed: () async {
              // 提前获取所有需要的本地化文本和主题，避免在异步操作后使用context
              final saveSuccessText = AppLocalizations.of(context)!.saveSuccess;
              final saveFailedText = AppLocalizations.of(context)!.saveFailed;
              final snackBarStyle = SnackBarBehavior.floating;
              final scaffoldMessenger = ScaffoldMessenger.of(context);
              final chatBloc = BlocProvider.of<ChatBloc>(context);
              
              try {
                await SharedPreferencesUtil().setOtaUrl(_otaUrlController.text);
                await SharedPreferencesUtil().setWebsocketUrl(
                  _websocketUrlController.text,
                );
                await SharedPreferencesUtil().setMacAddress(
                  _macAddressController.text,
                );
                
                if (!mounted) return;
                scaffoldMessenger.showSnackBar(
                  SnackBar(
                    content: Text(saveSuccessText),
                    behavior: snackBarStyle,
                  ),
                );
                
                if (!mounted) return;
                chatBloc.add(ChatInitialEvent());
              } catch (_) {
                if (!mounted) return;
                scaffoldMessenger.showSnackBar(
                  SnackBar(
                    content: Text(saveFailedText),
                    behavior: snackBarStyle,
                  ),
                );
              }
            },
            icon: Icon(
              Icons.save_rounded,
              color: primaryColor,
            ),
          ),
        ],
      ),
      body: ListView(
        padding: EdgeInsets.all(XConst.spacer),
        children: [
          TextField(
            controller: _otaUrlController,
            decoration: InputDecoration(
              labelText: AppLocalizations.of(context)!.otaUrl,
              border: OutlineInputBorder(),
              prefixIcon: Icon(Icons.cloud_download_rounded),
            ),
          ),
          SizedBox(height: XConst.spacer * 2),
          TextField(
            controller: _websocketUrlController,
            decoration: InputDecoration(
              labelText: AppLocalizations.of(context)!.websocketUrl,
              border: OutlineInputBorder(),
              prefixIcon: Icon(Icons.connect_without_contact_rounded),
            ),
          ),
          SizedBox(height: XConst.spacer * 2),
          TextField(
            controller: _macAddressController,
            decoration: InputDecoration(
              labelText: AppLocalizations.of(context)!.macAddress,
              helperText: AppLocalizations.of(context)!.macAddressEditTips,
              border: OutlineInputBorder(),
              prefixIcon: Icon(Icons.pin_rounded),
            ),
          ),
        ],
      ),
    );
  }
}
