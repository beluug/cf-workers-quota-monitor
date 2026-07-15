# 隐私说明 / Privacy Policy

CF额度监控不收集、上传、出售或分析用户数据，不包含广告、统计SDK、崩溃上报SDK或自建服务器。

## 所有平台

- Cloudflare Account ID、API Token和用量缓存仅保存在用户设备中。
- 用量查询由设备直接发送到`https://api.cloudflare.com/client/v4/graphql`。
- API Token只用于用户主动配置的Cloudflare查询。
- 删除账户时同步删除本地凭据和缓存。Android卸载会移除应用沙盒；Windows卸载默认保留当前用户数据，便于重装恢复，用户可先在应用内删除全部账户后再卸载。

## Android

- Token使用Android Keystore保护的AES-GCM密钥加密。
- 后台刷新默认关闭；开启后仅按所选周期查询Cloudflare官方API。
- 禁用Android云备份和设备迁移中的SharedPreferences备份。

## Windows

- Token使用Windows Data Protection API（DPAPI）按当前Windows用户加密，不能直接复制到其他用户或电脑解密。
- 设置和用量缓存保存在当前用户的`LocalAppData\CFQuotaMonitor`目录。
- 开机启动启用时，仅写入当前用户的`HKCU`启动项，不需要管理员权限。
- 托盘后台刷新只在程序运行且电脑未休眠时发生。

## 加密导出文件

用户主动导出时，选中的Account ID和API Token会写入密码保护的`.cfqm`文件。文件使用PBKDF2-HMAC-SHA256和AES-256-GCM加密。应用不会保存备份密码，也无法找回备份密码。

导出文件由用户自行保存和传输。任何获得该文件和正确备份密码的人都能导入其中账户，因此请分别安全保管文件和密码。

---

CF Quota Monitor does not collect, upload, sell, or analyze user data. It contains no advertising, analytics, crash-reporting SDK, or custom backend. Credentials stay on the user's device and requests go directly to Cloudflare's official API. Windows protects tokens with per-user DPAPI; Android protects them with Android Keystore. Password-protected `.cfqm` exports are created only when requested by the user.
