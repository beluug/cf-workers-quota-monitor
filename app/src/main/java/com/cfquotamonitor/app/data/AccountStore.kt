package com.cfquotamonitor.app.data

import android.content.Context
import com.cfquotamonitor.app.model.CfAccount
import com.cfquotamonitor.app.security.SecureTokenStore
import java.util.UUID

class AccountStore(context: Context) {
    private val prefs = context.getSharedPreferences("accounts", Context.MODE_PRIVATE)
    private val tokens = SecureTokenStore(context)

    fun loadAccounts(): List<CfAccount> = accountOrder().mapNotNull { id ->
        val accountId = prefs.getString("$id.account_id", null) ?: return@mapNotNull null
        CfAccount(
            localId = id,
            name = prefs.getString("$id.name", null).orEmpty().ifBlank { "Cloudflare 账号" },
            accountId = accountId,
            dailyLimit = prefs.getLong("$id.limit", DEFAULT_LIMIT).coerceAtLeast(1L),
        )
    }

    fun create(name: String, accountId: String, dailyLimit: Long, token: String): CfAccount {
        val account = CfAccount(
            localId = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Cloudflare 账号" },
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

    fun delete(localId: String) {
        val newOrder = accountOrder().filterNot { it == localId }
        prefs.edit()
            .putString("order", newOrder.joinToString("|"))
            .remove("$localId.name")
            .remove("$localId.account_id")
            .remove("$localId.limit")
            .apply()
        tokens.remove(localId)
    }

    private fun accountOrder(): List<String> = prefs.getString("order", "")
        .orEmpty()
        .split('|')
        .filter { it.isNotBlank() }

    private companion object {
        const val DEFAULT_LIMIT = 100_000L
    }
}
