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
  
  // Live2D按钮可见性状态
  bool _isGearVisible = true;
  bool _isPowerVisible = false;

  @override
  void initState() {
    _otaUrlController = TextEditingController();
    _websocketUrlController = TextEditingController();
    _macAddressController = TextEditingController();
    
    // 加载现有的配置
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
    
    // 加载Live2D按钮配置
    _loadLive2DButtonSettings();
    
    super.initState();
  }
  
  // 加载Live2D按钮设置
  Future<void> _loadLive2DButtonSettings() async {
    try {
      SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
      bool? gearVisible = await prefsUtil.getLive2DGearVisible();
      bool? powerVisible = await prefsUtil.getLive2DPowerVisible();
      
      if (mounted) {
        setState(() {
          _isGearVisible = gearVisible ?? true; // 默认可见
          _isPowerVisible = powerVisible ?? false; // 默认不可见
        });
      }
    } catch (e) {
      if (mounted) {
        // 如果加载失败，使用默认值
        setState(() {
          _isGearVisible = true;
          _isPowerVisible = false;
        });
      }
    }
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
                // 保存URL配置
                await SharedPreferencesUtil().setOtaUrl(_otaUrlController.text);
                await SharedPreferencesUtil().setWebsocketUrl(
                  _websocketUrlController.text,
                );
                await SharedPreferencesUtil().setMacAddress(
                  _macAddressController.text,
                );
                
                // 保存Live2D按钮配置
                SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
                await prefsUtil.setLive2DGearVisible(_isGearVisible);
                await prefsUtil.setLive2DPowerVisible(_isPowerVisible);
                 
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
          // URL 配置部分
          Text(
            '网络配置',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.bold,
              color: primaryColor,
            ),
          ),
          SizedBox(height: XConst.spacer),
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
          
          // Live2D 按钮控制部分
          SizedBox(height: XConst.spacer * 3),
          Text(
            'Live2D 界面设置',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.bold,
              color: primaryColor,
            ),
          ),
          SizedBox(height: XConst.spacer),
          
          // 齿轮按钮开关
          Card(
            elevation: 2,
            child: ListTile(
              leading: Icon(
                Icons.settings_rounded,
                color: _isGearVisible ? primaryColor : Colors.grey,
              ),
              title: Text('齿轮按钮'),
              subtitle: Text('控制是否显示Live2D界面的齿轮按钮'),
              trailing: Switch(
                value: _isGearVisible,
                onChanged: (bool value) {
                  setState(() {
                    _isGearVisible = value;
                  });
                },
                activeColor: primaryColor,
              ),
            ),
          ),
          
          SizedBox(height: XConst.spacer),
          
          // 电源按钮开关
          Card(
            elevation: 2,
            child: ListTile(
              leading: Icon(
                Icons.power_settings_new_rounded,
                color: _isPowerVisible ? primaryColor : Colors.grey,
              ),
              title: Text('电源按钮'),
              subtitle: Text('控制是否显示Live2D界面的电源按钮'),
              trailing: Switch(
                value: _isPowerVisible,
                onChanged: (bool value) {
                  setState(() {
                    _isPowerVisible = value;
                  });
                },
                activeColor: primaryColor,
              ),
            ),
          ),
          
          SizedBox(height: XConst.spacer * 2),
          
          // 说明文字
          Card(
            elevation: 1,
            color: Theme.of(context).colorScheme.surfaceVariant,
            child: Padding(
              padding: EdgeInsets.all(XConst.spacer),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '使用说明',
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  SizedBox(height: XConst.spacer * 0.5),
                  Text(
                    '• 齿轮按钮：用于切换Live2D模型\n'
                    '• 电源按钮：关闭应用程序\n'
                    '• 设置会自动保存并在下次启动时生效',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
