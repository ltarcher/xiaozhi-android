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
     * LAppModelのレンダリングターゲット
     */
    public enum RenderingTarget {
        NONE,   // デフォルトフレームバッファレンダリング
        MODEL_FRAME_BUFFER,     // LAppModelForSmallDemoがそれぞれ持つフレームバッファレンダリング
        VIEW_FRAME_BUFFER  // LAppViewForSmallDemoが持つフレームバッファレンダリング
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

    // 初期化
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

        // 対応デバイスのスクリーン範囲。Xの左端、Xの右端、Yの下端、Yの上端
        viewMatrix.setScreenRect(left, right, bottom, top);
        viewMatrix.scale(Scale.DEFAULT.getValue(), Scale.DEFAULT.getValue());

        // 初期化は単位行列
        deviceToScreen.loadIdentity();

        if (width > height) {
            float screenW = Math.abs(right - left);
            deviceToScreen.scaleRelative(screenW / width, -screenW / width);
        } else {
            float screenH = Math.abs(top - bottom);
            deviceToScreen.scaleRelative(screenH / height, -screenH / height);
        }
        deviceToScreen.translateRelative(-width * 0.5f, -height * 0.5f);

        // 表示範囲の設定
        viewMatrix.setMaxScale(Scale.MAX.getValue());   // 極限拡大率
        viewMatrix.setMinScale(Scale.MIN.getValue());   // 極限縮小率

        // 表示可能な最大範囲
        viewMatrix.setMaxScreenRect(
            MaxLogicalView.LEFT.getValue(),
            MaxLogicalView.RIGHT.getValue(),
            MaxLogicalView.BOTTOM.getValue(),
            MaxLogicalView.TOP.getValue()
        );

        spriteShader = new LAppSpriteShader();
    }

    // スプライトの初期化
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

        // シェーダープログラムIDの取得
        int programId = spriteShader.getShaderId();
        Log.d(TAG, "initializeSprite: Program ID = " + programId);
        
        if (programId == 0) {
            Log.e(TAG, "initializeSprite: Failed to load shader program");
            return;
        }

        // 背景画像読み込み
        String backPath = ResourcePath.ROOT.getPath() + ResourcePath.BACK_IMAGE.getPath();
        Log.d(TAG, "initializeSprite: Loading back texture from " + backPath);
        LAppTextureManager.TextureInfo backTexture = textureManager.createTextureFromPngFile(backPath);
        Log.d(TAG, "initializeSprite: Back texture loaded, null=" + (backTexture == null));
        
        if (backTexture == null) {
            Log.e(TAG, "initializeSprite: Failed to load back texture");
        } else {
            float x = windowWidth * 0.5f;
            float y = windowHeight * 0.5f;
            float fWidth = (float) backTexture.width;
            float fHeight = (float) backTexture.height;
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

        // ギア画像読み込み
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

        // 電源ボタン画像読み込み
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
        
        // 画面全体を覆うサイズ
        float x = windowWidth * 0.5f;
        float y = windowHeight * 0.5f;

        if (renderingSprite == null) {
            renderingSprite = new LAppSprite(x, y, windowWidth, windowHeight, 0, programId);
        } else {
            renderingSprite.resize(x, y, windowWidth, windowHeight);
        }
        Log.d(TAG, "initializeSprite: Sprites initialization completed");
    }

    // 描画
    public void render() {
        // 画面サイズの取得
        int maxWidth = LAppDelegate.getInstance().getWindowWidth();
        int maxHeight = LAppDelegate.getInstance().getWindowHeight();

        // nullチェックを追加し、NullPointerExceptionを防ぐ
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

        // UIと背景の描画
        if (backSprite != null) {
            backSprite.render();
        }
        if (gearSprite != null) {
            gearSprite.render();
        }
        if (powerSprite != null) {
            powerSprite.render();
        }

        if (isChangedModel) {
            isChangedModel = false;
            LAppLive2DManager.getInstance().nextScene();
        }

        // モデルの描画
        LAppLive2DManager live2dManager = LAppLive2DManager.getInstance();
        live2dManager.onUpdate();

        // 各モデルが描画先としてテクスチャを持っているとき
        if (renderingTarget == RenderingTarget.MODEL_FRAME_BUFFER && renderingSprite != null) {
            final float[] uvVertex = {
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f
            };

            for (int i = 0; i < live2dManager.getModelNum(); i++) {
                LAppModel model = live2dManager.getModel(i);
                float alpha = i < 1 ? 1.0f : model.getOpacity();    // 1つだけの不透明度を取得

                renderingSprite.setColor(1.0f * alpha, 1.0f * alpha, 1.0f * alpha, alpha);

                if (model != null && renderingSprite != null) {
                    renderingSprite.setWindowSize(maxWidth, maxHeight);
                    renderingSprite.renderImmediate(model.getRenderingBuffer().getColorBuffer()[0], uvVertex);
                }
            }
        }
    }

    /**
     * 各モデル描画前に呼び出される
     *
     * @param refModel モデルデータ
     */
    public void preModelDraw(LAppModel refModel) {
        // 他のレンダリングターゲットに描画するためのオフスクリーンサーフェイス
        CubismOffscreenSurfaceAndroid useTarget;

        // 透明度の設定
        GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        // 他のレンダリングターゲットに描画するとき
        if (renderingTarget != RenderingTarget.NONE) {

            // 使用するターゲット
            useTarget = (renderingTarget == RenderingTarget.VIEW_FRAME_BUFFER)
                        ? renderingBuffer
                        : refModel.getRenderingBuffer();

            // 描画先内部で作られていなければここで作成
            if (!useTarget.isValid()) {
                int width = LAppDelegate.getInstance().getWindowWidth();
                int height = LAppDelegate.getInstance().getWindowHeight();

                // モデル描画キャンバス
                useTarget.createOffscreenSurface((int) width, (int) height, null);
            }
            // 描画開始
            useTarget.beginDraw(null);
            useTarget.clear(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);   // 背景クリアカラー
        }
    }

    /**
     * 各モデル描画後に呼び出される
     *
     * @param refModel モデルデータ
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
     * レンダリングターゲットの切り替え
     *
     * @param targetType レンダリングターゲット
     */
    public void switchRenderingTarget(RenderingTarget targetType) {
        renderingTarget = targetType;
    }

    /**
     * タッチ時呼び出される
     *
     * @param pointX 画面X座標
     * @param pointY 画面Y座標
     */
    public void onTouchesBegan(float pointX, float pointY) {
        touchManager.touchesBegan(pointX, pointY);
    }

    /**
     * タッチ時指が動いたときに呼び出される
     *
     * @param pointX 画面X座標
     * @param pointY 画面Y座標
     */
    public void onTouchesMoved(float pointX, float pointY) {
        float viewX = transformViewX(touchManager.getLastX());
        float viewY = transformViewY(touchManager.getLastY());

        touchManager.touchesMoved(pointX, pointY);

        LAppLive2DManager.getInstance().onDrag(viewX, viewY);
    }

    /**
     * タッチ終了時に呼び出される
     *
     * @param pointX 画面X座標
     * @param pointY 画面Y座標
     */
    public void onTouchesEnded(float pointX, float pointY) {
        // タッチ終了
        LAppLive2DManager live2DManager = LAppLive2DManager.getInstance();
        live2DManager.onDrag(0.0f, 0.0f);

        // タップ
        // 論理座標変換後の座標を取得
        float x = deviceToScreen.transformX(touchManager.getLastX());
        // 論理座標変換後の座標を取得
        float y = deviceToScreen.transformY(touchManager.getLastY());

        if (DEBUG_TOUCH_LOG_ENABLE) {
            LAppPal.printLog("Touches ended x: " + x + ", y:" + y);
        }

        live2DManager.onTap(x, y);

        // ギアボタンが押されたか
        if (gearSprite.isHit(pointX, pointY)) {
            isChangedModel = true;
        }

        // 電源ボタンが押されたか
        if (powerSprite.isHit(pointX, pointY)) {
            // 应用程序结束
            LAppDelegate.getInstance().deactivateApp();
        }

    }

    /**
     * X座標をビュー座標に変換する
     *
     * @param deviceX デバイスX座標
     * @return ビューX座標
     */
    public float transformViewX(float deviceX) {
        // 論理座標変換後の座標を取得
        float screenX = deviceToScreen.transformX(deviceX);
        // 拡大、縮小、移動後の値
        return viewMatrix.invertTransformX(screenX);
    }

    /**
     * Y座標をビュー座標に変換する
     *
     * @param deviceY デバイスY座標
     * @return ビューY座標
     */
    public float transformViewY(float deviceY) {
        // 論理座標変換後の座標を取得
        float screenY = deviceToScreen.transformY(deviceY);
        // 拡大、縮小、移動後の値
        return viewMatrix.invertTransformX(screenY);
    }

    /**
     * X座標をスクリーン座標に変換する
     *
     * @param deviceX デバイスX座標
     * @return スクリーンX座標
     */
    public float transformScreenX(float deviceX) {
        return deviceToScreen.transformX(deviceX);
    }

    /**
     * Y座標をスクリーン座標に変換する
     *
     * @param deviceY デバイスY座標
     * @return スクリーンY座標
     */
    public float transformScreenY(float deviceY) {
        return deviceToScreen.transformX(deviceY);
    }

    /**
     * デフォルト以外のレンダリングターゲットに切り替えたときに背景クリアカラーを設定する
     *
     * @param r 赤(0.0~1.0)
     * @param g 緑(0.0~1.0)
     * @param b 青(0.0~1.0)
     */
    public void setRenderingTargetClearColor(float r, float g, float b) {
        clearColor[0] = r;
        clearColor[1] = g;
        clearColor[2] = b;
    }

    /**
     * 他のレンダリングターゲットにモデルを描画するときに描画時のαを決定する
     *
     * @param assign
     * @return
     */
    public float getSpriteAlpha(int assign) {
        // assignの値に応じて適宜加減
        float alpha = 0.4f + (float) assign * 0.5f;

        // 例えば適宜加減α
        if (alpha > 1.0f) {
            alpha = 1.0f;
        }
        if (alpha < 0.1f) {
            alpha = 0.1f;
        }
        return alpha;
    }

    /**
     * Return rendering target enum instance.
     *
     * @return rendering target
     */
    public RenderingTarget getRenderingTarget() {
        return renderingTarget;
    }

    private final CubismMatrix44 deviceToScreen = CubismMatrix44.create(); // デバイス座標からスクリーン座標への変換行列
    private final CubismViewMatrix viewMatrix = new CubismViewMatrix();   // 显示的放大・移动用矩阵
    private int windowWidth;
    private int windowHeight;

    /**
     * レンダリングターゲットの選択
     */
    private RenderingTarget renderingTarget = RenderingTarget.NONE;
    /**
     * レンダリングターゲットのクリアカラー
     */
    private final float[] clearColor = new float[4];

    private CubismOffscreenSurfaceAndroid renderingBuffer = new CubismOffscreenSurfaceAndroid();

    private LAppSprite backSprite;
    private LAppSprite gearSprite;
    private LAppSprite powerSprite;
    private LAppSprite renderingSprite;

    /**
     * モデル切り替えフラグ
     */
    private boolean isChangedModel;

    private final TouchManager touchManager = new TouchManager();

    /**
     * シェーダー作成委任クラス
     */
    private LAppSpriteShader spriteShader;
}
