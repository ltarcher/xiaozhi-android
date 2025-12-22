import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:xiaozhi/bloc/chat/chat_bloc.dart';
import 'package:xiaozhi/common/x_const.dart';
import 'package:xiaozhi/l10n/generated/app_localizations.dart';
import 'package:xiaozhi/util/shared_preferences_util.dart';
import 'dart:io';

class SettingPage extends StatefulWidget {
  const SettingPage({super.key});

  @override
  State<SettingPage> createState() => _SettingPageState();
}

class _SettingPageState extends State<SettingPage> {
  late TextEditingController _otaUrlController;
  late TextEditingController _websocketUrlController;
  late TextEditingController _macAddressController;
  late TextEditingController _wakeWordController;
  
  // Live2D按钮可见性状态
  bool _isGearVisible = true;
  bool _isPowerVisible = false;
  
  // 语音唤醒状态
  bool _isVoiceWakeUpEnabled = false;
  
  // Live2D模型选择
  String _selectedLive2DModel = 'Haru';
  List<String> _availableLive2DModels = [];
  
  // Live2D控制通道
  static const MethodChannel _live2dChannel = MethodChannel('live2d_channel');

  @override
  void initState() {
    _otaUrlController = TextEditingController();
    _websocketUrlController = TextEditingController();
    _macAddressController = TextEditingController();
    _wakeWordController = TextEditingController();
    
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
    
    // 加载唤醒词配置
    _loadWakeWordSettings();
    
    // 加载Live2D按钮配置
    _loadLive2DButtonSettings();
    
    // 加载Live2D模型配置
    _loadLive2DModelSettings();
    
    // 扫描可用的Live2D模型
    _scanAvailableLive2DModels();
    
    super.initState();
  }
  
  // 扫描可用的Live2D模型
  Future<void> _scanAvailableLive2DModels() async {
    try {
      List<String> models = [];
      
      // Live2D模型目录
      final live2dDir = Directory('assets/live2d');
      
      if (await live2dDir.exists()) {
        await for (final entity in live2dDir.list()) {
          if (entity is Directory && !entity.path.startsWith('.')) {
            final dirName = entity.path.split('/').last;
            // 检查目录是否包含模型文件
            final modelFile = File('${entity.path}/${dirName}.model3.json');
            if (await modelFile.exists()) {
              models.add(dirName);
            }
          }
        }
      }
      
      if (mounted) {
        setState(() {
          _availableLive2DModels = models;
          if (!_availableLive2DModels.contains(_selectedLive2DModel)) {
            _selectedLive2DModel = _availableLive2DModels.isNotEmpty ? _availableLive2DModels.first : 'Haru';
          }
        });
      }
    } catch (e) {
      if (kDebugMode) {
        print("SettingPage: Error scanning Live2D models: $e");
      }
      
      // 如果扫描失败，使用默认模型列表
      if (mounted) {
        setState(() {
          _availableLive2DModels = ['Hiyori', 'Haru', 'Mao', 'Mark', 'Natori', 'Rice', 'Wanko'];
        });
      }
    }
  }
  
  // 加载Live2D模型设置
  Future<void> _loadLive2DModelSettings() async {
    try {
      SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
      String? live2dModel = await prefsUtil.getLive2DModel();
      
      if (mounted) {
        setState(() {
          _selectedLive2DModel = live2dModel ?? 'Haru';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _selectedLive2DModel = 'Haru';
        });
      }
    }
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
  
  // 加载唤醒词设置
  Future<void> _loadWakeWordSettings() async {
    try {
      SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
      String? wakeWord = await prefsUtil.getWakeWord();
      
      if (mounted) {
        setState(() {
          _wakeWordController.text = wakeWord ?? '你好，小清';
          _isVoiceWakeUpEnabled = true; // 默认启用语音唤醒
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _wakeWordController.text = '你好，小清';
          _isVoiceWakeUpEnabled = true; // 默认启用语音唤醒
        });
      }
    }
  }
  
  // 立即应用Live2D按钮状态的方法
  Future<void> _applyLive2DButtonStates() async {
    try {
      if (kDebugMode) {
        print("SettingPage: Applying Live2D button states - gear: $_isGearVisible, power: $_isPowerVisible");
      }
      
      // 直接通过MethodChannel控制Live2D实例的按钮可见性
      await _live2dChannel.invokeMethod('setGearVisible', {
        'visible': _isGearVisible,
      });
      
      await _live2dChannel.invokeMethod('setPowerVisible', {
        'visible': _isPowerVisible,
      });
      
      if (kDebugMode) {
        print("SettingPage: Live2D button states applied successfully");
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("SettingPage: Failed to apply Live2D button states due to PlatformException: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("SettingPage: Failed to apply Live2D button states - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("SettingPage: Unexpected error applying Live2D button states: $e");
      }
    }
  }
  
  // 立即应用Live2D模型更改的方法
  Future<void> _applyLive2DModelChange() async {
    try {
      if (kDebugMode) {
        print("SettingPage: Applying Live2D model change to: $_selectedLive2DModel");
      }
      
      // 直接通过MethodChannel更新Live2D模型
      final newModelPath = "assets/live2d/$_selectedLive2DModel/$_selectedLive2DModel.model3.json";
      await _live2dChannel.invokeMethod('updateModel', {
        'modelPath': newModelPath,
      });
      
      if (kDebugMode) {
        print("SettingPage: Live2D model applied successfully");
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("SettingPage: Failed to apply Live2D model due to PlatformException: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("SettingPage: Failed to apply Live2D model - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("SettingPage: Unexpected error applying Live2D model: $e");
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
                
                // 保存Live2D按钮配置、唤醒词配置和模型配置
                SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
                await prefsUtil.setLive2DGearVisible(_isGearVisible);
                await prefsUtil.setLive2DPowerVisible(_isPowerVisible);
                await prefsUtil.setWakeWord(_wakeWordController.text.trim());
                await prefsUtil.setLive2DModel(_selectedLive2DModel);
                
                // 立即应用Live2D模型更改
                await _applyLive2DModelChange();
                  
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
          
          // Live2D 模型选择部分
          SizedBox(height: XConst.spacer * 3),
          Text(
            'Live2D 模型设置',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.bold,
              color: primaryColor,
            ),
          ),
          SizedBox(height: XConst.spacer),
          Card(
            elevation: 2,
            child: Padding(
              padding: EdgeInsets.all(XConst.spacer),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        Icons.person_rounded,
                        color: primaryColor,
                        size: 20,
                      ),
                      SizedBox(width: 8),
                      Text(
                        'Live2D模型',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: primaryColor,
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: XConst.spacer),
                  DropdownButtonFormField<String>(
                    value: _selectedLive2DModel,
                    decoration: InputDecoration(
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.face),
                      hintText: '选择Live2D模型',
                    ),
                    items: _availableLive2DModels.map((String model) {
                      return DropdownMenuItem<String>(
                        value: model,
                        child: Text(model),
                      );
                    }).toList(),
                    onChanged: (String? newValue) {
                      if (newValue != null) {
                        setState(() {
                          _selectedLive2DModel = newValue;
                        });
                      }
                    },
                  ),
                  SizedBox(height: XConst.spacer * 0.5),
                  Text(
                    '选择Live2D模型后会立即生效',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Colors.green,
                    ),
                  ),
                ],
              ),
            ),
          ),
          
