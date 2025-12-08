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

    public void loadModel(String modelDirectoryName) {
        String dir = "live2d/" + modelDirectoryName + "/";
        model = new LAppModel();
        model.loadAssets(dir, modelDirectoryName + ".model3.json", LAppDelegate.getInstance().getContext());
    }

    // 更新和绘制模型
    public void onUpdate() {
        int width = LAppDelegate.getInstance().getWindowWidth();
        int height = LAppDelegate.getInstance().getWindowHeight();

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

    /**
     * 拖拽屏幕时的处理
     *
     * @param x 屏幕的x坐标
     * @param y 屏幕的y坐标
     */
    public void onDrag(float x, float y) {
        model.setDragging(x, y);
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

    /**
     * 返回当前场景中持有的模型
     *
     * @return 模型实例
     */
    public LAppModel getModel() {
        return model;
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
        return model.startRandomMotion(group, priority);
    }

    /**
     * 设置指定的表情动作。
     *
     * @param expressionID 表情动作的ID
     */
    public void setExpression(final String expressionID) {
        model.setExpression(expressionID);
    }

    /**
     * 随机设置选定的表情动作。
     */
    public void setRandomExpression() {
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
    }

    private LAppModel model;

    // onUpdate方法中使用的缓存变量
    private final CubismMatrix44 viewMatrix = CubismMatrix44.create();
    private final CubismMatrix44 projection = CubismMatrix44.create();
}