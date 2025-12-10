package com.live2d;

/**
 * 资源路径常量定义类
 */
public class ResourcePath {
    // Flutter资源根路径
    public static final String FLUTTER_ASSETS_ROOT = "flutter_assets/assets/";
    
    // Live2D资源根路径（相对于assets目录）
    public static final String LIVE2D_ROOT = "live2d/";
    
    // 完整的Live2D资源路径（用于AssetManager访问）
    public static final String FULL_LIVE2D_PATH = FLUTTER_ASSETS_ROOT + LIVE2D_ROOT;
    
    // Android原生资源根路径
    public static final String NATIVE_ASSETS_ROOT = "";
    
    // Android原生Live2D资源路径
    public static final String NATIVE_LIVE2D_PATH = "live2d/";
    
    // 背景图片路径
    public static final String BACK_IMAGE = "back_class_normal.png";
    
    // 齿轮图标路径
    public static final String GEAR_IMAGE = "icon_gear.png";
    
    // 关闭按钮图标路径
    public static final String CLOSE_IMAGE = "close.png";
    
    private final String path;
    
    /**
     * 获取根路径（Flutter）
     * @return 根路径
     */
    public static ResourcePath ROOT = new ResourcePath(LIVE2D_ROOT);
    
    /**
     * 获取完整路径（Flutter）
     * @return 完整路径
     */
    public static ResourcePath FULL_PATH = new ResourcePath(FULL_LIVE2D_PATH);
    
    /**
     * 获取原生资源根路径
     * @return 原生资源根路径
     */
    public static ResourcePath NATIVE_ROOT = new ResourcePath(NATIVE_LIVE2D_PATH);
    
    /**
     * 构造函数
     * @param path 路径字符串
     */
    private ResourcePath(String path) {
        this.path = path;
    }
    
    /**
     * 获取路径字符串
     * @return 路径字符串
     */
    public String getPath() {
        return path;
    }
    
    /**
     * 拼接子路径
     * @param subPath 子路径
     * @return 新的ResourcePath对象
     */
    public ResourcePath append(String subPath) {
        // 确保路径分隔符正确
        String newPath = path;
        if (!newPath.endsWith("/")) {
            newPath += "/";
        }
        if (subPath.startsWith("/")) {
            subPath = subPath.substring(1);
        }
        return new ResourcePath(newPath + subPath);
    }
}