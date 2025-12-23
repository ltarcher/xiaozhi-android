import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
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
  late TextEditingController _wakeWordController;
  
  // Live2D按钮可见性状态
  bool _isGearVisible = true;
  bool _isPowerVisible = false;
  
  // 语音唤醒状态
  bool _isVoiceWakeUpEnabled = false;
  
  // Live2D模型选择相关
  List<String> _modelList = [];
  int _currentModelIndex = 0;
  bool _modelListLoaded = false;
  
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
    
    // 加载Live2D模型列表和当前选择的模型
    _loadLive2DModelSettings();
    
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
  
  // 加载Live2D模型设置
  Future<void> _loadLive2DModelSettings() async {
    try {
      SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
      int? modelIndex = await prefsUtil.getLive2DModelIndex();
      
      if (mounted) {
        setState(() {
          _currentModelIndex = modelIndex ?? 0; // 默认选择第一个模型
        });
      }
      
      // 获取模型列表
      await _getModelList();
    } catch (e) {
      if (kDebugMode) {
        print("SettingPage: Error loading Live2D model settings: $e");
      }
    }
  }
  
  // 从原生Android获取模型列表
  Future<void> _getModelList() async {
    try {
      if (kDebugMode) {
        print("SettingPage: Getting model list from native Android");
      }
      
      final List<dynamic>? modelList = await _live2dChannel.invokeListMethod('getModelList');
      
      if (modelList != null && mounted) {
        setState(() {
          _modelList = modelList.cast<String>();
          _modelListLoaded = true;
          
          // 如果当前模型索引超出范围，重置为0
          if (_currentModelIndex >= _modelList.length) {
            _currentModelIndex = 0;
          }
        });
        
        if (kDebugMode) {
          print("SettingPage: Model list loaded: $_modelList");
        }
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("SettingPage: Failed to get model list due to PlatformException: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("SettingPage: Failed to get model list - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("SettingPage: Unexpected error getting model list: $e");
      }
    }
  }
  
  // 切换模型
  Future<void> _changeModel(int index) async {
    try {
      if (kDebugMode) {
        print("SettingPage: Changing model to index: $index");
        print("SettingPage: Available models: $_modelList");
        print("SettingPage: Total model count: ${_modelList.length}");
      }
      
      // 检查索引是否有效
      if (index < 0 || index >= _modelList.length) {
        if (kDebugMode) {
          print("SettingPage: Invalid model index: $index, model count: ${_modelList.length}");
          print("SettingPage: Model list details: ${_modelList.map((m) => '$m').join(', ')}");
        }
        
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('无效的模型索引'),
              backgroundColor: Colors.red,
            ),
          );
        }
        return;
      }
      
      if (kDebugMode) {
        print("SettingPage: About to call native changeModel with index: $index, model: ${_modelList[index]}");
      }
      
      // 通知原生Android切换模型
      await _live2dChannel.invokeMethod('changeModel', {
        'index': index,
      });
      
      if (kDebugMode) {
        print("SettingPage: Native changeModel call completed successfully");
      }
      
      // 保存新的模型索引
      SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
      await prefsUtil.setLive2DModelIndex(index);
      
      if (mounted) {
        setState(() {
          _currentModelIndex = index;
        });
        
        // 显示成功提示
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('模型已切换到: ${_modelList[index]}'),
            backgroundColor: Colors.green,
          ),
        );
      }
      
      if (kDebugMode) {
        print("SettingPage: Model changed successfully to index: $index, model: ${_modelList[index]}");
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("SettingPage: Failed to change model due to PlatformException: ${e.message}");
        print("SettingPage: PlatformException details: ${e.details}");
      }
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('模型切换失败: ${e.message ?? "未知错误"}'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("SettingPage: Failed to change model - Missing plugin: ${e.message}");
      }
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('模型切换功能不可用'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } catch (e) {
      if (kDebugMode) {
        print("SettingPage: Unexpected error changing model: $e");
      }
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('模型切换失败，请重试'),
            backgroundColor: Colors.red,
          ),
        );
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
                
                // 保存Live2D按钮配置和唤醒词配置
                SharedPreferencesUtil prefsUtil = SharedPreferencesUtil();
                await prefsUtil.setLive2DGearVisible(_isGearVisible);
                await prefsUtil.setLive2DPowerVisible(_isPowerVisible);
                await prefsUtil.setWakeWord(_wakeWordController.text.trim());
                // 保存Live2D模型索引
                await prefsUtil.setLive2DModelIndex(_currentModelIndex);
                 
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
          
          // 模型选择下拉框
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
                        Icons.face_rounded,
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
                  _modelListLoaded
                      ? DropdownButtonFormField<int>(
                          value: _currentModelIndex,
                          decoration: InputDecoration(
                            border: OutlineInputBorder(),
                            prefixIcon: Icon(Icons.person_outline),
                            helperText: '选择要显示的Live2D模型',
                          ),
                          items: _modelList.asMap().entries.map<DropdownMenuItem<int>>((entry) {
                            return DropdownMenuItem<int>(
                              value: entry.key,
                              child: Text(entry.value),
                            );
                          }).toList(),
                          onChanged: (int? newIndex) {
                            if (newIndex != null && newIndex != _currentModelIndex) {
                              _changeModel(newIndex);
                            }
                          },
                        )
                      : Row(
                          children: [
                            SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                valueColor: AlwaysStoppedAnimation<Color>(primaryColor),
                              ),
                            ),
                            SizedBox(width: 16),
                            Text('加载模型列表中...'),
                          ],
                        ),
                ],
              ),
            ),
          ),
          
          // Live2D 按钮控制部分
          SizedBox(height: XConst.spacer * 3),
          /*
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
                    '• Live2D模型：选择要显示的Live2D角色模型\n'
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
          */
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
