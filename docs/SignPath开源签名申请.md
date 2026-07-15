# SignPath Foundation免费开源签名申请

## 是否收费

符合条件并获批的开源项目免费。签名证书属于SignPath Foundation，Windows文件的发布者将显示SignPath Foundation，而不是个人姓名。

这只解决Windows Authenticode签名，不能代替Apple Developer ID或iOS签名。

## 本项目申请前检查

1. GitHub仓库公开。
2. 使用OSI认可的MIT许可证。
3. 已发布可运行的Windows安装包。
4. README清楚说明功能、下载和隐私。
5. 仓库包含`PRIVACY.md`、`SECURITY.md`和`CODE_SIGNING_POLICY.md`。
6. 所有维护者开启GitHub双重验证。
7. 构建必须从公开源代码和可信CI产生。
8. 依赖必须允许开源分发，不能混入维护者自己的闭源组件。

## 申请步骤

1. 先把Windows源码、构建脚本和未签名Release推送到GitHub。
2. 打开`https://signpath.org/apply.html`。
3. 填写项目名称、公开仓库地址、Release下载页、许可证和项目简介。
4. 说明需要签名的文件：Windows x64/ARM64 Setup EXE；也可申请签名主程序EXE。
5. 提交后等待人工审核。新项目可能被要求先积累公开发布和维护记录。
6. 获批后按SignPath提供的信息创建组织、项目、Artifact Configuration和Signing Policy。
7. 将GitHub Actions设为Trusted Build System，开启来源验证，仅允许版本标签或release分支。
8. 设置维护者为Approver，每次正式Release人工批准签名请求。
9. 把签名后的文件上传Release，并验证数字签名、产品名称、版本和SHA-256。
10. 在README和Release页加入：`Free code signing provided by SignPath.io, certificate by SignPath Foundation.`

## 申请时可复制的英文简介

```text
CF Quota Monitor is an MIT-licensed, local-first desktop application for monitoring daily Cloudflare Workers usage across multiple accounts. The Windows client stores API tokens with current-user DPAPI, communicates only with Cloudflare's official GraphQL API, contains no ads or analytics, and is built from the public repository using reproducible release scripts. We request open-source Authenticode signing for the Windows x64 and ARM64 release executables and installers.
```

## 官方链接

- 申请：`https://signpath.org/apply.html`
- 免费开源签名：`https://signpath.org/`
- 申请条件：`https://signpath.org/terms.html`
- SignPath项目配置：`https://docs.signpath.io/projects`
