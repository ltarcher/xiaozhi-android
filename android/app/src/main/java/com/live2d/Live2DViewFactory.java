package com.live2d;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import java.util.Map;

import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class Live2DViewFactory extends PlatformViewFactory {
    private static final String TAG = "Live2DViewFactory";
    private final Context context;

    public Live2DViewFactory(Context context) {
        super(StandardMessageCodec.INSTANCE);
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        this.context = context;
    }

    @NonNull
    @Override
    public PlatformView create(Context context, int viewId, @Nullable Object args) {
        Log.d(TAG, "Creating Live2DPlatformView with viewId: " + viewId);
        @SuppressWarnings("unchecked")
        Map<String, Object> creationParams = (Map<String, Object>) args;
        return new Live2DPlatformView(context, creationParams);
    }
}