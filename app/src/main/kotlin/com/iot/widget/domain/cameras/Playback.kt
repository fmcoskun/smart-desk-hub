package com.iot.widget.domain.cameras

/** Domain layer · player arithmetic, kept out of the UI so it can be unit-tested. */
object Playback {
    const val SKIP_MS = 10_000L

    /** Position after a ±10 s jump, never before the start or past the end of the clip. */
    fun skip(positionMs: Long, deltaMs: Long, durationMs: Long): Long =
        (positionMs + deltaMs).coerceIn(0L, durationMs.coerceAtLeast(0L))

    /** "0:45", "1:00", "12:05" — whole seconds, rounded down like every player shows them. */
    fun formatDuration(ms: Long): String {
        val seconds = ms.coerceAtLeast(0) / 1_000
        return "%d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
    }
}