          SizedBox(height: XConst.spacer * 2),
          
          // Live2D 按钮控制部分
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
                onChanged: (bool value) async {
                  setState(() {
                    _isGearVisible = value;
                  });
                  
                  // 立即应用设置到Live2D实例
                  await _applyLive2DButtonStates();
                  
                  // 同时保存到持久化存储
                  try {
                    SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
                    await prefsUtil.setLive2DGearVisible(_isGearVisible);
                    if (kDebugMode) {
                      print("SettingPage: Gear visibility saved to preferences: $_isGearVisible");
                    }
                  } catch (e) {
                    if (kDebugMode) {
                      print("SettingPage: Error saving gear visibility to preferences: $e");
                    }
                  }
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
                onChanged: (bool value) async {
                  setState(() {
                    _isPowerVisible = value;
                  });
                  
                  // 立即应用设置到Live2D实例
                  await _applyLive2DButtonStates();
                  
                  // 同时保存到持久化存储
                  try {
                    SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
                    await prefsUtil.setLive2DPowerVisible(_isPowerVisible);
                    if (kDebugMode) {
                      print("SettingPage: Power visibility saved to preferences: $_isPowerVisible");
                    }
                  } catch (e) {
                    if (kDebugMode) {
                      print("SettingPage: Error saving power visibility to preferences: $e");
                    }
                  }
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
                    '• Live2D模型：选择不同的虚拟角色形象（立即生效）\n'
                    '• 齿轮按钮：用于切换Live2D模型\n'
                    '• 电源按钮：关闭应用程序\n'
                    '• 语音唤醒：通过唤醒词启动对话\n'
                    '• 设置会立即生效并自动保存',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
          ),
          
          // 语音唤醒设置部分
          Text(
            '语音唤醒设置',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.bold,
              color: primaryColor,
            ),
          ),
          SizedBox(height: XConst.spacer),
          
          // 语音唤醒开关
          Card(
            elevation: 2,
            child: ListTile(
              leading: Icon(
                Icons.mic_rounded,
                color: _isVoiceWakeUpEnabled ? primaryColor : Colors.grey,
              ),
              title: Text('语音唤醒'),
              subtitle: Text('开启后可以通过语音唤醒助手'),
              trailing: Switch(
                value: _isVoiceWakeUpEnabled,
                onChanged: (bool value) {
                  setState(() {
                    _isVoiceWakeUpEnabled = value;
                  });
                },
                activeColor: primaryColor,
              ),
            ),
          ),
          
          SizedBox(height: XConst.spacer),
          
          // 唤醒词设置
          Card(
            elevation: 2,
            child: Padding(
              padding: EdgeInsets.all(XConst.spacer),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        Icons.record_voice_over_rounded,
                        color: primaryColor,
                        size: 20,
                      ),
                      SizedBox(width: 8),
                      Text(
                        '唤醒词',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: primaryColor,
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: XConst.spacer),
                  TextField(
                    controller: _wakeWordController,
                    enabled: _isVoiceWakeUpEnabled,
                    decoration: InputDecoration(
                      hintText: '请输入唤醒词，如"你好，小清"',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.mic),
                      helperText: '建议使用简短易识别的唤醒词',
                    ),
                    maxLength: 20,
                  ),
                ],
              ),
            ),
          ),
          
          SizedBox(height: XConst.spacer * 2),
          
        ],
      ),
    );
  }
}
