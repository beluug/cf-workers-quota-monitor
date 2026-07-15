package com.cfquotamonitor.app.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.cfquotamonitor.app.R

class AppAuthenticator(private val activity: ComponentActivity) {
    var isAuthenticating: Boolean = false
        private set

    private var successCallback: (() -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    private val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val credentialLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isAuthenticating = false
        if (result.resultCode == Activity.RESULT_OK) {
            successCallback?.invoke()
        } else {
            errorCallback?.invoke(activity.getString(R.string.auth_cancelled))
        }
    }

    fun authenticate(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (isAuthenticating) return
        successCallback = onSuccess
        errorCallback = onError
        isAuthenticating = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showModernBiometricPrompt()
        } else {
            launchDeviceCredential()
        }
    }

    private fun showModernBiometricPrompt() {
        val builder = BiometricPrompt.Builder(activity)
            .setTitle(activity.getString(R.string.auth_title))
            .setSubtitle(activity.getString(R.string.auth_subtitle))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }

        val prompt = builder.build()
        prompt.authenticate(
            CancellationSignal(),
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    isAuthenticating = false
                    successCallback?.invoke()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    isAuthenticating = false
                    if ((errorCode == 11 || errorCode == 12) && keyguard.isDeviceSecure) {
                        isAuthenticating = true
                        launchDeviceCredential()
                        return
                    }
                    val message = when (errorCode) {
                        1 -> activity.getString(R.string.auth_unavailable)
                        7, 9 -> activity.getString(R.string.auth_too_many_attempts)
                        10, 13 -> activity.getString(R.string.auth_cancelled)
                        11 -> activity.getString(R.string.auth_not_enrolled)
                        12 -> activity.getString(R.string.auth_no_hardware)
                        else -> errString?.toString().orEmpty().ifBlank {
                            activity.getString(R.string.auth_failed)
                        }
                    }
                    errorCallback?.invoke(message)
                }
            }
        )
    }

    private fun launchDeviceCredential() {
        if (!keyguard.isDeviceSecure) {
            isAuthenticating = false
            errorCallback?.invoke(activity.getString(R.string.auth_no_device_lock))
            return
        }
        val intent: Intent? = keyguard.createConfirmDeviceCredentialIntent(
            activity.getString(R.string.auth_title),
            activity.getString(R.string.auth_credential_description),
        )
        if (intent == null) {
            isAuthenticating = false
            errorCallback?.invoke(activity.getString(R.string.auth_launch_failed))
        } else {
            credentialLauncher.launch(intent)
        }
    }
}
