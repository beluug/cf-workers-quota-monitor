package com.cfquotamonitor.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cfquotamonitor.app.security.AppAuthenticator
import com.cfquotamonitor.app.settings.SettingsStore
import com.cfquotamonitor.app.ui.CfQuotaApp
import com.cfquotamonitor.app.ui.LockScreen
import com.cfquotamonitor.app.ui.MainViewModel
import com.cfquotamonitor.app.ui.theme.CfQuotaTheme

class MainActivity : ComponentActivity() {
    private lateinit var authenticator: AppAuthenticator
    private lateinit var settingsStore: SettingsStore
    private var unlocked by mutableStateOf(true)
    private var authMessage by mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SettingsStore.localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authenticator = AppAuthenticator(this)
        settingsStore = SettingsStore(this)
        val skipLockOnce = intent.getBooleanExtra(EXTRA_SKIP_LOCK_ONCE, false)
        intent.removeExtra(EXTRA_SKIP_LOCK_ONCE)
        unlocked = skipLockOnce || !settingsStore.load().lockEnabled

        setContent {
            CfQuotaTheme {
                if (unlocked) {
                    val viewModel: MainViewModel = viewModel()
                    LaunchedEffect(Unit) { viewModel.onAppUnlocked() }
                    CfQuotaApp(
                        viewModel = viewModel,
                        onLanguageChanged = ::recreateForLanguageChange,
                    )
                } else {
                    LockScreen(
                        message = authMessage,
                        onUnlock = ::requestUnlock,
                    )
                    LaunchedEffect(Unit) { requestUnlock() }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations &&
            !authenticator.isAuthenticating &&
            settingsStore.load().lockEnabled
        ) {
            unlocked = false
        }
    }

    private fun requestUnlock() {
        authenticator.authenticate(
            onSuccess = {
                authMessage = null
                unlocked = true
            },
            onError = { message -> authMessage = message },
        )
    }

    private fun recreateForLanguageChange() {
        intent.putExtra(EXTRA_SKIP_LOCK_ONCE, true)
        recreate()
    }

    private companion object {
        const val EXTRA_SKIP_LOCK_ONCE = "skip_lock_once"
    }
}
