using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

namespace CFQuotaMonitor.Windows.Services;

public sealed class CloudflareService
{
    private static readonly HttpClient Client = new()
    {
        Timeout = TimeSpan.FromSeconds(25)
    };

    private const string Query = @"query WorkerUsage($accountTag: string, $datetimeStart: string, $datetimeEnd: string) {
      viewer {
        accounts(filter: {accountTag: $accountTag}) {
          workersInvocationsAdaptive(
            limit: 100,
            filter: {datetime_geq: $datetimeStart, datetime_leq: $datetimeEnd}
          ) { sum { requests } }
        }
      }
    }";

    public async Task<long> FetchTodayUsageAsync(string accountId, string token, CancellationToken cancellationToken)
    {
        var now = DateTimeOffset.UtcNow;
        var start = new DateTimeOffset(now.Year, now.Month, now.Day, 0, 0, 0, TimeSpan.Zero);
        var body = JsonSerializer.Serialize(new
        {
            query = Query,
            variables = new { accountTag = accountId, datetimeStart = start, datetimeEnd = now }
        });
        using var request = new HttpRequestMessage(HttpMethod.Post, "https://api.cloudflare.com/client/v4/graphql");
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        request.Headers.UserAgent.ParseAdd("CFQuotaMonitor-Windows/1.0.1");
        request.Content = new StringContent(body, Encoding.UTF8, "application/json");

        HttpResponseMessage response;
        try { response = await Client.SendAsync(request, cancellationToken); }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        { throw new CloudflareException("error_network"); }
        catch (HttpRequestException) { throw new CloudflareException("error_network"); }

        using (response)
        {
            var text = await response.Content.ReadAsStringAsync(cancellationToken);
            if (!response.IsSuccessStatusCode)
            {
                var key = response.StatusCode switch
                {
                    HttpStatusCode.Unauthorized => "error_token_invalid",
                    HttpStatusCode.Forbidden => "error_permission",
                    (HttpStatusCode)429 => "error_rate_limit",
                    _ when (int)response.StatusCode >= 500 => "error_cloudflare",
                    _ => "error_request"
                };
                throw new CloudflareException(key, (int)response.StatusCode);
            }

            try { return ParseUsageResponse(text); }
            catch (CloudflareException) { throw; }
            catch (Exception) { throw new CloudflareException("error_response"); }
        }
    }

    public static long ParseUsageResponse(string text)
    {
        using var document = JsonDocument.Parse(text);
        var root = document.RootElement;
        if (root.TryGetProperty("errors", out var errors))
        {
            // Cloudflare returns `errors: null` on successful GraphQL requests.
            // Treat null, an omitted property, and an empty array as no error.
            if (errors.ValueKind is not (JsonValueKind.Null or JsonValueKind.Array))
                throw new CloudflareException("error_response");
            if (errors.ValueKind == JsonValueKind.Array && errors.GetArrayLength() > 0)
            {
                var first = errors[0];
                var message = first.ValueKind == JsonValueKind.Object &&
                              first.TryGetProperty("message", out var value)
                    ? value.GetString() ?? ""
                    : "";
                var key = message.Contains("unauthorized", StringComparison.OrdinalIgnoreCase)
                    ? "error_token_invalid"
                    : message.Contains("rate", StringComparison.OrdinalIgnoreCase)
                        ? "error_rate_limit" : "error_query";
                throw new CloudflareException(key);
            }
        }

        var accounts = root.GetProperty("data").GetProperty("viewer").GetProperty("accounts");
        if (accounts.ValueKind != JsonValueKind.Array) throw new CloudflareException("error_response");
        if (accounts.GetArrayLength() == 0) throw new CloudflareException("error_account_access");
        var rows = accounts[0].GetProperty("workersInvocationsAdaptive");
        if (rows.ValueKind != JsonValueKind.Array) throw new CloudflareException("error_response");
        long total = 0;
        foreach (var row in rows.EnumerateArray())
            if (row.GetProperty("sum").TryGetProperty("requests", out var requests))
                total += requests.GetInt64();
        return total;
    }
}

public sealed class CloudflareException : Exception
{
    public string ResourceKey { get; }
    public int? StatusCode { get; }
    public CloudflareException(string resourceKey, int? statusCode = null) : base(resourceKey)
    {
        ResourceKey = resourceKey;
        StatusCode = statusCode;
    }
}
