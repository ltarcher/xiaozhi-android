import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';

/// Live2D模型查看器Widget
class Live2dViewer extends StatefulWidget {
  final String modelName;
  final double width;
  final double height;

  const Live2dViewer({
    Key? key,
    required this.modelName,
    this.width = 300,
    this.height = 300,
  }) : super(key: key);

  @override
  State<Live2dViewer> createState() => _Live2dViewerState();
}

class _Live2dViewerState extends State<Live2dViewer> {
  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return SizedBox(
        width: widget.width,
        height: widget.height,
        child: AndroidView(
          viewType: 'com.thinkerror.xiaozhi/live2d_view',
          creationParams: {
            "modelName": widget.modelName,
          },
          creationParamsCodec: const StandardMessageCodec(),
        ),
      );
    } else {
      return Container(
        width: widget.width,
        height: widget.height,
        color: Colors.grey[300],
        child: const Center(
          child: Text('Live2D viewer is only supported on Android'),
        ),
      );
    }
  }
}