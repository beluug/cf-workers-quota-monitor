# Windows安装与使用教程

## 1. 选择安装包

- 普通Intel/AMD电脑：下载`Windows-x64-Setup.exe`。
- 骁龙Windows电脑：下载`Windows-arm64-Setup.exe`。
- 不想安装：下载相同架构的`Portable.zip`并解压。

查看架构：打开Windows **设置 → 系统 → 系统信息 → 系统类型**。绝大多数电脑是x64。

## 2. 安装

运行Setup文件，选择安装目录，可选是否创建桌面快捷方式。程序按当前Windows用户安装，不要求管理员权限。

当前公开包尚未取得可信代码签名，SmartScreen可能显示“未知发布者”。请确认文件来自本项目GitHub Release，并核对`SHA256SUMS-Windows.txt`。

便携版解压后直接运行`CFQuotaMonitor.exe`，不要只把EXE从压缩包拖出来；旁边的运行库文件也必须保留。

## 3. 创建Cloudflare只读Token

1. 登录`https://dash.cloudflare.com`。
2. 进入 **Workers & Pages**，复制32位Account ID。
3. 打开 **个人资料 → API Tokens → Create Custom Token**。
4. 只添加 **Account → Account Analytics → Read**。
5. 把资源限制到需要监控的账户。
6. 复制Cloudflare只显示一次的Token。

不要使用Global API Key。

## 4. 添加账户

打开程序，点击 **添加账户**，填写：

- 账户备注：仅用于本地显示。
- Account ID：32位十六进制字符。
- API Token：上一步创建的只读Token。
- 每日额度：Workers Free默认100000，也可以自行修改。

保存后应用立即查询今日用量。查询范围是UTC 00:00到当前时间。

## 5. 后台刷新

在 **设置 → 后台刷新** 中可以：

- 开启或关闭托盘后台刷新。
- 选择15分钟到24小时的刷新间隔。
- 设置登录Windows时自动启动。
- 设置关闭窗口时最小化到托盘。

电脑休眠、关机或程序完全退出时不会刷新；恢复或重新打开后会立即刷新。

## 6. 应用锁

应用锁默认关闭。开启时必须设置至少6位备用PIN；支持的电脑会优先调用Windows Hello。重新打开窗口或离开达到所选时间后需要验证。

忘记备用PIN时，应用无法替你恢复PIN。Cloudflare Token仍由Windows当前用户加密保护。

## 7. 导出与导入

打开 **导入与导出**：

1. 勾选需要迁移的账户。
2. 设置至少8位备份密码并确认。
3. 保存`.cfqm`文件。
4. 在另一个支持CFQM的版本中选择该文件并输入密码。

备份文件包含所选账户的API Token，因此必须妥善保管。建议不要把文件和密码放在同一条聊天消息或同一个公开位置。

重复账户可以选择跳过、替换或同时保留。应用锁PIN、用量缓存、开机启动和语言设置不会导出。

## 8. 卸载

打开Windows **设置 → 应用 → 已安装的应用 → CF Quota Monitor → 卸载**。卸载程序会删除程序文件、快捷方式和开机启动项。

当前版本的账户数据保存在当前用户LocalAppData中。如需彻底清除，可先在应用内删除所有账户，再卸载。
