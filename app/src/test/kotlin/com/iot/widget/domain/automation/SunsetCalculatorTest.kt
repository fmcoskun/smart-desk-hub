package com.iot.widget.domain.automation

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SunsetCalculatorTest {

    private val ist = ZoneId.of("Europe/Istanbul")

    private fun assertNear(expected: LocalTime, date: LocalDate) {
        val actual = SunsetCalculator.sunset(date, ist).toLocalTime()
        val diff = Duration.between(expected, actual).abs()
        assertTrue("sunset $date was $actual, expected ≈ $expected", diff <= Duration.ofMinutes(3))
    }

    @Test
    fun istanbulSummerSolstice() = assertNear(LocalTime.of(20, 39), LocalDate.of(2026, 6, 21))

    // Istanbul has used UTC+3 all year since 2016, so the winter sunset is ~17:40 (not 16:40)
    @Test
    fun istanbulWinterSolstice() = assertNear(LocalTime.of(17, 40), LocalDate.of(2026, 12, 21))
}
