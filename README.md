<p align="center"><img src="docs/images/banner.svg" alt="CF额度监控" width="100%"></p>

<p align="center">
  <strong>简体中文</strong> · <a href="README_EN.md">English</a> · <a href="README_RU.md">Русский</a> · <a href="README_IT.md">Italiano</a> · <a href="README_FR.md">Français</a> · <a href="README_ES.md">Español</a> · <a href="README_AR.md">العربية</a>
</p>

<p align="center">
  <a href="../../releases/latest"><img alt="下载 APK" src="https://img.shields.io/badge/下载_APK-v1.2.0-F48120?style=for-the-badge&logo=android&logoColor=white"></a>
  <img alt="Android 8+" src="https://img.shields.io/badge/Android-8.0+-159567?style=for-the-badge&logo=android&logoColor=white">
  <a href="LICENSE"><img alt="MIT License" src="https://img.shields.io/badge/License-MIT-52606D?style=for-the-badge"></a>
</p>

<p align="center">美观、安全、纯本地的 Cloudflare Workers 多账号额度监控 Android App。</p>

## v1.2 界面

<p align="center">
  <img src="docs/images/v1.2-main-en.png" alt="英文主界面" width="300">
  &nbsp;&nbsp;
  <img src="docs/images/v1.2-settings-zh.png" alt="中文设置界面" width="300">
</p>

## 功能亮点

| 功能 | 说明 |
|---|---|
| 多账号同屏 | 每个账号独立进度条，显示已用、剩余和每日额度 |
| 可选应用锁 | 默认关闭；开启后离开再进入需指纹、面容或系统锁屏密码 |
| 7 种语言 | 中文、英语、俄语、意大利语、法语、西班牙语、阿拉伯语；首次跟随系统 |
| 可选后台刷新 | 默认关闭；可选 15/30 分钟或 1/3/6/12/24 小时 |
| 本机加密 | API Token 使用 AES-GCM 与 Android Keystore 加密 |
| 纯净开源 | 无广告、无统计 SDK、无自建服务器，MIT License |

> 后台执行时间由 Android 调度，可能因省电模式或无网络而延迟。每次打开 App 仍会立即刷新。

## 下载与安装

前往 [Releases](../../releases/latest) 下载：

```text
CF额度监控-v1.2.0.apk
```

支持 Android 8.0 及以上，可从 v1.0 直接覆盖安装并保留账号数据。

## 3 分钟配置

1. 登录 [Cloudflare Dashboard](https://dash.cloudflare.com)，在 **Workers & Pages** 找到 32 位 **Account ID**。
2. 打开 **个人资料 → API Tokens → Create Custom Token**。
3. 只授予 `Account → Account Analytics → Read`，并限制为需要监控的账号。
4. 打开 App，点击右下角 **＋**，粘贴 Account ID 和 API Token。

不要使用 Global API Key，也不要把 Token 发到聊天、Issue 或提交到 GitHub。详细步骤见 [零基础教程](docs/安装与配置教程.md)。

## 安全与隐私

```text
Android App ── HTTPS ──> Cloudflare 官方 GraphQL API
     │
     └── API Token：AES-GCM 加密 + Android Keystore 本机密钥
```

- Token 和用量缓存只保存在用户设备中
- 后台刷新开启后也只连接 `api.cloudflare.com`
- 禁用 Android 云备份和设备迁移备份
- 删除账号时同步删除加密凭证和用量缓存
- 签名文件、本机路径和构建缓存均被 `.gitignore` 排除

完整说明见 [PRIVACY.md](PRIVACY.md)。

## 本地构建

需要 JDK 17 和 Android SDK 35：

```powershell
.\gradlew.bat assembleDebug
```

## 说明与许可

App 使用 Cloudflare GraphQL Analytics API，数据可能延迟数分钟，也不是官方计费计数器；临近 100% 时请预留余量。

本项目使用 [MIT License](LICENSE)，与 Cloudflare, Inc. 无隶属或官方合作关系。
