package io.flutter.app;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.live2d.Live2DViewFactory;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "live2d_channel";

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        
        // 注册PlatformView工厂
        flutterEngine
                .getPlatformViewsController()
                .getRegistry()
                .registerViewFactory("live2d_view", new Live2DViewFactory(this));
        
        // 注册MethodChannel用于与Flutter通信
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (call, result) -> {
                            if (call.method.equals("initLive2D")) {
                                String modelPath = call.argument("modelPath");
                                // TODO: 实现初始化Live2D模型的逻辑
                                result.success(null);
                            } else if (call.method.equals("onTap")) {
                                Double x = call.argument("x");
                                Double y = call.argument("y");
                                // TODO: 实现处理点击事件的逻辑
                                result.success(null);
                            } else {
                                result.notImplemented();
                            }
                        }
                );
    }
}