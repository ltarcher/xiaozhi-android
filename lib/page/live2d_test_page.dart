import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:xiaozhi/widget/live2d_widget.dart';

class Live2DTestPage extends StatefulWidget {
  const Live2DTestPage({super.key});

  @override
  State<Live2DTestPage> createState() => _Live2DTestPageState();
}

class _Live2DTestPageState extends State<Live2DTestPage> with WidgetsBindingObserver {
  // 当前选择的模型路径
  late String _selectedModel;
  
  // 可选的模型列表
  final List<Map<String, String>> _models = [
    {'name': 'Haru', 'path': 'assets/live2d/Haru/Haru.model3.json'},
    {'name': 'Hiyori', 'path': 'assets/live2d/Hiyori/Hiyori.model3.json'},
    {'name': 'Mark', 'path': 'assets/live2d/Mark/Mark.model3.json'},
    {'name': 'Natori', 'path': 'assets/live2d/Natori/Natori.model3.json'},
    {'name': 'Rice', 'path': 'assets/live2d/Rice/Rice.model3.json'},
  ];

  final GlobalKey _live2DKey = GlobalKey(); // 使用不带泛型参数的GlobalKey
  bool _isPageActive = false;

  @override
  void initState() {
    super.initState();
    if (kDebugMode) {
      print('Live2DTestPage: initState called');
    }
    _selectedModel = _models[0]['path']!;
    _isPageActive = true;
    WidgetsBinding.instance.addObserver(this); // 添加观察者以监听页面可见性变化
    
    // 初始化完成后激活Live2D实例
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (kDebugMode) {
        print('Live2DTestPage: Post frame callback - activating Live2D instance');
      }
      _activateLive2DInstance();
    });
  }

  @override
  void dispose() {
    if (kDebugMode) {
      print('Live2DTestPage: dispose called');
    }
    _isPageActive = false;
    WidgetsBinding.instance.removeObserver(this); // 移除观察者
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (kDebugMode) {
      print('Live2DTestPage: didChangeAppLifecycleState called with state: $state');
    }
    // 应用生命周期变化时处理Live2D实例
    if (_live2DKey.currentWidget != null && _isPageActive) {
      if (state == AppLifecycleState.resumed) {
        if (kDebugMode) {
          print('Live2DTestPage: App resumed - activating Live2D instance');
        }
        // 使用新的公开方法
        (_live2DKey.currentWidget as Live2DWidget).activate();
      } else if (state == AppLifecycleState.paused) {
        if (kDebugMode) {
          print('Live2DTestPage: App paused - deactivating Live2D instance');
        }
        // 使用新的公开方法
        (_live2DKey.currentWidget as Live2DWidget).deactivate();
      }
    }
  }
  
  // 激活Live2D实例的辅助方法
  void _activateLive2DInstance() {
    if (kDebugMode) {
      print('Live2DTestPage: _activateLive2DInstance called, pageActive: $_isPageActive');
      print('Live2DTestPage: Current widget is ${_live2DKey.currentWidget}');
    }
    
    if (_live2DKey.currentWidget != null && _isPageActive) {
      if (kDebugMode) {
        print('Live2DTestPage: Calling activate on Live2DWidget');
      }
      (_live2DKey.currentWidget as Live2DWidget).activate();
    } else {
      if (kDebugMode) {
        print('Live2DTestPage: Live2DWidget is null or page not active, cannot activate');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (kDebugMode) {
      print('Live2DTestPage: build called with selected model: $_selectedModel');
    }
    
    return Scaffold(
      appBar: AppBar(
        title: const Text('Live2D Test'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            if (kDebugMode) {
              print('Live2DTestPage: Back button pressed');
            }
            Navigator.of(context).pop();
          },
        ),
      ),
      body: Column(
        children: [
          // 模型选择器
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: DropdownButtonFormField<String>(
              decoration: const InputDecoration(
                labelText: 'Select Model',
                border: OutlineInputBorder(),
              ),
              value: _selectedModel,
              items: _models.map((model) {
                if (kDebugMode) {
                  print('Live2DTestPage: Adding model option: ${model['name']} -> ${model['path']}');
                }
                return DropdownMenuItem<String>(
                  value: model['path'],
                  child: Text(model['name']!),
                );
              }).toList(),
              onChanged: (value) {
                if (kDebugMode) {
                  print('Live2DTestPage: Model selection changed to: $value');
                }
                setState(() {
                  _selectedModel = value!;
                  
                  // 模型路径改变后激活实例
                  WidgetsBinding.instance.addPostFrameCallback((_) {
                    if (kDebugMode) {
                      print('Live2DTestPage: Post frame callback after model change');
                    }
                    _activateLive2DInstance();
                  });
                });
              },
            ),
          ),
          
          // Live2D展示区域
          Expanded(
            child: Center(
              child: Container(
                width: 300,
                height: 500,
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey),
                ),
                child: Live2DWidget(
                  key: _live2DKey, // 添加key以便访问widget状态
                  modelPath: _selectedModel,
                  width: 300,
                  height: 500,
                  // 移除instanceId参数，使用单实例模式
                ),
              ),
            ),
          ),
          
          // 控制按钮
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                ElevatedButton(
                  onPressed: () {
                    if (kDebugMode) {
                      print('Live2DTestPage: Action 1 button pressed');
                    }
                    // 触发特定动作，使用新的公开方法
                    if (_live2DKey.currentWidget != null && _isPageActive) {
                      if (kDebugMode) {
                        print('Live2DTestPage: Calling playMotion(tap_body)');
                      }
                      (_live2DKey.currentWidget as Live2DWidget).playMotion('tap_body');
                    } else {
                      if (kDebugMode) {
                        print('Live2DTestPage: Live2DWidget is null or page not active, cannot play motion');
                      }
                    }
                  },
                  child: const Text('Action 1'),
                ),
                ElevatedButton(
                  onPressed: () {
                    if (kDebugMode) {
                      print('Live2DTestPage: Expression 1 button pressed');
                    }
                    // 触发特定表情，使用新的公开方法
                    if (_live2DKey.currentWidget != null && _isPageActive) {
                      if (kDebugMode) {
                        print('Live2DTestPage: Calling triggerExpression(Happy)');
                      }
                      (_live2DKey.currentWidget as Live2DWidget).triggerExpression('Happy');
                    } else {
                      if (kDebugMode) {
                        print('Live2DTestPage: Live2DWidget is null or page not active, cannot trigger expression');
                      }
                    }
                  },
                  child: const Text('Expression 1'),
                ),
                ElevatedButton(
                  onPressed: () {
                    if (kDebugMode) {
                      print('Live2DTestPage: Random button pressed');
                    }
                    // 触发随机动作，使用新的公开方法
                    if (_live2DKey.currentWidget != null && _isPageActive) {
                      if (kDebugMode) {
                        print('Live2DTestPage: Calling playMotion(shake)');
                      }
                      (_live2DKey.currentWidget as Live2DWidget).playMotion('shake');
                    } else {
                      if (kDebugMode) {
                        print('Live2DTestPage: Live2DWidget is null or page not active, cannot play motion');
                      }
                    }
                  },
                  child: const Text('Random'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}