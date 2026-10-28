package com.iot.widget.data

import com.iot.widget.data.db.RecordingDao
import com.iot.widget.data.db.RecordingEntity
import com.iot.widget.domain.cameras.Recording
import com.iot.widget.domain.logs.DateRange
import com.iot.widget.domain.logs.toMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Data layer · stored camera recordings (spec §6.7). */
interface RecordingRepository {
    /** [cameraId] null = all cameras. Newest first. */
    fun observe(cameraId: String?, range: DateRange): Flow<List<Recording>>
    suspend fun byId(id: Long): Recording?
    /**
     * Demo data: recordings are never made by the app, so once the newest sample is older
     * than [staleBefore] the sample is generated again around "now". The same happens when a
     * row names a clip outside [knownClips] (the clip files were renamed in an update).
     */
    suspend fun seedIfEmptyOrStale(staleBefore: Long, knownClips: Set<String> = emptySet(), make: () -> List<Recording>)
}

@Singleton
class RoomRecordingRepository @Inject constructor(
    private val dao: RecordingDao,
    private val clock: Clock,
) : RecordingRepository {

    override fun observe(cameraId: String?, range: DateRange): Flow<List<Recording>> {
        // same day windows as the Logs screen (local days, the phone's time zone)
        val r = range.toMillis(ZonedDateTime.now(clock))
        return dao.observe(cameraId, r.first, r.last).map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun byId(id: Long): Recording? = dao.getById(id)?.toDomain()

    override suspend fun seedIfEmptyOrStale(staleBefore: Long, knownClips: Set<String>, make: () -> List<Recording>) =
        dao.seedIfEmptyOrStale(staleBefore, knownClips) { make().map { it.toEntity() } }
}

private fun RecordingEntity.toDomain() = Recording(id, cameraId, startMillis, durationMs, event, clip)

private fun Recording.toEntity() = RecordingEntity(id, cameraId, startMillis, durationMs, event, clip)
