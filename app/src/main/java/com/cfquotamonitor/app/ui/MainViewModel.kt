package com.cfquotamonitor.app.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cfquotamonitor.app.R
import com.cfquotamonitor.app.background.BackgroundRefreshScheduler
import com.cfquotamonitor.app.data.AccountStore
import com.cfquotamonitor.app.model.AccountUiState
import com.cfquotamonitor.app.model.CfAccount
import com.cfquotamonitor.app.network.CloudflareClient
import com.cfquotamonitor.app.settings.AppSettings
import com.cfquotamonitor.app.settings.SettingsStore
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
    private val settingsStore = SettingsStore(application)
    private var loaded = false

    private val _accounts = MutableStateFlow<List<AccountUiState>>(emptyList())
    val accounts: StateFlow<List<AccountUiState>> = _accounts.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun onAppUnlocked() {
        if (!loaded) {
            _accounts.value = store.loadAccounts().map { account ->
                AccountUiState(account = account, usage = store.loadUsage(account.localId))
            }
            loaded = true
        }
        _settings.value = settingsStore.load()
        BackgroundRefreshScheduler.apply(getApplication(), _settings.value)
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

    fun setLockEnabled(enabled: Boolean) {
        settingsStore.setLockEnabled(enabled)
        _settings.value = settingsStore.load()
    }

    fun setLanguage(tag: String) {
        settingsStore.setLanguage(tag)
        _settings.value = settingsStore.load()
    }

    fun setBackgroundRefresh(enabled: Boolean) {
        settingsStore.setBackgroundRefresh(enabled)
        _settings.value = settingsStore.load()
    }

    fun setRefreshInterval(minutes: Long) {
        settingsStore.setRefreshInterval(minutes)
        _settings.value = settingsStore.load()
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
            return text(R.string.validation_account_id)
        }
        val limit = limitText.filter { it.isDigit() }.toLongOrNull()
            ?: return text(R.string.validation_daily_limit)
        if (limit <= 0L) return text(R.string.validation_limit_positive)
        if (existing == null && token.trim().length < 20) {
            return text(R.string.validation_api_token)
        }

        return runCatching {
            val normalizedName = name.trim().ifBlank { text(R.string.default_account_name) }
            if (existing == null) {
                val account = store.create(normalizedName, normalizedId, limit, token)
                _accounts.value = _accounts.value + AccountUiState(account, isLoading = true)
                refreshOne(account.localId)
            } else {
                val updated = existing.copy(
                    name = normalizedName,
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
        }.getOrElse { it.message ?: text(R.string.error_save_failed) }
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
            ?: return state.copy(isLoading = false, error = text(R.string.error_token_unreadable))
        return runCatching {
            val context = SettingsStore.localizedContext(getApplication())
            val usage = CloudflareClient(context)
                .fetchTodayUsage(state.account.accountId, token)
            store.saveUsage(state.account.localId, usage)
            state.copy(usage = usage, isLoading = false, error = null)
        }.getOrElse { error ->
            state.copy(
                isLoading = false,
                error = error.message ?: text(R.string.error_network),
            )
        }
    }

    private fun text(@StringRes id: Int): String =
        SettingsStore.localizedContext(getApplication<Application>()).getString(id)
}
