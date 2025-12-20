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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.live2d.LAppDefine.*;

/**
 * 在示例应用程序中管理CubismModel的类
 * 负责模型的创建和销毁、点击事件的处理以及模型切换
 * 现在支持多实例，通过实例ID区分不同的Live2D实例
 */
public class LAppLive2DManager {
    private static final String TAG = "LAppLive2DManager";
    
    /**
     * 实例ID到管理器实例的映射
     */
    private static final Map<String, LAppLive2DManager> s_instances = new HashMap<>();
    
    /**
     * 获取指定实例ID的LAppLive2DManager
     *
     * @param instanceId 实例ID，如果为null则返回默认实例
     * @return LAppLive2DManager实例
     */
    public static LAppLive2DManager getInstance(String instanceId) {
        //Log.d(TAG, "getInstance: Requested manager for instance: " + instanceId);
        
        if (instanceId == null) {
            instanceId = Live2DInstanceManager.getInstance().getDefaultInstanceId();
            if (instanceId == null) {
                Log.w(TAG, "getInstance: No default instance available, using singleton fallback");
                return getInstance();
            }
        }
        
        LAppLive2DManager manager = s_instances.get(instanceId);
        if (manager == null) {
            Log.d(TAG, "getInstance: Creating new LAppLive2DManager for instance: " + instanceId);
            manager = new LAppLive2DManager(instanceId);
            s_instances.put(instanceId, manager);
        }
        
        return manager;
    }
    
    /**
     * 获取默认实例（向后兼容）
     */
    public static LAppLive2DManager getInstance() {
        if (s_instances.isEmpty()) {
            Log.d(TAG, "getInstance: Creating default singleton manager");
            LAppLive2DManager manager = new LAppLive2DManager();
            s_instances.put("default", manager);
            return manager;
        }
        
        // 返回第一个可用的实例
        return s_instances.values().iterator().next();
    }
    
    /**
     * 释放指定实例ID的管理器
     *
     * @param instanceId 实例ID
     */
    public static void releaseInstance(String instanceId) {
        Log.d(TAG, "releaseInstance: Releasing manager for instance: " + instanceId);
        
        if (instanceId != null) {
            LAppLive2DManager manager = s_instances.remove(instanceId);
            if (manager != null) {
                manager.releaseAllModel();
                Log.d(TAG, "releaseInstance: Manager released for instance: " + instanceId);
            }
        }
    }
    
