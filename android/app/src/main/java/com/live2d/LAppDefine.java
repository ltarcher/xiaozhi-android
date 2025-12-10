package com.live2d;

import com.live2d.sdk.cubism.framework.CubismFrameworkConfig.LogLevel;

/**
 * Live2D应用的常量定义类
 */
public class LAppDefine {
    // 日志输出级别设置
    public static final LogLevel cubismLoggingLevel = LogLevel.VERBOSE;

    // 是否启用调试日志
    public static final boolean DEBUG_LOG_ENABLE = true;
    // 是否启用触摸调试日志
    public static final boolean DEBUG_TOUCH_LOG_ENABLE = false;
    
    // 是否启用模型轮廓绘制（用于调试）
    public static final boolean DEBUG_DRAW_HIT_AREA_ENABLE = false;
    // 是否启用自动眨眼
    public static final boolean AUTO_EYE_BLINK_ENABLE = true;
    // 是否启用呼吸效果
    public static final boolean AUTO_BREATH_ENABLE = true;
    
    // 是否启用物理运算
    public static final boolean PHYSICS_ENABLE = true;
    // 是否启用唇形同步
    public static final boolean LIP_SYNC_ENABLE = true;
    // 是否启用拖拽移动
    public static final boolean DRAG_SCALE_ENABLE = true;
    // 是否启用模型切换时淡入淡出
    public static final boolean FADE_IN_OUT_ENABLE = true;
    
    // moc3一致性验证标志
    public static final boolean MOC_CONSISTENCY_VALIDATION_ENABLE = true;
    // 动作一致性验证标志
    public static final boolean MOTION_CONSISTENCY_VALIDATION_ENABLE = true;
    
    // 默认姿势文件名
    public static final String DEFAULT_POSE_FILE = "pose.json";
    // 默认物理文件名
    public static final String DEFAULT_PHYSICS_FILE = "physics.json";
    
    // 默认等待时间（秒）
    public static final float DEFAULT_WAIT_TIME = 3.0f;
    // 默认淡入时间（毫秒）
    public static final float DEFAULT_FADE_IN_TIME = 1000.0f;
    // 默认淡出时间（毫秒）
    public static final float DEFAULT_FADE_OUT_TIME = 1000.0f;
    
    // 默认最大FPS
    public static final double FRAME_RATE = 60.0;
    
    // 视图缩放限制
    public enum Scale {
        DEFAULT(1.0f),
        MAX(2.0f),
        MIN(0.8f);
        
        private final float value;
        
        Scale(float value) {
            this.value = value;
        }
        
        public float getValue() {
            return value;
        }
    }
    
    // 逻辑视图范围
    public enum LogicalView {
        LEFT(-2.0f),
        RIGHT(2.0f),
        BOTTOM(-2.0f),
        TOP(2.0f);
        
        private final float value;
        
        LogicalView(float value) {
            this.value = value;
        }
        
        public float getValue() {
            return value;
        }
    }
    
    // 最大逻辑视图范围
    public enum MaxLogicalView {
        LEFT(-3.0f),
        RIGHT(3.0f),
        BOTTOM(-3.0f),
        TOP(3.0f);
        
        private final float value;
        
        MaxLogicalView(float value) {
            this.value = value;
        }
        
        public float getValue() {
            return value;
        }
    }
}