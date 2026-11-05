# 喵喵助手（MiaoAssistant）

一款基于 Android 无障碍服务的文本自动改写辅助工具：在微信 / QQ 等聊天应用中，按规则自动替换文本、追加标点触发内容与颜文字，支持 QQ 猫爪包裹，并提供悬浮窗手动处理面板。

- 作者：91917878qq
- 反馈邮箱：1490964435@qq.com
- 源码版本：3.0.0（versionCode 51）
- 仓库内置 APK：2.5.0（历史版本，位于 legacy/；不是当前源码构建产物）
- 包名：com.miaomiao.assistant

## 功能

- 无障碍文本改写：实时规则替换（支持括号保护、随机替换）、标点触发追加、颜文字定时轮换、QQ 猫爪包裹
- 悬浮窗：可拖拽、贴边吸附，大小与不透明度实时生效，手动处理面板全程离线
- 规则系统：行内编辑「原文本 → 替换为」，保存 / 删除 / 导入 / 导出规则，最多 30 套预设
- 规则分享码：离线生成与导入（`MR1:` 开头的分享码）
- 功能体检：一键检测无障碍服务 / 悬浮窗权限 / 已选应用 / 版本信息
- 崩溃日志：本地记录，可导出日志包反馈
- 液态玻璃底部导航与玻璃模糊效果（模糊等级 0–2 可调）

## 构建

本仓库为 apktool 解码工程（smali + 资源），使用 [apktool](https://ibotpeaches.github.io/Apktool/) 构建：

> **构建平台要求**：本仓库包含 1421 组仅大小写不同的 Smali 路径，共 2911 个文件参与冲突。Windows/macOS 默认文件系统无法完整检出和回编译，请在 Linux 或 WSL2 的大小写敏感目录中构建。Windows 默认目录只适合查看，不建议直接构建。

```bash
# 回编译
apktool b MiaoMiaoAssistant -o app-unsigned.apk

# 对齐 + 签名（需要 release keystore）
zipalign -p -f 4 app-unsigned.apk app-aligned.apk
apksigner sign --ks <your-release.keystore> \
  --ks-key-alias <alias> \
  --ks-pass pass:<store-pass> \
  --key-pass pass:<key-pass> \
  --out app-release.apk app-aligned.apk
```

> 建议使用 Android SDK build-tools 中的 `zipalign` 与 `apksigner`。

## 发布包说明

仓库根目录不再放置当前版本 APK，避免源码与安装包混淆。`legacy/喵喵助手2.5.0.apk` 是历史安装包（versionCode 50），仅用于兼容和复现，不是 3.0.0 的构建产物。

当前源码版本为 3.0.0。维护者发布 3.0.0 APK 时，请同时提供 SHA-256 和签名证书指纹。

## 隐私说明

当前代码未包含网络访问 API；无障碍事件只用于用户选择的应用，规则和设置在本地处理/保存；崩溃日志默认保存在应用文件目录，导出由用户主动触发。若后续加入联网功能，请同步更新此说明和权限。

## 授权状态

本仓库当前未附开源许可证，默认保留所有权利。原项目授权和第三方组件许可仍需维护者确认；在确认前，请勿将仓库内容用于再分发或商业用途。

## 使用声明

本软件仅用于个人学习与文本输入辅助。请遵守所在地区法律法规，勿用于任何违法违规用途。
