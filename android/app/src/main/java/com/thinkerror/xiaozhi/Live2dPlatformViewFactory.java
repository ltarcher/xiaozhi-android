package com.thinkerror.xiaozhi;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class Live2dPlatformViewFactory extends PlatformViewFactory {
    private static final String TAG = "Live2dPlatformViewFactory";
    private final Context context;

    public Live2dPlatformViewFactory(Context context) {
        super(StandardMessageCodec.INSTANCE);
        this.context = context;
    }

    @NonNull
    @Override
    public PlatformView create(@NonNull Context context, int viewId, @Nullable Object args) {
        Log.d(TAG, "Creating Live2D Platform View with ID: " + viewId);
        
        Map<String, Object> creationParams = (Map<String, Object>) args;
        return new Live2dPlatformView(context, viewId, creationParams);
    }
}