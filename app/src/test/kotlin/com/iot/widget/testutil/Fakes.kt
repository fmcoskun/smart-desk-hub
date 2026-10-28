package com.iot.widget.testutil

import com.iot.widget.domain.isOn
import com.iot.widget.data.DeskRepository
import com.iot.widget.data.mqtt.MqttMessage
import com.iot.widget.data.mqtt.MqttPublisher
import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.DeskState
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.DeviceState
import com.iot.widget.domain.devices.DeviceType
import com.iot.widget.domain.logs.matches
import com.iot.widget.domain.logs.toMillis
import com.iot.widget.domain.devices.Room
import com.iot.widget.domain.withDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Test doubles shared by ViewModel and worker tests. */

fun defaultDeskState(settings: DeskSettings = DeskSettings()) = DeskState(
    devices = linkedMapOf(
        DeviceId.Soundbar to DeviceState(DeviceId.Soundbar, "Bluetooth", "Audio Engine Connected", isOn = true),
        DeviceId.DeskLight to DeviceState(DeviceId.DeskLight, "Desk Halo LED", "Philips Hue ScreenBar", isOn = true),
        DeviceId.SmartPlug to DeviceState(DeviceId.SmartPlug, "Smart Plug", "Peripherals & Monitors", isOn = true),
        DeviceId.PrivacyGuard to DeviceState(DeviceId.PrivacyGuard, "Privacy Guard", "Mic & Webcam Shutter", isOn = false),
    ),
    settings = settings,
)

/** In-memory DeskRepository that counts writes. */
class FakeDeskRepository(initial: DeskState = defaultDeskState()) : DeskRepository {
    val flow = MutableStateFlow(initial)
    var settingsWrites = 0
        private set

    override val state: Flow<DeskState> = flow
    override suspend fun ensureSeeded() = Unit
    override suspend fun current(): DeskState = flow.value
    override suspend fun setDeviceOn(id: DeviceId, on: Boolean) {
        flow.value = flow.value.withDevice(id, on)
    }
    override suspend fun updateSettings(transform: (DeskSettings) -> DeskSettings) {
        settingsWrites++
        flow.value = flow.value.copy(settings = transform(flow.value.settings))
    }
    override suspend fun updateState(transform: (DeskState) -> DeskState): Pair<DeskState, DeskState> {
        val before = flow.value
        val after = transform(before)
        // like Room: only switches and settings are saved, names/online stay
        flow.value = before.copy(
            devices = before.devices.mapValues { (id, d) -> d.copy(isOn = after.devices[id]?.isOn ?: d.isOn) },
            settings = after.settings,
        )
        return before to flow.value
    }
    override suspend fun turnOffPlugIfPlanned(plannedEnd: Long): Boolean {
        val s = flow.value
        if (s.settings.plugTimerEndsAtMillis != plannedEnd) return false
        val wasOn = s.isOn(DeviceId.SmartPlug)
        flow.value = s.withDevice(DeviceId.SmartPlug, false)
        return wasOn
    }
}

/** MqttPublisher that remembers everything published. */
class RecordingPublisher : MqttPublisher {
    val messages = mutableListOf<MqttMessage>()
    private val last = MutableStateFlow<MqttMessage?>(null)
    override val lastMessage: StateFlow<MqttMessage?> = last
    override fun publish(message: MqttMessage) {
        messages += message
        last.value = message
    }
    fun topics(): List<String> = messages.map { it.topic }
}

/** A clock frozen at 2026-10-27 17:21:34 Istanbul time. */
fun fixedClock(instant: Instant = Instant.parse("2026-10-27T14:21:34Z")): Clock =
    Clock.fixed(instant, ZoneId.of("Europe/Istanbul"))

// ---- Step 2: users & session ----------------------------------------------------------------

val SEED_TEST_USERS = listOf(
    com.iot.widget.domain.users.User("ayse", "Ayşe Yılmaz", com.iot.widget.domain.users.Role.ADMIN, active = true, lastLoginAtMillis = null),
    com.iot.widget.domain.users.User("mehmet", "Mehmet Kaya", com.iot.widget.domain.users.Role.OPERATOR, active = true, lastLoginAtMillis = null),
    com.iot.widget.domain.users.User("zeynep", "Zeynep Demir", com.iot.widget.domain.users.Role.OPERATOR, active = false, lastLoginAtMillis = null),
    com.iot.widget.domain.users.User("guest", "Guest", com.iot.widget.domain.users.Role.GUEST, active = true, lastLoginAtMillis = null),
)