    /**
     * 释放所有实例
     */
    public static void releaseAllInstances() {
        Log.d(TAG, "releaseAllInstances: Releasing all managers");
        
        for (Map.Entry<String, LAppLive2DManager> entry : s_instances.entrySet()) {
            entry.getValue().releaseAllModel();
        }
        
        s_instances.clear();
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

        LAppDelegate appDelegate = LAppDelegate.getInstance(instanceId);
        if (appDelegate == null) {
            Log.e(TAG, "setUpModel: Cannot get app delegate for instance: " + instanceId);
            return;
        }
        
        final AssetManager assets = appDelegate.getActivity().getResources().getAssets();
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
        LAppDelegate appDelegate = LAppDelegate.getInstance(instanceId);
        if (appDelegate == null) {
            Log.e(TAG, "onUpdate: Cannot get app delegate for instance: " + instanceId);
            return;
        }
        
        int width = appDelegate.getWindowWidth();
        int height = appDelegate.getWindowHeight();

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
            LAppView view = appDelegate.getView();
            if (view != null) {
                view.preModelDraw(model);
            }

            model.update();

            model.draw(projection);     // 由于是引用传递，projection会发生变化

            // 单个模型绘制后调用
            if (view != null) {
                view.postModelDraw(model);
            }
        }
    }

    /**
     * 屏幕拖拽时的处理
     *
     * @param x 屏幕的x坐标
     * @param y 屏幕的y坐标
     */
    public void onDrag(float x, float y) {
        Log.d(TAG, "onDrag: x=" + x + ", y=" + y + " for instance: " + instanceId);
        
        for (int i = 0; i < models.size(); i++) {
            LAppModel model = getModel(i);
            if (model != null) {
                model.setDragging(x, y);
            }
        }
    }

    /**
     * 屏幕点击时的处理
     *
     * @param x 屏幕的x坐标
     * @param y 屏幕的y坐标
     */
    public void onTap(float x, float y) {
        Log.d(TAG, "onTap: x=" + x + ", y=" + y + " for instance: " + instanceId);
        
        if (DEBUG_LOG_ENABLE) {
            LAppPal.printLog("点击点: {" + x + ", y: " + y + " for instance: " + instanceId);
        }

        for (int i = 0; i < models.size(); i++) {
            LAppModel model = models.get(i);
            
            if (model == null) {
                continue;
            }

            // 点击头部时随机播放表情
            if (model.hitTest(HitAreaName.HEAD.getId(), x, y)) {
                if (DEBUG_LOG_ENABLE) {
                    LAppPal.printLog("点击区域: " + HitAreaName.HEAD.getId() + " for instance: " + instanceId);
                }
                model.setRandomExpression();
            }
            // 点击身体时开始随机动作
            else if (model.hitTest(HitAreaName.BODY.getId(), x, y)) {
                if (DEBUG_LOG_ENABLE) {
                    LAppPal.printLog("点击区域: " + HitAreaName.BODY.getId() + " for instance: " + instanceId);
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
        Log.d(TAG, "nextScene: Called for instance: " + instanceId);
        
        // 如果未找到模型则中止处理
        if (modelDir.isEmpty()) {
            Log.e(TAG, "nextScene: 未找到模型，中止场景切换 for instance: " + instanceId);
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
        Log.d(TAG, "changeScene: index=" + index + " for instance: " + instanceId);
        
        // 如果未找到模型则中止处理
        if (modelDir.isEmpty()) {
            Log.e(TAG, "changeScene: 未找到模型，中止场景切换 for instance: " + instanceId);
            return;
        }
        
        // 如果索引超出范围也中止处理
        if (index < 0 || index >= modelDir.size()) {
            Log.e(TAG, "changeScene: 索引超出范围. index=" + index + ", size=" + modelDir.size() + " for instance: " + instanceId);
            return;
        }
        
        currentModel = index;
        if (DEBUG_LOG_ENABLE) {
            LAppPal.printLog("模型索引: " + currentModel + " for instance: " + instanceId);
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
        LAppDelegate appDelegate = LAppDelegate.getInstance(instanceId);
        if (appDelegate != null) {
            LAppView view = appDelegate.getView();
            if (view != null) {
                view.switchRenderingTarget(useRenderingTarget);
                
                // 选择其他渲染目标时的背景清除颜色
                float[] clearColor = {0.0f, 0.0f, 0.0f};
                view.setRenderingTargetClearColor(clearColor[0], clearColor[1], clearColor[2]);
            }
        }
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
     * 实例ID
     */
    private final String instanceId;

    private LAppLive2DManager() {
        this.instanceId = "default";
        Log.d(TAG, "LAppLive2DManager: Created default manager");
        initialize();
    }
    
    /**
     * 带实例ID的构造函数
     *
     * @param instanceId 实例ID
     */
    private LAppLive2DManager(String instanceId) {
        this.instanceId = instanceId;
        Log.d(TAG, "LAppLive2DManager: Created manager for instance: " + instanceId);
        initialize();
    }
    
    /**
     * 初始化管理器
     */
    private void initialize() {
        Log.d(TAG, "LAppLive2DManager: 初始化管理器 for instance: " + instanceId);
        try {
            setUpModel();
            // 仅在找到模型时才切换场景
            if (!modelDir.isEmpty()) {
                Log.d(TAG, "LAppLive2DManager: 找到模型，切换到场景0 for instance: " + instanceId);
                try {
                    changeScene(0);
                } catch (Exception e) {
                    Log.e(TAG, "LAppLive2DManager: 切换场景时发生错误 for instance: " + instanceId, e);
                    // 即使切换场景失败，也不抛出异常，允许管理器继续初始化
                }
            } else {
                Log.e(TAG, "LAppLive2DManager: 初始化期间未找到模型 for instance: " + instanceId);
            }
        } catch (Exception e) {
            Log.e(TAG, "LAppLive2DManager: 初始化过程中发生错误 for instance: " + instanceId, e);
            // 不抛出异常，允许管理器继续初始化，只是可能没有模型
        }
    }
    
    /**
     * 获取实例ID
     *
     * @return 实例ID
     */
    public String getInstanceId() {
        return instanceId;
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