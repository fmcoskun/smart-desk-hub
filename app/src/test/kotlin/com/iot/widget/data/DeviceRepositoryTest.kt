package com.iot.widget.data

import android.content.Context
import androidx.room.Room as RoomDb
import androidx.test.core.app.ApplicationProvider
import com.iot.widget.data.db.AppDatabase
import com.iot.widget.domain.devices.BuiltIn
import com.iot.widget.domain.devices.Device
import com.iot.widget.domain.devices.DeviceInfo
import com.iot.widget.domain.devices.DeviceType
import com.iot.widget.domain.devices.Room
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeviceRepositoryTest {

    private val db = RoomDb.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val clock = fixedClock()
    private val repo = RoomDeviceRepository(db.deviceDao(), clock)

    @After
    fun close() = db.close()

    @Test
    fun listsTenBuiltInsDashboardFirst() = runTest {
        val all = repo.devices.first()
        assertEquals(10, all.size)
        assertEquals(listOf("soundbar", "desk_light", "smart_plug", "privacy_guard"), all.take(4).map { it.id })
        assertEquals(BuiltIn.CAMERA_IDS, all.drop(4).take(4).map { it.id })
        val meeting = all.first { it.id == BuiltIn.MEETING_SENSOR }
        assertFalse(meeting.online)
        assertEquals(12, meeting.info.batteryPct)
        assertEquals(Room.ENTRANCE, all.first { it.id == "cam_door" }.room)
        assertEquals(DeviceType.SOUNDBAR, all.first().type)
        assertTrue(all.first().info.firmware.isNotEmpty())
    }

    @Test
    fun renameAndRoomPersist() = runTest {
        repo.devices.first()
        repo.rename("desk_light", "Desk Lamp")
        repo.setRoom("cam_hallway", Room.ENTRANCE)
        assertEquals("Desk Lamp", repo.byId("desk_light")!!.name)
        assertEquals(Room.ENTRANCE, repo.byId("cam_hallway")!!.room)
    }

    @Test
    fun setOnlineStampsLastSeen() = runTest {
        repo.devices.first()
        repo.setOnline(BuiltIn.MEETING_SENSOR, true, atMillis = 777L)
        val d = repo.byId(BuiltIn.MEETING_SENSOR)!!
        assertTrue(d.online)
        assertEquals(777L, d.info.lastSeenMillis)
    }

    @Test
    fun addedDeviceAppendsAndCanBeDeleted() = runTest {
        repo.devices.first()
        val lamp = Device("dev_1", "Shelf Light", DeviceType.LIGHT, Room.HALLWAY, online = true, isOn = true,
            info = DeviceInfo("1.0.0", "192.168.1.60", "24:6F:28:A1:00:60", null, -48, 5L))
        repo.add(lamp)
        assertEquals("dev_1", repo.devices.first().last().id)
        repo.delete("dev_1")
        assertNull(repo.byId("dev_1"))
    }

    @Test
    fun seedDoesNotOverwriteRenamedCamera() = runTest {
        repo.devices.first()
        repo.rename("cam_desk", "Webcam")
        repo.devices.first() // seeding runs again on every new collection
        assertEquals("Webcam", repo.byId("cam_desk")!!.name)
    }
}
