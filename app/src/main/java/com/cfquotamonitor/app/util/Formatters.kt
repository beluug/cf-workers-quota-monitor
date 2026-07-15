package com.cfquotamonitor.app.util

import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatCount(value: Long): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

fun formatFetchedTime(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))

fun resetCountdownParts(now: Instant = Instant.now()): Pair<Long, Long> {
    val tomorrowUtc = LocalDate.now(ZoneOffset.UTC).plusDays(1)
        .atStartOfDay().toInstant(ZoneOffset.UTC)
    val duration = Duration.between(now, tomorrowUtc).coerceAtLeast(Duration.ZERO)
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes()
    return hours to minutes
}
