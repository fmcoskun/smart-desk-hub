package com.iot.widget.ui.devices

import com.iot.widget.domain.devices.DeviceType
import com.iot.widget.domain.devices.PairingStep
import com.iot.widget.domain.devices.Room
import com.iot.widget.domain.users.RuleViolation
import com.iot.widget.testutil.FakeDeviceRepository
import com.iot.widget.testutil.FakeSessionRepository
import com.iot.widget.testutil.RecordingEventLog
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class AddDeviceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeDeviceRepository()
    private val log = RecordingEventLog()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(user: String = "ayse") =
        AddDeviceViewModel(repo, FakeSessionRepository(startUserId = user), log, fixedClock(), Random(5))

    private fun kotlinx.coroutines.test.TestScope.searchAndContinue(vm: AddDeviceViewModel, type: DeviceType) {
        vm.chooseType(type)
        runCurrent()
        assertEquals(PairingStep.Searching(type), vm.uiState.value.step)
        advanceTimeBy(AddDeviceViewModel.SEARCH_MS + 1)
        runCurrent()
        vm.continueToNaming()
    }

    @Test
    fun fullFlowAddsOnlineDevice() = runTest(dispatcher) {
        val vm = viewModel()
        searchAndContinue(vm, DeviceType.LIGHT)
        vm.setName("Shelf Light")
        vm.setRoom(Room.HALLWAY)
        vm.add()
        runCurrent()
        val added = repo.flow.value.last()
        assertEquals("Shelf Light", added.name)
        assertEquals(Room.HALLWAY, added.room)
        assertTrue(added.online)
        assertTrue(added.id.startsWith("dev_"))
        assertEquals(PairingStep.Done, vm.uiState.value.step)
        assertEquals(listOf("Ayşe Yılmaz: added device Shelf Light"), log.lines)
    }

    @Test
    fun addRequiresAdmin() = runTest(dispatcher) {
        val vm = viewModel("mehmet")
        vm.chooseType(DeviceType.PLUG)
        runCurrent()
        assertEquals(PairingStep.ChooseType, vm.uiState.value.step)
        assertEquals(10, repo.flow.value.size)
    }

    @Test
    fun duplicateNameRejected() = runTest(dispatcher) {
        val vm = viewModel()
        searchAndContinue(vm, DeviceType.SENSOR)
        vm.setName("room sensor")
        vm.add()
        runCurrent()
        assertEquals(RuleViolation.NAME_TAKEN, vm.uiState.value.error)
        assertEquals(10, repo.flow.value.size)
    }

    @Test
    fun doubleTapAddCreatesOne() = runTest(dispatcher) {
        val vm = viewModel()
        searchAndContinue(vm, DeviceType.PLUG)
        vm.add()
        vm.add()
        runCurrent()
        assertEquals(11, repo.flow.value.size)
    }


    @Test
    fun secondTypeCancelsFirstSearch() = runTest(dispatcher) {
        val vm = viewModel()
        vm.chooseType(DeviceType.LIGHT)
        advanceTimeBy(AddDeviceViewModel.SEARCH_MS / 2)
        vm.cancel()
        vm.chooseType(DeviceType.PLUG)
        // the first search's timer must not end the second search early
        advanceTimeBy(AddDeviceViewModel.SEARCH_MS / 2 + 1)
        runCurrent()
        assertEquals(PairingStep.Searching(DeviceType.PLUG), vm.uiState.value.step)
    }

    @Test
    fun foundNameAvoidsExistingDevices() = runTest(dispatcher) {
        val vm = viewModel()
        searchAndContinue(vm, DeviceType.PLUG) // "Smart Plug" is a built-in
        assertEquals("Smart Plug 2", (vm.uiState.value.step as PairingStep.Naming).name)
    }


    @Test
    fun addedNameIsNormalized() = runTest(dispatcher) {
        val vm = viewModel()
        searchAndContinue(vm, DeviceType.LIGHT)
        // "ş" typed as s + combining cedilla, with spaces around: stored like a rename would store it
        val decomposed = " Ay" + "s" + Char(0x0327) + "e Lamp "
        vm.setName(decomposed)
        vm.add()
        runCurrent()
        val expected = java.text.Normalizer.normalize(decomposed.trim(), java.text.Normalizer.Form.NFC)
        assertEquals(expected, repo.flow.value.last().name)
    }
}
