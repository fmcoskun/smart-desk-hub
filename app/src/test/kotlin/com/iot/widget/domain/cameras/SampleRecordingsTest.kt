package com.iot.widget.domain.cameras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SampleRecordingsTest {

    // early in the morning: the hardest case for "never in the future"
    private val now = ZonedDateTime.of(2026, 10, 28, 0, 30, 0, 0, ZoneId.of("Europe/Istanbul"))

    @Test
    fun fourOnEachOfTheLastThreeLocalDays() {
        val afternoon = ZonedDateTime.of(2026, 10, 28, 17, 21, 0, 0, ZoneId.of("Europe/Istanbul"))
        val days = SampleRecordings.generate(afternoon).groupingBy {
            java.time.Instant.ofEpochMilli(it.startMillis).atZone(afternoon.zone).toLocalDate().dayOfMonth
        }.eachCount()
        assertEquals(mapOf(26 to 4, 27 to 4, 28 to 4), days)
        // just after midnight there is still something under "Today"
        val early = SampleRecordings.generate(now).count { it.startMillis >= now.toLocalDate().atStartOfDay(now.zone).toInstant().toEpochMilli() }
        assertEquals(4, early)
    }

    @Test
    fun sampleIsTwelveOverThreeDaysNeverFuture() {
        val list = SampleRecordings.generate(now)
        assertEquals(12, list.size)
        val from = now.toLocalDate().minusDays(2).atStartOfDay(now.zone).toInstant().toEpochMilli()
        val nowMillis = now.toInstant().toEpochMilli()
        assertTrue(list.all { it.startMillis >= from && it.startMillis + it.durationMs <= nowMillis })
        assertTrue(list.all { it.durationMs == Clips.RECORDINGS[it.clip] })
        assertEquals(RecordingEvent.entries.toSet(), list.map { it.event }.toSet())
        assertEquals(Clips.LIVE.keys, list.map { it.cameraId }.toSet())
        assertEquals(list, SampleRecordings.generate(now))
    }


    @Test
    fun recordingUsesItsCamerasClip() {
        SampleRecordings.generate(now).forEach { r ->
            assertTrue("${r.cameraId} plays ${r.clip}", r.clip in Clips.RECORDINGS_BY_CAMERA.getValue(r.cameraId))
        }
    }
}
