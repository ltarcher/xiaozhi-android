/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import android.content.res.AssetManager;
import android.util.Log;

import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.motion.ACubismMotion;
import com.live2d.sdk.cubism.framework.motion.IBeganMotionCallback;
import com.live2d.sdk.cubism.framework.motion.IFinishedMotionCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.live2d.LAppDefine.*;

/**
 * 在示例应用程序中管理CubismModel的类
 * 负责模型的创建和销毁、点击事件的处理以及模型切换
 */
public class LAppLive2DManager {
    private static final String TAG = "LAppLive2DManager";
    
    public static LAppLive2DManager getInstance() {
        if (s_instance == null) {
            Log.d(TAG, "getInstance: 创建新的LAppLive2DManager实例");
            s_instance = new LAppLive2DManager();
        } else {
            //Log.d(TAG, "getInstance: 返回现有的LAppLive2DManager实例");
        }
        return s_instance;
    }

    public static void releaseInstance() {
        Log.d(TAG, "releaseInstance: 释放LAppLive2DManager实例");
        s_instance = null;
    }

    /**
     * 释放当前场景中持有的所有模型
     */
    public void releaseAllModel() {
        Log.d(TAG, "releaseAllModel: 释放所有模型, 数量=" + models.size());
        for (LAppModel model : models) {
            model.deleteModel();
        }
        models.clear();
    }

