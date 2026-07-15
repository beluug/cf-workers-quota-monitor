package com.cfquotamonitor.app.model

data class CfAccount(
    val localId: String,
    val name: String,
    val accountId: String,
    val dailyLimit: Long = 100_000L,
)

data class UsageSnapshot(
    val used: Long,
    val fetchedAtEpochMillis: Long,
)

data class AccountUiState(
    val account: CfAccount,
    val usage: UsageSnapshot? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val remaining: Long
        get() = (account.dailyLimit - (usage?.used ?: 0L)).coerceAtLeast(0L)

    val progress: Float
        get() = if (account.dailyLimit <= 0L) 0f else
            ((usage?.used ?: 0L).toDouble() / account.dailyLimit.toDouble())
                .coerceIn(0.0, 1.0).toFloat()
}
