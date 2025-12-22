# Keep Live2D SDK classes to prevent R8 from removing them
-keep class com.live2d.sdk.cubism.core.** { *; }
-keep class com.live2d.sdk.cubism.framework.** { *; }
-keep class com.plugin.flutter_live2d.** { *; }

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Prevent renaming of source file attribute
-renamesourcefileattribute SourceFile

# Keep all native method names and classes needed for JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep classes that are accessed via reflection
-keep class com.live2d.** { *; }

# Keep the specific classes mentioned in the error logs
-keep class com.live2d.sdk.cubism.core.CubismCanvasInfo
-keep class com.live2d.sdk.cubism.core.CubismCoreVersion
-keep class com.live2d.sdk.cubism.core.CubismDrawableView
-keep class com.live2d.sdk.cubism.core.CubismDrawables
-keep class com.live2d.sdk.cubism.core.CubismMoc
-keep class com.live2d.sdk.cubism.core.CubismModel
-keep class com.live2d.sdk.cubism.core.CubismParameterView
-keep class com.live2d.sdk.cubism.core.CubismParameters$ParameterType
-keep class com.live2d.sdk.cubism.core.CubismParameters
-keep class com.live2d.sdk.cubism.core.CubismPartView
-keep class com.live2d.sdk.cubism.core.ICubismLogger
-keep class com.live2d.sdk.cubism.core.Live2DCubismCore

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn com.live2d.sdk.cubism.core.CubismCanvasInfo
-dontwarn com.live2d.sdk.cubism.core.CubismCoreVersion
-dontwarn com.live2d.sdk.cubism.core.CubismDrawableView
-dontwarn com.live2d.sdk.cubism.core.CubismDrawables
-dontwarn com.live2d.sdk.cubism.core.CubismMoc
-dontwarn com.live2d.sdk.cubism.core.CubismModel
-dontwarn com.live2d.sdk.cubism.core.CubismParameterView
-dontwarn com.live2d.sdk.cubism.core.CubismParameters$ParameterType
-dontwarn com.live2d.sdk.cubism.core.CubismParameters
-dontwarn com.live2d.sdk.cubism.core.CubismPartView
-dontwarn com.live2d.sdk.cubism.core.ICubismLogger
-dontwarn com.live2d.sdk.cubism.core.Live2DCubismCore
-dontwarn com.plugin.flutter_live2d.FlutterLive2dPlugin

# Keep Flutter plugin classes
-keep class com.plugin.flutter_live2d.FlutterLive2dPlugin

# Keep JNA (Java Native Access) classes and methods
-keep class com.sun.jna.** { *; }
-keep class com.sun.jna.internal.** { *; }
-keepclassmembers class * {
    native <methods>;
}

# Keep JNA Pointer and related classes
-keep class com.sun.jna.Pointer
-keep class com.sun.jna.Pointer$*
-keep class com.sun.jna.Structure
-keep class com.sun.jna.Structure$*
-keep class com.sun.jna.Memory
-keep class com.sun.jna.Callback
-keep class com.sun.jna.Library

# Keep Vosk speech recognition library classes
-keep class org.vosk.** { *; }
-keep class com.alphacephei.** { *; }

# Suppress warnings for JNA and Vosk
-dontwarn com.sun.jna.**
-dontwarn org.vosk.**
-dontwarn com.alphacephei.**

# Preserve all native methods in Vosk
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep classes that might be accessed via reflection by Vosk and JNA
-keep class org.vosk.LibVosk { *; }
-keep class org.vosk.Recognizer { *; }
-keep class org.vosk.Model { *; }
-keep class org.vosk.SpeakerModel { *; }