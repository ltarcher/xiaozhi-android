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
     * 递归打印AssetManager中的所有文件路径（用于调试）
     * @param assetManager AssetManager实例
     * @param path 当前路径
     * @param indent 缩进字符（用于显示层级结构）
     */
    private void printAssetsRecursively(AssetManager assetManager, String path, String indent) {
        if (!LAppDefine.DEBUG_LOG_ENABLE) {
            return;
        }
        
        try {
            String[] files = assetManager.list(path);
            if (files != null) {
                LAppPal.printLog(indent + "目录 [" + path + "] 包含 " + files.length + " 个项目:");
                for (String file : files) {
                    String fullPath = path.isEmpty() ? file : path + "/" + file;
                    // 检查是否为目录
                    try {
                        String[] subFiles = assetManager.list(fullPath);
                        if (subFiles != null && subFiles.length > 0) {
                            // 是目录
                            LAppPal.printLog(indent + "  [DIR] " + file);
                            // 递归打印子目录
                            printAssetsRecursively(assetManager, fullPath, indent + "    ");
                        } else {
                            // 是文件
                            LAppPal.printLog(indent + "  [FILE] " + file);
                        }
                    } catch (Exception e) {
                        // 可能是文件或者无法访问
                        LAppPal.printLog(indent + "  [FILE?] " + file + " (无法访问: " + e.getMessage() + ")");
                    }
                }
            } else {
                LAppPal.printLog(indent + "目录 [" + path + "] 无法列出内容");
            }
        } catch (Exception e) {
            LAppPal.printErrorLog("遍历Assets时出错，路径: " + path + ", 错误: " + e.getMessage());
        }
    }
    
    /**
     * 设置模型
     * 查找assets目录中的模型文件夹并设置模型目录列表
     */
    public void setupModels() {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppLive2DManager: setupModels called");
        }
        
        // 扫描模型目录
        scanModelDirs();
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppLive2DManager: setupModels completed");
        }
    }
    
    /**
     * 扫描模型目录
     * 查找assets目录中的模型文件夹并添加到modelDir列表
     */
    public void scanModelDirs() {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppLive2DManager: 开始扫描模型目录...");
        }
        
        modelDir.clear();
        
        try {
            AssetManager assetManager = LAppDelegate.getInstance().getAssetManager();
            
            // 调试：打印资源目录结构
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                LAppPal.printLog("=== 资源目录结构 ===");
                printAssetsRecursively(assetManager, "", "");
                LAppPal.printLog("==================");
            }
            
            // 使用FULL_LIVE2D_PATH来查找模型目录
            String live2dPath = ResourcePath.FULL_LIVE2D_PATH;
            String[] files = assetManager.list(live2dPath);
            
            if (files != null) {
                if (LAppDefine.DEBUG_LOG_ENABLE) {
                    LAppPal.printLog("LAppLive2DManager: " + live2dPath + " 目录包含 " + files.length + " 个项目");
                }
                
                // 遍历所有项目
                for (String file : files) {
                    try {
                        // 检查是否为目录且包含.model3.json文件
                        String subPath = live2dPath + file;
                        String[] subFiles = assetManager.list(subPath);
                        
                        if (subFiles != null && subFiles.length > 0) {
                            if (LAppDefine.DEBUG_LOG_ENABLE) {
                                LAppPal.printLog("LAppLive2DManager: 检查目录 " + subPath + " 包含 " + subFiles.length + " 个项目");
                                for (String subFile : subFiles) {
                                    LAppPal.printLog("LAppLive2DManager:   - " + subFile);
                                }
                            }
                            
                            // 检查是否存在同名的.model3.json文件
                            String modelSettingFile = file + ".model3.json";
                            if (Arrays.asList(subFiles).contains(modelSettingFile)) {
                                modelDir.add(file);
                                if (LAppDefine.DEBUG_LOG_ENABLE) {
                                    LAppPal.printLog("LAppLive2DManager: 发现模型目录: " + file);
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
                LAppPal.printErrorLog("无法列出目录: " + live2dPath);
                
                // 调试：尝试列出根目录内容
                if (LAppDefine.DEBUG_LOG_ENABLE) {
                    try {
                        String[] rootFiles = assetManager.list("");
                        if (rootFiles != null) {
                            LAppPal.printLog("根目录包含以下项目:");
                            for (String rootFile : rootFiles) {
                                LAppPal.printLog("  - " + rootFile);
                            }
                        }
                    } catch (IOException e) {
                        LAppPal.printErrorLog("无法列出根目录: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LAppPal.printErrorLog("扫描模型目录失败: " + e.getMessage());
            e.printStackTrace();
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
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppLive2DManager: onDraw called, model count: " + models.size());
        }
        
        for (int i = 0; i < models.size(); i++) {
            LAppModel model = models.get(i);
            
            if (model.getModel() == null) {
                if (LAppDefine.DEBUG_LOG_ENABLE) {
                    LAppPal.printLog("LAppLive2DManager: Model " + i + " is null, skipping");
                }
                continue;
            }
            
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                LAppPal.printLog("LAppLive2DManager: Drawing model " + i);
            }
            
            // 绘制模型并传入投影矩阵
            model.draw(projection);
        }
    }
    
    /**
     * 切换场景（加载模型）
     * @param index 场景索引
     */
    public void changeScene(int index) {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppLive2DManager: changeScene called with index " + index);
        }
        
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
        String modelName = modelDir.get(index);
        // 构造正确的路径：使用FULL_LIVE2D_PATH而不是LIVE2D_ROOT
        String modelDirectory = ResourcePath.FULL_LIVE2D_PATH + modelName + "/";
        
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppLive2DManager: Loading model from directory: " + modelDirectory);
            LAppPal.printLog("LAppLive2DManager: Model setting file: " + modelName + ".model3.json");
        }
        
        model.loadAssets(
            modelDirectory,
            modelName + ".model3.json"
        );
        
        // 检查模型是否成功加载
        // LAppModel的isInitialized()方法可能在loadAssets()后被重置
        // 改为检查模型对象是否存在来判断加载是否成功
        if (model.getModel() != null) {
            // 添加到模型列表
            models.add(model);
            
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                LAppPal.printLog("LAppLive2DManager: Changed scene to " + index + " (" + modelDirectory + ")");
            }
        } else {
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                LAppPal.printLog("LAppLive2DManager: Failed to initialize model for scene " + index);
            }
        }
    }
    
    /**
     * 通过模型名称切换场景
     * @param modelName 模型名称
     */
    public void changeScene(String modelName) {
        if (LAppDefine.DEBUG_LOG_ENABLE) {
            LAppPal.printLog("LAppLive2DManager: changeScene called with model name " + modelName);
        }
        
        // 查找模型名称对应的索引
        int index = modelDir.indexOf(modelName);
        if (index == -1) {
            LAppPal.printErrorLog("Model not found: " + modelName);
            // 如果找不到指定模型，使用第一个模型
            if (!modelDir.isEmpty()) {
                index = 0;
                LAppPal.printLog("Using first available model: " + modelDir.get(index));
            } else {
                LAppPal.printErrorLog("No models available");
                return;
            }
        }
        
        changeScene(index);
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