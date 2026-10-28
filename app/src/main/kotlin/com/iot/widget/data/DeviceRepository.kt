package com.iot.widget.data

import com.iot.widget.data.db.DeviceDao
import com.iot.widget.data.db.DeviceEntity
import com.iot.widget.data.db.SeedDevices
import com.iot.widget.domain.devices.Device
import com.iot.widget.domain.devices.DeviceInfo
import com.iot.widget.domain.devices.DeviceType
import com.iot.widget.domain.devices.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Data layer · every device, for the device manager (spec §6.8). */
interface DeviceRepository {
    /** Live list: dashboard devices, cameras, sensors, then added devices. */
    val devices: Flow<List<Device>>
    suspend fun byId(id: String): Device?
    suspend fun rename(id: String, name: String)
    suspend fun setRoom(id: String, room: Room)
    suspend fun setOnline(id: String, online: Boolean, atMillis: Long)
    suspend fun add(device: Device)
    suspend fun delete(id: String)
}

@Singleton
class RoomDeviceRepository @Inject constructor(
    private val dao: DeviceDao,
    private val clock: Clock,
) : DeviceRepository {

    override val devices: Flow<List<Device>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }.onStart { seed() }

    override suspend fun byId(id: String): Device? {
        seed()
        return dao.getById(id)?.toDomain()
    }

    override suspend fun rename(id: String, name: String) = dao.rename(id, name.trim())

    override suspend fun setRoom(id: String, room: Room) = dao.setRoom(id, room.name)

    override suspend fun setOnline(id: String, online: Boolean, atMillis: Long) = dao.setOnline(id, online, atMillis)

    override suspend fun add(device: Device) = dao.insertAtEnd(
        DeviceEntity(
            id = device.id,
            name = device.name.trim(),
            subtitle = "",
            type = device.type.name,
            room = device.room.name,
            isOn = device.isOn,
            online = device.online,
            sortOrder = 0, // replaced by insertAtEnd
            firmware = device.info.firmware,
            ipAddress = device.info.ipAddress,
            macAddress = device.info.macAddress,
            batteryPct = device.info.batteryPct,
            signalDbm = device.info.signalDbm,
            lastSeenMillis = device.info.lastSeenMillis,
        ),
    )

    override suspend fun delete(id: String) = dao.delete(id)

    private suspend fun seed() = dao.seed(SeedDevices.all(clock.millis()))
}

/** Step-1 rows store "soundbar"/"Desk"; newer rows enum names. Both are read case-insensitively. */
private fun DeviceEntity.toDomain() = Device(
    id = id,
    name = name,
    type = DeviceType.entries.first { it.name.equals(type, ignoreCase = true) },
    room = Room.entries.firstOrNull { it.name.equals(room, ignoreCase = true) } ?: Room.DESK,
    online = online,
    isOn = isOn,
    info = DeviceInfo(firmware, ipAddress, macAddress, batteryPct, signalDbm, lastSeenMillis),
)
