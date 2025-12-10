package com.live2d;

import android.content.res.AssetManager;

import com.live2d.sdk.cubism.framework.math.CubismMatrix44;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Live2D模型管理器类
 * 负责模型的加载、管理和切换
 */
public class LAppLive2DManager {
    // 单例实例
    private static LAppLive2DManager s_instance;
    
    /**
     * 获取单例实例
     * @return LAppLive2DManager实例
     */
    public static LAppLive2DManager getInstance() {
        if (s_instance == null) {
            s_instance = new LAppLive2DManager();
        }
        return s_instance;
    }
    
    /**
     * 释放单例实例
     */
    public static void releaseInstance() {
        if (s_instance != null) {
            s_instance = null;
        }
    }
    
    // 模型列表
    private final List<LAppModel> models = new ArrayList<>();
    
    // 模型目录列表
    private final List<String> modelDir = new ArrayList<>();
    
    // 投影矩阵
    private final CubismMatrix44 projection = CubismMatrix44.create();
    
    // 当前场景索引
    private int currentSceneIndex = 0;
    
    /**
     * 私有构造函数
     */
    private LAppLive2DManager() {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppLive2DManager: Created");
        }
    }
    
    /**
     * 释放所有模型
     */
    public void releaseAllModels() {
        for (LAppModel model : models) {
            model.deleteModel();
        }
        models.clear();
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppLive2DManager: Released all models");
        }
    }
    
    /**
     * 设置模型
     * 查找assets目录中的模型文件夹并设置模型目录列表
     */
    public void setupModels() {
        modelDir.clear();
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppLive2DManager: 开始扫描模型目录");
        }
        
        // 获取AssetManager
        AssetManager assetManager = LAppDelegate.getInstance().getAssetManager();
        if (assetManager == null) {
            LAppPal.printErrorLog("AssetManager不可用");
            return;
        }
        
        try {
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                LAppPal.printLog("LAppLive2DManager: 尝试列出目录 " + ResourcePath.LIVE2D_ROOT);
            }
            
            // 列出live2d目录下的所有文件和文件夹
            String[] files = assetManager.list(ResourcePath.LIVE2D_ROOT);
            if (files != null) {
                if (LAppDefine.DEBUG_LOG_ENABLE) {
                    LAppPal.printLog("LAppLive2DManager: 发现 " + files.length + " 个项目在 " + ResourcePath.LIVE2D_ROOT);
                }
                
                // 遍历所有文件夹，查找模型目录
                for (String file : files) {
                    if (LAppDefine.DEBUG_LOG_ENABLE) {
                        LAppPal.printLog("LAppLive2DManager: 检查项目 " + file);
                    }
                    
                    try {
                        // 尝试列出子目录的内容，判断是否为文件夹
                        String fullPath = ResourcePath.LIVE2D_ROOT + file;
                        String[] subFiles = assetManager.list(fullPath);
                        if (subFiles != null && subFiles.length > 0) {
                            if (LAppDefine.DEBUG_LOG_ENABLE) {
                                LAppPal.printLog("LAppLive2DManager: " + file + " 是目录，包含 " + subFiles.length + " 个项目");
                            }
                            
                            // 检查是否存在.model3.json文件
                            String modelSettingFile = file + ".model3.json";
                            boolean hasModelSetting = Arrays.asList(subFiles).contains(modelSettingFile);
                            
                            if (hasModelSetting) {
                                // 添加完整的模型路径
                                modelDir.add(ResourcePath.LIVE2D_ROOT + file + "/");
                                if (LAppDefine.DEBUG_LOG_ENABLE) {
                                    LAppPal.printLog("LAppLive2DManager: 找到模型目录: " + file);
                                }
                            } else {
                                if (LAppDefine.DEBUG_LOG_ENABLE) {
                                    LAppPal.printLog("LAppLive2DManager: " + file + " 目录不包含 " + modelSettingFile);
                                }
                            }
                        } else {
                            if (LAppDefine.DEBUG_LOG_ENABLE) {
                                LAppPal.printLog("LAppLive2DManager: " + file + " 不是有效目录或为空");
                            }
                        }
                    } catch (Exception e) {
                        // 忽略无法访问的文件
                        LAppPal.printErrorLog("访问目录出错: " + file + ", 错误: " + e.getMessage());
                    }
                }
            } else {
                LAppPal.printErrorLog("无法列出目录: " + ResourcePath.LIVE2D_ROOT);
            }
        } catch (IOException e) {
            LAppPal.printErrorLog("扫描模型目录失败: " + e.getMessage());
        }
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppLive2DManager: 扫描完成，共找到 " + modelDir.size() + " 个模型");
            for (int i = 0; i < modelDir.size(); i++) {
                LAppPal.printLog("LAppLive2DManager: 模型 " + i + ": " + modelDir.get(i));
            }
        }
    }
    
    /**
     * 更新处理
     */
    public void onUpdate() {
        final int width = LAppDelegate.getInstance().getWindowWidth();
        final int height = LAppDelegate.getInstance().getWindowHeight();
        
        for (int i = 0; i < models.size(); i++) {
            LAppModel model = models.get(i);
            
            if (model.getModel() == null) {
                LAppPal.printLog("LAppLive2DManager: Failed to get model.");
                continue;
            }
            
            // 初始化投影矩阵
            projection.loadIdentity();
            
            // 根据窗口大小和模型画布大小设置缩放
            if (model.getModel().getCanvasWidth() > 1.0f && width < height) {
                // 横向长模型在纵向窗口中显示时，根据模型的横向尺寸计算缩放
                model.getModelMatrix().setWidth(2.0f);
                projection.scale(1.0f, (float) width / (float) height);
            } else {
                projection.scale((float) height / (float) width, 1.0f);
            }
            
            // 更新模型
            model.update();
        }
    }
    
    /**
     * 渲染处理
     */
    public void onDraw() {
        for (int i = 0; i < models.size(); i++) {
            LAppModel model = models.get(i);
            
            if (model.getModel() == null) {
                continue;
            }
            
            // 绘制模型并传入投影矩阵
            model.draw(projection);
        }
    }
    
    /**
     * 切换场景
     * @param index 场景索引
     */
    public void changeScene(int index) {
        if (index < 0 || index >= modelDir.size()) {
            LAppPal.printErrorLog("Invalid scene index: " + index);
            return;
        }
        
        currentSceneIndex = index;
        
        // 释放当前模型
        releaseAllModels();
        
        // 创建新模型
        LAppModel model = new LAppModel();
        
        // 加载模型资源
        String modelDirectory = modelDir.get(index);
        // 从完整路径中提取模型目录名
        String modelName = modelDirectory.substring(ResourcePath.LIVE2D_ROOT.length(), modelDirectory.length() - 1);
        model.loadAssets(
            modelDirectory,
            modelName + ".model3.json"
        );
        
        // 添加到模型列表
        models.add(model);
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppLive2DManager: Changed scene to " + index + " (" + modelDirectory + ")");
        }
    }
    
    /**
     * 获取当前场景索引
     * @return 当前场景索引
     */
    public int getCurrentSceneIndex() {
        return currentSceneIndex;
    }
    
    /**
     * 获取模型数量
     * @return 模型数量
     */
    public int getModelCount() {
        return models.size();
    }
    
    /**
     * 获取指定索引的模型
     * @param index 模型索引
     * @return 模型对象
     */
    public LAppModel getModel(int index) {
        if (index < 0 || index >= models.size()) {
            return null;
        }
        return models.get(index);
    }
    
    /**
     * 获取当前模型
     * @return 当前模型对象
     */
    public LAppModel getCurrentModel() {
        if (models.isEmpty()) {
            return null;
        }
        return models.get(models.size() - 1);
    }
    
    /**
     * 处理拖拽事件
     * @param x 拖拽点x坐标
     * @param y 拖拽点y坐标
     */
    public void onDrag(float x, float y) {
        // TODO: 实现拖拽处理逻辑
        for (LAppModel model : models) {
            model.setDragging(x, y);
        }
    }
    
    /**
     * 处理点击事件
     * @param x 点击点x坐标
     * @param y 点击点y坐标
     */
    public void onTap(float x, float y) {
        for (LAppModel model : models) {
            if (model.hitTest("Head", x, y)) {
                // 点击头部
                // TODO: 实现表情切换逻辑
            } else if (model.hitTest("Body", x, y)) {
                // 点击身体
                // TODO: 实现动作播放逻辑
            }
        }
    }
    
    /**
     * 设置唇形同步值
     * @param value 唇形同步值
     */
    public void setLipSyncValue(float value) {
        for (LAppModel model : models) {
            model.setLipSyncValue(value);
        }
    }
}