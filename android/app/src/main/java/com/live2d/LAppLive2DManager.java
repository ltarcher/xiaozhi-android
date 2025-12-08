/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d;

import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.motion.ACubismMotion;
import com.live2d.sdk.cubism.framework.motion.IBeganMotionCallback;
import com.live2d.sdk.cubism.framework.motion.IFinishedMotionCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.res.AssetManager;

import static com.live2d.LAppDefine.*;

/**
 * 在示例应用程序中管理CubismModel的类。
 * 负责模型的创建和销毁、点击事件的处理、模型切换。
 */
public class LAppLive2DManager {
    public static LAppLive2DManager getInstance() {
        if (s_instance == null) {
            s_instance = new LAppLive2DManager();
        }
        return s_instance;
    }

    public static void releaseInstance() {
        s_instance = null;
    }

    /**
     * 释放当前场景中持有的所有模型
     */
    public void releaseAllModel() {
        for (LAppModel model : models) {
            model.deleteModel();
        }
        models.clear();
    }

    /**
     * 加载指定名称的模型
     *
     * @param modelDirectoryName 模型目录名
     */
    public void loadModel(String modelDirectoryName) {
        String dir = "assets/live2d/" + modelDirectoryName + "/";
        LAppModel model = new LAppModel();
        model.loadAssets(dir, modelDirectoryName + ".model3.json", LAppDelegate.getInstance().getContext());
        
        // 清除现有模型并添加新模型
        releaseAllModel();
        models.add(model);
    }

