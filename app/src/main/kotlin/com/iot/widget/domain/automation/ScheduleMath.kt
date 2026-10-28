package com.iot.widget.domain.automation

import com.iot.widget.domain.ScheduleDays
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Domain layer · when does a daily schedule fire next?
 *
 * Returns the first moment strictly after [now] at wall-clock [time] on an allowed day.
 * ZonedDateTime.of() handles daylight-saving gaps: a 02:30 schedule on the night clocks jump
 * from 02:00 to 03:00 fires at 03:30, which is what a person would expect.
 */
fun nextOccurrence(now: ZonedDateTime, time: LocalTime, days: ScheduleDays): ZonedDateTime {
    var date = now.toLocalDate()
    repeat(8) { // at most a week ahead (+1 for "today but already passed")
        val candidate = ZonedDateTime.of(date, time, now.zone)
        if (candidate.isAfter(now) && days.includes(date.dayOfWeek)) return candidate
        date = date.plusDays(1)
    }
    error("unreachable: every ScheduleDays value contains at least one day per week")
}

private fun ScheduleDays.includes(day: DayOfWeek): Boolean = when (this) {
    ScheduleDays.EVERY_DAY -> true
    ScheduleDays.WEEKDAYS -> day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
}
