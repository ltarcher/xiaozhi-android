# Keep Live2D classes
-keep class com.live2d.sdk.cubism.core.** { *; }
-keep class com.live2d.sdk.cubism.framework.** { *; }
-keep class com.live2d.sdk.cubism.framework.model.CubismModel { *; }
-keep class com.live2d.sdk.cubism.framework.model.CubismMoc { *; }
-keep class com.live2d.sdk.cubism.framework.rendering.** { *; }
-keep class com.live2d.sdk.cubism.framework.motion.** { *; }
-keep class com.live2d.sdk.cubism.framework.math.** { *; }
-keep class com.live2d.sdk.cubism.framework.utils.** { *; }
-keep class com.live2d.sdk.cubism.framework.id.** { *; }
-keep class com.live2d.sdk.cubism.framework.physics.** { *; }
-keep class com.live2d.sdk.cubism.framework.type.** { *; }
-keep class com.live2d.sdk.cubism.framework.json.** { *; }
-keep class com.live2d.sdk.cubism.framework.state.** { *; }
-keep class com.live2d.sdk.cubism.framework.effect.** { *; }

# Keep Flutter classes
-keep class io.flutter.** { *; }
-keep class androidx.lifecycle.** { *; }

# Keep Google Play Core classes
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

# Additional rules for Live2D SDK
-dontwarn com.live2d.sdk.cubism.core.**
-dontwarn com.live2d.sdk.cubism.framework.**