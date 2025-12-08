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
import static android.opengl.GLES20.*;

public class LAppView implements AutoCloseable {
    public LAppView(Context context) {
        this.context = context;
        clearColor[0] = 1.0f;
        clearColor[1] = 1.0f;
        clearColor[2] = 1.0f;
        clearColor[3] = 0.0f;
    }

    @Override
    public void close() {
        if (spriteShader != null) {
            spriteShader.close();
        }
    }

    // 初始化视图
    public void initialize() {
        int width = LAppDelegate.getInstance().getWindowWidth();
        int height = LAppDelegate.getInstance().getWindowHeight();

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

        spriteShader = new LAppSpriteShader();
    }

    // 初始化图像
    public void initializeSprite() {
        int windowWidth = LAppDelegate.getInstance().getWindowWidth();
        int windowHeight = LAppDelegate.getInstance().getWindowHeight();

        LAppTextureManager textureManager = LAppDelegate.getInstance().getTextureManager();

        // 加载背景图像
        LAppTextureManager.TextureInfo backgroundTexture = textureManager.createTextureFromPngFile(ResourcePath.ROOT.getPath() + ResourcePath.BACK_IMAGE.getPath());

        // x,y是图像的中心坐标
        float x = windowWidth * 0.5f;
        float y = windowHeight * 0.5f;
        float fWidth = backgroundTexture.width * 2.0f;
        float fHeight = windowHeight * 0.95f;

        int programId = spriteShader.getShaderId();

        if (backSprite == null) {
            backSprite = new LAppSprite(x, y, fWidth, fHeight, backgroundTexture.id, programId);
        } else {
            backSprite.resize(x, y, fWidth, fHeight);
        }

        // 加载齿轮图像
        LAppTextureManager.TextureInfo gearTexture = textureManager.createTextureFromPngFile(ResourcePath.ROOT.getPath() + ResourcePath.GEAR_IMAGE.getPath());

        x = windowWidth - gearTexture.width * 0.5f - 96.f;
        y = windowHeight - gearTexture.height * 0.5f;
        fWidth = (float) gearTexture.width;
        fHeight = (float) gearTexture.height;

        if (gearSprite == null) {
            gearSprite = new LAppSprite(x, y, fWidth, fHeight, gearTexture.id, programId);
        } else {
            gearSprite.resize(x, y, fWidth, fHeight);
        }

        // 加载电源图像
        LAppTextureManager.TextureInfo powerTexture = textureManager.createTextureFromPngFile(ResourcePath.ROOT.getPath() + ResourcePath.POWER_IMAGE.getPath());

        x = windowWidth - powerTexture.width * 0.5f - 96.0f;
        y = powerTexture.height * 0.5f;
        fWidth = (float) powerTexture.width;
        fHeight = (float) powerTexture.height;

        if (powerSprite == null) {
            powerSprite = new LAppSprite(x, y, fWidth, fHeight, powerTexture.id, programId);
        } else {
            powerSprite.resize(x, y, fWidth, fHeight);
        }

        // 覆盖整个屏幕的尺寸
        x = windowWidth * 0.5f;
        y = windowHeight * 0.5f;

        if (renderingSprite == null) {
            renderingSprite = new LAppSprite(x, y, windowWidth, windowHeight, 0, programId);
        } else {
            renderingSprite.resize(x, y, windowWidth, windowHeight);
        }
    }

    // 绘制
    public void render() {
        // 获取屏幕尺寸
        int maxWidth = LAppDelegate.getInstance().getWindowWidth();
        int maxHeight = LAppDelegate.getInstance().getWindowHeight();

        backSprite.setWindowSize(maxWidth, maxHeight);
        gearSprite.setWindowSize(maxWidth, maxHeight);
        powerSprite.setWindowSize(maxWidth, maxHeight);

        // 绘制UI和背景
        backSprite.render();
        gearSprite.render();
        powerSprite.render();

        // 模型的绘制
        LAppLive2DManager live2dManager = LAppLive2DManager.getInstance();
        live2dManager.onUpdate();

        // 如果使用各模型持有的绘图目标作为纹理
        if (renderingTarget == RenderingTarget.MODEL_FRAME_BUFFER && renderingSprite != null) {
            final float[] uvVertex = {
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f
            };

            LAppModel model = live2dManager.getModel();
            float alpha = model.getOpacity();    // 获取不透明度

            renderingSprite.setColor(1.0f * alpha, 1.0f * alpha, 1.0f * alpha, alpha);

            if (model != null) {
                renderingSprite.setWindowSize(maxWidth, maxHeight);
                renderingSprite.renderImmediate(model.getRenderingBuffer().getColorBuffer()[0], uvVertex);
            }
        }
    }

    /**
     * 绘制单个模型前调用
     *
     * @param refModel 模型数据
     */
    public void preModelDraw(LAppModel refModel) {
        // 用于向其他渲染目标绘制的离屏表面
        CubismOffscreenSurfaceAndroid useTarget;

        // 透明设置
        GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        // 向其他渲染目标绘制的情况
        if (renderingTarget != RenderingTarget.NONE) {
            // 使用的目标
            useTarget = (renderingTarget == RenderingTarget.VIEW_FRAME_BUFFER)
                ? renderingBuffer
                : refModel.getRenderingBuffer();

            // 如果未创建渲染目标内部，则在此处创建
            if (!useTarget.isValid()) {
                int width = LAppDelegate.getInstance().getWindowWidth();
                int height = LAppDelegate.getInstance().getWindowHeight();

                // 模型绘制画布
                useTarget.createOffscreenSurface((int) width, (int) height, null);
            }
            // 开始渲染
            useTarget.beginDraw(null);
            useTarget.clear(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);   // 背景清除颜色
        }
    }