/** [suspendReads]: reads and writes give up the thread once, like Room hopping to its database thread. */
class FakeUserRepository(
    initial: List<com.iot.widget.domain.users.User> = SEED_TEST_USERS,
    private val suspendReads: Boolean = false,
) : com.iot.widget.data.UserRepository {
    val flow = MutableStateFlow(initial)
    override val users: Flow<List<com.iot.widget.domain.users.User>> = flow
    override suspend fun ensureSeeded() = Unit
    override suspend fun all(): List<com.iot.widget.domain.users.User> {
        if (suspendReads) kotlinx.coroutines.yield()
        return flow.value
    }
    override suspend fun byId(id: String) = flow.value.firstOrNull { it.id == id }
    override suspend fun save(user: com.iot.widget.domain.users.User) {
        if (suspendReads) kotlinx.coroutines.yield() // writes hop threads too
        flow.value = if (flow.value.any { it.id == user.id }) flow.value.map { if (it.id == user.id) user else it } else flow.value + user
    }
    override suspend fun delete(id: String) {
        flow.value = flow.value.filter { it.id != id }
    }
    override suspend fun markLoggedIn(id: String, atMillis: Long) {
        flow.value = flow.value.map { if (it.id == id) it.copy(lastLoginAtMillis = atMillis) else it }
    }
}

/** Session fake: starts signed in as [startUserId] (Ayşe, the Admin, by default). */
class FakeSessionRepository(
    private val users: FakeUserRepository = FakeUserRepository(),
    startUserId: String? = "ayse",
) : com.iot.widget.data.SessionRepository {
    val userId = MutableStateFlow(startUserId)
    override val currentUser: Flow<com.iot.widget.domain.users.User?> =
        kotlinx.coroutines.flow.combine(userId, users.flow) { id, all -> all.firstOrNull { it.id == id && it.active } }
    override suspend fun signIn(userId: String): com.iot.widget.domain.users.User? {
        val u = users.byId(userId) ?: return null
        if (!u.active) return null
        this.userId.value = userId
        return u
    }
    override suspend fun signOut() {
        userId.value = null
    }
}

class RecordingEventLog : com.iot.widget.data.log.EventLog {
    val lines = mutableListOf<String>()
    val deviceLines = mutableListOf<String>()
    override fun record(actor: String, message: String, deviceId: String?, deviceName: String?) {
        lines += "$actor: $message" + (deviceId?.let { " [$it]" } ?: "")
    }
    override fun device(deviceId: String, deviceName: String, level: com.iot.widget.data.log.LogLevel, message: String) {
        deviceLines += "$level $deviceId: $message"
    }
}

// ---- Step 3: devices ----------------------------------------------------------------------

fun testDevice(
    id: String,
    name: String = id,
    type: com.iot.widget.domain.devices.DeviceType = com.iot.widget.domain.devices.DeviceType.SENSOR,
    room: com.iot.widget.domain.devices.Room = com.iot.widget.domain.devices.Room.DESK,
    online: Boolean = true,
    battery: Int? = null,
) = com.iot.widget.domain.devices.Device(
    id, name, type, room, online, isOn = true,
    info = com.iot.widget.domain.devices.DeviceInfo("1.0.0", "192.168.1.9", "24:6F:28:A1:00:09", battery, -50, 0L),
)

/** The ten built-ins in display order (names/rooms as seeded), for ViewModel tests. */
fun builtInTestDevices(): List<com.iot.widget.domain.devices.Device> {
    return listOf(
        testDevice("soundbar", "Bluetooth", DeviceType.SOUNDBAR),
        testDevice("desk_light", "Desk Halo LED", DeviceType.LIGHT),
        testDevice("smart_plug", "Smart Plug", DeviceType.PLUG),
        testDevice("privacy_guard", "Privacy Guard", DeviceType.PRIVACY),
        testDevice("cam_desk", "Desk Camera", DeviceType.CAMERA),
        testDevice("cam_door", "Door Camera", DeviceType.CAMERA, Room.ENTRANCE),
        testDevice("cam_meeting", "Meeting Room Camera", DeviceType.CAMERA, Room.MEETING_ROOM),
        testDevice("cam_hallway", "Hallway Camera", DeviceType.CAMERA, Room.HALLWAY),
        testDevice("room_sensor", "Room Sensor", DeviceType.SENSOR, battery = 78),
        testDevice("meeting_sensor", "Meeting Room Sensor", DeviceType.SENSOR, Room.MEETING_ROOM, online = false, battery = 12),
    )
}

class FakeDeviceRepository(initial: List<com.iot.widget.domain.devices.Device> = builtInTestDevices()) : com.iot.widget.data.DeviceRepository {
    val flow = MutableStateFlow(initial)
    override val devices: Flow<List<com.iot.widget.domain.devices.Device>> = flow
    override suspend fun byId(id: String) = flow.value.firstOrNull { it.id == id }
    private fun change(id: String, f: (com.iot.widget.domain.devices.Device) -> com.iot.widget.domain.devices.Device) {
        flow.value = flow.value.map { if (it.id == id) f(it) else it }
    }
    override suspend fun rename(id: String, name: String) = change(id) { it.copy(name = name.trim()) }
    override suspend fun setRoom(id: String, room: com.iot.widget.domain.devices.Room) = change(id) { it.copy(room = room) }
    override suspend fun setOnline(id: String, online: Boolean, atMillis: Long) =
        change(id) { it.copy(online = online, info = it.info.copy(lastSeenMillis = atMillis)) }
    override suspend fun add(device: com.iot.widget.domain.devices.Device) {
        flow.value = flow.value + device
    }
    override suspend fun delete(id: String) {
        flow.value = flow.value.filter { it.id != id }
    }
}

