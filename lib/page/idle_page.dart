import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:xiaozhi/util/live2d_manager.dart';
import 'package:xiaozhi/page/chat_page.dart';
import 'package:xiaozhi/page/setting_page.dart';

class IdlePage extends StatefulWidget {
  const IdlePage({super.key});

  @override
  State<IdlePage> createState() => _IdlePageState();
}

class _IdlePageState extends State<IdlePage> {
  bool _live2dReady = false;
  bool _live2dInitializationAttempted = false;
  bool _live2dViewCreated = false;

  @override
  void initState() {
    super.initState();
  }
  
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // 在didChangeDependencies中初始化Live2D，确保context可用
    if (!_live2dInitializationAttempted) {
      _live2dInitializationAttempted = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _initializeLive2D();
      });
    }
  }

  /// 初始化Live2D
  Future<void> _initializeLive2D() async {
    try {
      // 初始化Live2D引擎
      bool initSuccess = await Live2DManager().initialize();
      if (!initSuccess) {
        debugPrint("Failed to initialize Live2D engine");
        return;
      }
      
      // 标记视图即将创建
      if (mounted) {
        setState(() {
          _live2dViewCreated = true;
        });
      }
    } catch (e) {
      debugPrint("Live2D initialization error: $e");
    }
  }
  
  /// 在视图创建完成后加载模型
  Future<void> _loadModel() async {
    try {
      // 加载Haru模型
      bool loadSuccess = await Live2DManager().loadModel("assets/live2d/Haru/Haru.model3.json");
      if (!loadSuccess) {
        debugPrint("Failed to load Live2D model");
        return;
      }
      
      // 设置初始位置和缩放
      await Live2DManager().setPosition(0, 0);
      await Live2DManager().setScale(1.0);
      
      if (mounted) {
        setState(() {
          _live2dReady = true;
        });
        
        // 播放待机动画
        await Future.delayed(const Duration(milliseconds: 500));
        Live2DManager().startMotion("idle", 0);
      }
    } catch (e) {
      debugPrint("Live2D load model error: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          // Live2D显示区域
          if (_live2dReady)
            SizedBox(
              width: double.infinity,
              height: double.infinity,
              child: AndroidView(
                viewType: 'live2d_view',
                creationParams: <String, dynamic>{},
                creationParamsCodec: const StandardMessageCodec(),
                onPlatformViewCreated: (_) {
                  // 当平台视图创建完成后再加载模型
                  _loadModel();
                },
              ),
            )
          else if (_live2dViewCreated)
            SizedBox(
              width: double.infinity,
              height: double.infinity,
              child: AndroidView(
                viewType: 'live2d_view',
                creationParams: <String, dynamic>{},
                creationParamsCodec: const StandardMessageCodec(),
                onPlatformViewCreated: (_) {
                  // 当平台视图创建完成后再加载模型
                  _loadModel();
                },
              ),
            )
          else
            const Center(
              child: Text("Live2D not available"),
            ),
          
          // 页面顶部的操作按钮
          Positioned(
            top: MediaQuery.of(context).padding.top + 16,
            right: 16,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                // 设置按钮
                IconButton(
                  onPressed: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(builder: (context) => const SettingPage()),
                    );
                  },
                  icon: const Icon(Icons.settings_rounded),
                  color: Colors.white,
                  style: IconButton.styleFrom(
                    backgroundColor: Colors.black.withOpacity(0.3),
                  ),
                ),
                const SizedBox(width: 8),
                // 聊天按钮
                IconButton(
                  onPressed: () {
                    Navigator.of(context).push(
                      MaterialPageRoute(builder: (context) => const ChatPage()),
                    );
                  },
                  icon: const Icon(Icons.chat_rounded),
                  color: Colors.white,
                  style: IconButton.styleFrom(
                    backgroundColor: Colors.black.withOpacity(0.3),
                  ),
                ),
              ],
            ),
          ),
          
          // 页面底部的提示文字
          Positioned(
            bottom: MediaQuery.of(context).padding.bottom + 32,
            left: 0,
            right: 0,
            child: const Column(
              children: [
                Text(
                  "小智",
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                  textAlign: TextAlign.center,
                ),
                SizedBox(height: 8),
                Text(
                  "您的AI助手",
                  style: TextStyle(
                    fontSize: 16,
                    color: Colors.white70,
                  ),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}