using System.Text.Json.Serialization;

namespace CFQuotaMonitor.Windows;

public sealed class AccountRecord
{
    public string LocalId { get; set; } = Guid.NewGuid().ToString("N");
    public string Name { get; set; } = "Cloudflare";
    public string AccountId { get; set; } = "";
    public long DailyLimit { get; set; } = 100_000;
    public string EncryptedToken { get; set; } = "";
    public long Used { get; set; }
    public DateTimeOffset? FetchedAt { get; set; }

    [JsonIgnore] public long Remaining => Math.Max(0, DailyLimit - Used);
    [JsonIgnore] public double Progress => DailyLimit <= 0 ? 0 : Math.Clamp((double)Used / DailyLimit, 0, 1);
}

public sealed class AppSettings
{
    public string Language { get; set; } = "";
    public bool AppLockEnabled { get; set; }
    public string PinSalt { get; set; } = "";
    public string PinHash { get; set; } = "";
    public int LockAfterMinutes { get; set; } = 5;
    public bool BackgroundRefreshEnabled { get; set; }
    public int RefreshIntervalMinutes { get; set; } = 60;
    public bool StartWithWindows { get; set; }
    public bool MinimizeToTray { get; set; } = true;
}

public sealed class BackupAccount
{
    public string Name { get; set; } = "";
    public string AccountId { get; set; } = "";
    public long DailyLimit { get; set; }
    public string Token { get; set; } = "";
}

public sealed class BackupPayload
{
    public int SchemaVersion { get; set; } = 1;
    public DateTimeOffset ExportedAtUtc { get; set; } = DateTimeOffset.UtcNow;
    public string SourcePlatform { get; set; } = "windows";
    public List<BackupAccount> Accounts { get; set; } = new();
}

public sealed class EncryptedBackup
{
    public string Format { get; set; } = "cfqm-encrypted-backup";
    public int Version { get; set; } = 1;
    public string Kdf { get; set; } = "PBKDF2-HMAC-SHA256";
    public int Iterations { get; set; } = 310_000;
    public string Salt { get; set; } = "";
    public string Cipher { get; set; } = "AES-256-GCM";
    public string Nonce { get; set; } = "";
    public string Tag { get; set; } = "";
    public string Payload { get; set; } = "";
}

public enum DuplicateMode
{
    Skip,
    Replace,
    KeepBoth
}
