# 隐私说明

CF额度监控不收集、上传、出售或分析用户数据。

- Cloudflare Account ID、API Token 和用量缓存仅保存在用户设备中。
- Token 使用 Android Keystore 生成的 AES-GCM 密钥加密。
- 用量查询由设备直接发送到 `https://api.cloudflare.com/client/v4/graphql`。
- 应用不使用自建后端、广告 SDK、统计 SDK 或崩溃上报 SDK。
- 后台刷新默认关闭。用户开启后，Android 仅按所选周期直接查询 Cloudflare，并把最新用量缓存到本机。
- 应用禁用 Android 云备份和设备迁移中的 SharedPreferences 备份。

用户可以随时关闭后台刷新、关闭应用锁、删除单个账号，或通过卸载应用删除全部本地数据。
