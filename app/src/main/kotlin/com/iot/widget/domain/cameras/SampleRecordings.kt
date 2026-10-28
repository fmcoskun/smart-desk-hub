package com.iot.widget.domain.cameras

import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * Domain layer · demo recordings (spec §6.7: ~12 over the last 3 days): four on each of the
 * last three local days, so Today, Yesterday and the day before all have something to show.
 * A fixed seed makes them the same on every phone and in tests. Today's ones end before
 * "now"; if it is too soon after midnight for even one clip, they move to yesterday.
 */
object SampleRecordings {
    const val PER_DAY = 4
    const val DAYS = 3

    fun generate(now: ZonedDateTime): List<Recording> {
        val random = Random(2026)
        val cameras = Clips.LIVE.keys.toList()
        val nowMillis = now.toInstant().toEpochMilli()
        fun midnight(daysAgo: Long) = now.toLocalDate().minusDays(daysAgo).atStartOfDay(now.zone).toInstant().toEpochMilli()

        val result = mutableListOf<Recording>()
        for (daysAgo in (DAYS - 1).toLong() downTo 0L) {
            repeat(PER_DAY) {
                val i = result.size
                val camera = cameras[i % cameras.size] // every camera appears, in turn
                val clips = Clips.RECORDINGS_BY_CAMERA.getValue(camera)
                val clip = clips[random.nextInt(clips.size)]
                val duration = Clips.RECORDINGS.getValue(clip)
                var from = midnight(daysAgo)
                var latest = minOf(midnight(daysAgo - 1), nowMillis) - duration // the clip must end within the day and before now
                if (latest <= from) { // e.g. 00:00:20 today: no room yet, use yesterday
                    from = midnight(daysAgo + 1)
                    latest = midnight(daysAgo) - duration
                }
                val start = from + random.nextLong(latest - from)
                // both event types appear: every third one is a PERSON
                result += Recording(0, camera, start, duration, if (i % 3 == 0) RecordingEvent.PERSON else RecordingEvent.MOTION, clip)
            }
        }
        return result.sortedBy { it.startMillis }
    }
}
