using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace CFQuotaMonitor.Windows.Services;

public sealed class BackupService
{
    public const string SaveFileDialogFilter = "CF Quota Monitor backup (*.cfqm)|*.cfqm";
    public const string ImportFileDialogFilter =
        "CF Quota Monitor backup (*.cfqm;*.cfqm.json)|*.cfqm;*.cfqm.json|All files (*.*)|*.*";

    private const int Iterations = 310_000;
    private const int MaxFileBytes = 10 * 1024 * 1024;
    private readonly JsonSerializerOptions _json = new()
    {
        WriteIndented = true,
        PropertyNameCaseInsensitive = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    public void Export(string path, IEnumerable<AccountRecord> accounts, StorageService storage, string password)
    {
        if (password.Length < 8) throw new BackupException("backup_password_short");
        var payload = new BackupPayload();
        foreach (var account in accounts)
        {
            var token = storage.GetToken(account) ?? throw new BackupException("token_unreadable");
            payload.Accounts.Add(new BackupAccount
            {
                Name = account.Name,
                AccountId = account.AccountId,
                DailyLimit = account.DailyLimit,
                Token = token
            });
        }
        if (payload.Accounts.Count == 0) throw new BackupException("backup_select_account");

        var clear = JsonSerializer.SerializeToUtf8Bytes(payload, _json);
        var salt = RandomNumberGenerator.GetBytes(16);
        var nonce = RandomNumberGenerator.GetBytes(12);
        var tag = new byte[16];
        var encrypted = new byte[clear.Length];
        var key = Rfc2898DeriveBytes.Pbkdf2(password, salt, Iterations, HashAlgorithmName.SHA256, 32);
        try
        {
            using var aes = new AesGcm(key);
            aes.Encrypt(nonce, clear, encrypted, tag, Encoding.UTF8.GetBytes("CFQM:1"));
            var backup = new EncryptedBackup
            {
                Iterations = Iterations,
                Salt = Convert.ToBase64String(salt),
                Nonce = Convert.ToBase64String(nonce),
                Tag = Convert.ToBase64String(tag),
                Payload = Convert.ToBase64String(encrypted)
            };
            File.WriteAllText(path, JsonSerializer.Serialize(backup, _json), new UTF8Encoding(false));
        }
        finally
        {
            CryptographicOperations.ZeroMemory(clear);
            CryptographicOperations.ZeroMemory(key);
        }
    }

    public BackupPayload Import(string path, string password)
    {
        var info = new FileInfo(path);
        if (!info.Exists || info.Length <= 0 || info.Length > MaxFileBytes)
            throw new BackupException("backup_invalid");
        EncryptedBackup backup;
        try { backup = JsonSerializer.Deserialize<EncryptedBackup>(File.ReadAllText(path), _json)!; }
        catch { throw new BackupException("backup_invalid"); }
        if (backup == null || backup.Format != "cfqm-encrypted-backup" || backup.Version != 1 ||
            backup.Kdf != "PBKDF2-HMAC-SHA256" || backup.Cipher != "AES-256-GCM" ||
            backup.Iterations is < 100_000 or > 2_000_000)
            throw new BackupException("backup_unsupported");

        byte[] salt, nonce, tag, encrypted;
        try
        {
            salt = Convert.FromBase64String(backup.Salt);
            nonce = Convert.FromBase64String(backup.Nonce);
            tag = Convert.FromBase64String(backup.Tag);
            encrypted = Convert.FromBase64String(backup.Payload);
        }
        catch { throw new BackupException("backup_invalid"); }
        if (salt.Length != 16 || nonce.Length != 12 || tag.Length != 16 || encrypted.Length > MaxFileBytes)
            throw new BackupException("backup_invalid");

        var clear = new byte[encrypted.Length];
        var key = Rfc2898DeriveBytes.Pbkdf2(password, salt, backup.Iterations, HashAlgorithmName.SHA256, 32);
        try
        {
            try
            {
                using var aes = new AesGcm(key);
                aes.Decrypt(nonce, encrypted, tag, clear, Encoding.UTF8.GetBytes("CFQM:1"));
            }
            catch (CryptographicException) { throw new BackupException("backup_wrong_password"); }
            var payload = JsonSerializer.Deserialize<BackupPayload>(clear, _json)
                          ?? throw new BackupException("backup_invalid");
            Validate(payload);
            return payload;
        }
        finally
        {
            CryptographicOperations.ZeroMemory(clear);
            CryptographicOperations.ZeroMemory(key);
        }
    }

    private static void Validate(BackupPayload payload)
    {
        if (payload.SchemaVersion != 1 || payload.Accounts.Count is < 1 or > 500)
            throw new BackupException("backup_invalid");
        foreach (var account in payload.Accounts)
        {
            account.Name = account.Name.Trim();
            account.AccountId = account.AccountId.Trim().ToLowerInvariant();
            account.Token = account.Token.Trim();
            if (account.Name.Length is < 1 or > 100 ||
                !Regex.IsMatch(account.AccountId, "^[a-f0-9]{32}$") ||
                account.DailyLimit is < 1 or > 10_000_000_000 ||
                account.Token.Length is < 20 or > 4096)
                throw new BackupException("backup_invalid");
        }
    }
}

public sealed class BackupException : Exception
{
    public string ResourceKey { get; }
    public BackupException(string resourceKey) : base(resourceKey) => ResourceKey = resourceKey;
}
