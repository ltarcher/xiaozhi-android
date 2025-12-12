/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.opengl.GLES20;
import android.util.Log;

import com.live2d.TouchManager;
import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.math.CubismViewMatrix;
import com.live2d.sdk.cubism.framework.rendering.android.CubismOffscreenSurfaceAndroid;

import static com.live2d.LAppDefine.*;

import static android.opengl.GLES20.*;

public class LAppView implements AutoCloseable {
    private static final String TAG = "LAppView";
    
    /**
     * LAppModel的渲染目标
     */
    public enum RenderingTarget {
        NONE,   // 默认帧缓冲区渲染
        MODEL_FRAME_BUFFER,     // LAppModelForSmallDemo各自持有的帧缓冲区渲染
        VIEW_FRAME_BUFFER  // LAppViewForSmallDemo持有的帧缓冲区渲染
    }

    public LAppView() {
        Log.d(TAG, "LAppView constructor called");
        clearColor[0] = 1.0f;
        clearColor[1] = 1.0f;
        clearColor[2] = 1.0f;
        clearColor[3] = 0.0f;
    }

    @Override
    public void close() {
        Log.d(TAG, "close: Closing LAppView");
        spriteShader.close();
    }

    // 初始化
    public void initialize() {
        Log.d(TAG, "initialize: Initializing LAppView");
        int width = LAppDelegate.getInstance().getWindowWidth();
        int height = LAppDelegate.getInstance().getWindowHeight();
        Log.d(TAG, "initialize: Window size - width=" + width + ", height=" + height);

        float ratio = (float) width / (float) height;
        float left = -ratio;
        float right = ratio;
        float bottom = LogicalView.LEFT.getValue();
        float top = LogicalView.RIGHT.getValue();

        // 对应设备的屏幕范围。X的左端、X的右端、Y的下端、Y的上端
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

        // 显示范围设置
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

    // 精灵初始化
    public void initializeSprite() {
        Log.d(TAG, "initializeSprite: Initializing sprites");
        int windowWidth = LAppDelegate.getInstance().getWindowWidth();
        int windowHeight = LAppDelegate.getInstance().getWindowHeight();
        Log.d(TAG, "initializeSprite: Window size - width=" + windowWidth + ", height=" + windowHeight);

        LAppTextureManager textureManager = LAppDelegate.getInstance().getTextureManager();
        Log.d(TAG, "initializeSprite: Got texture manager");

        if (textureManager == null) {
            Log.e(TAG, "initializeSprite: Texture manager is null");
            return;
        }

        // 获取着色器程序ID
        int programId = spriteShader.getShaderId();
        Log.d(TAG, "initializeSprite: Program ID = " + programId);
        
        if (programId == 0) {
            Log.e(TAG, "initializeSprite: Failed to load shader program");
            return;
        }

        // 背景图片读取
        String backPath = ResourcePath.ROOT.getPath() + ResourcePath.BACK_IMAGE.getPath();
        Log.d(TAG, "initializeSprite: Loading back texture from " + backPath);
        LAppTextureManager.TextureInfo backTexture = textureManager.createTextureFromPngFile(backPath);
        Log.d(TAG, "initializeSprite: Back texture loaded, null=" + (backTexture == null));
        
        if (backTexture == null) {
            Log.e(TAG, "initializeSprite: Failed to load back texture");
        } else {
            float x = windowWidth * 0.5f;
            float y = windowHeight * 0.5f;
            float fWidth = (float) windowWidth;
            float fHeight = (float) windowHeight;
            Log.d(TAG, "initializeSprite: Back sprite params - x=" + x + ", y=" + y + 
                  ", width=" + fWidth + ", height=" + fHeight);

            if (backSprite == null) {
                backSprite = new LAppSprite(x, y, fWidth, fHeight, backTexture.id, programId);
                Log.d(TAG, "initializeSprite: Created new back sprite");
            } else {
                backSprite.resize(x, y, fWidth, fHeight);
                Log.d(TAG, "initializeSprite: Resized existing back sprite");
            }
        }

        // 齿轮图片读取
        String gearPath = ResourcePath.ROOT.getPath() + ResourcePath.GEAR_IMAGE.getPath();
        Log.d(TAG, "initializeSprite: Loading gear texture from " + gearPath);
        LAppTextureManager.TextureInfo gearTexture = textureManager.createTextureFromPngFile(gearPath);
        Log.d(TAG, "initializeSprite: Gear texture loaded, null=" + (gearTexture == null));
        
        if (gearTexture == null) {
            Log.e(TAG, "initializeSprite: Failed to load gear texture");
        } else {
            float x = windowWidth - gearTexture.width * 0.5f;
            float y = windowHeight - gearTexture.height * 0.5f;
            float fWidth = (float) gearTexture.width;
            float fHeight = (float) gearTexture.height;
            Log.d(TAG, "initializeSprite: Gear sprite params - x=" + x + ", y=" + y + 
                  ", width=" + fWidth + ", height=" + fHeight);

            if (gearSprite == null) {
                gearSprite = new LAppSprite(x, y, fWidth, fHeight, gearTexture.id, programId);
                Log.d(TAG, "initializeSprite: Created new gear sprite");
            } else {
                gearSprite.resize(x, y, fWidth, fHeight);
                Log.d(TAG, "initializeSprite: Resized existing gear sprite");
            }
        }

        // 电源按钮图片读取
        String powerPath = ResourcePath.ROOT.getPath() + ResourcePath.POWER_IMAGE.getPath();
        Log.d(TAG, "initializeSprite: Loading power texture from " + powerPath);
        LAppTextureManager.TextureInfo powerTexture = textureManager.createTextureFromPngFile(powerPath);
        Log.d(TAG, "initializeSprite: Power texture loaded, null=" + (powerTexture == null));
        
        if (powerTexture == null) {
            Log.e(TAG, "initializeSprite: Failed to load power texture");
        } else {
            float x = powerTexture.width * 0.5f;
            float y = windowHeight - powerTexture.height * 0.5f;
            float fWidth = (float) powerTexture.width;
            float fHeight = (float) powerTexture.height;
            Log.d(TAG, "initializeSprite: Power sprite params - x=" + x + ", y=" + y + 
                  ", width=" + fWidth + ", height=" + fHeight);

            if (powerSprite == null) {
                powerSprite = new LAppSprite(x, y, fWidth, fHeight, powerTexture.id, programId);
                Log.d(TAG, "initializeSprite: Created new power sprite");
            } else {
                powerSprite.resize(x, y, fWidth, fHeight);
                Log.d(TAG, "initializeSprite: Resized existing power sprite");
            }
        }
        
        // 覆盖整个屏幕的尺寸
        float x = windowWidth * 0.5f;
        float y = windowHeight * 0.5f;

        if (renderingSprite == null) {
            renderingSprite = new LAppSprite(x, y, windowWidth, windowHeight, 0, programId);
        } else {
            renderingSprite.resize(x, y, windowWidth, windowHeight);
        }
        Log.d(TAG, "initializeSprite: Sprites initialization completed");
    }

    // 绘制
    public void render() {
        // 获取屏幕尺寸
        int maxWidth = LAppDelegate.getInstance().getWindowWidth();
        int maxHeight = LAppDelegate.getInstance().getWindowHeight();

        // 添加空值检查，防止NullPointerException
        if (backSprite != null) {
            backSprite.setWindowSize(maxWidth, maxHeight);
        } else {
            Log.w(TAG, "render: backSprite is null");
        }
        
        if (gearSprite != null) {
            gearSprite.setWindowSize(maxWidth, maxHeight);
        } else {
            Log.w(TAG, "render: gearSprite is null");
        }
        
        if (powerSprite != null) {
            powerSprite.setWindowSize(maxWidth, maxHeight);
        } else {
            Log.w(TAG, "render: powerSprite is null");
        }

        // UI与背景的绘制
        // 注释掉下面这段代码来移除背景图片，使背景变为透明
        if (backSprite != null) {
            backSprite.render();
        }
        // 根据显示标志决定是否渲染齿轮按钮
        if (gearSprite != null && isGearVisible) {
            //Log.d(TAG, "render: Rendering gear sprite, visible=" + isGearVisible);
            gearSprite.render();
        } else {
            Log.d(TAG, "render: Not rendering gear sprite, gearSprite=" + gearSprite + ", isGearVisible=" + isGearVisible);
        }
        // 根据显示标志决定是否渲染关闭按钮
        if (powerSprite != null && isPowerVisible) {
            //Log.d(TAG, "render: Rendering power sprite, visible=" + isPowerVisible);
            powerSprite.render();
        } else {
            Log.d(TAG, "render: Not rendering power sprite, powerSprite=" + powerSprite + ", isPowerVisible=" + isPowerVisible);
        }

        if (isChangedModel) {
            isChangedModel = false;
            LAppLive2DManager.getInstance().nextScene();
        }

        // 模型绘制
        LAppLive2DManager live2dManager = LAppLive2DManager.getInstance();
        live2dManager.onUpdate();

        // 当各模型持有纹理作为绘制目标时
        if (renderingTarget == RenderingTarget.MODEL_FRAME_BUFFER && renderingSprite != null) {
            final float[] uvVertex = {
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f
            };

            for (int i = 0; i < live2dManager.getModelNum(); i++) {
                LAppModel model = live2dManager.getModel(i);
                float alpha = i < 1 ? 1.0f : model.getOpacity();    // 获取第一个的不透明度

                renderingSprite.setColor(1.0f * alpha, 1.0f * alpha, 1.0f * alpha, alpha);

                if (model != null && renderingSprite != null) {
                    renderingSprite.setWindowSize(maxWidth, maxHeight);
                    renderingSprite.renderImmediate(model.getRenderingBuffer().getColorBuffer()[0], uvVertex);
                }
            }
        }
    }

    /**
     * 在每个模型绘制前调用
     *
     * @param refModel 模型数据
     */
    public void preModelDraw(LAppModel refModel) {
        // 用于在其他渲染目标上绘制的离屏表面
        CubismOffscreenSurfaceAndroid useTarget;

        // 透明度设置
        GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        // 当在其他渲染目标上绘制时
        if (renderingTarget != RenderingTarget.NONE) {

            // 使用的目标
            useTarget = (renderingTarget == RenderingTarget.VIEW_FRAME_BUFFER)
                        ? renderingBuffer
                        : refModel.getRenderingBuffer();

            // 如果绘制目标内部尚未创建，则在此处创建
            if (!useTarget.isValid()) {
                int width = LAppDelegate.getInstance().getWindowWidth();
                int height = LAppDelegate.getInstance().getWindowHeight();

                // 模型绘制画布
                useTarget.createOffscreenSurface((int) width, (int) height, null);
            }
            // 开始绘制
            useTarget.beginDraw(null);
            useTarget.clear(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);   // 背景清除颜色
        }
    }

    /**
     * 在每个模型绘制后调用
     *
     * @param refModel 模型数据
     */
    public void postModelDraw(LAppModel refModel) {
        CubismOffscreenSurfaceAndroid useTarget = null;

        // 当对其他渲染目标进行绘制时
        if (renderingTarget != RenderingTarget.NONE) {
            // 使用的目标
            useTarget = (renderingTarget == RenderingTarget.VIEW_FRAME_BUFFER)
                        ? renderingBuffer
                        : refModel.getRenderingBuffer();

            // 绘制结束
            useTarget.endDraw();

            // 当使用LAppView拥有的帧缓冲区时在这里进行精灵绘制
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
        Log.d(TAG, "onTouchesBegan: pointX=" + pointX + ", pointY=" + pointY);
        touchManager.touchesBegan(pointX, pointY);
    }

    /**
     * 触摸时手指移动时调用
     *
     * @param pointX 屏幕X坐标
     * @param pointY 屏幕Y坐标
     */
    public void onTouchesMoved(float pointX, float pointY) {
        Log.d(TAG, "onTouchesMoved: pointX=" + pointX + ", pointY=" + pointY);
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
        Log.d(TAG, "onTouchesEnded: pointX=" + pointX + ", pointY=" + pointY);
        // 触摸结束
        LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
        live2DManager.onDrag(0.0f, 0.0f);

        // 点击
        // 获取逻辑坐标转换后的坐标
        float x = deviceToScreen.transformX(touchManager.getLastX());
        // 获取逻辑坐标转换后的坐标
        float y = deviceToScreen.transformY(touchManager.getLastY());

        if (DEBUG_TOUCH_LOG_ENABLE) {
            LAppPal.printLog("Touches ended x: " + x + ", y:" + y);
        }

        live2DManager.onTap(x, y);

        // 齿轮按钮是否被按下 (只有在可见时才响应点击)
        if (isGearVisible && gearSprite != null && gearSprite.isHit(pointX, pointY)) {
            Log.d(TAG, "onTouchesEnded: Gear button clicked");
            isChangedModel = true;
        } else {
            Log.d(TAG, "onTouchesEnded: Gear button not clicked. Visible: " + isGearVisible + 
                  ", gearSprite: " + gearSprite + ", hit: " + (gearSprite != null ? gearSprite.isHit(pointX, pointY) : "N/A"));
        }

        // 电源按钮是否被按下 (只有在可见时才响应点击)
        if (isPowerVisible && powerSprite != null && powerSprite.isHit(pointX, pointY)) {
            Log.d(TAG, "onTouchesEnded: Power button clicked");
            // 应用程序结束
            LAppDelegate.getInstance().deactivateApp();
        } else {
            Log.d(TAG, "onTouchesEnded: Power button not clicked. Visible: " + isPowerVisible + 
                  ", powerSprite: " + powerSprite + ", hit: " + (powerSprite != null ? powerSprite.isHit(pointX, pointY) : "N/A"));
        }

    }

    /**
     * 将X坐标转换为视图坐标
     *
     * @param deviceX 设备X坐标
     * @return 视图X坐标
     */
    public float transformViewX(float deviceX) {
        // 获取逻辑坐标转换后的坐标
        float screenX = deviceToScreen.transformX(deviceX);
        // 放大、缩小、移动后的值
        return viewMatrix.invertTransformX(screenX);
    }

    /**
     * 将Y坐标转换为视图坐标
     *
     * @param deviceY 设备Y坐标
     * @return 视图Y坐标
     */
    public float transformViewY(float deviceY) {
        // 获取逻辑坐标转换后的坐标
        float screenY = deviceToScreen.transformY(deviceY);
        // 放大、缩小、移动后的值
        return viewMatrix.invertTransformX(screenY);
    }

    /**
     * 将X坐标转换为屏幕坐标
     *
     * @param deviceX 设备X坐标
     * @return 屏幕X坐标
     */
    public float transformScreenX(float deviceX) {
        return deviceToScreen.transformX(deviceX);
    }

    /**
     * 将Y坐标转换为屏幕坐标
     *
     * @param deviceY 设备Y坐标
     * @return 屏幕Y坐标
     */
    public float transformScreenY(float deviceY) {
        return deviceToScreen.transformX(deviceY);
    }

    /**
     * 切换到默认以外的渲染目标时设置背景清除颜色
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
     * 在其他渲染目标上绘制模型时决定绘制时的alpha值
     *
     * @param assign
     * @return
     */
    public float getSpriteAlpha(int assign) {
        // 根据assign的值适当增减
        float alpha = 0.4f + (float) assign * 0.5f;

        // 例如适当增减alpha
        if (alpha > 1.0f) {
            alpha = 1.0f;
        }
        if (alpha < 0.1f) {
            alpha = 0.1f;
        }
        return alpha;
    }

    /**
     * 设置齿轮按钮的可见性
     * 
     * @param visible true表示显示，false表示隐藏
     */
    public void setGearVisible(boolean visible) {
        Log.d(TAG, "setGearVisible: Setting gear visible to " + visible);
        this.isGearVisible = visible;
        // 强制下次渲染时更新显示
        requestRender();
    }

    /**
     * 设置电源按钮的可见性
     * 
     * @param visible true表示显示，false表示隐藏
     */
    public void setPowerVisible(boolean visible) {
        Log.d(TAG, "setPowerVisible: Setting power visible to " + visible);
        this.isPowerVisible = visible;
        // 强制下次渲染时更新显示
        requestRender();
    }

    /**
     * 请求重新渲染视图
     */
    private void requestRender() {
        // 通知LAppDelegate需要重新渲染
        LAppDelegate appDelegate = LAppDelegate.getInstance();
        if (appDelegate != null) {
            appDelegate.requestRender();
            Log.d(TAG, "requestRender: Render requested");
            
            // 额外的同步渲染请求，确保立即更新
            if (appDelegate.getActivity() != null) {
                // 在主线程中强制刷新
                appDelegate.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 再次请求渲染以确保UI更新
                        appDelegate.requestRender();
                        Log.d(TAG, "requestRender: Additional render request from UI thread");
                    }
                });
            }
        } else {
            Log.w(TAG, "requestRender: LAppDelegate is null");
        }
    }

    /**
     * 获取齿轮按钮的可见性状态
     * 
     * @return true表示显示，false表示隐藏
     */
    public boolean isGearVisible() {
        Log.d(TAG, "isGearVisible: Returning " + isGearVisible);
        return isGearVisible;
    }

    /**
     * 获取电源按钮的可见性状态
     * 
     * @return true表示显示，false表示隐藏
     */
    public boolean isPowerVisible() {
        Log.d(TAG, "isPowerVisible: Returning " + isPowerVisible);
        return isPowerVisible;
    }

    /**
     * Return rendering target enum instance.
     *
     * @return rendering target
     */
    public RenderingTarget getRenderingTarget() {
        return renderingTarget;
    }

    private final CubismMatrix44 deviceToScreen = CubismMatrix44.create(); // 设备坐标到屏幕坐标转换矩阵
    private final CubismViewMatrix viewMatrix = new CubismViewMatrix();   // 显示的放大・移动用矩阵
    private int windowWidth;
    private int windowHeight;

    /**
     * 渲染目标的选择
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

    /**
     * 模型切换标志
     */
    private boolean isChangedModel;

    /**
     * 齿轮按钮可见性标志，默认为true（可见）
     */
    private boolean isGearVisible = true;

    /**
     * 电源按钮可见性标志，默认为true（可见）
     */
    private boolean isPowerVisible = true;

    private final TouchManager touchManager = new TouchManager();

    /**
     * 着色器创建委托类
     */
    private LAppSpriteShader spriteShader;
}