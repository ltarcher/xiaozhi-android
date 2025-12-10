import 'package:flutter/material.dart';
import 'package:xiaozhi/widget/live2d_viewer.dart';
import 'package:xiaozhi/util/live2d_controller.dart';

class IdlePage extends StatefulWidget {
  const IdlePage({super.key});

  @override
  State<IdlePage> createState() => _IdlePageState();
}

class _IdlePageState extends State<IdlePage> {
  bool _isLive2dInitialized = false;
  String _currentModel = 'Haru';

  @override
  void initState() {
    super.initState();
    _initializeLive2D();
  }

  Future<void> _initializeLive2D() async {
    bool result = await Live2dController.initLive2D();
    if (result) {
      // 加载默认模型
      await Live2dController.loadModel(_currentModel);
      
      setState(() {
        _isLive2dInitialized = true;
      });
    }
  }

  Future<void> _loadModel(String modelName) async {
    bool result = await Live2dController.loadModel(modelName);
    if (result) {
      setState(() {
        _currentModel = modelName;
      });
    }
  }

  Future<void> _playRandomMotion() async {
    List<String> motionGroups = ['idle', 'tap_body', 'pinch_in', 'pinch_out', 'shake', 'flick_head'];
    String randomGroup = motionGroups[DateTime.now().millisecondsSinceEpoch % motionGroups.length];
    int no = DateTime.now().millisecondsSinceEpoch % 3;
    await Live2dController.startMotion(randomGroup, no: no);
  }

  Future<void> _setRandomExpression() async {
    List<String> expressions = ['f01', 'f02', 'f03', 'f04', 'f05', 'f06', 'f07', 'f08'];
    String randomExpression = expressions[DateTime.now().millisecondsSinceEpoch % expressions.length];
    await Live2dController.setExpression(randomExpression);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            // Live2D Viewer - 占据大部分屏幕空间
            Expanded(
              flex: 3,
              child: Container(
                color: Colors.transparent,
                child: _isLive2dInitialized
                    ? Live2dViewer(
                        modelName: _currentModel,
                        width: double.infinity,
                        height: double.infinity,
                      )
                    : const Center(
                        child: CircularProgressIndicator(),
                      ),
              ),
            ),
            
            // 控制按钮区域
            Expanded(
              flex: 1,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    // 模型选择按钮
                    Wrap(
                      spacing: 8.0,
                      runSpacing: 4.0,
                      children: [
                        'Haru',
                        'Hiyori',
                        'Mao',
                        'Mark',
                        'Natori',
                        'Rice',
                        'Wanko'
                      ].map((modelName) {
                        return ElevatedButton(
                          onPressed: () => _loadModel(modelName),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: _currentModel == modelName
                                ? Theme.of(context).primaryColor
                                : null,
                          ),
                          child: Text(modelName),
                        );
                      }).toList(),
                    ),
                    
                    // 动作控制按钮
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        ElevatedButton(
                          onPressed: _playRandomMotion,
                          child: const Text('Play Motion'),
                        ),
                        ElevatedButton(
                          onPressed: _setRandomExpression,
                          child: const Text('Change Expression'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    Live2dController.dispose();
    super.dispose();
  }
}