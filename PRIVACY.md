# 隐私说明

CF额度监控不收集、上传、出售或分析用户数据。

- Cloudflare Account ID 和 API Token 仅保存在用户设备中。
- Token 使用 Android Keystore 生成的 AES-GCM 密钥加密。
- 用量查询由设备直接发送到 `https://api.cloudflare.com/client/v4/graphql`。
- 应用不使用自建后端、广告 SDK、统计 SDK、崩溃上报 SDK或后台任务。
- 应用禁用 Android 云备份和设备迁移中的 SharedPreferences 备份。

用户可以在应用中删除单个账号，也可以通过卸载应用删除全部本地数据。
