package com.cfquotamonitor.app.network

import com.cfquotamonitor.app.model.UsageSnapshot
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CloudflareClient {
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
            setRequestProperty("User-Agent", "CFQuotaMonitor-Android/1.0")
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
                ?: throw CloudflareException("Cloudflare 返回的数据格式异常")
            if (accounts.length() == 0) {
                throw CloudflareException("Token 无权访问该 Account ID")
            }

            val rows = accounts.optJSONObject(0)?.optJSONArray("workersInvocationsAdaptive")
                ?: throw CloudflareException("未获取到 Workers 用量数据")
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
        401 -> "Token 无效或已经过期"
        403 -> "权限不足，请授予 Account Analytics Read"
        429 -> "Cloudflare 查询过于频繁，请稍后再试"
        in 500..599 -> "Cloudflare 服务暂时异常（$status）"
        else -> runCatching {
            JSONObject(response).optJSONArray("errors")?.optJSONObject(0)?.optString("message")
        }.getOrNull().orEmpty().ifBlank { "请求失败（HTTP $status）" }
    }

    private fun friendlyGraphQlError(message: String): String = when {
        message.contains("unauthorized", ignoreCase = true) -> "Token 无效或没有访问权限"
        message.contains("rate", ignoreCase = true) -> "查询过于频繁，请稍后再试"
        else -> message.ifBlank { "Cloudflare 查询失败" }
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
