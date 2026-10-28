package com.iot.widget.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.iot.widget.data.db.AppDatabase
import com.iot.widget.domain.cameras.Recording
import com.iot.widget.domain.cameras.RecordingEvent
import com.iot.widget.domain.logs.DateRange
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordingRepositoryTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val clock = fixedClock() // 2026-10-27 17:21:34 Istanbul
    private val repo = RoomRecordingRepository(db.recordingDao(), clock)
    private val now = clock.millis()
    private val hour = 3_600_000L

    @After
    fun close() = db.close()

    private fun rec(ago: Long, camera: String) = Recording(0, camera, now - ago, 15_000, RecordingEvent.MOTION, "rec_15")

    @Test
    fun observeFiltersByCameraAndDayNewestFirst() = runTest {
        repo.seedIfEmptyOrStale(staleBefore = 0) { listOf(rec(3 * hour, "cam_door"), rec(hour, "cam_door"), rec(2 * hour, "cam_desk"), rec(20 * hour, "cam_door")) }

        val today = repo.observe(null, DateRange.Today).first()
        assertEquals(listOf(now - hour, now - 2 * hour, now - 3 * hour), today.map { it.startMillis })
        assertEquals(2, repo.observe("cam_door", DateRange.Today).first().size)
        assertEquals(listOf(now - 20 * hour), repo.observe("cam_door", DateRange.Yesterday).first().map { it.startMillis })
        val id = today.first().id
        assertEquals("cam_door", repo.byId(id)?.cameraId)
    }

    @Test
    fun seedOnlyIntoEmptyTable() = runTest {
        repo.seedIfEmptyOrStale(staleBefore = 0) { listOf(rec(hour, "cam_door")) }
        repo.seedIfEmptyOrStale(staleBefore = 0) { listOf(rec(hour, "cam_desk"), rec(hour, "cam_desk")) }
        assertEquals(listOf("cam_door"), repo.observe(null, DateRange.Today).first().map { it.cameraId })
    }

    @Test
    fun staleSampleIsReplaced() = runTest {
        // installed days ago: the newest recording is older than "yesterday"
        repo.seedIfEmptyOrStale(staleBefore = 0) { listOf(rec(80 * hour, "cam_door"), rec(90 * hour, "cam_desk")) }
        repo.seedIfEmptyOrStale(staleBefore = now - 30 * hour) { listOf(rec(hour, "cam_hallway")) }
        assertEquals(listOf("cam_hallway"), repo.observe(null, DateRange.Last7Days).first().map { it.cameraId })
    }


    @Test
    fun recordingsWithUnknownClipsAreReplaced() = runTest {
        // an app update renamed the clip files: rows pointing at old names would crash the player
        repo.seedIfEmptyOrStale(staleBefore = 0, knownClips = setOf("rec_15")) { listOf(rec(hour, "cam_door")) }
        repo.seedIfEmptyOrStale(staleBefore = 0, knownClips = setOf("rec_door_30")) {
            listOf(Recording(0, "cam_door", now - hour, 30_000, RecordingEvent.MOTION, "rec_door_30"))
        }
        assertEquals(listOf("rec_door_30"), repo.observe(null, DateRange.Today).first().map { it.clip })
    }
}
