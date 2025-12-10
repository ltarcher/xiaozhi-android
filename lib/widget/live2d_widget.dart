import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class Live2DWidget extends StatefulWidget {
  final String modelPath;
  final double width;
  final double height;

  const Live2DWidget({
    super.key,
    required this.modelPath,
    required this.width,
    required this.height,
  });

  @override
  State<Live2DWidget> createState() => _Live2DWidgetState();
}

class _Live2DWidgetState extends State<Live2DWidget> {
  static const MethodChannel _channel = MethodChannel('live2d_channel');

  @override
  void initState() {
    super.initState();
    _initLive2D();
  }

  Future<void> _initLive2D() async {
    try {
      await _channel.invokeMethod('initLive2D', {
        'modelPath': widget.modelPath,
      });
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

  Future<void> onTap(double x, double y) async {
    try {
      await _channel.invokeMethod('onTap', {
        'x': x,
        'y': y,
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

  @override
  Widget build(BuildContext context) {
    // 在Android平台上使用AndroidView来嵌入原生视图
    if (defaultTargetPlatform == TargetPlatform.android) {
      return SizedBox(
        width: widget.width,
        height: widget.height,
        child: GestureDetector(
          onTapUp: (details) {
            final RenderBox renderBox = context.findRenderObject() as RenderBox;
            final position = renderBox.globalToLocal(details.globalPosition);
            onTap(position.dx, position.dy);
          },
          child: AndroidView(
            viewType: 'live2d_view',
            creationParams: {
              'modelPath': widget.modelPath,
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
      print("Live2D platform view created with id: $id");
    }
  }
}