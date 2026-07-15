package com.cfquotamonitor.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cfquotamonitor.app.data.AccountStore
import com.cfquotamonitor.app.model.AccountUiState
import com.cfquotamonitor.app.model.CfAccount
import com.cfquotamonitor.app.network.CloudflareClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AccountStore(application)
    private val client = CloudflareClient()
    private var loaded = false

    private val _accounts = MutableStateFlow<List<AccountUiState>>(emptyList())
    val accounts: StateFlow<List<AccountUiState>> = _accounts.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun onAppUnlocked() {
        if (!loaded) {
            _accounts.value = store.loadAccounts().map(::AccountUiState)
            loaded = true
        }
        refreshAll()
    }

    fun refreshAll() {
        val current = _accounts.value
        if (current.isEmpty() || _isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            _accounts.value = current.map { it.copy(isLoading = true, error = null) }
            val refreshed = supervisorScope {
                current.map { state ->
                    async(Dispatchers.IO) { refreshState(state) }
                }.awaitAll()
            }
            _accounts.value = refreshed
            _isRefreshing.value = false
        }
    }

    fun saveAccount(
        existing: CfAccount?,
        name: String,
        accountId: String,
        limitText: String,
        token: String,
    ): String? {
        val normalizedId = accountId.trim().lowercase()
        if (!normalizedId.matches(Regex("^[a-f0-9]{32}${'$'}"))) {
            return "Account ID 应为 32 位字母和数字"
        }
        val limit = limitText.filter { it.isDigit() }.toLongOrNull()
            ?: return "请输入有效的每日额度"
        if (limit <= 0L) return "每日额度必须大于 0"
        if (existing == null && token.trim().length < 20) return "请填写 Cloudflare API Token"

        return runCatching {
            if (existing == null) {
                val account = store.create(name, normalizedId, limit, token)
                _accounts.value = _accounts.value + AccountUiState(account, isLoading = true)
                refreshOne(account.localId)
            } else {
                val updated = existing.copy(
                    name = name.trim().ifBlank { "Cloudflare 账号" },
                    accountId = normalizedId,
                    dailyLimit = limit,
                )
                store.update(updated, token.ifBlank { null })
                _accounts.value = _accounts.value.map {
                    if (it.account.localId == updated.localId) {
                        it.copy(account = updated, isLoading = true, error = null)
                    } else it
                }
                refreshOne(updated.localId)
            }
            null
        }.getOrElse { it.message ?: "保存失败" }
    }

    fun deleteAccount(localId: String) {
        store.delete(localId)
        _accounts.value = _accounts.value.filterNot { it.account.localId == localId }
    }

    private fun refreshOne(localId: String) {
        val target = _accounts.value.firstOrNull { it.account.localId == localId } ?: return
        viewModelScope.launch {
            val refreshed = withContext(Dispatchers.IO) { refreshState(target) }
            _accounts.value = _accounts.value.map {
                if (it.account.localId == localId) refreshed else it
            }
        }
    }

    private fun refreshState(state: AccountUiState): AccountUiState {
        val token = store.tokenFor(state.account.localId)
            ?: return state.copy(isLoading = false, error = "本机加密 Token 无法读取，请编辑账号后重新填写")
        return runCatching {
            val usage = client.fetchTodayUsage(state.account.accountId, token)
            state.copy(usage = usage, isLoading = false, error = null)
        }.getOrElse { error ->
            state.copy(
                isLoading = false,
                error = error.message ?: "网络连接失败，请检查网络后重试",
            )
        }
    }
}
