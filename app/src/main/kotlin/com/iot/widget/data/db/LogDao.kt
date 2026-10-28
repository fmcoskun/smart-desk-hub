package com.iot.widget.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data layer · SQL for the event log. Source, level, device and time are filtered here in
 * SQL; the free-text search runs afterwards in Kotlin (SQLite's LIKE cannot fold Turkish ı/i).
 * `IN (:sources)` with a list parameter: Room expands it to the right number of `?`s.
 */
@Dao
abstract class LogDao {

    @Query(
        "SELECT * FROM logs WHERE timeMillis BETWEEN :from AND :to AND source IN (:sources) AND level IN (:levels) " +
            "AND (:deviceId IS NULL OR deviceId = :deviceId) ORDER BY timeMillis DESC, id DESC",
    )
    abstract fun observe(from: Long, to: Long, sources: List<String>, levels: List<String>, deviceId: String?): Flow<List<LogEntity>>

    @Query(
        "SELECT * FROM logs WHERE timeMillis BETWEEN :from AND :to AND source IN (:sources) AND level IN (:levels) " +
            "AND (:deviceId IS NULL OR deviceId = :deviceId) ORDER BY timeMillis DESC, id DESC",
    )
    abstract suspend fun get(from: Long, to: Long, sources: List<String>, levels: List<String>, deviceId: String?): List<LogEntity>

    @Query("SELECT * FROM logs WHERE id = :id")
    abstract suspend fun getById(id: Long): LogEntity?

    @Insert
    abstract suspend fun insert(entry: LogEntity): Long

    @Insert
    abstract suspend fun insertAll(entries: List<LogEntity>)

    @Query("SELECT COUNT(*) FROM logs")
    abstract suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM logs WHERE timeMillis < :before")
    abstract suspend fun countBefore(before: Long): Int

    @Query("DELETE FROM logs WHERE timeMillis < :cutoff")
    abstract suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM logs WHERE id NOT IN (SELECT id FROM logs ORDER BY timeMillis DESC, id DESC LIMIT :keep)")
    abstract suspend fun keepNewest(keep: Int)

    /** Retention in one transaction: first by age, then by count. */
    @Transaction
    open suspend fun prune(cutoff: Long, keep: Int) {
        deleteOlderThan(cutoff)
        keepNewest(keep)
    }

    @Transaction
    open suspend fun seedIfEmpty(entries: () -> List<LogEntity>) {
        if (count() == 0) insertAll(entries())
    }
}
