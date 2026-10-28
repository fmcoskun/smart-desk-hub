package com.iot.widget.data

import com.iot.widget.data.db.LogDao
import com.iot.widget.data.db.LogEntity
import com.iot.widget.domain.logs.DateRange
import com.iot.widget.domain.logs.LogEntry
import com.iot.widget.domain.logs.LogQuery
import com.iot.widget.domain.logs.LogRetention
import com.iot.widget.domain.logs.matches
import com.iot.widget.domain.logs.toMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** What the Logs list shows: up to [LogRetention.LIST_LIMIT] newest matches, and how many there are. */
data class LogPage(val entries: List<LogEntry>, val total: Int)

/** Data layer · the stored event log (spec §6.9). */
interface LogRepository {
    /** Live: emits again whenever a new entry is written. */
    fun observe(query: LogQuery): Flow<LogPage>
    suspend fun add(entry: LogEntry): Long
    suspend fun byId(id: Long): LogEntry?
    suspend fun prune(nowMillis: Long)
    suspend fun seedIfEmpty(entries: () -> List<LogEntry>)

    suspend fun addAll(entries: List<LogEntry>)

    /** Is there any entry older than [millis]? (Tells an older install that already has history.) */
    suspend fun hasEntriesBefore(millis: Long): Boolean

    /** Every match, uncapped (for the CSV export). */
    suspend fun export(query: LogQuery): List<LogEntry>
}

@Singleton
class RoomLogRepository @Inject constructor(
    private val dao: LogDao,
    private val clock: Clock,
) : LogRepository {

    override fun observe(query: LogQuery): Flow<LogPage> {
        val (from, to) = window(query.range)
        return dao.observe(from, to, query.sources.map { it.name }, query.levels.map { it.name }, query.deviceId)
            .map { rows ->
                val matches = rows.map { it.toDomain() }.filter { it.matches(query.text) }
                LogPage(matches.take(LogRetention.LIST_LIMIT), matches.size)
            }
    }

    override suspend fun export(query: LogQuery): List<LogEntry> {
        val (from, to) = window(query.range)
        return dao.get(from, to, query.sources.map { it.name }, query.levels.map { it.name }, query.deviceId)
            .map { it.toDomain() }
            .filter { it.matches(query.text) }
    }

    override suspend fun add(entry: LogEntry): Long = dao.insert(entry.toEntity())

    override suspend fun byId(id: Long): LogEntry? = dao.getById(id)?.toDomain()

    override suspend fun prune(nowMillis: Long) =
        dao.prune(cutoff = nowMillis - LogRetention.MAX_DAYS * 24 * 60 * 60_000L, keep = LogRetention.MAX_ENTRIES)

    override suspend fun seedIfEmpty(entries: () -> List<LogEntry>) = dao.seedIfEmpty { entries().map { it.toEntity() } }

    override suspend fun addAll(entries: List<LogEntry>) = dao.insertAll(entries.map { it.toEntity() })

    override suspend fun hasEntriesBefore(millis: Long): Boolean = dao.countBefore(millis) > 0

    /**
     * Ranges that end "now" (Today, Last 7 days) get no upper limit, so entries written while
     * the screen is open still show up live in the list.
     */
    private fun window(range: DateRange): Pair<Long, Long> {
        val r = range.toMillis(ZonedDateTime.now(clock))
        val openEnded = range == DateRange.Today || range == DateRange.Last7Days
        return r.first to (if (openEnded) Long.MAX_VALUE else r.last)
    }
}

private fun LogEntity.toDomain() = LogEntry(id, timeMillis, source, level, actor, deviceId, deviceName, message, topic, payload)

private fun LogEntry.toEntity() = LogEntity(id, timeMillis, source, level, actor, deviceId, deviceName, message, topic, payload)
