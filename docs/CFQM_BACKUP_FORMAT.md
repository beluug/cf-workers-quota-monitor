# CFQM encrypted backup format v1

CFQM is a portable, versioned account-backup format shared by CF Quota Monitor clients. The file extension is `.cfqm`.

## Container

The outer file is UTF-8 JSON containing only encryption metadata and ciphertext:

```json
{
  "format": "cfqm-encrypted-backup",
  "version": 1,
  "kdf": "PBKDF2-HMAC-SHA256",
  "iterations": 310000,
  "salt": "base64(16 bytes)",
  "cipher": "AES-256-GCM",
  "nonce": "base64(12 bytes)",
  "tag": "base64(16 bytes)",
  "payload": "base64(ciphertext)"
}
```

The 32-byte encryption key is derived from the UTF-8 backup password. AES-GCM additional authenticated data is the UTF-8 string `CFQM:1`.

## Decrypted payload

```json
{
  "schemaVersion": 1,
  "exportedAtUtc": "ISO-8601 timestamp",
  "sourcePlatform": "windows",
  "accounts": [
    {
      "name": "local display name",
      "accountId": "32 hexadecimal characters",
      "dailyLimit": 100000,
      "token": "Cloudflare API Token"
    }
  ]
}
```

Importers must reject unknown versions, invalid Base64, unexpected field sizes, authentication failures, files larger than 10 MiB, more than 500 accounts, invalid Account IDs, invalid limits, and tokens outside the accepted length range. Importers must never log decrypted payloads or tokens.

Device-specific settings, cached usage, lock credentials, and operating-system startup settings are deliberately excluded.
