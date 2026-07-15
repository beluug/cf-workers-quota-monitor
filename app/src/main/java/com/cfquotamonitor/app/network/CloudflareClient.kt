package com.cfquotamonitor.app.network

import android.content.Context
import com.cfquotamonitor.app.R
import com.cfquotamonitor.app.model.UsageSnapshot
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CloudflareClient(private val context: Context) {
    fun fetchTodayUsage(accountId: String, token: String): UsageSnapshot {
        val now = Instant.now()
        val start = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC)
        val body = JSONObject()
            .put("query", QUERY)
            .put(
                "variables",
                JSONObject()
                    .put("accountTag", accountId)
                    .put("datetimeStart", start.toString())
                    .put("datetimeEnd", now.toString())
            )
            .toString()

        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "CFQuotaMonitor-Android/1.3.1")
        }

        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw CloudflareException(httpError(status, response))

            val root = JSONObject(response)
            val errors = root.optJSONArray("errors")
            if (errors != null && errors.length() > 0) {
                val message = errors.optJSONObject(0)?.optString("message").orEmpty()
                throw CloudflareException(friendlyGraphQlError(message))
            }

            val accounts = root.optJSONObject("data")
                ?.optJSONObject("viewer")
                ?.optJSONArray("accounts")
                ?: throw CloudflareException(context.getString(R.string.error_bad_response))
            if (accounts.length() == 0) {
                throw CloudflareException(context.getString(R.string.error_account_access))
            }

            val rows = accounts.optJSONObject(0)?.optJSONArray("workersInvocationsAdaptive")
                ?: throw CloudflareException(context.getString(R.string.error_no_usage))
            var requests = 0L
            for (index in 0 until rows.length()) {
                requests += rows.optJSONObject(index)?.optJSONObject("sum")
                    ?.optLong("requests", 0L) ?: 0L
            }
            return UsageSnapshot(requests, System.currentTimeMillis())
        } finally {
            connection.disconnect()
        }
    }

    private fun httpError(status: Int, response: String): String = when (status) {
        401 -> context.getString(R.string.error_token_invalid)
        403 -> context.getString(R.string.error_permission)
        429 -> context.getString(R.string.error_rate_limit)
        in 500..599 -> context.getString(R.string.error_cloudflare_status, status)
        else -> runCatching {
            JSONObject(response).optJSONArray("errors")?.optJSONObject(0)?.optString("message")
        }.getOrNull().orEmpty().ifBlank { context.getString(R.string.error_http_status, status) }
    }

    private fun friendlyGraphQlError(message: String): String = when {
        message.contains("unauthorized", ignoreCase = true) -> context.getString(R.string.error_unauthorized)
        message.contains("rate", ignoreCase = true) -> context.getString(R.string.error_rate_limit)
        else -> message.ifBlank { context.getString(R.string.error_query_failed) }
    }

    private companion object {
        const val ENDPOINT = "https://api.cloudflare.com/client/v4/graphql"
        const val QUERY = """
            query WorkerUsage(
              ${'$'}accountTag: string,
              ${'$'}datetimeStart: string,
              ${'$'}datetimeEnd: string
            ) {
              viewer {
                accounts(filter: {accountTag: ${'$'}accountTag}) {
                  workersInvocationsAdaptive(
                    limit: 100,
                    filter: {
                      datetime_geq: ${'$'}datetimeStart,
                      datetime_leq: ${'$'}datetimeEnd
                    }
                  ) {
                    sum { requests }
                  }
                }
              }
            }
        """
    }
}

class CloudflareException(message: String) : Exception(message)
