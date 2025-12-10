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

## 4. 模块架构关系图

```
graph TD
    A[Flutter UI层] -->|MethodChannel| B[LAppDelegate]
    B --> C[LAppLive2DManager]
    C --> D[LAppModel]
    B --> E[LAppView]
    E --> F[GLRenderer]
    D --> G[Cubism SDK Framework]
    F --> G
    D --> H[LAppTextureManager]
    E --> I[TouchManager]
    
    subgraph "Flutter Layer"
        A
    end
    
    subgraph "Android Native Layer"
        B
        C
        D
        E
        F
        H
        I
    end
    
    subgraph "Live2D SDK"
        G
    end
```

## 5. 各模块职责说明

### 5.1 LAppDelegate (应用程序委托类)
- 整个Live2D系统的入口点和协调者
- 管理应用程序生命周期事件
- 初始化和销毁核心组件
- 协调各模块之间的交互

### 5.2 LAppLive2DManager (模型管理器)
- 管理多个Live2D模型实例
- 负责模型的加载、切换和释放
- 维护模型列表和当前活动模型
- 处理场景切换逻辑

### 5.3 LAppModel (模型类)
- 封装单个Live2D模型的所有功能
- 处理模型文件的加载和解析
- 管理模型的动作、表情和物理效果
- 更新模型状态和参数

### 5.4 LAppView (视图类)
- 管理视图矩阵和坐标变换
- 处理用户触摸事件
- 控制模型渲染流程
- 管理屏幕显示逻辑

### 5.5 GLRenderer (OpenGL渲染器)
- 实现GLSurfaceView.Renderer接口
- 处理OpenGL上下文的创建和销毁
- 执行实际的渲染命令
- 管理帧缓冲和渲染目标

### 5.6 LAppTextureManager (纹理管理器)
- 加载和管理模型纹理
- 缓存纹理资源以提高性能
- 处理纹理的创建和释放

## 6. Live2D初始化流程图

```
flowchart TD
    A[Application Start] --> B[MainActivity.onCreate]
    B --> C[创建LAppDelegate实例]
    C --> D[LAppDelegate.onStart]
    D --> E[创建TextureManager]
    E --> F[创建LAppView]
    F --> G[初始化时间系统]
    G --> H[Activity.onResume]
    H --> I[GLSurfaceView创建]
    I --> J[onSurfaceCreated回调]
    J --> K[设置OpenGL参数]
    K --> L[初始化CubismFramework]
    L --> M[onSurfaceChanged回调]
    M --> N[设置视口大小]
    N --> O[LAppView初始化]
    O --> P[加载模型]
    P --> Q[开始渲染循环]
    Q --> R[onDrawFrame持续调用]
```

### 6.1 详细初始化步骤说明

1. **应用程序启动** - Flutter应用启动，进入MainActivity
2. **创建LAppDelegate实例** - 初始化整个Live2D系统的核心代理
3. **LAppDelegate.onStart** - 在Activity启动时进行初始化
4. **创建TextureManager** - 准备纹理管理系统
5. **创建LAppView** - 初始化视图系统
6. **初始化时间系统** - 设置时间相关参数用于动画计算
7. **Activity.onResume** - Activity进入活跃状态
8. **GLSurfaceView创建** - 创建OpenGL渲染表面
9. **onSurfaceCreated回调** - OpenGL上下文创建完成
10. **设置OpenGL参数** - 配置纹理过滤、混合模式等
11. **初始化CubismFramework** - 初始化Live2D核心框架
12. **onSurfaceChanged回调** - 表面尺寸改变，进行相应设置
13. **设置视口大小** - 配置OpenGL视口
14. **LAppView初始化** - 初始化视图相关参数
15. **加载模型** - 从assets目录加载Live2D模型
16. **开始渲染循环** - 进入持续渲染状态

## 7. 模型加载流程图