// ---- Step 4: logs ---------------------------------------------------------------------------

/** In-memory LogRepository using the same domain filters as the Room one. */
class FakeLogRepository(
    initial: List<com.iot.widget.domain.logs.LogEntry> = emptyList(),
    private val clock: Clock = fixedClock(),
) : com.iot.widget.data.LogRepository {
    val flow = MutableStateFlow(initial.mapIndexed { i, e -> e.copy(id = i + 1L) })

    /** Same rules as RoomLogRepository: SQL-like filters, open-ended "now" ranges, then text. */
    private fun matching(q: com.iot.widget.domain.logs.LogQuery): List<com.iot.widget.domain.logs.LogEntry> {
        val r = q.range.toMillis(java.time.ZonedDateTime.now(clock))
        val openEnded = q.range == com.iot.widget.domain.logs.DateRange.Today || q.range == com.iot.widget.domain.logs.DateRange.Last7Days
        val window = r.first..(if (openEnded) Long.MAX_VALUE else r.last)
        return flow.value
            .filter { it.source in q.sources && it.level in q.levels && (q.deviceId == null || it.deviceId == q.deviceId) && it.timeMillis in window }
            .filter { it.matches(q.text) }
            .sortedWith(compareByDescending<com.iot.widget.domain.logs.LogEntry> { it.timeMillis }.thenByDescending { it.id })
    }

    override fun observe(query: com.iot.widget.domain.logs.LogQuery): Flow<com.iot.widget.data.LogPage> =
        kotlinx.coroutines.flow.flow {
            flow.collect {
                val m = matching(query)
                emit(com.iot.widget.data.LogPage(m.take(com.iot.widget.domain.logs.LogRetention.LIST_LIMIT), m.size))
            }
        }
    override suspend fun add(entry: com.iot.widget.domain.logs.LogEntry): Long {
        val id = flow.value.size + 1L
        flow.value = flow.value + entry.copy(id = id)
        return id
    }
    override suspend fun byId(id: Long) = flow.value.firstOrNull { it.id == id }
    override suspend fun prune(nowMillis: Long) = Unit
    override suspend fun seedIfEmpty(entries: () -> List<com.iot.widget.domain.logs.LogEntry>) = Unit
    override suspend fun addAll(entries: List<com.iot.widget.domain.logs.LogEntry>) {
        entries.forEach { add(it) }
    }
    override suspend fun hasEntriesBefore(millis: Long) = flow.value.any { it.timeMillis < millis }
    override suspend fun export(query: com.iot.widget.domain.logs.LogQuery) = matching(query)
}

// ---- Step 5: recordings ----------------------------------------------------------------------

/** Same rules as RoomRecordingRepository: camera filter, local-day window, newest first. */
class FakeRecordingRepository(
    initial: List<com.iot.widget.domain.cameras.Recording> = emptyList(),
    private val clock: Clock = fixedClock(),
) : com.iot.widget.data.RecordingRepository {
    val flow = MutableStateFlow(initial)

    override fun observe(cameraId: String?, range: com.iot.widget.domain.logs.DateRange): Flow<List<com.iot.widget.domain.cameras.Recording>> =
        flow.map { all ->
            val window = range.toMillis(java.time.ZonedDateTime.now(clock))
            all.filter { (cameraId == null || it.cameraId == cameraId) && it.startMillis in window }
                .sortedByDescending { it.startMillis }
        }

    override suspend fun byId(id: Long) = flow.value.firstOrNull { it.id == id }
    override suspend fun seedIfEmptyOrStale(staleBefore: Long, knownClips: Set<String>, make: () -> List<com.iot.widget.domain.cameras.Recording>) {
        val unknown = knownClips.isNotEmpty() && flow.value.any { it.clip !in knownClips }
        if (flow.value.isEmpty() || flow.value.maxOf { it.startMillis } < staleBefore || unknown) flow.value = make()
    }
}

// ---- Step 6: onboarding ----------------------------------------------------------------------

class FakeOnboardingRepository(seen: Boolean = false) : com.iot.widget.data.OnboardingRepository {
    val flow = MutableStateFlow(seen)
    override val seen: Flow<Boolean> = flow
    override suspend fun markSeen() {
        flow.value = true
    }
    override suspend fun reset() {
        flow.value = false
    }
}
