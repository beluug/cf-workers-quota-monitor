<p align="center"><img src="docs/images/banner.svg" alt="CF Quota Monitor" width="100%"></p>

<p align="center"><a href="README.md">简体中文</a> · <strong>English</strong> · <a href="README_RU.md">Русский</a> · <a href="README_IT.md">Italiano</a> · <a href="README_FR.md">Français</a> · <a href="README_ES.md">Español</a> · <a href="README_AR.md">العربية</a></p>

# CF Quota Monitor v1.2

A beautiful, secure, local-first Android dashboard for monitoring daily Cloudflare Workers quota across multiple accounts.

<p align="center"><img src="docs/images/v1.2-main-en.png" alt="Main screen" width="300"> &nbsp; <img src="docs/images/v1.2-settings-zh.png" alt="Settings" width="300"></p>

## Highlights

- Multiple accounts and progress bars on one screen
- Optional app lock using fingerprint, face, or device credential; off by default
- English, Chinese, Russian, Italian, French, Spanish, and Arabic with automatic system-language selection
- Optional background refresh at 15/30 minutes or 1/3/6/12/24 hours; off by default
- API Tokens encrypted with AES-GCM and Android Keystore
- No ads, analytics SDK, custom server, or cloud backup

Android schedules background work approximately and may delay it to save battery. Opening the app always triggers an immediate refresh.

## Install

Download `CF额度监控-v1.2.0.apk` from [Releases](../../releases/latest). Android 8.0 or newer is required. v1.0 can be upgraded in place.

## Three-minute setup

1. In [Cloudflare Dashboard](https://dash.cloudflare.com), open **Workers & Pages** and copy the 32-character **Account ID**.
2. Go to **Profile → API Tokens → Create Custom Token**.
3. Grant only `Account → Account Analytics → Read` and limit it to the monitored account.
4. Tap **＋** in the app and paste the Account ID and API Token.

Never use a Global API Key or publish a token in chat, issues, or GitHub.

## Privacy and license

Tokens and cached usage remain on the device. Requests go directly to `api.cloudflare.com`; background refresh uses the same official endpoint. See [PRIVACY.md](PRIVACY.md).

Licensed under the [MIT License](LICENSE). This independent project is not affiliated with Cloudflare, Inc. Analytics data may lag and is not the official billing counter.
