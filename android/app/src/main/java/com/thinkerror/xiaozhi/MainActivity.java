package com.thinkerror.xiaozhi;

import android.content.Context;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;

public class MainActivity extends FlutterActivity {
    @Override
    public void configureFlutterEngine(FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        
        // 注册自定义音频播放插件，传递Context
        AudioPlayerPlugin.registerWith(flutterEngine, this);
    }
}
