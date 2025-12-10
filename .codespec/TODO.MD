# Live2D集成任务计划清单

## 1. 准备工作
- [x] 在Flutter Android项目中创建libs/live2d目录
- [x] 将Live2DCubismSdkForJava-5-r.4.1/Core/android/Live2DCubismCore.aar文件拷贝到libs/live2d目录
- [x] 将所有Live2D模型资源文件（包括Haru、Hiyori等）复制到Flutter项目的assets/live2d目录下

## 2. 学习研究
- [x] 分析live2d的sdk中示例Sample的实现原理、架构、实现流程图、使用live2d sdk的方法，详细学习分析后保存到.codespec/live2d_sample学习.md中

## 3. 修改Android项目结构
- [x] 修改android/app/build.gradle.kts文件，添加对Live2D AAR库的引用
- [x] 在Flutter Android项目中创建Live2D相关的Java类（参考Sample中的实现）
  - [x] 创建LAppDefine类（常量定义）
  - [x] 创建LAppPal类（平台相关功能处理）
  - [x] 创建TouchManager类（触摸管理）
  - [x] 创建ResourcePath类（资源路径管理）
  - [x] 创建LAppTextureManager类（纹理管理）
  - [x] 创建GLRenderer类（OpenGL渲染器）
  - [x] 创建LAppModel类（模型类）
  - [x] 创建LAppView类（视图类）
  - [x] 创建LAppLive2DManager类（模型管理器）
  - [x] 创建LAppDelegate类（应用程序委托类）

## 4. 与Flutter集成
- [ ] 实现Android与Flutter的MethodChannel通信机制
- [ ] 在Flutter端创建Live2D控制器接口
- [ ] 实现从Flutter控制Live2D模型（播放动作、切换表情等）

## 5. 实现待机页面（基础验证）
- [x] 创建一个Android原生的欢迎页面，在其中显示Live2D的Haru模型
- [x] 实现页面跳转逻辑，从Live2D待机页面跳转到Flutter主界面
- [x] 处理触摸事件，让Haru模型可以响应用户交互
- [x] 验证模型加载和显示功能正常

## 6. 集成步骤
- [ ] 修改MainActivity.java，继承FlutterActivity并添加Live2D相关初始化代码
- [ ] 添加OpenGL ES支持
- [ ] 添加必要的权限声明到AndroidManifest.xml（如果需要）
- [ ] 配置ProGuard规则以防SDK被混淆

## 7. 口型同步功能
- [ ] 实现音频输入检测和分析
- [ ] 将音频特征参数传递给Live2D模型
- [ ] 调整模型口型参数实现同步效果

## 8. 资源优化
- [ ] 移除不需要的模型文件减小APK大小
- [ ] 优化纹理资源压缩