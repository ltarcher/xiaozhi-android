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
  
  // 添加公共方法供外部调用 - 直接返回Future，让外部通过GlobalKey调用State方法
  Future<void> activate() async {
    // 这个方法将在State中实现
    return Future.value();
  }
  
  Future<void> deactivate() async {
    return Future.value();
  }
  
  Future<void> playMotion(String motionGroup, [int priority = 1]) async {
    return Future.value();
  }
  
  Future<void> triggerExpression(String expressionName) async {
    return Future.value();
  }
  
  // 添加控制按钮可见性的方法
  Future<void> setGearVisible(bool visible) async {
    return Future.value();
  }
  
  Future<void> setPowerVisible(bool visible) async {
    return Future.value();
  }
  
  Future<bool?> isGearVisible() async {
    return Future.value(null);
  }
  
  Future<bool?> isPowerVisible() async {
    return Future.value(null);
  }
}

class _Live2DWidgetState extends State<Live2DWidget> {
  static const MethodChannel _channel = MethodChannel('live2d_channel');
  String? _actualInstanceId; // 实际使用的实例ID
  int? _viewId; // 用于存储平台视图的ID
  bool _isActive = false; // 标记当前实例是否处于激活状态
  bool _isDisposed = false; // 标记当前组件是否已被销毁
  bool _needsRebuild = false; // 标记是否需要重建PlatformView

  @override
  void initState() {
    super.initState();
    
    // 如果没有提供instanceId，则使用widget的hashCode作为唯一标识
    _actualInstanceId = widget.instanceId ?? 'live2d_${widget.hashCode}';
    if (kDebugMode) {
      print("Live2DWidget: initState called with instanceId: $_actualInstanceId");
    }
    _initLive2D();
  }

  Future<void> _initLive2D() async {
    try {
      if (kDebugMode) {
        print("Live2DWidget: Initializing Live2D with model path: ${widget.modelPath}, instanceId: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('initLive2D', {
        'modelPath': widget.modelPath,
        'instanceId': _actualInstanceId,
      });
      
      if (kDebugMode) {
        print("Live2DWidget: Live2D initialized successfully for instance: $_actualInstanceId");
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to init Live2D due to PlatformException: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to init Live2D - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Unexpected error initializing Live2D: $e");
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
    
    try {
      _isActive = true;
      if (kDebugMode) {
        print("Activating Live2D instance: $_actualInstanceId");
      }
      
      // 简化的激活逻辑，只请求渲染，不强制重新加载模型
      await _channel.invokeMethod('activateInstance', {
        'instanceId': _actualInstanceId,
        // 移除modelPath参数以避免强制重新加载导致的崩溃
      });
      
      // 简单的延迟，确保激活完成
      await Future.delayed(Duration(milliseconds: 50));
      
      // 轻量级刷新
      if (mounted) {
        if (kDebugMode) {
          print("Requesting refresh for Live2D instance: $_actualInstanceId");
        }
        await _refreshView();
      }
      
      if (kDebugMode) {
        print("Live2D instance activated successfully: $_actualInstanceId");
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Failed to activate Live2D instance: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Failed to activate Live2D instance - Missing plugin: ${e.message}");
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
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Failed to deactivate Live2D instance - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Unexpected error deactivating Live2D instance: $e");
      }
    }
  }

  Future<void> onTap(double x, double y) async {
    try {
      if (kDebugMode) {
        print("Live2DWidget: onTap called with x:$x, y:$y for instance: $_actualInstanceId");
      }
      await _channel.invokeMethod('onTap', {
        'x': x,
        'y': y,
        'instanceId': _actualInstanceId, // 传递实例ID
      });
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to handle tap: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to handle tap - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Unexpected error handling tap: $e");
      }
    }
  }

  // 添加触发表情动作的方法
  Future<void> _triggerExpression(String expressionName) async {
    try {
      if (kDebugMode) {
        print("Live2DWidget: Triggering expression: $expressionName for instance: $_actualInstanceId");
      }
      await _channel.invokeMethod('triggerExpression', {
        'expressionName': expressionName,
        'instanceId': _actualInstanceId, // 传递实例ID
      });
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to trigger expression: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to trigger expression - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Unexpected error triggering expression: $e");
      }
    }
  }

