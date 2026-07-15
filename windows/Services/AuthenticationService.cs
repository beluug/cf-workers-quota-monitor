using System.Security.Cryptography;
using Windows.Security.Credentials.UI;

namespace CFQuotaMonitor.Windows.Services;

public sealed class AuthenticationService
{
    private const int PinIterations = 210_000;

    public async Task<bool?> TryWindowsHelloAsync(string prompt)
    {
        try
        {
            var availability = await UserConsentVerifier.CheckAvailabilityAsync();
            if (availability != UserConsentVerifierAvailability.Available) return null;
            var result = await UserConsentVerifier.RequestVerificationAsync(prompt);
            return result == UserConsentVerificationResult.Verified;
        }
        catch { return null; }
    }

    public static void SetPin(AppSettings settings, string pin)
    {
        var salt = RandomNumberGenerator.GetBytes(16);
        var hash = Rfc2898DeriveBytes.Pbkdf2(pin, salt, PinIterations, HashAlgorithmName.SHA256, 32);
        settings.PinSalt = Convert.ToBase64String(salt);
        settings.PinHash = Convert.ToBase64String(hash);
        CryptographicOperations.ZeroMemory(hash);
    }

    public static bool VerifyPin(AppSettings settings, string pin)
    {
        try
        {
            var salt = Convert.FromBase64String(settings.PinSalt);
            var expected = Convert.FromBase64String(settings.PinHash);
            var actual = Rfc2898DeriveBytes.Pbkdf2(pin, salt, PinIterations, HashAlgorithmName.SHA256, expected.Length);
            try { return CryptographicOperations.FixedTimeEquals(actual, expected); }
            finally
            {
                CryptographicOperations.ZeroMemory(actual);
                CryptographicOperations.ZeroMemory(expected);
            }
        }
        catch { return false; }
    }
}
