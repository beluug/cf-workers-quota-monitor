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
            errorCallback?.invoke("身份验证已取消")
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
            .setTitle("解锁 CF额度监控")
            .setSubtitle("使用指纹、面容或锁屏密码验证")

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
                        1 -> "生物识别暂时不可用，请稍后重试"
                        7, 9 -> "验证尝试次数过多，请稍后再试或使用锁屏密码"
                        10, 13 -> "身份验证已取消"
                        11 -> "尚未录入指纹或面容，也未设置可用的锁屏验证"
                        12 -> "此设备没有可用的生物识别硬件"
                        else -> errString?.toString().orEmpty().ifBlank { "身份验证失败" }
                    }
                    errorCallback?.invoke(message)
                }
            }
        )
    }

    private fun launchDeviceCredential() {
        if (!keyguard.isDeviceSecure) {
            isAuthenticating = false
            errorCallback?.invoke("手机尚未设置锁屏密码，请先在系统设置中设置")
            return
        }
        val intent: Intent? = keyguard.createConfirmDeviceCredentialIntent(
            "解锁 CF额度监控",
            "验证身份后查看 Cloudflare 用量",
        )
        if (intent == null) {
            isAuthenticating = false
            errorCallback?.invoke("无法调用系统身份验证")
        } else {
            credentialLauncher.launch(intent)
        }
    }
}
