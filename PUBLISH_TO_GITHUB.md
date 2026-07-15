# 发布到 GitHub

仓库名建议使用：`cf-workers-quota-monitor`

以下操作只需要执行一次。GitHub 登录完全在你自己的终端和浏览器中完成。

## 1. 安装 GitHub CLI

打开 PowerShell：

```powershell
winget install --id GitHub.cli --exact
```

安装后关闭并重新打开 PowerShell，然后登录：

```powershell
gh auth login
```

如果最后提示连接 `github.com/login/oauth/access_token` 超时，而电脑正在运行 Clash Verge，请在同一个 PowerShell 窗口执行：

```powershell
$env:HTTP_PROXY="http://127.0.0.1:7897"
$env:HTTPS_PROXY="http://127.0.0.1:7897"
gh auth login
```

依次选择：

```text
GitHub.com
HTTPS
Login with a web browser
```

浏览器会显示一次性验证码，由你自己确认授权。

## 2. 一键发布

```powershell
Set-Location "C:\path\to\cf-quota-monitor"
powershell -ExecutionPolicy Bypass -File .\publish-to-github.ps1
```

脚本会自动完成：

1. 初始化独立 Git 仓库；
2. 使用 GitHub 用户名和 `users.noreply.github.com` 邮箱提交，避免公开真实邮箱；
3. 再次阻止签名密钥、本机配置和 APK 被提交到源码；
4. 创建公开仓库 `cf-workers-quota-monitor`；
5. 推送 `main` 分支；
6. 创建 `v1.2.0` Release；
7. 上传签名 APK 和 SHA-256 校验文件。

## 手动命令

如果不想运行脚本，也可以逐行执行：

```powershell
Set-Location "C:\path\to\cf-quota-monitor"

$login = gh api user --jq .login
git init
git config --global --add safe.directory (Get-Location).Path.Replace('\', '/')
git branch -M main
git config user.name $login
git config user.email "$login@users.noreply.github.com"

git add .
git status --short
git commit -m "Release CF额度监控 v1.2.0"

gh repo create cf-workers-quota-monitor --public --source . --remote origin --push --description "安全、纯本地的 Cloudflare Workers 多账号额度监控 Android App"

gh release create v1.2.0 ".\release\CF额度监控-v1.2.0.apk" ".\release\SHA256SUMS.txt" --title "CF额度监控 v1.2.0" --notes-file ".\release\RELEASE_NOTES.md"
```

## 绝对不要上传

这些内容已经被 `.gitignore` 排除：

- `.signing/`：应用签名私钥和密码；
- `local.properties`：本机 Android SDK 路径；
- `app/build/`、`.gradle/`：本机构建缓存；
- `release/`：APK 只通过 GitHub Release 发布，不进入源码历史。

发布后请另外备份 `.signing` 文件夹。丢失签名密钥后，将无法为已安装用户提供可覆盖安装的更新。
