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
- [x] 创建Live2D管理器类（类似Sample中的LAppLive2DManager）
- [x] 创建视图类（类似Sample中的LAppView）
- [x] 创建渲染器类（类似Sample中的GLRenderer）
- [x] 创建纹理管理类（类似Sample中的LAppTextureManager）
- [x] 创建模型类（类似Sample中的LAppModel）
- [x] 创建委托类（类似Sample中的LAppDelegate）

## 4. 集成步骤
- [x] 修改MainActivity.java，继承FlutterActivity并添加Live2D相关初始化代码
- [x] 添加OpenGL ES支持
- [x] 添加必要的权限声明到AndroidManifest.xml（如果需要）
- [x] 配置ProGuard规则以防SDK被混淆

## 5. 与Flutter集成
- [x] 实现Android与Flutter的MethodChannel通信机制
- [x] 在Flutter端创建Live2D控制器接口
- [x] 实现从Flutter控制Live2D模型（播放动作、切换表情等）

## 6. 实现待机页面
- [x] 创建一个Android原生的欢迎页面，在其中显示Live2D的Haru模型
- [x] 实现页面跳转逻辑，从Live2D待机页面跳转到Flutter主界面
- [x] 处理触摸事件，让Haru模型可以响应用户交互

## 7. 口型同步功能
- [ ] 实现音频输入检测和分析
- [ ] 将音频特征参数传递给Live2D模型
- [ ] 调整模型口型参数实现同步效果

## 8. 资源优化
- [ ] 移除不需要的模型文件减小APK大小
- [ ] 优化纹理资源压缩