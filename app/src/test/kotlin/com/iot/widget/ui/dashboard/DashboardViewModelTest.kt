package com.iot.widget.ui.dashboard

import com.iot.widget.domain.PlugTimer
import com.iot.widget.R
import com.iot.widget.domain.AudioOutput
import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.Scene
import com.iot.widget.domain.Topics
import com.iot.widget.domain.isOn
import com.iot.widget.simulation.SensorSimulator
import com.iot.widget.testutil.FakeDeskRepository
import com.iot.widget.testutil.FakeDeviceRepository
import com.iot.widget.testutil.FakeScheduler
import com.iot.widget.testutil.FakeSessionRepository
import com.iot.widget.testutil.RecordingPublisher
import com.iot.widget.testutil.defaultDeskState
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * ViewModel tests run on the JVM with fakes and virtual time: Dispatchers.Main is replaced by
 * a test dispatcher, and advanceTimeBy() moves the clock without waiting.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeDeskRepository
    private lateinit var publisher: RecordingPublisher
    private var session = FakeSessionRepository()
    private val devices = FakeDeviceRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeDeskRepository()
        publisher = RecordingPublisher()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): DashboardViewModel {
        val vm = DashboardViewModel(repo, publisher, SensorSimulator(Random(1)), FakeScheduler(), session, devices, fixedClock(), Random(2))
        // uiState only runs while someone collects it (like the screen does)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun toggleLightPublishesAndPersists() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onToggle(DeviceId.DeskLight, false)
        runCurrent()

        val msg = publisher.messages.single()
        assertEquals(Topics.LIGHTS, msg.topic)
        assertTrue(msg.json.contains(""""state":"OFF""""))
        assertFalse(repo.flow.value.isOn(DeviceId.DeskLight))
        assertEquals("129W", vm.uiState.value.power)
        assertFalse(vm.uiState.value.lightOn)
    }

    @Test
    fun rapidSliderChangesPublishOnce() = runTest(dispatcher) {
        val vm = viewModel()
        repeat(10) { i ->
            vm.onBrightnessChange(40 + i)
            advanceTimeBy(10)
        }
        assertEquals(49, vm.uiState.value.brightness) // UI follows the finger immediately
        assertTrue(publisher.messages.isEmpty())

        advanceTimeBy(151)
        runCurrent()
        val dims = publisher.messages.filter { it.topic == Topics.LIGHTS_DIM }
        assertEquals(1, dims.size)
        assertEquals("""{"brightness":49}""", dims.single().json)
        assertEquals(1, repo.settingsWrites)
        assertEquals(49, repo.flow.value.settings.brightness)
    }

    @Test
    fun leaveScenePublishesInOrderThenDim() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onScene(Scene.Leave)
        advanceTimeBy(200)
        runCurrent()

        assertEquals(
            listOf(
                Topics.SOUNDBAR, Topics.LIGHTS, Topics.PLUG, Topics.WIDGET_EVENT, Topics.PRIVACY, Topics.SCENES,
                Topics.LIGHTS_DIM,
            ),
            publisher.topics(),
        )
        assertEquals("""{"brightness":5}""", publisher.messages.last().json)
        assertEquals(Scene.Leave, vm.uiState.value.activeScene)
        assertTrue(repo.flow.value.isOn(DeviceId.PrivacyGuard))
    }

    @Test
    fun headphonesChangesBluetoothSubtitle() = runTest(dispatcher) {
        repo = FakeDeskRepository(defaultDeskState(DeskSettings(audioOutput = AudioOutput.Headphones)))
        val vm = viewModel()
        val card = vm.uiState.value.devices.first { it.id == DeviceId.Soundbar }
        assertEquals(R.string.bt_headphones_connected, card.statusRes)
    }

    @Test
    fun notLoadedUntilPermissionKnown() = runTest(dispatcher) {
        // a session that has not answered yet: the dashboard must not draw a Guest look meanwhile
        val pending = object : com.iot.widget.data.SessionRepository {
            override val currentUser = kotlinx.coroutines.flow.MutableSharedFlow<com.iot.widget.domain.users.User?>()
            override suspend fun signIn(userId: String): com.iot.widget.domain.users.User? = null
            override suspend fun signOut() = Unit
        }
        val vm = DashboardViewModel(repo, publisher, SensorSimulator(Random(1)), FakeScheduler(), pending, devices, fixedClock(), Random(2))
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        assertFalse(vm.uiState.value.loaded)
        pending.currentUser.emit(com.iot.widget.testutil.SEED_TEST_USERS.first())
        runCurrent()
        assertTrue(vm.uiState.value.loaded)
        assertTrue(vm.uiState.value.canControl)
    }

    @Test
    fun offlineDeviceToggleIgnored() = runTest(dispatcher) {
        repo.flow.value = repo.flow.value.let { s ->
            s.copy(devices = s.devices + (DeviceId.SmartPlug to s.devices.getValue(DeviceId.SmartPlug).copy(online = false)))
        }
        val vm = viewModel()
        assertFalse(vm.uiState.value.devices.first { it.id == DeviceId.SmartPlug }.online)
        vm.onToggle(DeviceId.SmartPlug, false)
        runCurrent()
        assertTrue(publisher.messages.isEmpty())
        assertTrue(repo.flow.value.isOn(DeviceId.SmartPlug))
    }

    @Test
    fun dimmerIgnoredWhenLightOffline() = runTest(dispatcher) {
        repo.flow.value = repo.flow.value.let { s ->
            s.copy(devices = s.devices + (DeviceId.DeskLight to s.devices.getValue(DeviceId.DeskLight).copy(online = false)))
        }
        val vm = viewModel()
        vm.onBrightnessChange(30)
        advanceTimeBy(500)
        runCurrent()
        assertTrue(publisher.messages.isEmpty())
        assertEquals(80, repo.flow.value.settings.brightness)
    }

    @Test
    fun sceneSkipsDimWhenLightOffline() = runTest(dispatcher) {
        repo.flow.value = repo.flow.value.let { s ->
            s.copy(devices = s.devices + (DeviceId.DeskLight to s.devices.getValue(DeviceId.DeskLight).copy(online = false)))
        }
        val vm = viewModel()
        vm.onScene(Scene.Meet)
        advanceTimeBy(500)
        runCurrent()
        assertTrue(publisher.topics().none { it == Topics.LIGHTS_DIM || it == Topics.LIGHTS })
        assertEquals(80, repo.flow.value.settings.brightness)
    }

    @Test
    fun renamedDeviceShowsOnCard() = runTest(dispatcher) {
        repo.flow.value = repo.flow.value.let { s ->
            s.copy(devices = s.devices + (DeviceId.DeskLight to s.devices.getValue(DeviceId.DeskLight).copy(name = "Desk Lamp")))
        }
        val vm = viewModel()
        assertEquals("Desk Lamp", vm.uiState.value.devices.first { it.id == DeviceId.DeskLight }.name)
    }

    @Test
    fun footerDashesWhenRoomSensorOffline() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals("22.4°C", vm.uiState.value.temperature)
        devices.setOnline("room_sensor", false, 0L)
        runCurrent()
        assertEquals("—", vm.uiState.value.temperature)
        assertEquals("—", vm.uiState.value.humidity)
    }

    @Test
    fun guestToggleIsIgnored() = runTest(dispatcher) {
        session = FakeSessionRepository(startUserId = "guest")
        val vm = viewModel()
        assertFalse(vm.uiState.value.canControl)
        vm.onToggle(DeviceId.DeskLight, false)
        vm.onScene(Scene.Leave)
        vm.onBrightnessChange(10)
        advanceTimeBy(500)
        runCurrent()
        assertTrue(publisher.messages.isEmpty())
        assertTrue(repo.flow.value.isOn(DeviceId.DeskLight))
    }

    @Test
    fun operatorCanControl() = runTest(dispatcher) {
        session = FakeSessionRepository(startUserId = "mehmet")
        val vm = viewModel()
        assertTrue(vm.uiState.value.canControl)
        vm.onToggle(DeviceId.DeskLight, false)
        runCurrent()
        assertEquals(1, publisher.messages.size)
    }

    @Test
    fun privacyOnPublishesNoticeFirst() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onToggle(DeviceId.PrivacyGuard, true)
        runCurrent()
        assertEquals(listOf(Topics.WIDGET_EVENT, Topics.PRIVACY), publisher.topics())
    }


    @Test
    fun manualPlugOffPublishesTimerOff() = runTest(dispatcher) {
        repo = FakeDeskRepository(defaultDeskState(DeskSettings(plugTimer = PlugTimer.M30, plugTimerEndsAtMillis = 1_000L)))
        val vm = viewModel()
        vm.onToggle(DeviceId.SmartPlug, false)
        runCurrent()
        val timer = publisher.messages.single { it.topic == Topics.PLUG_TIMER }
        assertEquals("""{"value":"off"}""", timer.json)
    }

    @Test
    fun leavePublishesTimerOff() = runTest(dispatcher) {
        repo = FakeDeskRepository(defaultDeskState(DeskSettings(plugTimer = PlugTimer.M30, plugTimerEndsAtMillis = 1_000L)))
        val vm = viewModel()
        vm.onScene(Scene.Leave)
        runCurrent()
        assertEquals("""{"value":"off"}""", publisher.messages.single { it.topic == Topics.PLUG_TIMER }.json)
    }

    @Test
    fun noTimerOffWhenNoTimerWasSet() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onToggle(DeviceId.SmartPlug, false)
        runCurrent()
        assertTrue(publisher.messages.none { it.topic == Topics.PLUG_TIMER })
    }


    @Test
    fun lateTimerShowsTurningOff() = runTest(dispatcher) {
        // the auto-off job is a little late: the plug is still on after the end time
        val ended = fixedClock().millis() - 60_000
        repo = FakeDeskRepository(defaultDeskState(DeskSettings(plugTimer = PlugTimer.M30, plugTimerEndsAtMillis = ended)))
        val vm = viewModel()
        val plug = vm.uiState.value.devices.first { it.id == DeviceId.SmartPlug }
        assertEquals(null, plug.timerMinutesLeft)
        assertTrue(plug.timerOverdue)
    }
}
