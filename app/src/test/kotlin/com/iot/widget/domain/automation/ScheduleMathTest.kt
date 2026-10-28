package com.iot.widget.domain.automation

import com.iot.widget.domain.ScheduleDays
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleMathTest {

    private val ist = ZoneId.of("Europe/Istanbul")
    private val at0830 = LocalTime.of(8, 30)

    // 2026-10-27 is a Tuesday; 2026-10-30 a Friday
    private fun t(day: Int, h: Int, m: Int = 0, zone: ZoneId = ist, month: Int = 10) =
        ZonedDateTime.of(2026, month, day, h, m, 0, 0, zone)

    @Test
    fun nextOccurrence_sameDayBeforeTime() =
        assertEquals(t(27, 8, 30), nextOccurrence(t(27, 7), at0830, ScheduleDays.WEEKDAYS))

    @Test
    fun nextOccurrence_afterTimeWeekday() =
        assertEquals(t(28, 8, 30), nextOccurrence(t(27, 9), at0830, ScheduleDays.WEEKDAYS))

    @Test
    fun nextOccurrence_fridayAfterTimeWeekdaysGoesToMonday() =
        assertEquals(ZonedDateTime.of(2026, 11, 2, 8, 30, 0, 0, ist), nextOccurrence(t(30, 9), at0830, ScheduleDays.WEEKDAYS))

    @Test
    fun nextOccurrence_everyDayRollsToSaturday() =
        assertEquals(t(31, 8, 30), nextOccurrence(t(30, 9), at0830, ScheduleDays.EVERY_DAY))

    @Test
    fun nextOccurrence_dstSpringForwardBerlin() {
        val berlin = ZoneId.of("Europe/Berlin")
        // 2026-03-29 clocks jump 02:00 → 03:00: a 02:30 schedule fires at 03:30 that day
        val next = nextOccurrence(t(29, 1, zone = berlin, month = 3), LocalTime.of(2, 30), ScheduleDays.EVERY_DAY)
        assertEquals(LocalTime.of(3, 30), next.toLocalTime())
        assertEquals(29, next.dayOfMonth)
    }

    @Test
    fun nextOccurrence_strictlyAfterNow() =
        assertEquals(t(28, 8, 30), nextOccurrence(t(27, 8, 30), at0830, ScheduleDays.EVERY_DAY))
}
