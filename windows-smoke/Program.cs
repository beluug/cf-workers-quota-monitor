using CFQuotaMonitor.Windows;
using CFQuotaMonitor.Windows.Services;
using System.Reflection;

var folder = Path.Combine(Path.GetTempPath(), "cfqm-smoke-" + Guid.NewGuid().ToString("N"));
Directory.CreateDirectory(folder);
try
{
    var storage = new StorageService(folder);
    var original = storage.AddAccount(
        "Smoke test", "0123456789abcdef0123456789abcdef", 100_000,
        "test_token_that_is_long_enough_but_not_real");
    if (storage.GetToken(original) != "test_token_that_is_long_enough_but_not_real")
        throw new Exception("DPAPI round trip failed");

    var backupPath = Path.Combine(folder, "roundtrip.cfqm");
    var backup = new BackupService();
    backup.Export(backupPath, new[] { original }, storage, "correct-horse-battery-staple");
    var containerJson = File.ReadAllText(backupPath);
    if (!containerJson.Contains("\"format\"") || containerJson.Contains("\"Format\""))
        throw new Exception("CFQM container is not using the documented camelCase schema");
    var payload = backup.Import(backupPath, "correct-horse-battery-staple");
    if (payload.Accounts.Count != 1 || payload.Accounts[0].AccountId != original.AccountId ||
        payload.Accounts[0].Token != "test_token_that_is_long_enough_but_not_real")
        throw new Exception("Encrypted backup round trip failed");

    try
    {
        backup.Import(backupPath, "wrong-password");
        throw new Exception("Wrong password was accepted");
    }
    catch (BackupException ex) when (ex.ResourceKey == "backup_wrong_password") { }

    var importedStorage = new StorageService(Path.Combine(folder, "imported"));
    var result = importedStorage.ImportAccounts(payload.Accounts, DuplicateMode.Skip);
    if (result.Imported != 1 || importedStorage.GetToken(importedStorage.Accounts[0]) != payload.Accounts[0].Token)
        throw new Exception("Imported account secure storage failed");

    var localizer = new LocalizationService("en");
    var stringsField = typeof(LocalizationService).GetField("_strings", BindingFlags.Instance | BindingFlags.NonPublic)
                       ?? throw new Exception("Localization dictionary missing");
    var strings = (Dictionary<string, string[]>)stringsField.GetValue(localizer)!;
    if (strings.Count < 80 || strings.Any(entry => entry.Value.Length != 7 || entry.Value.Any(string.IsNullOrWhiteSpace)))
        throw new Exception("A translation is missing or incomplete");

    Console.WriteLine($"PASS: DPAPI, encrypted backup, wrong-password rejection, account import, and {strings.Count} translated UI strings");
}
finally
{
    Directory.Delete(folder, true);
}
