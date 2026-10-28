package com.iot.widget.domain.logs

import java.time.LocalDate

/**
 * Domain layer · one line of the event log (spec §6.9). Two sources:
 *  - CLIENT: something a person (or the app itself: "System", "Scheduler") did,
 *  - DEVICE: something a (simulated) device reported.
 */
enum class LogSource { CLIENT, DEVICE }

/** How serious a log line is. */
enum class LogLevel { INFO, WARN, ERROR }

data class LogEntry(
    /** Database id; 0 for a new entry (Room assigns one). */
    val id: Long,
    val timeMillis: Long,
    val source: LogSource,
    val level: LogLevel,
    /** Who did it (CLIENT); null for DEVICE entries. */
    val actor: String?,
    val deviceId: String?,
    val deviceName: String?,
    val message: String,
    /** MQTT topic and JSON payload, when the entry is about a message. */
    val topic: String?,
    val payload: String?,
)

/** A period of local calendar days in the phone's time zone. */
sealed interface DateRange {
    data object Today : DateRange
    data object Yesterday : DateRange
    data object Last7Days : DateRange
    data class Custom(val from: LocalDate, val to: LocalDate) : DateRange
}

/** Everything the Logs screen can filter by. Defaults = everything from the last 7 days. */
data class LogQuery(
    val text: String = "",
    val sources: Set<LogSource> = LogSource.entries.toSet(),
    val levels: Set<LogLevel> = LogLevel.entries.toSet(),
    val deviceId: String? = null,
    val range: DateRange = DateRange.Last7Days,
)
