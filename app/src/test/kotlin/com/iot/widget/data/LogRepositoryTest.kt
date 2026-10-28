package com.iot.widget.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.iot.widget.data.db.AppDatabase
import com.iot.widget.domain.logs.DateRange
import com.iot.widget.domain.logs.LogEntry
import com.iot.widget.domain.logs.LogLevel
import com.iot.widget.domain.logs.LogQuery
import com.iot.widget.domain.logs.LogSource
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LogRepositoryTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val clock = fixedClock() // 2026-10-27 17:21:34 Istanbul
    private val repo = RoomLogRepository(db.logDao(), clock)
    private val now = clock.millis()
    private val hour = 3_600_000L
    private val day = 24 * hour

    @After
    fun close() = db.close()

    private fun entry(
        ago: Long,
        source: LogSource = LogSource.CLIENT,
        level: LogLevel = LogLevel.INFO,
        message: String = "msg",
        device: String? = null,
    ) = LogEntry(0, now - ago, source, level, if (source == LogSource.CLIENT) "Ayşe Yılmaz" else null, device, device, message, null, null)

    @Test
    fun filtersBySourceLevelDeviceAndRange() = runTest {
        repo.add(entry(hour, LogSource.CLIENT))
        repo.add(entry(hour, LogSource.DEVICE, LogLevel.ERROR, device = "meeting_sensor"))
        repo.add(entry(hour, LogSource.DEVICE, LogLevel.INFO, device = "cam_door"))
        repo.add(entry(2 * day, LogSource.DEVICE, LogLevel.ERROR, device = "meeting_sensor"))

        assertEquals(4, repo.observe(LogQuery()).first().total)
        assertEquals(1, repo.observe(LogQuery(sources = setOf(LogSource.CLIENT))).first().total)
        assertEquals(2, repo.observe(LogQuery(levels = setOf(LogLevel.ERROR))).first().total)
        assertEquals(2, repo.observe(LogQuery(deviceId = "meeting_sensor")).first().total)
        assertEquals(3, repo.observe(LogQuery(range = DateRange.Today)).first().total)
    }

    @Test
    fun textFilterRunsAfterSql() = runTest {
        repo.add(entry(hour, message = "IŞIK SENSÖRÜ timeout"))
        repo.add(entry(hour, message = "telemetry ok"))
        val page = repo.observe(LogQuery(text = "ışık")).first()
        assertEquals(listOf("IŞIK SENSÖRÜ timeout"), page.entries.map { it.message })
    }

    @Test
    fun newestFirstCappedWithTotal() = runTest {
        repo.seedIfEmpty { (1..600).map { entry(it * 60_000L, message = "e$it") } }
        val page = repo.observe(LogQuery()).first()
        assertEquals(600, page.total)
        assertEquals(500, page.entries.size)
        assertEquals("e1", page.entries.first().message) // newest first
        assertEquals(600, repo.export(LogQuery()).size) // export is not capped
    }

    @Test
    fun liveEntriesAppearInTodayAfterTheQueryStarted() = runTest {
        val flow = repo.observe(LogQuery(range = DateRange.Today))
        assertEquals(0, flow.first().total)
        repo.add(entry(ago = -5_000)) // written "after now", like a new event while the screen is open
        assertEquals(1, flow.first().total)
    }

    @Test
    fun pruneKeepsNewest5000AndDrops30Days() = runTest {
        repo.seedIfEmpty { (1..5_100).map { entry(it * 1_000L) } + entry(31 * day, message = "old") }
        repo.prune(now)
        val all = repo.export(LogQuery(range = DateRange.Custom(java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 10, 27))))
        assertEquals(5_000, all.size)
        assertTrue(all.none { it.message == "old" })
    }

    @Test
    fun seedIfEmptyOnlyOnce() = runTest {
        repo.seedIfEmpty { listOf(entry(hour)) }
        repo.seedIfEmpty { listOf(entry(hour), entry(hour)) }
        assertEquals(1, repo.observe(LogQuery()).first().total)
    }
}
