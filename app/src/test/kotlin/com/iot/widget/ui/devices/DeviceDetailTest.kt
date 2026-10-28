package com.iot.widget.ui.devices

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import com.iot.widget.domain.RelativeTime
import com.iot.widget.domain.devices.Room
import com.iot.widget.domain.users.RuleViolation
import com.iot.widget.simulation.DeviceSimulator
import com.iot.widget.testutil.FakeDeviceRepository
import com.iot.widget.testutil.FakeSessionRepository
import com.iot.widget.testutil.RecordingEventLog
import com.iot.widget.testutil.builtInTestDevices
import com.iot.widget.testutil.fixedClock
import com.iot.widget.testutil.testDevice
import com.iot.widget.ui.theme.SmartDeskTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** Device detail: info, rename, change room, restart, delete (spec §6.8). Admin unless stated. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeviceDetailTest {

    @get:Rule
    val rule = createComposeRule()

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeDeviceRepository(builtInTestDevices() + testDevice("dev_1", "Shelf Light"))
    private val log = RecordingEventLog()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private lateinit var sim: DeviceSimulator

    private fun TestScope.viewModel(id: String, user: String = "ayse"): DeviceDetailViewModel {
        sim = DeviceSimulator(repo, log, fixedClock(), Random(1), backgroundScope)
        val vm = DeviceDetailViewModel(SavedStateHandle(mapOf("id" to id)), repo, FakeSessionRepository(startUserId = user), sim, log, fixedClock())
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun renameValidatesAndLogs() = runTest(dispatcher) {
        val vm = viewModel("desk_light")
        vm.startRename()
        vm.setNameDraft("  Desk Lamp ")
        vm.saveName()
        runCurrent()
        assertEquals("Desk Lamp", repo.flow.value.first { it.id == "desk_light" }.name)
        assertNull(vm.uiState.value.nameDraft)
        assertEquals(listOf("Ayşe Yılmaz: renamed device Desk Halo LED to Desk Lamp"), log.lines)
    }

    @Test
    fun renameDuplicateRejected() = runTest(dispatcher) {
        val vm = viewModel("desk_light")
        vm.startRename()
        vm.setNameDraft("room sensor")
        vm.saveName()
        runCurrent()
        assertEquals(RuleViolation.NAME_TAKEN, vm.uiState.value.error)
        assertEquals("Desk Halo LED", repo.flow.value.first { it.id == "desk_light" }.name)
    }

    @Test
    fun roomChangePersists() = runTest(dispatcher) {
        val vm = viewModel("cam_hallway")
        vm.setRoom(Room.ENTRANCE)
        runCurrent()
        assertEquals(Room.ENTRANCE, repo.flow.value.first { it.id == "cam_hallway" }.room)
    }

    @Test
    fun restartCallsSimulatorOnce() = runTest(dispatcher) {
        val vm = viewModel("smart_plug")
        vm.restart()
        vm.restart()
        runCurrent()
        assertTrue(vm.uiState.value.restarting)
        assertEquals(1, log.deviceLines.count { it.contains("restarting") })
    }

    @Test
    fun deleteBuiltInRefused() = runTest(dispatcher) {
        val vm = viewModel("cam_desk")
        assertFalse(vm.uiState.value.canDelete)
        vm.delete()
        runCurrent()
        assertEquals(11, repo.flow.value.size)
        assertFalse(vm.uiState.value.deleted)
    }

    @Test
    fun deleteAddedDeviceAsAdmin() = runTest(dispatcher) {
        val vm = viewModel("dev_1")
        assertTrue(vm.uiState.value.canDelete)
        vm.delete()
        runCurrent()
        assertTrue(vm.uiState.value.deleted)
        assertNull(repo.flow.value.firstOrNull { it.id == "dev_1" })
        assertEquals(listOf("Ayşe Yılmaz: deleted device Shelf Light"), log.lines)
    }

    @Test
    fun guestCannotEdit() = runTest(dispatcher) {
        val vm = viewModel("desk_light", user = "guest")
        assertFalse(vm.uiState.value.canEdit)
        vm.setRoom(Room.HALLWAY)
        vm.restart()
        vm.startRename()
        vm.setNameDraft("Guest Lamp")
        vm.saveName()
        runCurrent()
        assertEquals(Room.DESK, repo.flow.value.first { it.id == "desk_light" }.room)
        assertEquals("Desk Halo LED", repo.flow.value.first { it.id == "desk_light" }.name)
        assertTrue(log.deviceLines.isEmpty())
    }

    @Test
    fun deleteNotOfferedForBuiltIn() {
        rule.setContent {
            SmartDeskTheme {
                DeviceDetailContent(
                    state = DeviceDetailUiState(
                        device = builtInTestDevices().first { it.id == "cam_desk" },
                        lastSeen = RelativeTime.Now,
                        canEdit = true,
                        // the real rule, as the ViewModel computes it, for an Admin
                        canDelete = com.iot.widget.domain.devices.DeviceRules.canDelete(
                            com.iot.widget.testutil.SEED_TEST_USERS.first { it.id == "ayse" },
                            builtInTestDevices().first { it.id == "cam_desk" },
                        ),
                    ),
                    actions = DeviceDetailActions.NONE,
                )
            }
        }
        rule.onNodeWithText("Restart").assertExists()
        rule.onNodeWithText("Delete device").assertDoesNotExist()
    }


    @Test
    fun onlineDeviceLastSeenIsNow() = runTest(dispatcher) {
        assertEquals(com.iot.widget.domain.RelativeTime.Now, viewModel("smart_plug").uiState.value.lastSeen)
    }

    @Test
    fun renameToSameNameLogsNothing() = runTest(dispatcher) {
        val vm = viewModel("dev_1")
        vm.startRename()
        vm.setNameDraft(" Shelf Light ")
        vm.saveName()
        runCurrent()
        assertTrue(log.lines.isEmpty())
        assertEquals(null, vm.uiState.value.nameDraft) // the editor closes
    }

    @Test
    fun doubleSaveRenamesOnce() = runTest(dispatcher) {
        val vm = viewModel("dev_1")
        vm.startRename()
        vm.setNameDraft("Shelf Lamp")
        vm.saveName()
        vm.saveName()
        runCurrent()
        assertEquals(1, log.lines.count { it.contains("renamed device") })
    }


    @Test
    fun unknownDeviceShowsNotFound() {
        rule.setContent {
            com.iot.widget.ui.theme.SmartDeskTheme { DeviceDetailContent(DeviceDetailUiState(loaded = true), DeviceDetailActions.NONE) }
        }
        rule.onNodeWithText("This device no longer exists.").assertExists()
    }
}
