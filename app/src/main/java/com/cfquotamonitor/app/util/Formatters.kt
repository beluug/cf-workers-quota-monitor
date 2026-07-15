package com.cfquotamonitor.app.util

import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val integerFormatter = NumberFormat.getIntegerInstance(Locale.CHINA)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

fun formatCount(value: Long): String = integerFormatter.format(value)

fun formatFetchedTime(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(timeFormatter)

fun resetCountdown(now: Instant = Instant.now()): String {
    val tomorrowUtc = LocalDate.now(ZoneOffset.UTC).plusDays(1)
        .atStartOfDay().toInstant(ZoneOffset.UTC)
    val duration = Duration.between(now, tomorrowUtc).coerceAtLeast(Duration.ZERO)
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes()
    return "${hours}小时${minutes}分钟后重置"
}