    /**
     * 设置assets文件夹中的模型文件夹名称
     */
    public void setUpModel() {
        // 遍历assets文件夹中的所有文件夹名称，定义包含模型的文件夹
        // 如果只有文件夹但没有同名的.model3.json文件，则不包含在列表中
        modelDir.clear();

        final AssetManager assets = LAppDelegate.getInstance().getActivity().getResources().getAssets();
        try {
            // Flutter assets的live2d目录内的文件列表
            String[] root = assets.list(LAppDefine.ResourcePath.ROOT.getPath());
            Log.d(TAG, "setUpModel: 在live2d目录中找到 " + (root != null ? root.length : 0) + " 个项目");
            
            if (root != null) {
                for (String item : root) {
                    Log.d(TAG, "setUpModel: 检查项目: " + item);
                    
                    // 仅检查可能是模型目录的项
                    if (!item.endsWith(".png") && !item.endsWith(".vert") && !item.endsWith(".frag") && !item.equals("Shaders")) {
                        try {
                            // 检查目录中的文件
                            String[] files = assets.list(LAppDefine.ResourcePath.ROOT.getPath() + item);
                            
                            if (files != null) {
                                String target = item + ".model3.json";
                                Log.d(TAG, "setUpModel: 在 " + item + " 中查找 " + target);
                                
                                // 查找与文件夹同名的.model3.json文件
                                for (String file : files) {
                                    if (file.equals(target)) {
                                        modelDir.add(item);
                                        Log.d(TAG, "setUpModel: 添加模型目录: " + item);
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "setUpModel: 检查项目 " + item + " 时出错: " + e.getMessage());
                        }
                    }
                }
            }
            
            Collections.sort(modelDir);
            Log.d(TAG, "setUpModel: 找到的最终模型目录数量: " + modelDir.size());
            for (String dir : modelDir) {
                Log.d(TAG, "setUpModel: 模型目录: " + dir);
            }
        } catch (IOException ex) {
            Log.e(TAG, "setUpModel: 发生IOException", ex);
            throw new IllegalStateException(ex);
        }
    }

    // 执行模型更新和绘制处理
    public void onUpdate() {
        int width = LAppDelegate.getInstance().getWindowWidth();
        int height = LAppDelegate.getInstance().getWindowHeight();

        for (int i = 0; i < models.size(); i++) {
            LAppModel model = models.get(i);

            if (model.getModel() == null) {
                LAppPal.printLog("模型获取失败");
                continue;
            }

            projection.loadIdentity();

            if (model.getModel().getCanvasWidth() > 1.0f && width < height) {
                // 在纵向窗口中显示横向较长的模型时，根据模型的横向尺寸计算缩放比例
                model.getModelMatrix().setWidth(2.0f);
                projection.scale(1.0f, (float) width / (float) height);
            } else {
                projection.scale((float) height / (float) width, 1.0f);
            }

            // 如有必要在此处相乘
            if (viewMatrix != null) {
                viewMatrix.multiplyByMatrix(projection);
            }

            // 单个模型绘制前调用
            LAppDelegate.getInstance().getView().preModelDraw(model);

            model.update();

            model.draw(projection);     // 由于是引用传递，projection会发生变化

            // 单个模型绘制后调用
            LAppDelegate.getInstance().getView().postModelDraw(model);
        }
    }

    /**
     * 屏幕拖拽时的处理
     *
     * @param x 屏幕的x坐标
     * @param y 屏幕的y坐标
     */
    public void onDrag(float x, float y) {
        for (int i = 0; i < models.size(); i++) {
            LAppModel model = getModel(i);
            model.setDragging(x, y);
        }
    }

    /**
     * 屏幕点击时的处理
     *
     * @param x 屏幕的x坐标
     * @param y 屏幕的y坐标
     */
    public void onTap(float x, float y) {
        if (DEBUG_LOG_ENABLE) {
            LAppPal.printLog("点击点: {" + x + ", y: " + y);
        }

        for (int i = 0; i < models.size(); i++) {
            LAppModel model = models.get(i);

            // 点击头部时随机播放表情
            if (model.hitTest(HitAreaName.HEAD.getId(), x, y)) {
                if (DEBUG_LOG_ENABLE) {
                    LAppPal.printLog("点击区域: " + HitAreaName.HEAD.getId());
                }
                model.setRandomExpression();
            }
            // 点击身体时开始随机动作
            else if (model.hitTest(HitAreaName.BODY.getId(), x, y)) {
                if (DEBUG_LOG_ENABLE) {
                    LAppPal.printLog("点击区域: " + HitAreaName.HEAD.getId());
                }

                model.startRandomMotion(MotionGroup.TAP_BODY.getId(), Priority.NORMAL.getPriority(), finishedMotion, beganMotion);
            }
        }
    }

    /**
     * 切换到下一个场景
     * 示例应用程序中执行模型集的切换
     */
    public void nextScene() {
        // 如果未找到模型则中止处理
        if (modelDir.isEmpty()) {
            Log.e(TAG, "nextScene: 未找到模型，中止场景切换");
            return;
        }
        
        final int number = (currentModel + 1) % modelDir.size();
        changeScene(number);
    }

    /**
     * 切换场景
     *
     * @param index 要切换的场景索引
     */
    public void changeScene(int index) {
        // 如果未找到模型则中止处理
        if (modelDir.isEmpty()) {
            Log.e(TAG, "changeScene: 未找到模型，中止场景切换");
            return;
        }
        
        // 如果索引超出范围也中止处理
        if (index < 0 || index >= modelDir.size()) {
            Log.e(TAG, "changeScene: 索引超出范围. index=" + index + ", size=" + modelDir.size());
            return;
        }
        
        currentModel = index;
        if (DEBUG_LOG_ENABLE) {
            LAppPal.printLog("模型索引: " + currentModel);
        }

        String modelDirName = modelDir.get(index);

        String modelPath = LAppDefine.ResourcePath.ROOT.getPath() + modelDirName + "/";
        String modelJsonName = modelDirName + ".model3.json";

        releaseAllModel();

        models.add(new LAppModel());
        models.get(0).loadAssets(modelPath, modelJsonName);

        /*
         * 提供模型半透明显示的示例
         * 当定义了USE_RENDER_TARGET、USE_MODEL_RENDER_TARGET时
         * 将模型绘制到另一个渲染目标上，并将绘制结果作为纹理贴到另一个精灵上
         */
        LAppView.RenderingTarget useRenderingTarget;
        if (USE_RENDER_TARGET) {
            // 选择向LAppView持有的目标进行绘制
            useRenderingTarget = LAppView.RenderingTarget.VIEW_FRAME_BUFFER;
        } else if (USE_MODEL_RENDER_TARGET) {
            // 选择向各个LAppModel持有的目标进行绘制
            useRenderingTarget = LAppView.RenderingTarget.MODEL_FRAME_BUFFER;
        } else {
            // 渲染到默认的主帧缓冲区（常规）
            useRenderingTarget = LAppView.RenderingTarget.NONE;
        }

        if (USE_RENDER_TARGET || USE_MODEL_RENDER_TARGET) {
            // 作为单独设置alpha的模型示例，再创建一个模型并稍微移动位置
            models.add(new LAppModel());
            models.get(1).loadAssets(modelPath, modelJsonName);
            models.get(1).getModelMatrix().translateX(0.2f);
        }

        // 切换渲染目标
        LAppDelegate.getInstance().getView().switchRenderingTarget(useRenderingTarget);

        // 选择其他渲染目标时的背景清除颜色
        float[] clearColor = {0.0f, 0.0f, 0.0f};
        LAppDelegate.getInstance().getView().setRenderingTargetClearColor(clearColor[0], clearColor[1], clearColor[2]);
    }

    /**
     * 返回当前场景持有的模型
     *
     * @param number 模型列表的索引值
     * @return 返回模型实例。如果索引值超出范围则返回null
     */
    public LAppModel getModel(int number) {
        if (number < models.size()) {
            return models.get(number);
        }
        return null;
    }

    /**
     * 返回场景索引
     *
     * @return 场景索引
     */
    public int getCurrentModel() {
        return currentModel;
    }

    /**
     * 返回此LAppLive2DManager实例拥有的模型数量
     *
     * @return 此LAppLive2DManager实例拥有的模型数量。如果模型列表为null，则返回0
     */
    public int getModelNum() {
        if (models == null) {
            return 0;
        }
        return models.size();
    }

    /**
     * 返回模型目录的数量
     *
     * @return 模型目录的数量
     */
    public int getModelDirSize() {
        return modelDir.size();
    }

    /**
     * 动作播放时执行的回调函数
     */
    private static class BeganMotion implements IBeganMotionCallback {
        @Override
        public void execute(ACubismMotion motion) {
            LAppPal.printLog("动作开始: " + motion);
        }
    }

    private static final BeganMotion beganMotion = new BeganMotion();

    /**
     * 动作结束时执行的回调函数
     */
    private static class FinishedMotion implements IFinishedMotionCallback {
        @Override
        public void execute(ACubismMotion motion) {
            LAppPal.printLog("动作结束: " + motion);
        }
    }

    private static final FinishedMotion finishedMotion = new FinishedMotion();

    /**
     * 单例实例
     */
    private static LAppLive2DManager s_instance;

    private LAppLive2DManager() {
        Log.d(TAG, "LAppLive2DManager: 初始化管理器");
        setUpModel();
        // 仅在找到模型时才切换场景
        if (!modelDir.isEmpty()) {
            Log.d(TAG, "LAppLive2DManager: 找到模型，切换到场景0");
            changeScene(0);
        } else {
            Log.e(TAG, "LAppLive2DManager: 初始化期间未找到模型");
        }
    }

    private final List<LAppModel> models = new ArrayList<>();

    /**
     * 显示场景的索引值
     */
    private int currentModel;

    /**
     * 模型目录名称
     */
    private final List<String> modelDir = new ArrayList<>();

    // onUpdate方法中使用的缓存变量
    private final CubismMatrix44 viewMatrix = CubismMatrix44.create();
    private final CubismMatrix44 projection = CubismMatrix44.create();
}