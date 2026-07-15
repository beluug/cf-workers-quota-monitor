package com.cfquotamonitor.app

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
import com.cfquotamonitor.app.ui.CfQuotaApp
import com.cfquotamonitor.app.ui.LockScreen
import com.cfquotamonitor.app.ui.MainViewModel
import com.cfquotamonitor.app.ui.theme.CfQuotaTheme

class MainActivity : ComponentActivity() {
    private lateinit var authenticator: AppAuthenticator
    private var unlocked by mutableStateOf(false)
    private var authMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authenticator = AppAuthenticator(this)

        setContent {
            CfQuotaTheme {
                if (unlocked) {
                    val viewModel: MainViewModel = viewModel()
                    LaunchedEffect(Unit) { viewModel.onAppUnlocked() }
                    CfQuotaApp(viewModel)
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
        if (!isChangingConfigurations && !authenticator.isAuthenticating) {
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
}
