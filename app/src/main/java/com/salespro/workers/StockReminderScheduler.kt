package com.salespro.workers

import android.content.Context
import androidx.work.*
import com.salespro.data.StockReminderType
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class StockReminderScheduler(
    private val context: Context?,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun scheduleReminder(eventId: String, type: StockReminderType) {
        if (context == null) return
        val workManager = WorkManager.getInstance(context)
        val workName = "stock-reminder-$eventId"

        if (type == StockReminderType.NOT_NEEDED) {
            workManager.cancelUniqueWork(workName)
            return
        }

        val now = ZonedDateTime.now(clock).withZoneSameInstant(zoneId)
        val delayMillis = calculateDelayMillis(now, type)

        val request = OneTimeWorkRequestBuilder<StockReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun calculateDelayMillis(now: ZonedDateTime, type: StockReminderType): Long {
        val targetDateTime: ZonedDateTime = when (type) {
            StockReminderType.UPDATE_TODAY -> calculateUpdateToday(now)
            StockReminderType.TONIGHT -> calculateTonight(now)
            StockReminderType.NOT_NEEDED -> return 0L
        }
        val diff = ChronoUnit.MILLIS.between(now, targetDateTime)
        return diff.coerceAtLeast(0L)
    }

    private fun calculateUpdateToday(now: ZonedDateTime): ZonedDateTime {
        val onePM = LocalTime.of(13, 0)
        val sevenPM = LocalTime.of(19, 0)

        return when {
            now.toLocalTime().isBefore(onePM) -> now.with(onePM)
            now.toLocalTime().isBefore(sevenPM) -> now.with(sevenPM)
            else -> now.plusDays(1).with(onePM)
        }
    }

    private fun calculateTonight(now: ZonedDateTime): ZonedDateTime {
        val sevenPM = LocalTime.of(19, 0)
        return if (now.toLocalTime().isBefore(sevenPM)) {
            now.with(sevenPM)
        } else {
            now.plusDays(1).with(sevenPM)
        }
    }
}
