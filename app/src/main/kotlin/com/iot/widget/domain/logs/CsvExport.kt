package com.iot.widget.domain.logs

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Domain layer · log entries as CSV (RFC 4180 style), ready for Excel or Google Sheets.
 *
 * - Starts with a UTF-8 byte-order mark: without it Excel guesses the wrong encoding and
 *   shows "ÅŸ" instead of "ş".
 * - A field containing a comma, a quote or a line break is wrapped in quotes, and quotes
 *   inside it are doubled (`say "hi"` → `"say ""hi"""`). JSON payloads need this a lot.
 * - A field that a spreadsheet would run as a formula gets a leading `'` (CSV injection).
 */
object CsvExport {
    private val BOM = Char(0xFEFF)
    private val HEADER = listOf("time", "source", "level", "actor", "device", "message", "topic", "payload")
    private val TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val FORMULA_START = setOf('=', '+', '-', '@', '\t', '\r')

    fun format(entries: List<LogEntry>, zone: ZoneId): String = buildString {
        append(BOM)
        append(HEADER.joinToString(",")).append('\n')
        entries.forEach { e ->
            val fields = listOf(
                TIME.format(Instant.ofEpochMilli(e.timeMillis).atZone(zone)),
                e.source.name,
                e.level.name,
                e.actor.orEmpty(),
                e.deviceName.orEmpty(),
                e.message,
                e.topic.orEmpty(),
                e.payload.orEmpty(),
            )
            append(fields.joinToString(",") { escape(it) }).append('\n')
        }
    }

    private fun escape(raw: String): String {
        // A cell starting with = + - @ (or tab/CR) is run as a formula by Excel and Sheets; a
        // device named "=HYPERLINK(…)" would become a live link. A leading ' makes it plain text.
        val field = if (raw.isNotEmpty() && raw[0] in FORMULA_START) "'$raw" else raw
        return if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }
}
