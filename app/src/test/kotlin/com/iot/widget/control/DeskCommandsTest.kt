package com.iot.widget.control

import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.PlugTimer
import com.iot.widget.domain.Scene
import com.iot.widget.domain.Topics
import com.iot.widget.domain.isOn
import com.iot.widget.testutil.FakeDeskRepository
import com.iot.widget.testutil.FakeScheduler
import com.iot.widget.testutil.FakeSessionRepository
import com.iot.widget.testutil.RecordingPublisher
import com.iot.widget.testutil.defaultDeskState
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The switching rules shared by the Dashboard and the home-screen widget. */
class DeskCommandsTest {

    private val publisher = RecordingPublisher()
    private val scheduler = FakeScheduler()

    private fun commands(repo: FakeDeskRepository = FakeDeskRepository(), user: String? = "ayse") =
        DeskCommands(repo, publisher, scheduler, FakeSessionRepository(startUserId = user), fixedClock())

    @Test
    fun toggleSwitchesAndPublishes() = runTest {
        val repo = FakeDeskRepository()
        assertTrue(commands(repo).toggle(DeviceId.Soundbar, false))
        assertFalse(repo.flow.value.isOn(DeviceId.Soundbar))
        assertEquals(listOf(Topics.SOUNDBAR), publisher.topics())
    }

    @Test
    fun guestCannotToggle() = runTest {
        val repo = FakeDeskRepository()
        assertFalse(commands(repo, user = "guest").toggle(DeviceId.Soundbar, false))
        assertFalse(commands(repo, user = null).toggle(DeviceId.Soundbar, false)) // signed out
        assertTrue(repo.flow.value.isOn(DeviceId.Soundbar))
        assertTrue(publisher.topics().isEmpty())
    }

    @Test
    fun offlineDeviceNotToggled() = runTest {
        val start = defaultDeskState()
        val repo = FakeDeskRepository(start.copy(devices = start.devices + (DeviceId.SmartPlug to start.devices.getValue(DeviceId.SmartPlug).copy(online = false))))
        assertFalse(commands(repo).toggle(DeviceId.SmartPlug, false))
        assertTrue(repo.flow.value.isOn(DeviceId.SmartPlug))
        assertTrue(publisher.topics().isEmpty())
    }

    @Test
    fun plugOffFromAnywhereCancelsTimer() = runTest {
        val repo = FakeDeskRepository(defaultDeskState(DeskSettings(plugTimer = PlugTimer.H1, plugTimerEndsAtMillis = 5_000L)))
        commands(repo).toggle(DeviceId.SmartPlug, false)
        assertEquals(listOf("cancel"), scheduler.calls)
        assertEquals(listOf(Topics.PLUG, Topics.PLUG_TIMER), publisher.topics())
        assertEquals(PlugTimer.Off, repo.flow.value.settings.plugTimer)
    }

    @Test
    fun sceneAppliesAndPublishes() = runTest {
        val repo = FakeDeskRepository()
        val result = commands(repo).scene(Scene.Meet, publishDim = true)
        assertEquals(Scene.Meet, repo.flow.value.settings.activeScene)
        assertEquals(100, result?.state?.settings?.brightness)
        assertEquals(Topics.SCENES, publisher.topics().dropLast(1).last())
        assertEquals(Topics.LIGHTS_DIM, publisher.topics().last()) // the widget sends the dim at once
    }

    @Test
    fun guestCannotRunScene() = runTest {
        val repo = FakeDeskRepository()
        assertNull(commands(repo, user = "guest").scene(Scene.Leave, publishDim = true))
        assertTrue(publisher.topics().isEmpty())
    }
}
