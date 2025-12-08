/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import com.live2d.sdk.cubism.framework.CubismFrameworkConfig.LogLevel;

/**
 * Constants used in this Live2D application.
 */
public class LAppDefine {
    /**
     * Scaling rate.
     */
    public enum Scale {
        /**
         * Default scaling rate
         */
        DEFAULT(1.0f),
        /**
         * Maximum scaling rate
         */
        MAX(2.0f),
        /**
         * Minimum scaling rate
         */
        MIN(0.8f);

        private final float value;

        Scale(float value) {
            this.value = value;
        }

        public float getValue() {
            return value;
        }
    }

    /**
     * Logical view coordinate system.
     */
    public enum LogicalView {
        /**
         * Left end
         */
        LEFT(-1.0f),
        /**
         * Right end
         */
        RIGHT(1.0f),
        /**
         * Bottom end
         */
        BOTTOM(-1.0f),
        /**
         * Top end
         */
        TOP(1.0f);

        private final float value;

        LogicalView(float value) {
            this.value = value;
        }

        public float getValue() {
            return value;
        }
    }

    /**
     * Maximum logical view coordinate system.
     */
    public enum MaxLogicalView {
        /**
         * Maximum left end
         */
        LEFT(-2.0f),
        /**
         * Maximum right end
         */
        RIGHT(2.0f),
        /**
         * Maximum bottom end
         */
        BOTTOM(-2.0f),
        /**
         * Maximum top end
         */
        TOP(2.0f);

        private final float value;

        MaxLogicalView(float value) {
            this.value = value;
        }

        public float getValue() {
            return value;
        }
    }

    /**
     * 图像素材路径
     */
    public enum ResourcePath {
        /**
         * 文件分隔符
         */
        SEPARATOR("/"),
        /**
         * 素材目录的相对路径
         */
        ROOT("live2d/"),
        /**
         * 着色器目录的相对路径
         */
        SHADER_ROOT("live2d/Shaders/"),
        /**
         * 背景图像文件
         */
        BACK_IMAGE("back_class_normal.png"),
        /**
         * 齿轮图像ファイル
         */
        GEAR_IMAGE("icon_gear.png"),
        /**
         * 电源按钮图像文件
         */
        POWER_IMAGE("close.png"),
        /**
         * 顶点着色器ファイル
         */
        VERT_SHADER("VertSprite.vert"),
        /**
         * 片段着色器ファイル
         */
        FRAG_SHADER("FragSprite.frag");

        private final String path;

        ResourcePath(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    /**
     * Motion group
     */
    public enum MotionGroup {
        /**
         * ID of the motion to be played at idling.
         */
        IDLE("Idle"),
        /**
         * ID of the motion to be played at tapping body.
         */
        TAP_BODY("TapBody");

        private final String id;

        MotionGroup(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * [Head] tag for hit detection.
     * (Match with external definition file(json))
     */
    public enum HitAreaName {
        HEAD("Head"),
        BODY("Body");

        private final String id;

        HitAreaName(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * Motion priority
     */
    public enum Priority {
        NONE(0),
        IDLE(1),
        NORMAL(2),
        FORCE(3);

        private final int priority;

        Priority(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    /**
     * MOC3の整合性を検証するかどうか。有効ならtrue。
     */
    public static final boolean MOC_CONSISTENCY_VALIDATION_ENABLE = true;

    /**
     * motion3.jsonの整合性を検証するかどうか。有効ならtrue。
     */
    public static final boolean MOTION_CONSISTENCY_VALIDATION_ENABLE = true;

    /**
     * Enable/Disable debug logging.
     */
    public static final boolean DEBUG_LOG_ENABLE = true;

    /**
     * 是否在日志中显示处理结果
     */
    public static final boolean DEBUG_TOUCH_LOG_ENABLE = true;
    /**
     * Setting the level of the log output from the Framework.
     */
    public static final LogLevel cubismLoggingLevel = LogLevel.VERBOSE;
    /**
     * 是否在日志中显示更新信息
     */
    public static final boolean PREMULTIPLIED_ALPHA_ENABLE = true;

    /**
     * Flag whether to draw to the target held by LAppView. (If both USE_RENDER_TARGET and USE_MODEL_RENDER_TARGET are true, this variable is given priority over USE_MODEL_RENDER_TARGET.)
     */
    public static final boolean USE_RENDER_TARGET = false;
    /**
     * Flag whether to draw to the target that each LAppModel has.
     */
    public static final boolean USE_MODEL_RENDER_TARGET = false;
}