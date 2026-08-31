package com.salespro.workers

import com.salespro.data.StockReminderType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class StockReminderSchedulerTest {

    private val scheduler = StockReminderScheduler(
        context = null,
        zoneId = ZoneId.of("UTC")
    )

    private val testZone = ZoneId.of("UTC")

    @Test
    fun `UPDATE_TODAY at 12_59 PM schedules for same-day 1_00 PM`() {
        val now = ZonedDateTime.of(2023, 10, 10, 12, 59, 0, 0, testZone)
        val expected = ZonedDateTime.of(2023, 10, 10, 13, 0, 0, 0, testZone)
        val expectedDelay = ChronoUnit.MILLIS.between(now, expected)
        val actualDelay = scheduler.calculateDelayMillis(now, StockReminderType.UPDATE_TODAY)
        assertEquals(expectedDelay, actualDelay)
    }

    @Test
    fun `UPDATE_TODAY at 1_00 PM schedules for same-day 7_00 PM`() {
        val now = ZonedDateTime.of(2023, 10, 10, 13, 0, 0, 0, testZone)
        val expected = ZonedDateTime.of(2023, 10, 10, 19, 0, 0, 0, testZone)
        val expectedDelay = ChronoUnit.MILLIS.between(now, expected)
        val actualDelay = scheduler.calculateDelayMillis(now, StockReminderType.UPDATE_TODAY)
        assertEquals(expectedDelay, actualDelay)
    }

    @Test
    fun `UPDATE_TODAY at 6_59 PM schedules for same-day 7_00 PM`() {
        val now = ZonedDateTime.of(2023, 10, 10, 18, 59, 0, 0, testZone)
        val expected = ZonedDateTime.of(2023, 10, 10, 19, 0, 0, 0, testZone)
        val expectedDelay = ChronoUnit.MILLIS.between(now, expected)
        val actualDelay = scheduler.calculateDelayMillis(now, StockReminderType.UPDATE_TODAY)
        assertEquals(expectedDelay, actualDelay)
    }

    @Test
    fun `UPDATE_TODAY at 7_00 PM schedules for next-day 1_00 PM`() {
        val now = ZonedDateTime.of(2023, 10, 10, 19, 0, 0, 0, testZone)
        val expected = ZonedDateTime.of(2023, 10, 11, 13, 0, 0, 0, testZone)
        val expectedDelay = ChronoUnit.MILLIS.between(now, expected)
        val actualDelay = scheduler.calculateDelayMillis(now, StockReminderType.UPDATE_TODAY)
        assertEquals(expectedDelay, actualDelay)
    }

    @Test
    fun `UPDATE_TODAY at 11_59 PM schedules for next-day 1_00 PM`() {
        val now = ZonedDateTime.of(2023, 10, 10, 23, 59, 0, 0, testZone)
        val expected = ZonedDateTime.of(2023, 10, 11, 13, 0, 0, 0, testZone)
        val expectedDelay = ChronoUnit.MILLIS.between(now, expected)
        val actualDelay = scheduler.calculateDelayMillis(now, StockReminderType.UPDATE_TODAY)
        assertEquals(expectedDelay, actualDelay)
    }

    @Test
    fun `TONIGHT before 7_00 PM schedules for same-day 7_00 PM`() {
        val now = ZonedDateTime.of(2023, 10, 10, 15, 0, 0, 0, testZone)
        val expected = ZonedDateTime.of(2023, 10, 10, 19, 0, 0, 0, testZone)
        val expectedDelay = ChronoUnit.MILLIS.between(now, expected)
        val actualDelay = scheduler.calculateDelayMillis(now, StockReminderType.TONIGHT)
        assertEquals(expectedDelay, actualDelay)
    }

    @Test
    fun `TONIGHT after 7_00 PM schedules for next-day 7_00 PM`() {
        val now = ZonedDateTime.of(2023, 10, 10, 20, 0, 0, 0, testZone)
        val expected = ZonedDateTime.of(2023, 10, 11, 19, 0, 0, 0, testZone)
        val expectedDelay = ChronoUnit.MILLIS.between(now, expected)
        val actualDelay = scheduler.calculateDelayMillis(now, StockReminderType.TONIGHT)
        assertEquals(expectedDelay, actualDelay)
    }

    @Test
    fun `NOT_NEEDED returns zero delay instantly canceling`() {
        val now = ZonedDateTime.of(2023, 10, 10, 12, 0, 0, 0, testZone)
        val actualDelay = scheduler.calculateDelayMillis(now, StockReminderType.NOT_NEEDED)
        assertEquals(0L, actualDelay)
    }
}
