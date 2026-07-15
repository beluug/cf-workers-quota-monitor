using System.Reflection;
using System.Text.Json;
using Microsoft.Win32;

namespace CFQuotaMonitor.Windows.Services;

public sealed class StorageService
{
    private readonly string _folder;
    private readonly JsonSerializerOptions _json = new() { WriteIndented = true };
    private List<AccountRecord> _accounts = new();

    private string AccountsPath => Path.Combine(_folder, "accounts.json");
    private string SettingsPath => Path.Combine(_folder, "settings.json");

    public IReadOnlyList<AccountRecord> Accounts => _accounts;
    public AppSettings Settings { get; private set; } = new();

    public StorageService(string? folder = null)
    {
        _folder = folder ?? Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "CFQuotaMonitor");
        Directory.CreateDirectory(_folder);
        _accounts = Read<List<AccountRecord>>(AccountsPath) ?? new();
        Settings = Read<AppSettings>(SettingsPath) ?? new();
        Settings.RefreshIntervalMinutes = Math.Clamp(Settings.RefreshIntervalMinutes, 15, 24 * 60);
        Settings.LockAfterMinutes = Math.Clamp(Settings.LockAfterMinutes, 0, 60);
    }

    public string? GetToken(AccountRecord account)
    {
        if (string.IsNullOrWhiteSpace(account.EncryptedToken)) return null;
        try { return WindowsDataProtection.UnprotectString(account.EncryptedToken); }
        catch { return null; }
    }

    public AccountRecord AddAccount(string name, string accountId, long dailyLimit, string token)
    {
        var record = new AccountRecord
        {
            Name = string.IsNullOrWhiteSpace(name) ? "Cloudflare" : name.Trim(),
            AccountId = accountId.Trim().ToLowerInvariant(),
            DailyLimit = Math.Max(1, dailyLimit),
            EncryptedToken = WindowsDataProtection.ProtectString(token.Trim())
        };
        _accounts.Add(record);
        SaveAccounts();
        return record;
    }

    public void UpdateAccount(AccountRecord account, string name, string accountId, long dailyLimit, string? newToken)
    {
        account.Name = string.IsNullOrWhiteSpace(name) ? "Cloudflare" : name.Trim();
        account.AccountId = accountId.Trim().ToLowerInvariant();
        account.DailyLimit = Math.Max(1, dailyLimit);
        if (!string.IsNullOrWhiteSpace(newToken))
            account.EncryptedToken = WindowsDataProtection.ProtectString(newToken.Trim());
        SaveAccounts();
    }

    public void DeleteAccount(AccountRecord account)
    {
        _accounts.Remove(account);
        SaveAccounts();
    }

    public void SaveUsage(AccountRecord account, long used)
    {
        account.Used = Math.Max(0, used);
        account.FetchedAt = DateTimeOffset.UtcNow;
        SaveAccounts();
    }

    public (int Imported, int Skipped) ImportAccounts(IEnumerable<BackupAccount> incoming, DuplicateMode mode)
    {
        var imported = 0;
        var skipped = 0;
        foreach (var item in incoming)
        {
            var existing = _accounts.FirstOrDefault(a =>
                string.Equals(a.AccountId, item.AccountId, StringComparison.OrdinalIgnoreCase));
            if (existing != null && mode == DuplicateMode.Skip)
            {
                skipped++;
                continue;
            }

            if (existing != null && mode == DuplicateMode.Replace)
            {
                existing.Name = item.Name;
                existing.DailyLimit = item.DailyLimit;
                existing.EncryptedToken = WindowsDataProtection.ProtectString(item.Token);
                existing.Used = 0;
                existing.FetchedAt = null;
            }
            else
            {
                _accounts.Add(new AccountRecord
                {
                    Name = item.Name,
                    AccountId = item.AccountId,
                    DailyLimit = item.DailyLimit,
                    EncryptedToken = WindowsDataProtection.ProtectString(item.Token)
                });
            }
            imported++;
        }
        SaveAccounts();
        return (imported, skipped);
    }

    public void SaveSettings()
    {
        WriteAtomic(SettingsPath, JsonSerializer.Serialize(Settings, _json));
        ApplyStartWithWindows(Settings.StartWithWindows);
    }

    public void SaveAccounts() => WriteAtomic(AccountsPath, JsonSerializer.Serialize(_accounts, _json));

    private T? Read<T>(string path)
    {
        try
        {
            if (!File.Exists(path)) return default;
            var info = new FileInfo(path);
            if (info.Length > 5 * 1024 * 1024) return default;
            return JsonSerializer.Deserialize<T>(File.ReadAllText(path), _json);
        }
        catch { return default; }
    }

    private static void WriteAtomic(string path, string content)
    {
        var temp = path + ".tmp";
        File.WriteAllText(temp, content);
        File.Move(temp, path, true);
    }

    private static void ApplyStartWithWindows(bool enabled)
    {
        try
        {
            using var key = Registry.CurrentUser.CreateSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run");
            if (enabled)
            {
                var location = Environment.ProcessPath ?? Path.Combine(AppContext.BaseDirectory, "CFQuotaMonitor.exe");
                key.SetValue("CFQuotaMonitor", $"\"{location}\" --background");
            }
            else key.DeleteValue("CFQuotaMonitor", false);
        }
        catch { /* Non-fatal: managed PCs may block startup registration. */ }
    }
}
