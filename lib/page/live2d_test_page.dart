import 'package:flutter/material.dart';
import 'package:xiaozhi/widget/live2d_widget.dart';

class Live2DTestPage extends StatefulWidget {
  const Live2DTestPage({super.key});

  @override
  State<Live2DTestPage> createState() => _Live2DTestPageState();
}

class _Live2DTestPageState extends State<Live2DTestPage> {
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
                  modelPath: _selectedModel,
                  width: 300,
                  height: 500,
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
                    // 可以在这里添加触发特定动作的代码
                  },
                  child: const Text('Action 1'),
                ),
                ElevatedButton(
                  onPressed: () {
                    // 可以在这里添加触发特定表情的代码
                  },
                  child: const Text('Expression 1'),
                ),
                ElevatedButton(
                  onPressed: () {
                    // 可以在这里添加触发随机动作的代码
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