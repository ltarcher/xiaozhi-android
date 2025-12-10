import 'package:flutter/material.dart';
import 'package:xiaozhi/util/live2d_controller.dart';
import 'package:xiaozhi/widget/live2d_viewer.dart';

class Live2dDemoPage extends StatefulWidget {
  const Live2dDemoPage({super.key});

  @override
  State<Live2dDemoPage> createState() => _Live2dDemoPageState();
}

class _Live2dDemoPageState extends State<Live2dDemoPage> {
  bool _isLive2dInitialized = false;
  String _currentModel = 'Haru';
  double _lipSyncValue = 0.0;

  @override
  void initState() {
    super.initState();
    _initializeLive2D();
  }

  Future<void> _initializeLive2D() async {
    bool result = await Live2dController.initLive2D();
    if (result) {
      setState(() {
        _isLive2dInitialized = true;
      });
      
      // 加载默认模型
      await Live2dController.loadModel(_currentModel);
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

  Future<void> _updateLipSyncValue(double value) async {
    setState(() {
      _lipSyncValue = value;
    });
    await Live2dController.setLipSyncValue(value);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Live2D Demo'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Live2D Viewer
            Container(
              height: 300,
              color: Colors.grey[200],
              child: _isLive2dInitialized
                  ? Live2dViewer(
                      modelName: _currentModel,
                      width: double.infinity,
                      height: 300,
                    )
                  : const Center(
                      child: CircularProgressIndicator(),
                    ),
            ),
            const SizedBox(height: 20),
            
            // Model Selection
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
            const SizedBox(height: 20),
            
            // Motion Controls
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
            const SizedBox(height: 20),
            
            // Lip Sync Slider
            const Text('Lip Sync Control'),
            Slider(
              value: _lipSyncValue,
              min: 0.0,
              max: 1.0,
              onChanged: _updateLipSyncValue,
            ),
            Text('Value: ${_lipSyncValue.toStringAsFixed(2)}'),
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