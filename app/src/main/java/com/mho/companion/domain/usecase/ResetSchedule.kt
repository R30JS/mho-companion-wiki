package com.mho.companion.domain.usecase

import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class ResetSnapshot(
    val dailyLeft: Duration,
    val weeklyLeft: Duration,
    val dailyAt: ZonedDateTime,
    val weeklyAt: ZonedDateTime
)

/**
 * MHO resets are assumed at 05:00 device-local time. Weekly reset follows on Monday 05:00.
 * Both values are intentionally constants so they are easy to adjust after a game update.
 */
object ResetSchedule {
    const val RESET_HOUR: Int = 5

    fun snapshot(now: ZonedDateTime = ZonedDateTime.now(), hour: Int = RESET_HOUR): ResetSnapshot {
        var daily = now.toLocalDate().atTime(hour, 0).atZone(now.zone)
        if (!daily.isAfter(now)) daily = daily.plusDays(1)

        val monday = now.toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atTime(hour, 0)
            .atZone(now.zone)
        var weekly = monday
        if (!weekly.isAfter(now)) weekly = weekly.plusWeeks(1)

        return ResetSnapshot(
            dailyLeft = Duration.between(now, daily),
            weeklyLeft = Duration.between(now, weekly),
            dailyAt = daily,
            weeklyAt = weekly
        )
    }
}

fun Duration.countdownText(): String {
    val total = seconds.coerceAtLeast(0)
    val days = total / 86_400
    val hours = (total % 86_400) / 3_600
    val minutes = (total % 3_600) / 60
    val secs = total % 60
    return if (days > 0) {
        String.format(Locale.US, "%dd %02d:%02d:%02d", days, hours, minutes, secs)
    } else {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
    }
}