  // 添加触发动作的方法
  Future<void> _playMotion(String motionGroup, [int priority = 1]) async {
    try {
      if (kDebugMode) {
        print("Live2DWidget: Playing motion: $motionGroup with priority: $priority for instance: $_actualInstanceId");
      }
      await _channel.invokeMethod('playMotion', {
        'motionGroup': motionGroup,
        'priority': priority,
        'instanceId': _actualInstanceId, // 传递实例ID
      });
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to play motion: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to play motion - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Unexpected error playing motion: $e");
      }
    }
  }
  
  // 添加控制按钮可见性的私有方法
  Future<void> _setGearVisible(bool visible) async {
    try {
      if (kDebugMode) {
        print("Live2DWidget: Setting gear visible to: $visible for instance: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('setGearVisible', {
        'visible': visible,
        'instanceId': _actualInstanceId,
      });
      
      if (kDebugMode) {
        print("Live2DWidget: Gear visibility set successfully");
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to set gear visible due to PlatformException: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to set gear visible - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Unexpected error setting gear visible: $e");
      }
    }
  }
  
  Future<void> _setPowerVisible(bool visible) async {
    try {
      if (kDebugMode) {
        print("Live2DWidget: Setting power visible to: $visible for instance: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('setPowerVisible', {
        'visible': visible,
        'instanceId': _actualInstanceId,
      });
      
      if (kDebugMode) {
        print("Live2DWidget: Power visibility set successfully");
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to set power visible due to PlatformException: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to set power visible - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Unexpected error setting power visible: $e");
      }
    }
  }
  
  Future<bool?> _isGearVisible() async {
    try {
      if (kDebugMode) {
        print("Live2DWidget: Getting gear visible state for instance: $_actualInstanceId");
      }
      final result = await _channel.invokeMethod('isGearVisible', {
        'instanceId': _actualInstanceId,
      });
      if (kDebugMode) {
        print("Live2DWidget: Gear visible state: $result");
      }
      return result as bool?;
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to get gear visible state: ${e.message}");
      }
      return null;
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to get gear visible state - Missing plugin: ${e.message}");
      }
      return null;
    } catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Unexpected error getting gear visible state: $e");
      }
      return null;
    }
  }
  
  Future<bool?> _isPowerVisible() async {
    try {
      if (kDebugMode) {
        print("Live2DWidget: Getting power visible state for instance: $_actualInstanceId");
      }
      final result = await _channel.invokeMethod('isPowerVisible', {
        'instanceId': _actualInstanceId,
      });
      if (kDebugMode) {
        print("Live2DWidget: Power visible state: $result");
      }
      return result as bool?;
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to get power visible state: ${e.message}");
      }
      return null;
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to get power visible state - Missing plugin: ${e.message}");
      }
      return null;
    } catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Unexpected error getting power visible state: $e");
      }
      return null;
    }
  }
  
  // 添加刷新视图的方法
  Future<void> _refreshView() async {
    try {
      if (kDebugMode) {
        print("Live2DWidget: Refreshing view for instance: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('refreshView', {
        'instanceId': _actualInstanceId,
      });
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to refresh view: ${e.message}");
      }
    } on MissingPluginException catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Failed to refresh view - Missing plugin: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Live2DWidget: Unexpected error refreshing view: $e");
      }
    }
  }

  void _onPlatformViewCreated(int id) {
    if (kDebugMode) {
      print("Live2DWidget: Live2D platform view created with id: $id, instanceId: $_actualInstanceId");
    }
    _viewId = id;
  }

  @override
  void dispose() {
    // 组件销毁时通知原生层清理资源
    if (kDebugMode) {
      print("Live2DWidget: Disposing widget for instance: $_actualInstanceId");
    }
    
    _isDisposed = true;
    _cleanup();
    super.dispose();
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
      
      // 只有在插件可用时才调用cleanupInstance
      try {
        await _channel.invokeMethod('cleanupInstance', {
          'instanceId': _actualInstanceId,
          'viewId': _viewId,
        });
      } catch (e) {
        // 忽略清理过程中的错误，因为可能是平台视图已经被销毁
        if (kDebugMode) {
          print("Warning: Error during cleanupInstance call: $e");
        }
      }
      
      if (kDebugMode) {
        print("Live2D instance cleanup call completed: $_actualInstanceId");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Error cleaning up Live2D instance: $e");
      }
    }
  }
  
  // 添加一个新的方法用于强制刷新视图
  Future<void> refresh() async {
    try {
      if (kDebugMode) {
        print("Refreshing Live2D instance: $_actualInstanceId");
      }
      
      await _channel.invokeMethod('refreshView', {
        'instanceId': _actualInstanceId,
      });
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Failed to refresh Live2D instance: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Unexpected error refreshing Live2D instance: $e");
      }
    }
  }
  
  // 为了方便从外部调用，我们将这些方法暴露出去
  Future<void> activate() => _activate();
  Future<void> deactivate() => _deactivate();
  Future<void> playMotion(String motionGroup, [int priority = 1]) => _playMotion(motionGroup, priority);
  Future<void> triggerExpression(String expressionName) => _triggerExpression(expressionName);
  Future<void> setGearVisible(bool visible) => _setGearVisible(visible);
  Future<void> setPowerVisible(bool visible) => _setPowerVisible(visible);
  Future<bool?> isGearVisible() => _isGearVisible();
  Future<bool?> isPowerVisible() => _isPowerVisible();
  
  @override
  Widget build(BuildContext context) {
    // 在Android平台上使用AndroidView来嵌入原生视图
    if (defaultTargetPlatform == TargetPlatform.android) {
      if (kDebugMode) {
        print("Live2DWidget: Building Live2D AndroidView with params: ${widget.modelPath}, instanceId: $_actualInstanceId, needsRebuild: $_needsRebuild");
      }
      
      // 如果需要重建，先返回一个空容器然后在下一帧重建
      if (_needsRebuild) {
        _needsRebuild = false;
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) {
            if (kDebugMode) {
              print("Live2DWidget: Rebuilding after delay for instance: $_actualInstanceId");
            }
            setState(() {});
          }
        });
        
        // 返回一个占位容器
        return SizedBox(
          width: widget.width,
          height: widget.height,
          child: Container(
            color: Colors.transparent,
          ),
        );
      }
      
      return SizedBox(
        width: widget.width,
        height: widget.height,
        child: GestureDetector(
          onTapUp: (details) {
            if (kDebugMode) {
              print("Live2DWidget: Live2D widget tapped on instance: $_actualInstanceId");
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
}