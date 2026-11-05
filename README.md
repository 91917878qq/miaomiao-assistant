# 喵喵助手（MiaoAssistant）

喵喵助手是一款通过无障碍服务在聊天应用中自动改写输入文本的 Android 辅助工具：支持 QQ / 微信 / 抖音 / 快手 / 全局模式 / 自定义应用，把输入文字一键「喵化」——替换人称、追加句尾「喵」、插入颜文字与猫爪符号。

- **作者**：91917878qq
- **反馈邮箱**：1490964435@qq.com
- **当前版本**：3.0.1（versionCode 52）
- **包名**：`com.miaomiao.assistant`

## 功能

- **无障碍文本改写**：监听微信/QQ 输入框 → 规则替换（括号保护、随机替换）→ 标点触发追加 → 颜文字轮换 → QQ 猫爪包裹 → `ACTION_SET_TEXT` 写回
- **悬浮窗**：可拖拽气泡 + 手动处理面板（输入→处理→复制），贴边吸附，大小/透明度实时生效
- **规则系统**：替换规则 JSON 存取、预设保存/导入导出、MR1 分享码
- **功能体检**：一键检测无障碍服务 / 悬浮窗权限 / 已选应用 / 版本信息
- **液态玻璃 UI**：渐变背景折射、玻璃导航栏、玻璃卡片（lens 效果）

## 技术栈

| 组件 | 版本 |
|---|---|
| Kotlin | 2.4.10 |
| Jetpack Compose | 1.9.4（ui/foundation/animation/material3） |
| Android Gradle Plugin | 9.3.2 |
| compileSdk / minSdk / targetSdk | 37 / 26 / 34 |
| Liquid Glass（Backdrop） | io.github.kyant0:backdrop:2.0.1（Apache-2.0） |

## 工程结构

```
app/src/main/
├── AndroidManifest.xml
├── java/com/miaomiao/assistant/
│   ├── MainActivity.kt              # Compose 主界面（首页/工具/关于 + 玻璃效果）
│   ├── MiaoApp.kt                   # Application：通知渠道初始化
│   ├── service/
│   │   ├── MiaoAccessibilityService.kt  # 无障碍文本改写核心
│   │   └── MiaoOverlayService.kt        # 悬浮窗服务
│   └── util/
│       ├── Prefs.kt                  # 配置键常量
│       ├── Settings.kt               # 配置访问封装
│       ├── TextProcessor.kt          # 文本处理管线
│       └── Rule.kt                   # 规则数据类
├── res/                              # 资源（图标/字符串/主题）
└── assets/sponsor.webp               # 赞助图
libs/AndroidLiquidGlass-src/          # 液态玻璃库源码备份（Apache-2.0，供参考）
```

## 构建

标准 Gradle 工程，直接使用本仓库根目录构建：

```bash
# 配置 SDK 路径（按本机修改）
echo "sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk" > local.properties

# 编译 debug APK
./gradlew :app:assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

环境要求：JDK 17+ / Android SDK platform-37 + build-tools 36+ / Gradle 9.x（工程自带 Gradle Wrapper）

`local.properties` 不入库，首次构建时按本机 SDK 位置创建即可。

## 权限说明

| 权限 | 用途 |
|---|---|
| 无障碍服务（BIND_ACCESSIBILITY_SERVICE） | 核心：监听输入框并改写文本 |
| SYSTEM_ALERT_WINDOW | 悬浮窗气泡与手动处理面板 |
| FOREGROUND_SERVICE / SPECIAL_USE | 前台服务保活 |
| QUERY_ALL_PACKAGES | 查询支持 PROCESS_TEXT 的应用 |
| POST_NOTIFICATIONS | 常驻通知「喵喵助手运行中」 |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | 防杀后台 |
| WRITE_EXTERNAL_STORAGE（≤28） | 保存赞助图到相册 |

当前版本不包含网络访问功能；无障碍事件仅用于用户选择的应用，规则与设置在本地处理。

## 免责声明

本软件仅用于个人学习与文本输入辅助。请遵守所在地区法律法规，勿用于任何违法违规用途。