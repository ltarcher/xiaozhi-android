package com.thinkerror.xiaozhi;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import com.thinkerror.xiaozhi.AudioPlayerCompat;
import com.thinkerror.xiaozhi.DeviceInfoCompat;

public class MainActivity extends FlutterActivity {
    @Override
    public void configureFlutterEngine(FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        // 注册Android 9兼容音频播放器
        AudioPlayerCompat.registerWith(flutterEngine, getApplicationContext());
        // 注册设备信息兼容性工具
        DeviceInfoCompat.registerWith(flutterEngine);
    }
}