    /**
     * 绘制单个模型后立即调用
     *
     * @param refModel 模型数据
     */
    public void postModelDraw(LAppModel refModel) {
        CubismOffscreenSurfaceAndroid useTarget = null;

        // 向其他渲染目标绘制的情况
        if (renderingTarget != RenderingTarget.NONE) {
            // 使用的目标
            useTarget = (renderingTarget == RenderingTarget.VIEW_FRAME_BUFFER)
                ? renderingBuffer
                : refModel.getRenderingBuffer();

            // 结束渲染
            useTarget.endDraw();

            // 如果使用LAppView持有的帧缓冲，则此处成为精灵绘制
            if (renderingTarget == RenderingTarget.VIEW_FRAME_BUFFER && renderingSprite != null) {
                final float[] uvVertex = {
                    1.0f, 1.0f,
                    0.0f, 1.0f,
                    0.0f, 0.0f,
                    1.0f, 0.0f
                };
                renderingSprite.setColor(1.0f * getSpriteAlpha(0), 1.0f * getSpriteAlpha(0), 1.0f * getSpriteAlpha(0), getSpriteAlpha(0));

                // 获取屏幕尺寸
                int maxWidth = LAppDelegate.getInstance().getWindowWidth();
                int maxHeight = LAppDelegate.getInstance().getWindowHeight();

                renderingSprite.setWindowSize(maxWidth, maxHeight);
                renderingSprite.renderImmediate(useTarget.getColorBuffer()[0], uvVertex);
            }
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
     * 触摸结束时调用
     *
     * @param pointX 屏幕X坐标
     * @param pointY 屏幕Y坐标
     */
    public void onTouchesEnded(float pointX, float pointY) {
        // 触摸结束
        LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
        live2DManager.onDrag(0.0f, 0.0f);

        // 单击
        // 获取逻辑坐标变换后的坐标
        float x = deviceToScreen.transformX(touchManager.getLastX());
        // 获取逻辑坐标变换后的坐标
        float y = deviceToScreen.transformY(touchManager.getLastY());

        if (DEBUG_TOUCH_LOG_ENABLE) {
            LAppPal.printLog("Touches ended x: " + x + ", y:" + y);
        }

        live2DManager.onTap(x, y);

        // 检查是否点击了齿轮按钮
        if (gearSprite != null && gearSprite.isHit(pointX, pointY)) {
            // 切换模型
            LAppLive2DManager.getInstance().loadModel("Hiyori");
        }

        // 检查是否点击了电源按钮
        if (powerSprite != null && powerSprite.isHit(pointX, pointY)) {
            // 退出应用
            System.exit(0);
        }
    }

    /**
     * 将X坐标转换为View坐标
     *
     * @param deviceX 设备X坐标
     * @return ViewX坐标
     */
    public float transformViewX(float deviceX) {
        // 获取逻辑坐标变换后的坐标
        float screenX = deviceToScreen.transformX(deviceX);
        // 放大、缩小、移动后的值
        return viewMatrix.invertTransformX(screenX);
    }

    /**
     * 将Y坐标转换为View坐标
     *
     * @param deviceY 设备Y坐标
     * @return ViewY坐标
     */
    public float transformViewY(float deviceY) {
        // 获取逻辑坐标变换后的坐标
        float screenY = deviceToScreen.transformY(deviceY);
        // 放大、缩小、移动后的值
        return viewMatrix.invertTransformY(screenY);
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
     * 在示例中绘制模型到其他渲染目标时确定绘制时的α
     *
     * @param assign
     * @return
     */
    public float getSpriteAlpha(int assign) {
        // 根据assign数值适当添加差异
        float alpha = 0.4f + (float) assign * 0.5f;

        // 作为示例为α添加适当的差异
        if (alpha > 1.0f) {
            alpha = 1.0f;
        }
        if (alpha < 0.1f) {
            alpha = 0.1f;
        }
        return alpha;
    }

    /**
     * 返回渲染目标枚举实例。
     *
     * @return 渲染目标
     */
    public RenderingTarget getRenderingTarget() {
        return renderingTarget;
    }

    private final Context context;
    private final CubismMatrix44 deviceToScreen = CubismMatrix44.create(); // 用于将设备坐标转换为屏幕坐标的矩阵
    private final CubismViewMatrix viewMatrix = new CubismViewMatrix();   // 执行画面显示的缩放和移动变换的矩阵
    private int windowWidth;
    private int windowHeight;

    /**
     * 渲染目标的选择项
     */
    private RenderingTarget renderingTarget = RenderingTarget.NONE;
    /**
     * 渲染目标的清除颜色
     */
    private final float[] clearColor = new float[4];

    private CubismOffscreenSurfaceAndroid renderingBuffer = new CubismOffscreenSurfaceAndroid();

    private LAppSprite backSprite;
    private LAppSprite gearSprite;
    private LAppSprite powerSprite;
    private LAppSprite renderingSprite;

    private final TouchManager touchManager = new TouchManager();

    /**
     * 着色器创建委托类
     */
    private LAppSpriteShader spriteShader;

    /**
     * LAppModel的渲染目标
     */
    public enum RenderingTarget {
        NONE,   // 渲染到默认帧缓冲
        MODEL_FRAME_BUFFER,     // 渲染到LAppModel各自持有的帧缓冲
        VIEW_FRAME_BUFFER  // 渲染到LAppView持有的帧缓冲
    }
}