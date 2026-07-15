# Security Policy

## Supported releases

Security fixes are applied to the latest Android and Windows releases. Users should verify downloads against the SHA-256 files published with each release.

## Reporting a vulnerability

Please do not include Cloudflare API Tokens, passwords, backup files, Account IDs, personal paths, or other private data in a public issue.

Open a GitHub issue containing only a non-sensitive description and request a private contact channel. The repository owner can then provide a safe way to share reproduction details.

## Security design

- The application communicates directly with Cloudflare's official GraphQL endpoint over HTTPS.
- Android uses Android Keystore-backed encryption for API Tokens.
- Windows uses current-user DPAPI for API Tokens.
- Portable `.cfqm` backups use PBKDF2-HMAC-SHA256 and AES-256-GCM.
- Tokens are never intentionally written to logs, crash reports, build output, or source control.
- The project contains no analytics, advertising, or remote-update execution code.
