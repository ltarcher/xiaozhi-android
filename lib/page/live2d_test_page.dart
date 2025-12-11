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
  // 默认使用的模型路径
  String _selectedModel = 'assets/live2d/Haru/Haru.model3.json';
  
  // 可选的模型列表
  final List<Map<String, String>> _models = [
    {'name': 'Haru', 'path': 'assets/live2d/Haru/Haru.model3.json'},
    {'name': 'Hiyori', 'path': 'assets/live2d/Hiyori/Hiyori.model3.json'},
    {'name': 'Mark', 'path': 'assets/live2d/Mark/Mark.model3.json'},
    {'name': 'Natori', 'path': 'assets/live2d/Natori/Natori.model3.json'},
    {'name': 'Rice', 'path': 'assets/live2d/Rice/Rice.model3.json'},
  ];

  final GlobalKey _live2DKey = GlobalKey(); // 使用不带泛型参数的GlobalKey

  @override
  void initState() {
    WidgetsBinding.instance.addObserver(this); // 添加观察者以监听页面可见性变化
    super.initState();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this); // 移除观察者
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // 应用生命周期变化时处理Live2D实例
    if (_live2DKey.currentState != null) {
      if (state == AppLifecycleState.resumed) {
        // 使用新的公开方法
        (_live2DKey.currentState as Live2DWidget).activate();
      } else {
        // 使用新的公开方法
        (_live2DKey.currentState as Live2DWidget).deactivate();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Live2D Test'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
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
                return DropdownMenuItem<String>(
                  value: model['path'],
                  child: Text(model['name']!),
                );
              }).toList(),
              onChanged: (value) {
                setState(() {
                  _selectedModel = value!;
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
                  instanceId: 'live2d_test_page_live2d', // 为这个实例指定特定ID
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
                    // 触发特定动作，使用新的公开方法
                    if (_live2DKey.currentWidget != null) {
                      (_live2DKey.currentWidget as Live2DWidget).playMotion('tap_body');
                    }
                  },
                  child: const Text('Action 1'),
                ),
                ElevatedButton(
                  onPressed: () {
                    // 触发特定表情，使用新的公开方法
                    if (_live2DKey.currentWidget != null) {
                      (_live2DKey.currentWidget as Live2DWidget).triggerExpression('Happy');
                    }
                  },
                  child: const Text('Expression 1'),
                ),
                ElevatedButton(
                  onPressed: () {
                    // 触发随机动作，使用新的公开方法
                    if (_live2DKey.currentWidget != null) {
                      (_live2DKey.currentWidget as Live2DWidget).playMotion('shake');
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