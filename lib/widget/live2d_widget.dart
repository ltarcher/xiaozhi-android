import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class Live2DWidget extends StatefulWidget {
  final String modelPath;
  final double width;
  final double height;
  final String? instanceId; // 新增：实例ID，用于区分不同页面的Live2D实例

  const Live2DWidget({
    super.key,
    required this.modelPath,
    required this.width,
    required this.height,
    this.instanceId, // 可选的实例ID参数
  });

  @override
  State<Live2DWidget> createState() => _Live2DWidgetState();
  
  // 添加公共方法供外部调用
  Future<void> activate() {
    // 这些方法将在页面中通过key.currentWidget直接调用
    // 实现留在State类中
    return Future.value();
  }
  
  Future<void> deactivate() {
    return Future.value();
  }
  
  Future<void> playMotion(String motionGroup, [int priority = 1]) {
    return Future.value();
  }
  
  Future<void> triggerExpression(String expressionName) {
    return Future.value();
  }
}

class _Live2DWidgetState extends State<Live2DWidget> {
  static const MethodChannel _channel = MethodChannel('live2d_channel');
  String? _actualInstanceId; // 实际使用的实例ID

  @override
  void initState() {
    super.initState();
    // 如果没有提供instanceId，则使用widget的hashCode作为唯一标识
    _actualInstanceId = widget.instanceId ?? 'live2d_${widget.hashCode}';
    _initLive2D();
  }

  Future<void> _initLive2D() async {
    try {
      if (kDebugMode) {
        print("Initializing Live2D with model path: ${widget.modelPath}, instanceId: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('initLive2D', {
        'modelPath': widget.modelPath,
        'instanceId': _actualInstanceId,
      });
      
      if (kDebugMode) {
        print("Live2D initialized successfully for instance: $_actualInstanceId");
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Failed to init Live2D: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Failed to init Live2D - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Unexpected error initializing Live2D: $e");
      }
    }
  }

  /// 激活当前实例（当页面变为可见时调用）
  Future<void> _activate() async {
    try {
      if (kDebugMode) {
        print("Activating Live2D instance: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('activateInstance', {
        'instanceId': _actualInstanceId,
      });
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Failed to activate Live2D instance: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Unexpected error activating Live2D instance: $e");
      }
    }
  }

  /// 停用当前实例（当页面变为不可见时调用）
  Future<void> _deactivate() async {
    try {
      if (kDebugMode) {
        print("Deactivating Live2D instance: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('deactivateInstance', {
        'instanceId': _actualInstanceId,
      });
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Failed to deactivate Live2D instance: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Unexpected error deactivating Live2D instance: $e");
      }
    }
  }

  Future<void> onTap(double x, double y) async {
    try {
      await _channel.invokeMethod('onTap', {
        'x': x,
        'y': y,
        'instanceId': _actualInstanceId, // 传递实例ID
      });
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Failed to handle tap: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Failed to handle tap - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Unexpected error handling tap: $e");
      }
    }
  }

  // 添加触发表情动作的方法
  Future<void> _triggerExpression(String expressionName) async {
    try {
      await _channel.invokeMethod('triggerExpression', {
        'expressionName': expressionName,
        'instanceId': _actualInstanceId, // 传递实例ID
      });
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Failed to trigger expression: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Failed to trigger expression - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Unexpected error triggering expression: $e");
      }
    }
  }

  // 添加触发动作的方法
  Future<void> _playMotion(String motionGroup, [int priority = 1]) async {
    try {
      await _channel.invokeMethod('playMotion', {
        'motionGroup': motionGroup,
        'priority': priority,
        'instanceId': _actualInstanceId, // 传递实例ID
      });
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Failed to play motion: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Failed to play motion - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Unexpected error playing motion: $e");
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    // 在Android平台上使用AndroidView来嵌入原生视图
    if (defaultTargetPlatform == TargetPlatform.android) {
      if (kDebugMode) {
        print("Building Live2D AndroidView with params: ${widget.modelPath}, instanceId: $_actualInstanceId");
      }
      
      return SizedBox(
        width: widget.width,
        height: widget.height,
        child: GestureDetector(
          onTapUp: (details) {
            if (kDebugMode) {
              print("Live2D widget tapped on instance: $_actualInstanceId");
            }
            
            final RenderBox renderBox = context.findRenderObject() as RenderBox;
            final position = renderBox.globalToLocal(details.globalPosition);
            onTap(position.dx, position.dy);
          },
          child: AndroidView(
            viewType: 'live2d_view',
            creationParams: {
              'modelPath': widget.modelPath,
              'instanceId': _actualInstanceId, // 传递实例ID给原生层
            },
            creationParamsCodec: const StandardMessageCodec(),
            onPlatformViewCreated: _onPlatformViewCreated,
          ),
        ),
      );
    }

    // 对于非Android平台，显示一个占位符
    return Container(
      width: widget.width,
      height: widget.height,
      color: Colors.grey[300],
      child: const Center(
        child: Text('Live2D Widget (仅支持Android)'),
      ),
    );
  }

  void _onPlatformViewCreated(int id) {
    if (kDebugMode) {
      print("Live2D platform view created with id: $id, instanceId: $_actualInstanceId");
    }
  }

  @override
  void dispose() {
    // 组件销毁时通知原生层清理资源
    _cleanup();
    super.dispose();
  }

  Future<void> _cleanup() async {
    try {
      if (kDebugMode) {
        print("Cleaning up Live2D instance: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('cleanupInstance', {
        'instanceId': _actualInstanceId,
      });
    } catch (e) {
      if (kDebugMode) {
        print("Error cleaning up Live2D instance: $e");
      }
    }
  }
  
  // 为了方便从外部调用，我们将这些方法暴露出去
  Future<void> activate() => _activate();
  Future<void> deactivate() => _deactivate();
  Future<void> playMotion(String motionGroup, [int priority = 1]) => _playMotion(motionGroup, priority);
  Future<void> triggerExpression(String expressionName) => _triggerExpression(expressionName);
  
}