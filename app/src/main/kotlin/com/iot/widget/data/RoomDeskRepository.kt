package com.iot.widget.data

import com.iot.widget.data.db.DeskDao
import com.iot.widget.data.db.DeskSettingsEntity
import com.iot.widget.data.db.DeviceEntity
import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.DeskState
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.DeviceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data layer · DeskRepository backed by Room.
 *
 * @Inject constructor: Hilt can build this class by itself (it gets the DAO from AppModule).
 * @Singleton: one instance for the whole app, so every screen observes the same data.
 */
@Singleton
class RoomDeskRepository @Inject constructor(
    private val dao: DeskDao,
) : DeskRepository {

    // combine() merges the two live queries: whenever either table changes, a new DeskState is built.
    // onStart seeds first, so the very first observer (screen or worker) never sees an empty desk.
    override val state: Flow<DeskState> =
        combine(dao.observeDevices(), dao.observeSettings().filterNotNull()) { devices, settings ->
            toDomain(devices, settings)
        }.onStart { ensureSeeded() }

    override suspend fun ensureSeeded() {
        dao.insertDevicesIfAbsent(SEED_DEVICES)
        dao.insertSettingsIfAbsent(DeskSettings().toEntity())
    }

    override suspend fun current(): DeskState {
        ensureSeeded() // two "insert if absent" statements: cheap, and makes current() always safe
        return toDomain(dao.getDevices(), checkNotNull(dao.getSettings()))
    }

    override suspend fun setDeviceOn(id: DeviceId, on: Boolean) =
        dao.setDeviceOnAndClearTimer(id.key, on, isPlug = id == DeviceId.SmartPlug)

    override suspend fun updateSettings(transform: (DeskSettings) -> DeskSettings) =
        dao.updateSettings { entity -> transform(entity.toDomain()).toEntity() }

    override suspend fun updateState(transform: (DeskState) -> DeskState): Pair<DeskState, DeskState> {
        ensureSeeded()
        var result: Pair<DeskState, DeskState>? = null
        dao.updateDesk { devices, settings ->
            val before = toDomain(devices, settings)
            val after = transform(before)
            result = before to after
            after.devices.entries.associate { (id, d) -> id.key to d.isOn } to after.settings.toEntity()
        }
        return checkNotNull(result)
    }

    override suspend fun turnOffPlugIfPlanned(plannedEnd: Long): Boolean =
        dao.turnOffPlugIfPlanned(DeviceId.SmartPlug.key, plannedEnd)

    private fun toDomain(devices: List<DeviceEntity>, settings: DeskSettingsEntity): DeskState = DeskState(
        devices = devices.mapNotNull { e ->
            val id = DeviceId.entries.firstOrNull { it.key == e.id } ?: return@mapNotNull null
            id to DeviceState(id, e.name, e.subtitle, e.isOn, e.online)
        }.toMap(),
        settings = settings.toDomain(),
    )

    companion object {
        /** The four reference devices (spec §6.1). Card titles come from `name` (spec §9a). */
        val SEED_DEVICES = listOf(
            DeviceEntity(DeviceId.Soundbar.key, "Bluetooth", "Audio Engine Connected", "soundbar", "Desk", isOn = true, online = true, sortOrder = 0),
            DeviceEntity(DeviceId.DeskLight.key, "Desk Halo LED", "Philips Hue ScreenBar", "light", "Desk", isOn = true, online = true, sortOrder = 1),
            DeviceEntity(DeviceId.SmartPlug.key, "Smart Plug", "Peripherals & Monitors", "plug", "Desk", isOn = true, online = true, sortOrder = 2),
            DeviceEntity(DeviceId.PrivacyGuard.key, "Privacy Guard", "Mic & Webcam Shutter", "privacy", "Desk", isOn = false, online = true, sortOrder = 3),
        )
    }
}

private fun DeskSettingsEntity.toDomain() = DeskSettings(
    brightness = brightness,
    colorTempK = colorTempK,
    autoDim = autoDim,
    scheduleOn = scheduleOn,
    scheduleOnDays = scheduleOnDays,
    scheduleOff = scheduleOff,
    scheduleOffDays = scheduleOffDays,
    scheduleEnabled = scheduleEnabled,
    audioOutput = audioOutput,
    volume = volume,
    plugTimer = plugTimer,
    plugTimerEndsAtMillis = plugTimerEndsAtMillis,
    privacyOnLeave = privacyOnLeave,
    privacyOnLock = privacyOnLock,
    activeScene = activeScene,
)

private fun DeskSettings.toEntity() = DeskSettingsEntity(
    brightness = brightness,
    colorTempK = colorTempK,
    autoDim = autoDim,
    scheduleOn = scheduleOn,
    scheduleOnDays = scheduleOnDays,
    scheduleOff = scheduleOff,
    scheduleOffDays = scheduleOffDays,
    scheduleEnabled = scheduleEnabled,
    audioOutput = audioOutput,
    volume = volume,
    plugTimer = plugTimer,
    plugTimerEndsAtMillis = plugTimerEndsAtMillis,
    privacyOnLeave = privacyOnLeave,
    privacyOnLock = privacyOnLock,
    activeScene = activeScene,
)
