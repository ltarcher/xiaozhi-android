package com.live2d;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import java.util.Map;

import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class Live2DViewFactory extends PlatformViewFactory {
    private final Context context;

    public Live2DViewFactory(Context context) {
        super(StandardMessageCodec.INSTANCE);
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        this.context = context;
    }

    private Live2DPlatformView currentLive2DView;
    
    @NonNull
    @Override
    public PlatformView create(Context context, int viewId, @Nullable Object args) {
        @SuppressWarnings("unchecked")
        Map<String, Object> creationParams = (Map<String, Object>) args;
        currentLive2DView = new Live2DPlatformView(context, creationParams);
        return currentLive2DView;
    }
    
    /**
     * 更新Live2D模型
     * @param modelPath 新的模型路径
     */
    public void updateModel(String modelPath) {
        if (currentLive2DView != null) {
            currentLive2DView.updateModel(modelPath);
        }
    }
}