```
flowchart TD
    A[LAppLive2DManager.changeScene] --> B[释放当前模型]
    B --> C[创建新的LAppModel实例]
    C --> D[LAppModel.loadAssets]
    D --> E[读取.model3.json文件]
    E --> F[解析模型设置]
    F --> G[加载.moc3模型文件]
    G --> H[创建CubismModel]
    H --> I[设置渲染器]
    I --> J[加载纹理]
    J --> K[加载动作文件]
    K --> L[加载表情文件]
    L --> M[设置物理效果]
    M --> N[设置呼吸效果]
    N --> O[设置眨眼效果]
    O --> P[模型准备完成]
```

### 7.1 模型加载详细步骤说明

1. **changeScene调用** - 切换到新的模型场景
2. **释放当前模型** - 清理当前正在使用的模型资源
3. **创建新的LAppModel实例** - 创建新的模型对象
4. **loadAssets调用** - 开始加载模型资源
5. **读取.model3.json文件** - 读取模型配置文件
6. **解析模型设置** - 解析模型配置信息
7. **加载.moc3模型文件** - 加载实际的模型数据文件
8. **创建CubismModel** - 使用模型数据创建Live2D模型实例
9. **设置渲染器** - 为模型配置渲染器
10. **加载纹理** - 加载模型所需的纹理图片
11. **加载动作文件** - 加载模型的动作文件(.motion3.json)
12. **加载表情文件** - 加载模型的表情文件(.exp3.json)
13. **设置物理效果** - 配置模型的物理模拟效果
14. **设置呼吸效果** - 配置模型的呼吸动画效果
15. **设置眨眼效果** - 配置模型的自动眨眼效果
16. **模型准备完成** - 模型加载完毕，可以开始渲染和交互

## 8. 实现步骤
1. 创建Live2D基础类框架
2. 实现模型加载和渲染功能
3. 实现与Flutter的交互接口
4. 实现口型同步功能
5. 测试和优化

## 9. 与Flutter的交互设计
通过MethodChannel实现Flutter与Android原生Live2D代码的交互：
- Flutter端发送指令控制Live2D模型（如播放动作、切换表情等）
- Android端通知Flutter端模型状态变化

## 10. 口型同步实现方案
口型同步可以通过以下方式实现：
1. 在Flutter端识别语音或文本的音素特征
2. 通过MethodChannel将口型参数传递给Android端
3. Android端根据参数调整模型口型相关参数

## 11. 需要注意的问题
1. 确保Live2D SDK正确集成到项目中
2. 正确配置OpenGL ES环境
3. 处理好Flutter与Android原生代码的生命周期管理
4. 实现高效的内存管理和资源释放机制

## 12. SDK路径信息

### 12.1 Live2D SDK文件结构
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

### 12.2 SDK集成路径
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

## 13. Flutter资源路径处理注意事项

### 13.1 资源目录结构
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

### 13.2 Flutter资源声明
在`pubspec.yaml`中声明Live2D资源：
```yaml
flutter:
  assets:
    - assets/live2d/
```

### 13.3 资源访问路径说明
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

### 13.4 资源访问最佳实践

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

## 14. 调试命令

在开发和调试过程中，可以使用以下命令来查看日志：

1. **查看Live2D相关日志**：
   ```bash
   adb logcat -s "Live2dSplashActivity:*" "LAppDelegate:*" "LAppLive2DManager:*" "LAppModel:*" "LAppPal:*" "GLRenderer:*"
   ```

2. **查看所有错误和严重日志**：
   ```bash
   adb logcat "*:E" "*:F"
   ```

3. **查看特定标签的日志并保存到文件**：
   ```bash
   adb logcat -s "LApp*" > live2d_debug.log
   ```

4. **清空日志缓冲区**：
   ```bash
   adb logcat -c
   ```

5. **实时查看日志**：
   ```bash
   adb logcat
   ```

通过这些调试命令，可以有效地跟踪Live2D集成过程中的各种问题，包括模型加载、纹理处理、渲染过程等关键环节。
