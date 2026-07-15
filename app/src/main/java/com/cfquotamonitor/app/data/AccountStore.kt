package com.cfquotamonitor.app.data

import android.content.Context
import com.cfquotamonitor.app.backup.BackupAccount
import com.cfquotamonitor.app.backup.DuplicateMode
import com.cfquotamonitor.app.backup.ImportResult
import com.cfquotamonitor.app.model.CfAccount
import com.cfquotamonitor.app.model.UsageSnapshot
import com.cfquotamonitor.app.security.SecureTokenStore
import java.util.UUID

class AccountStore(context: Context) {
    private val prefs = context.getSharedPreferences("accounts", Context.MODE_PRIVATE)
    private val tokens = SecureTokenStore(context)

    fun loadAccounts(): List<CfAccount> = accountOrder().mapNotNull { id ->
        val accountId = prefs.getString("$id.account_id", null) ?: return@mapNotNull null
        CfAccount(
            localId = id,
            name = prefs.getString("$id.name", null).orEmpty().ifBlank { "Cloudflare" },
            accountId = accountId,
            dailyLimit = prefs.getLong("$id.limit", DEFAULT_LIMIT).coerceAtLeast(1L),
        )
    }

    fun create(name: String, accountId: String, dailyLimit: Long, token: String): CfAccount {
        val account = CfAccount(
            localId = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Cloudflare" },
            accountId = accountId.trim(),
            dailyLimit = dailyLimit.coerceAtLeast(1L),
        )
        val order = accountOrder() + account.localId
        prefs.edit()
            .putString("order", order.joinToString("|"))
            .putString("${account.localId}.name", account.name)
            .putString("${account.localId}.account_id", account.accountId)
            .putLong("${account.localId}.limit", account.dailyLimit)
            .apply()
        tokens.put(account.localId, token.trim())
        return account
    }

    fun update(account: CfAccount, newToken: String?) {
        prefs.edit()
            .putString("${account.localId}.name", account.name.trim())
            .putString("${account.localId}.account_id", account.accountId.trim())
            .putLong("${account.localId}.limit", account.dailyLimit.coerceAtLeast(1L))
            .apply()
        if (!newToken.isNullOrBlank()) tokens.put(account.localId, newToken.trim())
    }

    fun tokenFor(localId: String): String? = tokens.get(localId)

    fun loadUsage(localId: String): UsageSnapshot? {
        val fetchedAt = prefs.getLong("$localId.usage_fetched_at", 0L)
        if (fetchedAt <= 0L) return null
        return UsageSnapshot(
            used = prefs.getLong("$localId.usage_used", 0L).coerceAtLeast(0L),
            fetchedAtEpochMillis = fetchedAt,
        )
    }

    fun saveUsage(localId: String, usage: UsageSnapshot) {
        prefs.edit()
            .putLong("$localId.usage_used", usage.used.coerceAtLeast(0L))
            .putLong("$localId.usage_fetched_at", usage.fetchedAtEpochMillis)
            .apply()
    }

    fun delete(localId: String) {
        val newOrder = accountOrder().filterNot { it == localId }
        prefs.edit()
            .putString("order", newOrder.joinToString("|"))
            .remove("$localId.name")
            .remove("$localId.account_id")
            .remove("$localId.limit")
            .remove("$localId.usage_used")
            .remove("$localId.usage_fetched_at")
            .apply()
        tokens.remove(localId)
    }

    fun importAccounts(incoming: List<BackupAccount>, mode: DuplicateMode): ImportResult {
        val known = loadAccounts().toMutableList()
        var imported = 0
        var skipped = 0
        incoming.forEach { item ->
            val existing = known.firstOrNull { it.accountId.equals(item.accountId, ignoreCase = true) }
            when {
                existing != null && mode == DuplicateMode.SKIP -> skipped++
                existing != null && mode == DuplicateMode.REPLACE -> {
                    val updated = existing.copy(
                        name = item.name,
                        accountId = item.accountId,
                        dailyLimit = item.dailyLimit,
                    )
                    update(updated, item.token)
                    prefs.edit()
                        .remove("${existing.localId}.usage_used")
                        .remove("${existing.localId}.usage_fetched_at")
                        .apply()
                    known[known.indexOf(existing)] = updated
                    imported++
                }
                else -> {
                    known += create(item.name, item.accountId, item.dailyLimit, item.token)
                    imported++
                }
            }
        }
        return ImportResult(imported, skipped)
    }

    private fun accountOrder(): List<String> = prefs.getString("order", "")
        .orEmpty()
        .split('|')
        .filter { it.isNotBlank() }

    private companion object {
        const val DEFAULT_LIMIT = 100_000L
    }
}
