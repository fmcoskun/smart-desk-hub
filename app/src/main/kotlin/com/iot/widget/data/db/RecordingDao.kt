package com.iot.widget.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Data layer · SQL for camera recordings: one camera or all, within a time window, newest first. */
@Dao
abstract class RecordingDao {

    @Query(
        "SELECT * FROM recordings WHERE (:cameraId IS NULL OR cameraId = :cameraId) " +
            "AND startMillis BETWEEN :from AND :to ORDER BY startMillis DESC, id DESC",
    )
    abstract fun observe(cameraId: String?, from: Long, to: Long): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    abstract suspend fun getById(id: Long): RecordingEntity?

    @Insert
    abstract suspend fun insertAll(entries: List<RecordingEntity>)

    @Query("SELECT COUNT(*) FROM recordings")
    abstract suspend fun count(): Int

    @Query("SELECT MAX(startMillis) FROM recordings")
    abstract suspend fun newestStart(): Long?

    @Query("DELETE FROM recordings")
    abstract suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM recordings WHERE clip NOT IN (:known)")
    abstract suspend fun countUnknownClips(known: List<String>): Int

    /**
     * Fills an empty table, or replaces it when even the newest row is older than [staleBefore],
     * or when a row points at a clip the app no longer ships ([knownClips]; empty = don't check).
     */
    @Transaction
    open suspend fun seedIfEmptyOrStale(staleBefore: Long, knownClips: Set<String>, entries: () -> List<RecordingEntity>) {
        val newest = newestStart()
        val unknown = knownClips.isNotEmpty() && countUnknownClips(knownClips.toList()) > 0
        if (newest != null && newest >= staleBefore && !unknown) return
        deleteAll()
        insertAll(entries())
    }
}
