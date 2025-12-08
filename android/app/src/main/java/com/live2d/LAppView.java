/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.content.Context;
import android.opengl.GLES20;
import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.math.CubismViewMatrix;
import com.live2d.sdk.cubism.framework.rendering.android.CubismOffscreenSurfaceAndroid;

import static com.live2d.LAppDefine.*;

/**
 * View class to display Live2D models and UI elements.
 */
public class LAppView {
    /**
     * Rendering target
     */
    public enum RenderingTarget {
        NONE,               // Default framebuffer
        MODEL_FRAME_BUFFER, // Offscreen buffer for each model
        VIEW_FRAME_BUFFER   // Offscreen buffer owned by this LAppView
    }

    public LAppView(Context context) {
        this.context = context;
        clearColor[0] = 1.0f;
        clearColor[1] = 1.0f;
        clearColor[2] = 1.0f;
        clearColor[3] = 0.0f;
    }

    /**
     * Initialize the view.
     */
    public void initialize() {
        final int width = LAppDelegate.getInstance().getWindowWidth();
        final int height = LAppDelegate.getInstance().getWindowHeight();

        float ratio = (float) width / (float) height;
        float left = -ratio;
        float right = ratio;
        float bottom = LogicalView.LEFT.getValue();
        float top = LogicalView.RIGHT.getValue();

        // 对应设备的屏幕范围。X左端、X右端、Y下端、Y上端
        viewMatrix.setScreenRect(left, right, bottom, top);
        viewMatrix.scale(Scale.DEFAULT.getValue(), Scale.DEFAULT.getValue());

        // 初始化为单位矩阵
        deviceToScreen.loadIdentity();

        if (width > height) {
            float screenW = Math.abs(right - left);
            deviceToScreen.scaleRelative(screenW / width, -screenW / width);
        } else {
            float screenH = Math.abs(top - bottom);
            deviceToScreen.scaleRelative(screenH / height, -screenH / height);
        }
        deviceToScreen.translateRelative(-width * 0.5f, -height * 0.5f);

        // 设置显示范围
        viewMatrix.setMaxScale(Scale.MAX.getValue());   // 极限放大率
        viewMatrix.setMinScale(Scale.MIN.getValue());   // 极限缩小率

        // 可显示的最大范围
        viewMatrix.setMaxScreenRect(
            MaxLogicalView.LEFT.getValue(),
            MaxLogicalView.RIGHT.getValue(),
            MaxLogicalView.BOTTOM.getValue(),
            MaxLogicalView.TOP.getValue()
        );
    }

    /**
     * Initialize sprites.
     */
    public void initializeSprite() {
        int windowWidth = LAppDelegate.getInstance().getWindowWidth();
        int windowHeight = LAppDelegate.getInstance().getWindowHeight();

        LAppTextureManager textureManager = LAppDelegate.getInstance().getTextureManager();
        int programId = spriteShader.getShaderId();

        // 背景图像
        LAppTextureManager.TextureInfo backgroundTexture = textureManager.createTextureFromPngFile(ResourcePath.ROOT.getPath() + ResourcePath.BACK_IMAGE.getPath());
        
        if (backgroundTexture != null) {
            // x,y是图像的中心坐标
            float x = windowWidth * 0.5f;
            float y = windowHeight * 0.5f;
            float fWidth = backgroundTexture.width * 2.0f;
            float fHeight = windowHeight * 0.95f;

            if (backSprite == null) {
                backSprite = new LAppSprite(x, y, fWidth, fHeight, backgroundTexture.id, programId);
            } else {
                backSprite.resize(x, y, fWidth, fHeight);
            }
        }

        // 齿轮图像
        LAppTextureManager.TextureInfo gearTexture = textureManager.createTextureFromPngFile(ResourcePath.ROOT.getPath() + ResourcePath.GEAR_IMAGE.getPath());
        
        if (gearTexture != null) {
            float x = windowWidth - gearTexture.width * 0.5f;
            float y = windowHeight - gearTexture.height * 0.5f;
            float fWidth = gearTexture.width;
            float fHeight = gearTexture.height;

            if (gearSprite == null) {
                gearSprite = new LAppSprite(x, y, fWidth, fHeight, gearTexture.id, programId);
            } else {
                gearSprite.resize(x, y, fWidth, fHeight);
            }
        }

        // 电源按钮图像
        LAppTextureManager.TextureInfo powerTexture = textureManager.createTextureFromPngFile(ResourcePath.ROOT.getPath() + ResourcePath.POWER_IMAGE.getPath());
        
        if (powerTexture != null) {
            float x = powerTexture.width * 0.5f;
            float y = windowHeight - powerTexture.height * 0.5f;
            float fWidth = powerTexture.width;
            float fHeight = powerTexture.height;

            if (powerSprite == null) {
                powerSprite = new LAppSprite(x, y, fWidth, fHeight, powerTexture.id, programId);
            } else {
                powerSprite.resize(x, y, fWidth, fHeight);
            }
        }

        // 渲染目标精灵
        float x = windowWidth * 0.5f;
        float y = windowHeight * 0.5f;

        if (renderingSprite == null) {
            renderingSprite = new LAppSprite(x, y, windowWidth, windowHeight, 0, programId);
        } else {
            renderingSprite.resize(x, y, windowWidth, windowHeight);
        }
    }

