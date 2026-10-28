package com.iot.widget.ui.logs

import androidx.lifecycle.SavedStateHandle
import com.iot.widget.domain.logs.DateRange
import com.iot.widget.domain.logs.LogEntry
import com.iot.widget.domain.logs.LogLevel
import com.iot.widget.domain.logs.LogSource
import com.iot.widget.testutil.FakeDeviceRepository
import com.iot.widget.testutil.FakeLogRepository
import com.iot.widget.testutil.FakeSessionRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class LogsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = fixedClock()
    private val now = clock.millis()
    private val hour = 3_600_000L

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun e(ago: Long, source: LogSource, level: LogLevel, message: String, device: String? = null) =
        LogEntry(0, now - ago, source, level, if (source == LogSource.CLIENT) "Ayşe Yılmaz" else null, device, device?.let { "Dev $it" }, message, null, null)

    private val sample = listOf(
        e(hour, LogSource.CLIENT, LogLevel.INFO, "published office/desk/plug_main"),
        e(hour, LogSource.DEVICE, LogLevel.INFO, "ack ON", "smart_plug"),
        e(2 * hour, LogSource.DEVICE, LogLevel.ERROR, "connection timeout", "meeting_sensor"),
        e(30 * hour, LogSource.DEVICE, LogLevel.WARN, "low battery (12%)", "meeting_sensor"),
    )

    private fun TestScope.viewModel(entries: List<LogEntry> = sample, user: String = "ayse", deviceId: String? = null): LogsViewModel {
        val handle = SavedStateHandle(if (deviceId == null) emptyMap() else mapOf("deviceId" to deviceId))
        val vm = LogsViewModel(handle, FakeLogRepository(entries, clock), FakeDeviceRepository(), FakeSessionRepository(startUserId = user), clock)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun filtersCombine() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(4, vm.uiState.value.total)
        vm.toggleSource(LogSource.CLIENT) // turn CLIENT off
        runCurrent()
        assertEquals(3, vm.uiState.value.total)
        vm.toggleLevel(LogLevel.INFO) // and INFO off → only WARN/ERROR device lines
        runCurrent()
        assertEquals(listOf("connection timeout", "low battery (12%)"), vm.uiState.value.rows.map { it.message })
        vm.setRange(DateRange.Today)
        runCurrent()
        assertEquals(listOf("connection timeout"), vm.uiState.value.rows.map { it.message })
    }

    @Test
    fun turningOffTheLastSourceKeepsIt() = runTest(dispatcher) {
        val vm = viewModel()
        vm.toggleSource(LogSource.CLIENT)
        vm.toggleSource(LogSource.DEVICE)
        runCurrent()
        assertEquals(setOf(LogSource.DEVICE), vm.uiState.value.query.sources)
    }

    @Test
    fun textSearchDebounced300ms() = runTest(dispatcher) {
        val vm = viewModel()
        vm.setText("timeout")
        runCurrent()
        assertEquals("timeout", vm.uiState.value.text) // the field shows it at once
        assertEquals(4, vm.uiState.value.total) // but the search waits
        advanceTimeBy(301)
        runCurrent()
        assertEquals(1, vm.uiState.value.total)
    }

    @Test
    fun deviceShortcutPrefilters() = runTest(dispatcher) {
        val vm = viewModel(deviceId = "meeting_sensor")
        assertEquals(2, vm.uiState.value.total)
        assertEquals("Meeting Room Sensor", vm.uiState.value.deviceName)
        vm.clearDevice()
        runCurrent()
        assertEquals(4, vm.uiState.value.total)
    }

    @Test
    fun listCapsAt500WithTotal() = runTest(dispatcher) {
        val many = (1..650).map { e(it * 1_000L, LogSource.DEVICE, LogLevel.INFO, "t$it", "cam_door") }
        val vm = viewModel(many)
        assertEquals(650, vm.uiState.value.total)
        assertEquals(500, vm.uiState.value.rows.size)
    }

    @Test
    fun exportUsesAllMatchesNotJust500() = runTest(dispatcher) {
        val many = (1..600).map { e(it * 1_000L, LogSource.DEVICE, LogLevel.INFO, "t$it", "cam_door") }
        val vm = viewModel(many)
        val csv = checkNotNull(vm.exportCsv())
        assertEquals(601, csv.trimEnd().lines().size) // header + every match, not the 500 shown
        assertTrue(csv.contains("t600"))
    }

    @Test
    fun guestCannotExport() = runTest(dispatcher) {
        assertEquals(null, viewModel(user = "guest").exportCsv())
    }

    @Test
    fun guestGetsNoEntries() = runTest(dispatcher) {
        val vm = viewModel(user = "guest")
        assertFalse(vm.uiState.value.allowed)
        assertTrue(vm.uiState.value.rows.isEmpty())
        assertEquals(0, vm.uiState.value.total)
    }


    @Test
    fun todayRollsOverAtMidnight() = runTest(dispatcher) {
        // 23:59:30 in Istanbul; the clock follows the test's virtual time
        val start = java.time.ZonedDateTime.of(2026, 10, 27, 23, 59, 30, 0, java.time.ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli()
        val moving = object : java.time.Clock() {
            override fun getZone() = java.time.ZoneId.of("Europe/Istanbul")
            override fun withZone(zone: java.time.ZoneId?) = this
            override fun instant() = java.time.Instant.ofEpochMilli(start + testScheduler.currentTime)
        }
        val late = LogEntry(0, start - 30_000, LogSource.DEVICE, LogLevel.INFO, null, "smart_plug", "Smart Plug", "ack ON", null, null)
        val vm = LogsViewModel(SavedStateHandle(), FakeLogRepository(listOf(late), moving), FakeDeviceRepository(), FakeSessionRepository(startUserId = "ayse"), moving)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        vm.setRange(DateRange.Today)
        runCurrent()
        assertEquals(listOf("23:59"), vm.uiState.value.rows.map { it.time })
        advanceTimeBy(31_000) // past midnight, with the screen still open
        runCurrent()
        assertTrue(vm.uiState.value.rows.isEmpty()) // yesterday's entry left "Today"
        vm.setRange(DateRange.Yesterday)
        runCurrent()
        assertEquals(listOf("Tue 23:59"), vm.uiState.value.rows.map { it.time })
    }
}
