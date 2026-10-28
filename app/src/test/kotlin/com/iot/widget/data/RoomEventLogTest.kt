package com.iot.widget.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.iot.widget.data.db.AppDatabase
import com.iot.widget.data.log.RoomEventLog
import com.iot.widget.domain.logs.LogLevel
import com.iot.widget.domain.logs.LogQuery
import com.iot.widget.domain.logs.LogSource
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RoomEventLogTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val repo = RoomLogRepository(db.logDao(), fixedClock())

    @After
    fun close() = db.close()

    @Test
    fun recordWritesClientInfo() = runTest {
        RoomEventLog(repo, fixedClock(), backgroundScope).record("Ayşe Yılmaz", "signed in")
        runCurrent()
        val e = repo.observe(LogQuery()).first().entries.single()
        assertEquals(LogSource.CLIENT, e.source)
        assertEquals(LogLevel.INFO, e.level)
        assertEquals("Ayşe Yılmaz", e.actor)
        assertEquals("signed in", e.message)
    }

    @Test
    fun eventLogRecordKeepsDevice() = runTest {
        RoomEventLog(repo, fixedClock(), backgroundScope).record("Ayşe Yılmaz", "took a snapshot", "cam_door", "Door Camera")
        runCurrent()
        val e = repo.observe(LogQuery(deviceId = "cam_door")).first().entries.single()
        assertEquals(LogSource.CLIENT, e.source)
        assertEquals("Door Camera", e.deviceName)
    }

    @Test
    fun deviceWritesDeviceEntry() = runTest {
        RoomEventLog(repo, fixedClock(), backgroundScope).device("meeting_sensor", "Meeting Room Sensor", LogLevel.ERROR, "connection timeout")
        runCurrent()
        val e = repo.observe(LogQuery()).first().entries.single()
        assertEquals(LogSource.DEVICE, e.source)
        assertEquals("meeting_sensor", e.deviceId)
        assertEquals("Meeting Room Sensor", e.deviceName)
        assertEquals(LogLevel.ERROR, e.level)
    }
}
