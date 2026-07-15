package com.cfquotamonitor.app.background

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import com.cfquotamonitor.app.settings.AppSettings

object BackgroundRefreshScheduler {
    private const val JOB_ID = 0xCF120
    private const val MIN_INTERVAL_MILLIS = 15L * 60L * 1000L

    fun apply(context: Context, settings: AppSettings) {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        if (!settings.backgroundRefreshEnabled) {
            scheduler.cancel(JOB_ID)
            return
        }
        val interval = (settings.refreshIntervalMinutes * 60L * 1000L)
            .coerceAtLeast(MIN_INTERVAL_MILLIS)
        val job = JobInfo.Builder(
            JOB_ID,
            ComponentName(context, BackgroundRefreshJobService::class.java),
        )
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setPeriodic(interval)
            .build()
        scheduler.schedule(job)
    }
}
