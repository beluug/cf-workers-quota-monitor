package com.cfquotamonitor.app.background

import android.app.job.JobParameters
import android.app.job.JobService
import com.cfquotamonitor.app.data.AccountStore
import com.cfquotamonitor.app.network.CloudflareClient
import com.cfquotamonitor.app.settings.SettingsStore
import java.util.concurrent.Executors

class BackgroundRefreshJobService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var stopped = false

    override fun onStartJob(params: JobParameters): Boolean {
        stopped = false
        executor.execute {
            val store = AccountStore(applicationContext)
            val client = CloudflareClient(SettingsStore.localizedContext(applicationContext))
            store.loadAccounts().forEach { account ->
                if (stopped) return@forEach
                val token = store.tokenFor(account.localId) ?: return@forEach
                runCatching { client.fetchTodayUsage(account.accountId, token) }
                    .onSuccess { store.saveUsage(account.localId, it) }
            }
            if (!stopped) jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        stopped = true
        return true
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
