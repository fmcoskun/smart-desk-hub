package com.iot.widget.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.iot.widget.domain.AudioOutput
import com.iot.widget.domain.DetailType
import com.iot.widget.domain.PlugTimer
import com.iot.widget.domain.ScheduleDays
import com.iot.widget.domain.Topics
import com.iot.widget.testutil.FakeDeskRepository
import com.iot.widget.testutil.FakeExactAlarms
import com.iot.widget.testutil.FakeScheduler
import com.iot.widget.testutil.FakeSessionRepository
import com.iot.widget.testutil.RecordingPublisher
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeDeskRepository()
    private val publisher = RecordingPublisher()
    private var session = FakeSessionRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // The navigation library puts route arguments into the SavedStateHandle under their property names
    private fun handle(type: DetailType) = SavedStateHandle(mapOf("type" to type.key))

    private fun TestScope.viewModel(h: SavedStateHandle): DetailViewModel {
        val vm = DetailViewModel(h, repo, publisher, FakeScheduler(), FakeExactAlarms(allowed = true), session, fixedClock())
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun opensPublishesDetailOnce() = runTest(dispatcher) {
        val h = handle(DetailType.Bluetooth)
        viewModel(h)
        viewModel(h) // same SavedStateHandle = same screen re-created (e.g. after rotation)
        assertEquals(listOf(Topics.WIDGET_DETAIL), publisher.topics())
        assertEquals("""{"open":"bluetooth"}""", publisher.messages.single().json)
    }

    @Test
    fun colorTempPublishesStringValue() = runTest(dispatcher) {
        val vm = viewModel(handle(DetailType.Light))
        vm.setColorTemp(6500)
        runCurrent()
        assertEquals(Topics.LIGHTS_CT, publisher.messages.last().topic)
        assertEquals("""{"value":"6500"}""", publisher.messages.last().json)
        assertEquals(6500, repo.flow.value.settings.colorTempK)
    }

    @Test
    fun audioOutputPublishesAndMovesActivePill() = runTest(dispatcher) {
        val vm = viewModel(handle(DetailType.Bluetooth))
        vm.setAudioOutput(AudioOutput.Headphones)
        runCurrent()
        assertEquals(Topics.SOUNDBAR_OUTPUT, publisher.messages.last().topic)
        assertEquals("""{"value":"headphones"}""", publisher.messages.last().json)
        assertEquals(AudioOutput.Headphones, vm.uiState.value!!.settings.audioOutput)
    }

    @Test
    fun privacyOnLeavePublishesState() = runTest(dispatcher) {
        val vm = viewModel(handle(DetailType.Privacy))
        vm.setPrivacyOnLeave(false)
        runCurrent()
        assertEquals(Topics.PRIVACY_ON_LEAVE, publisher.messages.last().topic)
        assertEquals("""{"state":"OFF"}""", publisher.messages.last().json)
        assertFalse(repo.flow.value.settings.privacyOnLeave)
    }

    @Test
    fun guestCannotSetDetailSettings() = runTest(dispatcher) {
        session = FakeSessionRepository(startUserId = "guest")
        val vm = viewModel(handle(DetailType.Light))
        publisher.messages.clear()
        vm.setColorTemp(6500)
        vm.setAutoDim(false)
        runCurrent()
        assertTrue(publisher.messages.isEmpty())
        assertEquals(4000, repo.flow.value.settings.colorTempK)
        assertFalse(vm.uiState.value!!.canControl)
    }

    @Test
    fun volumeIsDebounced() = runTest(dispatcher) {
        val vm = viewModel(handle(DetailType.Bluetooth))
        vm.setVolume(50)
        advanceTimeBy(50)
        vm.setVolume(60)
        advanceTimeBy(151)
        runCurrent()
        val volumes = publisher.messages.filter { it.topic == Topics.SOUNDBAR_VOLUME }
        assertEquals(listOf("""{"value":60}"""), volumes.map { it.json })
        assertEquals(60, repo.flow.value.settings.volume)
    }


    @Test
    fun guestCannotUseAnyDetailSetting() = runTest(dispatcher) {
        session = FakeSessionRepository(startUserId = "guest")
        val before = repo.flow.value
        listOf(DetailType.Bluetooth, DetailType.Light, DetailType.Rig).forEach { type ->
            val vm = viewModel(handle(type))
            publisher.messages.clear()
            vm.setPlugTimer(PlugTimer.H1)
            vm.setSchedule(java.time.LocalTime.of(6, 0), ScheduleDays.EVERY_DAY, java.time.LocalTime.of(23, 0), ScheduleDays.EVERY_DAY, true)
            vm.setVolume(10)
            vm.setAudioOutput(AudioOutput.Headphones)
            advanceTimeBy(1_000)
            runCurrent()
            assertTrue("$type published ${publisher.messages}", publisher.messages.isEmpty())
        }
        assertEquals(before.settings, repo.flow.value.settings)
    }
}