    /**
     * 设置assets文件夹中的模型文件夹名
     */
    public void setUpModel() {
        // 扫描assets文件夹中的所有文件夹，找出包含.model3.json文件的文件夹
        // 如果只有文件夹但没有对应的.model3.json文件，则不包含在列表中
        modelDir.clear();

        final AssetManager assets = LAppDelegate.getInstance().getContext().getAssets();
        try {
            String[] live2dDirs = assets.list("assets/live2d");
            if (live2dDirs != null) {
                for (String modelDirName : live2dDirs) {
                    // 跳过非目录文件
                    if (modelDirName.contains(".")) {
                        continue;
                    }
                    
                    String[] files = assets.list("assets/live2d/" + modelDirName);
                    String target = modelDirName + ".model3.json";
                    // 查找与文件夹同名的.model3.json文件
                    if (files != null) {
                        for (String file : files) {
                            if (file.equals(target)) {
                                modelDir.add(modelDirName);
                                break;
                            }
                        }
                    }
                }
            }
            Collections.sort(modelDir);
        } catch (IOException ex) {
            ex.printStackTrace();
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                LAppPal.printLog("Failed to list assets: " + ex.getMessage());
            }
        }
    }

    // 更新和绘制模型
    public void onUpdate() {
        int width = LAppDelegate.getInstance().getWindowWidth();
        int height = LAppDelegate.getInstance().getWindowHeight();

        for (int i = 0; i < models.size(); i++) {
            LAppModel model = models.get(i);

            if (model.getModel() == null) {
                LAppPal.printLog("Failed to model.getModel().");
                continue;
            }

            projection.loadIdentity();

            if (model.getModel().getCanvasWidth() > 1.0f && width < height) {
                // 在纵向窗口中显示横向长模型时，根据模型的横向大小计算比例
                model.getModelMatrix().setWidth(2.0f);
                projection.scale(1.0f, (float) width / (float) height);
            } else {
                projection.scale((float) height / (float) width, 1.0f);
            }

            // 如果需要在此处相乘
            if (viewMatrix != null) {
                viewMatrix.multiplyByMatrix(projection);
            }

            // 绘制前调用
            LAppDelegate.getInstance().getView().preModelDraw(model);

            model.update();

            model.draw(projection);     // 由于是引用传递，projection会发生变化

            // 绘制后调用
            LAppDelegate.getInstance().getView().postModelDraw(model);
        }
    }

    /**
     * 拖拽屏幕时的处理
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
     * 点击屏幕时的处理
     *
     * @param x 屏幕的x坐标
     * @param y 屏幕的y坐标
     */
    public void onTap(float x, float y) {
        if (DEBUG_LOG_ENABLE) {
            LAppPal.printLog("tap point: {" + x + ", y: " + y);
        }

        for (int i = 0; i < models.size(); i++) {
            LAppModel model = models.get(i);

            // 点击头部时随机播放表情
            if (model.hitTest(HitAreaName.HEAD.getId(), x, y)) {
                if (DEBUG_LOG_ENABLE) {
                    LAppPal.printLog("hit area: " + HitAreaName.HEAD.getId());
                }
                model.setRandomExpression();
            }
            // 点击身体时开始随机动作
            else if (model.hitTest(HitAreaName.BODY.getId(), x, y)) {
                if (DEBUG_LOG_ENABLE) {
                    LAppPal.printLog("hit area: " + HitAreaName.BODY.getId());
                }

                model.startRandomMotion(MotionGroup.TAP_BODY.getId(), Priority.NORMAL.getPriority(), finishedMotion, beganMotion);
            }
        }
    }

    /**
     * 切换到下一个场景
     * 在示例应用程序中，这会切换模型集
     */
    public void nextScene() {
        final int number = (currentModel + 1) % modelDir.size();
        changeScene(number);
    }

    /**
     * 切换场景
     *
     * @param index 要切换的场景索引
     */
    public void changeScene(int index) {
        currentModel = index;
        if (DEBUG_LOG_ENABLE) {
            LAppPal.printLog("model index: " + currentModel);
        }

        if (modelDir.size() <= index) {
            LAppPal.printLog("Invalid model index: " + index);
            return;
        }

        String modelDirName = modelDir.get(index);

        String modelPath = "assets/live2d/" + modelDirName + "/";
        String modelJsonName = modelDirName + ".model3.json";

        releaseAllModel();

        models.add(new LAppModel());
        models.get(0).loadAssets(modelPath, modelJsonName, LAppDelegate.getInstance().getContext());

        /*
         * 提供一个使用模型半透明显示的示例。
         * 当在这里定义USE_RENDER_TARGET、USE_MODEL_RENDER_TARGET时
         * 将模型绘制到另一个渲染目标，并将绘制结果作为纹理贴到另一个精灵上。
         */
        LAppView.RenderingTarget useRenderingTarget;
        if (USE_RENDER_TARGET) {
            // 选择绘制到LAppView持有的目标
            useRenderingTarget = LAppView.RenderingTarget.VIEW_FRAME_BUFFER;
        } else if (USE_MODEL_RENDER_TARGET) {
            // 选择绘制到各个LAppModel持有的目标
            useRenderingTarget = LAppView.RenderingTarget.MODEL_FRAME_BUFFER;
        } else {
            // 默认渲染到主帧缓冲区（常规）
            useRenderingTarget = LAppView.RenderingTarget.NONE;
        }

        if (USE_RENDER_TARGET || USE_MODEL_RENDER_TARGET) {
            // 作为单独为模型添加alpha的示例，再创建一个模型并稍微移动位置。
            models.add(new LAppModel());
            models.get(1).loadAssets(modelPath, modelJsonName, LAppDelegate.getInstance().getContext());
            models.get(1).getModelMatrix().translateX(0.2f);
        }

        // 切换渲染目标
        LAppDelegate.getInstance().getView().switchRenderingTarget(useRenderingTarget);

        // 选择其他渲染目标时的背景清除色
        float[] clearColor = {0.0f, 0.0f, 0.0f};
        LAppDelegate.getInstance().getView().setRenderingTargetClearColor(clearColor[0], clearColor[1], clearColor[2]);
    }

    /**
     * 返回当前场景中持有的模型
     *
     * @param number 模型列表的索引值
     * @return 模型实例。如果索引超出范围则返回null
     */
    public LAppModel getModel(int number) {
        if (number < models.size()) {
            return models.get(number);
        }
        return null;
    }

    /**
     * 返回当前场景中持有的模型
     *
     * @return 模型实例
     */
    public LAppModel getModel() {
        if (!models.isEmpty()) {
            return models.get(0);
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
     * 播放指定的动作。
     *
     * @param group      动作组名
     * @param number     组内编号
     * @param priority   优先级
     * @return 返回开始的动作的标识号。用于判断个别动作是否结束的isFinished()参数。开始失败时返回"-1"
     */
    public int startMotion(final String group, int number, int priority) {
        LAppModel model = getModel();
        if (model == null) {
            return -1;
        }
        return model.startMotion(group, number, priority);
    }

    /**
     * 随机播放选定的动作。
     *
     * @param group    动作组名
     * @param priority 优先级
     * @return 返回开始的动作的标识号。用于判断个别动作是否结束的isFinished()参数。开始失败时返回-1
     */
    public int startRandomMotion(final String group, int priority) {
        LAppModel model = getModel();
        if (model == null) {
            return -1;
        }
        return model.startRandomMotion(group, priority);
    }

    /**
     * 设置指定的表情动作。
     *
     * @param expressionID 表情动作的ID
     */
    public void setExpression(final String expressionID) {
        LAppModel model = getModel();
        if (model == null) {
            return;
        }
        model.setExpression(expressionID);
    }

    /**
     * 随机设置选定的表情动作。
     */
    public void setRandomExpression() {
        LAppModel model = getModel();
        if (model == null) {
            return;
        }
        model.setRandomExpression();
    }

    /**
     * 动作播放时执行的回调函数
     */
    private static class BeganMotion implements IBeganMotionCallback {
        @Override
        public void execute(ACubismMotion motion) {
            LAppPal.printLog("Motion Began: " + motion);
        }
    }

    private static final BeganMotion beganMotion = new BeganMotion();

    /**
     * 动作结束时执行的回调函数
     */
    private static class FinishedMotion implements IFinishedMotionCallback {
        @Override
        public void execute(ACubismMotion motion) {
            LAppPal.printLog("Motion Finished: " + motion);
        }
    }

    private static final FinishedMotion finishedMotion = new FinishedMotion();

    /**
     * 单例实例
     */
    private static LAppLive2DManager s_instance;

    private LAppLive2DManager() {
        setUpModel();
        // 初始化时加载第一个模型
        if (!modelDir.isEmpty()) {
            changeScene(0);
        }
    }

    private final List<LAppModel> models = new ArrayList<>();
    
    /**
     * 显示的场景索引值
     */
    private int currentModel;
    
    /**
     * 模型目录名列表
     */
    private final List<String> modelDir = new ArrayList<>();

    // onUpdate方法中使用的缓存变量
    private final CubismMatrix44 viewMatrix = CubismMatrix44.create();
    private final CubismMatrix44 projection = CubismMatrix44.create();
}