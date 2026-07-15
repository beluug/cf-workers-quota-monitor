package com.cfquotamonitor.app.backup

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.util.Base64

class BackupCompatibilityInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val result = Bundle()
        var stage = "windows-import"
        try {
            val password = "correct-horse-battery-staple"
            val expectedToken = "test_token_that_is_long_enough_but_not_real"
            check(CfqmBackupService.MIME_TYPE == "application/octet-stream")
            val windowsBytes = context.assets.open("windows-v1.cfqm").use { it.readBytes() }
            val windowsPayload = CfqmBackupService.import(windowsBytes, password)
            check(windowsPayload.sourcePlatform == "windows")
            check(windowsPayload.accounts.single().token == expectedToken)

            stage = "android-export"
            val androidBytes = CfqmBackupService.export(
                listOf(BackupAccount(
                    name = "Smoke test",
                    accountId = "0123456789abcdef0123456789abcdef",
                    dailyLimit = 100_000,
                    token = expectedToken,
                )),
                password,
            )
            stage = "android-round-trip"
            val roundTrip = CfqmBackupService.import(androidBytes, password)
            check(roundTrip.sourcePlatform == "android")
            check(roundTrip.accounts.single().token == expectedToken)

            stage = "wrong-password"
            try {
                CfqmBackupService.import(androidBytes, "wrong-password")
                error("Wrong password was accepted")
            } catch (error: BackupException) {
                check(error.reason == BackupError.WRONG_PASSWORD)
            }

            result.putString("result", "PASS")
            result.putString("androidVectorBase64", Base64.encodeToString(androidBytes, Base64.NO_WRAP))
            finish(Activity.RESULT_OK, result)
        } catch (error: Throwable) {
            val causes = generateSequence(error) { it.cause }
                .joinToString(" -> ") { "${it.javaClass.simpleName}: ${it.message}" }
            result.putString("result", "FAIL at $stage: $causes")
            finish(Activity.RESULT_CANCELED, result)
        }
    }
}
