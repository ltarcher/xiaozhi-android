import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class Live2DWidget extends StatefulWidget {
  final String modelPath;
  final double width;
  final double height;
  final String? instanceId;

  const Live2DWidget({
    super.key,
    required this.modelPath,
    required this.width,
    required this.height,
    this.instanceId,
  });

  @override
  State<Live2DWidget> createState() => _Live2DWidgetState();
  
  // 添加公共方法供外部调用
  Future<void> activate() async {
    final state = _Live2DWidgetState.maybeOf(this);
    await state?._activate();
  }
  
  Future<void> deactivate() async {
    final state = _Live2DWidgetState.maybeOf(this);
    await state?._deactivate();
  }
  
  Future<void> playMotion(String motionGroup, [int priority = 1]) async {
    final state = _Live2DWidgetState.maybeOf(this);
    await state?._playMotion(motionGroup, priority);
  }
  
  Future<void> triggerExpression(String expressionName) async {
    final state = _Live2DWidgetState.maybeOf(this);
    await state?._triggerExpression(expressionName);
  }
}

class _Live2DWidgetState extends State<Live2DWidget> {
  static const MethodChannel _channel = MethodChannel('live2d_channel');
  String? _actualInstanceId;
  bool _isActive = false;
  int? _viewId;
  bool _isDisposed = false;
  bool _needsRebuild = false;

  static final Map<_Live2DWidgetState, _Live2DWidgetState> _states = {};

  static _Live2DWidgetState? maybeOf(Live2DWidget widget) {
    for (final state in _states.keys) {
      if (state.widget == widget) {
        return state;
      }
    }
    return null;
  }

  @override
  void initState() {
    super.initState();
    _states[this] = this;
    // 如果没有提供instanceId，则使用widget的hashCode作为唯一标识
    _actualInstanceId = widget.instanceId ?? 'live2d_${widget.hashCode}';
    _isActive = true;
    if (kDebugMode) {
      print("Live2DWidget initState called with instanceId: $_actualInstanceId");
    }
    _initLive2D();
  }

  Future<void> _initLive2D() async {
    try {
      if (_isDisposed) return;
      
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
    if (_isDisposed) {
      if (kDebugMode) {
        print("Live2D activation skipped - widget disposed");
      }
      return;
    }
    
    if (_isActive) {
      if (kDebugMode) {
        print("Live2D activation skipped - already active");
      }
      return;
    }
    
    try {
      _isActive = true;
      if (kDebugMode) {
        print("Activating Live2D instance: $_actualInstanceId");
      }
      
      // 重新初始化Live2D，因为PlatformView可能已被销毁
      await _initLive2D();
      
      await _channel.invokeMethod('activateInstance', {
        'instanceId': _actualInstanceId,
      });
      
      // 强制刷新UI
      if (mounted) {
        if (kDebugMode) {
          print("Refreshing UI for Live2D instance: $_actualInstanceId");
        }
        // 标记需要重建PlatformView
        _needsRebuild = true;
        setState(() {});
      }
      
      if (kDebugMode) {
        print("Live2D instance activated successfully: $_actualInstanceId");
      }
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
    if (_isDisposed) {
      if (kDebugMode) {
        print("Live2D deactivation skipped - widget disposed");
      }
      return;
    }
    
    if (!_isActive) {
      if (kDebugMode) {
        print("Live2D deactivation skipped - already inactive");
      }
      return;
    }
    
    try {
      _isActive = false;
      if (kDebugMode) {
        print("Deactivating Live2D instance: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('deactivateInstance', {
        'instanceId': _actualInstanceId,
      });
      
      if (kDebugMode) {
        print("Live2D instance deactivated successfully: $_actualInstanceId");
      }
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
    if (_isDisposed) return;
    
    try {
      if (kDebugMode) {
        print("Live2D widget tapped at ($x, $y) on instance: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('onTap', {
        'x': x,
        'y': y,
        'instanceId': _actualInstanceId,
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
    if (_isDisposed) return;
    
    try {
      if (kDebugMode) {
        print("Triggering expression '$expressionName' on instance: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('triggerExpression', {
        'expressionName': expressionName,
        'instanceId': _actualInstanceId,
      });
      
      if (kDebugMode) {
        print("Expression '$expressionName' triggered successfully on instance: $_actualInstanceId");
      }
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
    if (_isDisposed) return;
    
    try {
      if (kDebugMode) {
        print("Playing motion '$motionGroup' with priority $priority on instance: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('playMotion', {
        'motionGroup': motionGroup,
        'priority': priority,
        'instanceId': _actualInstanceId,
      });
      
      if (kDebugMode) {
        print("Motion '$motionGroup' played successfully on instance: $_actualInstanceId");
      }
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
      
      // 如果需要重建PlatformView，则重新创建
      if (_needsRebuild) {
        if (kDebugMode) {
          print("Rebuilding Live2D PlatformView for instance: $_actualInstanceId");
        }
        _needsRebuild = false;
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
    _viewId = id;
  }

  @override
  void dispose() {
    if (kDebugMode) {
      print("Live2DWidget disposing instance: $_actualInstanceId");
    }
    _isDisposed = true;
    // 组件销毁时通知原生层清理资源
    _cleanup();
    _states.remove(this);
    super.dispose();
    if (kDebugMode) {
      print("Live2DWidget disposed instance: $_actualInstanceId");
    }
  }

  Future<void> _cleanup() async {
    if (_viewId == null) {
      if (kDebugMode) {
        print("Skipping cleanup - no viewId for instance: $_actualInstanceId");
      }
      return;
    }
    
    try {
      if (kDebugMode) {
        print("Cleaning up Live2D instance: $_actualInstanceId with viewId: $_viewId");
      }
      
      await _channel.invokeMethod('cleanupInstance', {
        'instanceId': _actualInstanceId,
        'viewId': _viewId,
      });
      
      if (kDebugMode) {
        print("Live2D instance cleaned up successfully: $_actualInstanceId");
      }
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