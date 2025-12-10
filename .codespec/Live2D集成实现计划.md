# Live2D集成实现计划

## 1. 实现思路
我们将参考Live2D SDK Sample的完整示例(full目录)，在Flutter Android项目中实现类似的Live2D基础架构。由于Flutter本身不直接支持Live2D，我们需要通过Android原生代码实现Live2D渲染，并通过Platform Channel与Flutter进行交互。

## 2. 类结构设计
我们需要创建以下核心类来实现Live2D功能：

1. **LAppDelegate** - 应用程序委托类，负责整体初始化和管理
2. **LAppLive2DManager** - 模型管理器，负责模型加载和管理
3. **LAppModel** - 模型类，封装单个Live2D模型的功能
4. **LAppView** - 视图类，负责渲染和用户交互
5. **GLRenderer** - OpenGL渲染器，实现GLSurfaceView.Renderer接口
6. **LAppTextureManager** - 纹理管理器，负责纹理加载和管理
7. **相关辅助类** - 如TouchManager等

## 3. 文件组织结构
```
android/app/src/main/java/com/live2d/
  ├── LAppDelegate.java
  ├── LAppLive2DManager.java
  ├── LAppModel.java
  ├── LAppView.java
  ├── GLRenderer.java
  ├── LAppTextureManager.java
  ├── TouchManager.java
  ├── LAppDefine.java
  ├── LAppPal.java
  └── utils/
      └── ... (其他工具类)
```

## 4. 实现步骤
1. 创建Live2D基础类框架
2. 实现模型加载和渲染功能
3. 实现与Flutter的交互接口
4. 实现口型同步功能
5. 测试和优化

## 5. 与Flutter的交互设计
通过MethodChannel实现Flutter与Android原生Live2D代码的交互：
- Flutter端发送指令控制Live2D模型（如播放动作、切换表情等）
- Android端通知Flutter端模型状态变化

## 6. 口型同步实现方案
口型同步可以通过以下方式实现：
1. 在Flutter端识别语音或文本的音素特征
2. 通过MethodChannel将口型参数传递给Android端
3. Android端根据参数调整模型口型相关参数

## 7. 需要注意的问题
1. 确保Live2D SDK正确集成到项目中
2. 正确配置OpenGL ES环境
3. 处理好Flutter与Android原生代码的生命周期管理
4. 实现高效的内存管理和资源释放机制

## 8. SDK路径信息

### 8.1 Live2D SDK文件结构
```
xiaozhi-android/
└── Live2DCubismSdkForJava-5-r.4.1/
    ├── Core/
    │   ├── Live2DCubismCore.aar     # 核心库文件
    │   └── ...                      # 其他核心文件
    ├── Framework/
    │   └── framework/
    │       └── src/
    │           └── main/
    │               └── java/
    │                   └── com/
    │                       └── live2d/
    │                           └── sdk/
    │                               └── cubism/
    │                                   └── framework/
    │                                       ├── CMakeLists.txt
    │                                       ├── CubismFramework.java
    │                                       ├── CubismModel.java
    │                                       └── ...              # 其他框架文件
    └── Sample/
        └── src/
            ├── full/                # 完整示例代码
            └── main/
                └── java/
                    └── com/
                        └── live2d/
                            └── sample/
                                └── ...                      # 示例代码
```

### 8.2 SDK集成路径
1. 将Live2D核心库`Live2DCubismCore.aar`复制到`android/app/libs/`目录下
2. 在`android/app/build.gradle`中添加依赖：
   ```gradle
   dependencies {
       implementation files('../libs/Live2DCubismCore.aar')
   }
   ```
3. Live2D框架源码将被直接复制到项目中：
   ```
   android/app/src/main/java/com/live2d/sdk/cubism/framework/
   ```

## 9. Flutter资源路径处理注意事项

### 9.1 资源目录结构
```
xiaozhi-android/
├── assets/
│   └── live2d/
│       ├── Haru/
│       ├── Hiyori/
│       └── ...                  # 其他模型目录
└── android/
    └── app/
        └── src/
            └── main/
                └── assets/      # Android原生资源目录(可选)
```

### 9.2 Flutter资源声明
在`pubspec.yaml`中声明Live2D资源：
```yaml
flutter:
  assets:
    - assets/live2d/
```

### 9.3 资源访问路径说明
在Android原生代码中访问Flutter打包的资源时需要注意以下几点：

1. **Flutter打包后的实际路径**：
   Flutter打包后，资源的实际路径为`/flutter_assets/assets/live2d/`，这是Android AssetManager可以访问的路径。

2. **资源访问方式**：
   ```java
   // 错误的方式 - 直接使用Flutter项目中的路径
   // String wrongPath = "assets/live2d/";
   
   // 正确的方式 - 使用构建后的路径
   String correctPath = "/flutter_assets/assets/live2d/";
   ```

3. **模型加载路径**：
   当使用Live2D SDK的loadModel方法时，应该使用相对路径：
   ```java
   // 加载Haru模型时使用相对路径
   String modelPath = "live2d/Haru/";
   ```

### 9.4 资源访问最佳实践

1. **路径常量定义**：
   ```java
   public class ResourcePath {
       // Flutter资源根路径
       public static final String FLUTTER_ASSETS_ROOT = "/flutter_assets/assets/";
       
       // Live2D资源根路径
       public static final String LIVE2D_ROOT = "live2d/";
       
       // 完整路径
       public static final String FULL_LIVE2D_PATH = FLUTTER_ASSETS_ROOT + LIVE2D_ROOT;
   }
   ```

2. **资源访问示例**：
   ```java
   // 枚举模型目录时使用完整路径
   String[] modelDirs = assetManager.list(ResourcePath.FULL_LIVE2D_PATH);
   
   // 加载具体模型时使用相对路径
   LAppModel.loadAssets("live2d/Haru/", "Haru.model3.json");
   ```

3. **注意事项**：
   - 不要混用相对路径和绝对路径访问同一资源
   - 保持路径格式与SDK API期望的一致性
   - 在不同场景下使用正确的路径格式（AssetManager访问 vs SDK内部方法调用）