    /**
     * Render the view.
     */
    public void render() {
        LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();

        // Update view matrix
        viewMatrix.adjustTranslate(-deviceToScreen.getArray()[13], -deviceToScreen.getArray()[14]);

        if (renderingTarget == RenderingTarget.MODEL_FRAME_BUFFER) {
            live2DManager.onUpdate();
        } else {
            live2DManager.onUpdate();

            // Render sprites
            float[] uvVertex = {
                    0.0f, 1.0f,
                    0.0f, 0.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f
            };

            // Background
            if (backSprite != null) {
                backSprite.renderImmediate(backSprite.getTextureId(), uvVertex);
            }

            // Gear
            if (gearSprite != null) {
                gearSprite.renderImmediate(gearSprite.getTextureId(), uvVertex);
            }

            // Power button
            if (powerSprite != null) {
                powerSprite.renderImmediate(powerSprite.getTextureId(), uvVertex);
            }
        }

        // Reset the view matrix by re-applying the adjustment
        viewMatrix.adjustTranslate(-deviceToScreen.getArray()[13], -deviceToScreen.getArray()[14]);
    }

    /**
     * 绘制单个模型前调用
     *
     * @param refModel 模型数据
     */
    /**
     * Pre-draw processing for each model.
     *
     * @param model Model to draw
     */
    public void preModelDraw(LAppModel model) {
        // Use offscreen buffer for each model
        if (renderingTarget == RenderingTarget.MODEL_FRAME_BUFFER && model.getRenderingBuffer() != null) {
            // Start rendering to the offscreen buffer
            model.getRenderingBuffer().beginDraw();
            // Clear the buffer
            GLES20.glClearColor(clearColor[0], clearColor[1], clearColor[2], 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        }
    }

    /**
     * Post-draw processing for each model.
     *
     * @param model Model that was drawn
     */
    public void postModelDraw(LAppModel model) {
        // End rendering to the offscreen buffer for each model
        if (renderingTarget == RenderingTarget.MODEL_FRAME_BUFFER && model.getRenderingBuffer() != null) {
            model.getRenderingBuffer().endDraw();
        }
    }

    /**
     * 切换渲染目标
     *
     * @param targetType 渲染目标
     */
    public void switchRenderingTarget(RenderingTarget targetType) {
        renderingTarget = targetType;
    }

    /**
     * 触摸时调用
     *
     * @param pointX 屏幕X坐标
     * @param pointY 屏幕Y坐标
     */
    public void onTouchesBegan(float pointX, float pointY) {
        touchManager.touchesBegan(pointX, pointY);
    }

    /**
     * 触摸时指针移动调用
     *
     * @param pointX 屏幕X坐标
     * @param pointY 屏幕Y坐标
     */
    public void onTouchesMoved(float pointX, float pointY) {
        float viewX = transformViewX(touchManager.getLastX());
        float viewY = transformViewY(touchManager.getLastY());

        touchManager.touchesMoved(pointX, pointY);

        LAppLive2DManager.getInstance().onDrag(viewX, viewY);
    }

    /**
     * Handle touch ended event.
     *
     * @param pointX X coordinate
     * @param pointY Y coordinate
     */
    public void onTouchesEnded(float pointX, float pointY) {
        // Touch handling
        LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
        live2DManager.onDrag(0.0f, 0.0f);

        // Tap event handling
        if (DEBUG_TOUCH_LOG_ENABLE) {
            LAppPal.printLog("touchesEnded x:" + pointX + " y:" + pointY);
        }

        // Perform hit testing on sprites
        if (powerSprite != null && powerSprite.isHit(pointX, pointY)) {
            // Exit the app if the power button is tapped
            LAppDelegate.getInstance().onStop();
            return;
        }

        if (gearSprite != null && gearSprite.isHit(pointX, pointY)) {
            // Switch models if the gear is tapped
            live2DManager.nextScene();
            return;
        }

        // Perform hit testing on the model
        live2DManager.onTap(pointX, pointY);
    }

    /**
     * 切换渲染目标为默认以外时的背景清除颜色设置
     *
     * @param r 红(0.0~1.0)
     * @param g 绿(0.0~1.0)
     * @param b 蓝(0.0~1.0)
     */
    public void setRenderingTargetClearColor(float r, float g, float b) {
        clearColor[0] = r;
        clearColor[1] = g;
        clearColor[2] = b;
    }

    /**
     * Close the view.
     */
    public void close() {
        // Release sprites
        if (backSprite != null) {
            backSprite.deleteTexture();
            backSprite = null;
        }

        if (gearSprite != null) {
            gearSprite.deleteTexture();
            gearSprite = null;
        }

        if (powerSprite != null) {
            powerSprite.deleteTexture();
            powerSprite = null;
        }

        if (renderingSprite != null) {
            renderingSprite.deleteTexture();
            renderingSprite = null;
        }
    }

    /**
     * Get sprite alpha value.
     *
     * @param index Sprite index
     * @return Alpha value
     */
    public float getSpriteAlpha(int index) {
        // 根据index数值适当添加差异
        float alpha = 0.25f + (float) index * 0.5f;

        // 作为示例为alpha添加适当的差异
        if (alpha > 1.0f) {
            alpha = 1.0f;
        }
        if (alpha < 0.1f) {
            alpha = 0.1f;
        }
        return alpha;
    }

    private final Context context;

    /**
     * Touch manager
     */
    private final TouchManager touchManager = new TouchManager();

    /**
     * 将X坐标转换为View坐标
     *
     * @param deviceX 设备X坐标
     * @return ViewX坐标
     */
    public float transformViewX(float deviceX) {
        float screenX = deviceToScreen.transformX(deviceX);  // 获取屏幕坐标系中的坐标
        return viewMatrix.invertTransformX(screenX);        // 通过逆矩阵变换转换为View坐标
    }

    /**
     * 将Y坐标转换为View坐标
     *
     * @param deviceY 设备Y坐标
     * @return ViewY坐标
     */
    public float transformViewY(float deviceY) {
        float screenY = deviceToScreen.transformY(deviceY);  // 获取屏幕坐标系中的坐标
        return viewMatrix.invertTransformY(screenY);        // 通过逆矩阵变换转换为View坐标
    }

    /**
     * 将X坐标转换为Screen坐标
     *
     * @param deviceX 设备X坐标
     * @return ScreenX坐标
     */
    public float transformScreenX(float deviceX) {
        return deviceToScreen.transformX(deviceX);
    }

    /**
     * 将Y坐标转换为Screen坐标
     *
     * @param deviceY 设备Y坐标
     * @return ScreenY坐标
     */
    public float transformScreenY(float deviceY) {
        return deviceToScreen.transformY(deviceY);
    }

    /**
     * Device-to-screen transformation matrix
     */
    private final CubismMatrix44 deviceToScreen = CubismMatrix44.create();
    /**
     * View matrix for display scaling and translation
     */
    private final CubismViewMatrix viewMatrix = new CubismViewMatrix();

    /**
     * Sprites
     */
    private LAppSprite backSprite;      // Background
    private LAppSprite gearSprite;      // Gear
    private LAppSprite powerSprite;     // Power button
    private LAppSprite renderingSprite; // Rendering target

    /**
     * Rendering target
     */
    private RenderingTarget renderingTarget = RenderingTarget.NONE;

    /**
     * Clear color for rendering target
     */
    private final float[] clearColor = new float[4];

    private CubismOffscreenSurfaceAndroid renderingBuffer = new CubismOffscreenSurfaceAndroid();

    /**
     * Shader creation delegation class
     */
    private LAppSpriteShader spriteShader = new LAppSpriteShader();
}