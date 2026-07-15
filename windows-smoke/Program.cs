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
    var vectorOutput = Environment.GetEnvironmentVariable("CFQM_VECTOR_OUTPUT");
    if (!string.IsNullOrWhiteSpace(vectorOutput)) File.Copy(backupPath, vectorOutput, true);
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

    var androidVector = Environment.GetEnvironmentVariable("CFQM_ANDROID_VECTOR_BASE64");
    if (!string.IsNullOrWhiteSpace(androidVector))
    {
        var androidPath = Path.Combine(folder, "android-vector.cfqm");
        File.WriteAllBytes(androidPath, Convert.FromBase64String(androidVector));
        var androidPayload = backup.Import(androidPath, "correct-horse-battery-staple");
        if (androidPayload.SourcePlatform != "android" || androidPayload.Accounts.Count != 1 ||
            androidPayload.Accounts[0].Token != "test_token_that_is_long_enough_but_not_real")
            throw new Exception("Android export was not compatible with Windows");
    }

    var importedStorage = new StorageService(Path.Combine(folder, "imported"));
    var result = importedStorage.ImportAccounts(payload.Accounts, DuplicateMode.Skip);
    if (result.Imported != 1 || importedStorage.GetToken(importedStorage.Accounts[0]) != payload.Accounts[0].Token)
        throw new Exception("Imported account secure storage failed");

    const string successfulGraphQlWithNullErrors =
        "{\"data\":{\"viewer\":{\"accounts\":[{\"workersInvocationsAdaptive\":[{\"sum\":{\"requests\":41}},{\"sum\":{\"requests\":1}}]}]}},\"errors\":null}";
    if (CloudflareService.ParseUsageResponse(successfulGraphQlWithNullErrors) != 42)
        throw new Exception("Cloudflare response with errors:null was not parsed correctly");

    const string successfulGraphQlWithEmptyErrors =
        "{\"data\":{\"viewer\":{\"accounts\":[{\"workersInvocationsAdaptive\":[]}]}},\"errors\":[]}";
    if (CloudflareService.ParseUsageResponse(successfulGraphQlWithEmptyErrors) != 0)
        throw new Exception("Cloudflare response with an empty errors array was not parsed correctly");

    const string failedGraphQl =
        "{\"data\":null,\"errors\":[{\"message\":\"Unauthorized\"}]}";
    try
    {
        CloudflareService.ParseUsageResponse(failedGraphQl);
        throw new Exception("Cloudflare GraphQL error was accepted as usage data");
    }
    catch (CloudflareException ex) when (ex.ResourceKey == "error_token_invalid") { }

    var localizer = new LocalizationService("en");
    var stringsField = typeof(LocalizationService).GetField("_strings", BindingFlags.Instance | BindingFlags.NonPublic)
                       ?? throw new Exception("Localization dictionary missing");
    var strings = (Dictionary<string, string[]>)stringsField.GetValue(localizer)!;
    if (strings.Count < 80 || strings.Any(entry => entry.Value.Length != 7 || entry.Value.Any(string.IsNullOrWhiteSpace)))
        throw new Exception("A translation is missing or incomplete");

    Console.WriteLine($"PASS: Cloudflare errors:null response, DPAPI, encrypted backup, wrong-password rejection, account import, and {strings.Count} translated UI strings");
}
finally
{
    Directory.Delete(folder, true);
}
