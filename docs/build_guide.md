# Xiaozhi Flutter 客户端构建指南

## 项目概述

Xiaozhi/小智是一个基于Flutter开发的WebSocket协议客户端，支持Android和iOS平台。该应用允许用户通过语音与AI助手进行交互，并支持连续通话、历史消息等功能。

## 系统要求

### 开发环境要求
- Flutter 3.32.8
- Dart 3.8.1
- Android Studio / VS Code
- Xcode (仅限iOS开发)
- Android SDK (API 30+)

### 运行环境要求
- Android 11+ (API 30)
- iOS 13+

## 项目结构

```
xiaozhi-android/
├── android/                 # Android原生代码
├── ios/                     # iOS原生代码
├── lib/                     # Flutter核心代码
│   ├── bloc/               # 状态管理(BLoC模式)
│   ├── common/             # 公共常量
│   ├── model/              # 数据模型
│   ├── page/               # 页面组件
│   ├── util/               # 工具类
│   ├── widget/             # 自定义组件
│   └── main.dart           # 应用入口
├── assets/                  # 静态资源
├── docs/                    # 文档
└── test/                    # 测试代码
```

## 核心功能模块

1. **聊天界面 (Chat Page)**
   - 支持按住说话功能
   - 历史消息展示
   - 下拉加载更多

2. **通话界面 (Call Page)**
   - 连续通话模式

3. **设置界面 (Setting Page)**
   - OTA地址配置
   - WebSocket地址配置
   - MAC地址配置

4. **状态管理**
   - 使用BLoC模式管理应用状态
   - 聊天状态管理(chat_bloc)
   - OTA状态管理(ota_bloc)

## 依赖库说明

主要第三方库包括：
- `flutter_bloc`: 状态管理
- `web_socket_channel`: WebSocket通信
- `record`: 录音功能
- `permission_handler`: 权限管理
- `opus_flutter`, `opus_dart`: Opus音频编码解码
- `shared_preferences`: 本地数据存储
- `sqflite`: SQLite数据库
- `pull_to_refresh_flutter3`: 下拉刷新
- `dio`: HTTP网络请求
- `url_launcher`: URL跳转

## 构建步骤

### 1. 环境准备

确保已安装Flutter SDK并配置好环境变量：

```bash
flutter doctor
```

检查是否有缺失的依赖项并按照提示完成安装。

### 2. 获取依赖包

```bash
flutter pub get
```

### 3. 生成本地化文件

项目支持中英文本地化，需要生成相关文件：

```bash
flutter gen-l10n
```

### 4. Android构建

#### 调试版本
```bash
flutter build apk --debug
```

#### 发布版本
```bash
flutter build apk --release
```

生成的APK文件位于 `build/app/outputs/flutter-apk/app-release.apk`

### 5. iOS构建

#### 调试版本
```bash
flutter build ios --debug
```

#### 发布版本
```bash
flutter build ios --release
```

注意：iOS构建需要在Mac环境下使用Xcode完成。

## 配置说明

### 应用配置 (pubspec.yaml)
- 应用名称、描述、版本号
- 依赖库管理
- Flutter相关配置（图标、本地化等）

### Android配置
- 包名: `com.thinkerror.xiaozhi`
- 最小SDK版本: API 30 (Android 11)
- 目标SDK版本: 最新稳定版

### iOS配置
- 最低支持版本: iOS 13
- Bundle Identifier: 根据开发者账号配置

## 部署流程

1. **首次部署**
   - 下载最新APK安装
   - 打开APP复制激活码
   - 在管理后台注册账号并激活设备
   - 配置智能体
   - 重启APP开始使用

2. **已有账号部署**
   - 点击APP左上角设置按钮
   - 填入已注册设备的MAC地址
   - 点击右上角保存按钮

## 服务器配置

默认使用xinnan-tech服务器：

- 管理后台地址：https://2662r3426b.vicp.fun
- OTA地址：https://2662r3426b.vicp.fun/xiaozhi/ota/
- WebSocket地址：wss://2662r3426b.vicp.fun/xiaozhi/v1/

备用服务器(xiaozhi.me)：
- 管理后台地址：https://xiaozhi.me
- OTA地址：https://api.tenclass.net/xiaozhi/ota/
- WebSocket地址：wss://api.tenclass.net/xiaozhi/v1/

## 常见问题

1. **权限问题**
   - 确保授予麦克风权限
   - Android需要在设置中手动开启权限

2. **连接问题**
   - 检查网络连接
   - 验证服务器地址配置是否正确
   - 确认设备已在后台激活

3. **编译问题**
   - 清理项目缓存：`flutter clean`
   - 重新获取依赖：`flutter pub get`
   - 重新生成本地化文件：`flutter gen-l10n`

## 参考项目

- [78/xiaozhi-esp32](https://github.com/78/xiaozhi-esp32)
- [xinnan-tech/xiaozhi-esp32-server](https://github.com/xinnan-tech/xiaozhi-esp32-server)
- [TOM88812/xiaozhi-android-client](https://github.com/TOM88812/xiaozhi-android-client)