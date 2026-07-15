# 发布到GitHub

仓库建议名称：`cf-workers-quota-monitor`

## 1. 准备GitHub CLI

```powershell
winget install --id GitHub.cli --exact
gh auth login
```

如果浏览器授权连接超时，并且你在使用本地代理，可在同一个PowerShell窗口设置实际代理地址后重试：

```powershell
$env:HTTP_PROXY="http://127.0.0.1:你的端口"
$env:HTTPS_PROXY=$env:HTTP_PROXY
gh auth login
```

## 2. 生成Windows文件

```powershell
Set-Location "C:\path\to\cf-quota-monitor"
.\build-windows.ps1 -Architecture all
```

确认`release`目录包含：

- Android APK
- Windows x64/ARM64 Setup EXE
- Windows x64/ARM64 Portable ZIP
- 两个平台的SHA-256文件
- `RELEASE_NOTES.md`和`INSTALL.txt`

## 3. 一键推送源码和Release

```powershell
powershell -ExecutionPolicy Bypass -File .\publish-to-github.ps1
```

脚本会：

1. 使用GitHub用户名和`users.noreply.github.com`邮箱提交，避免公开真实邮箱。
2. 阻止签名密钥、Token、本机配置、构建缓存和安装包进入源码历史。
3. 创建或更新公开仓库。
4. 创建或更新`v1.2.0` Release。
5. 上传Android和四个Windows安装资产及校验文件。

## 手动上传Release

如果源码已经推送，只上传Windows文件：

```powershell
gh release upload v1.2.0 `
  .\release\CF-Quota-Monitor-v1.0.1-Windows-x64-Setup.exe `
  .\release\CF-Quota-Monitor-v1.0.1-Windows-x64-Portable.zip `
  .\release\CF-Quota-Monitor-v1.0.1-Windows-arm64-Setup.exe `
  .\release\CF-Quota-Monitor-v1.0.1-Windows-arm64-Portable.zip `
  .\release\SHA256SUMS-Windows.txt `
  --clobber
```

## 绝对不要上传到源码历史

- `.signing/`和任何`.jks`、`.keystore`、`.pfx`、`.p12`证书或私钥
- `local.properties`
- `%LocalAppData%\CFQuotaMonitor`中的真实账户数据
- `.cfqm`备份文件
- `bin/`、`obj/`、`build/`、`.gradle/`
- Release二进制文件；它们应作为GitHub Release资产上传

发布完成后再按 [SignPath申请教程](docs/SignPath开源签名申请.md) 申请Windows开源签名。
