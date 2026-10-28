package com.iot.widget.domain

import com.iot.widget.data.mqtt.MqttMessage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/** Pure desk rules ported from the reference app.js: scenes, power, payloads, formatting. */
class DeskLogicTest {

    private val now = ZonedDateTime.of(2026, 10, 27, 17, 21, 34, 0, ZoneId.of("Europe/Istanbul"))
    private val originalLocale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(originalLocale)

    private fun defaultState() = DeskState(
        devices = mapOf(
            DeviceId.Soundbar to DeviceState(DeviceId.Soundbar, "Bluetooth", "Audio Engine Connected", isOn = true),
            DeviceId.DeskLight to DeviceState(DeviceId.DeskLight, "Desk Halo LED", "Philips Hue ScreenBar", isOn = true),
            DeviceId.SmartPlug to DeviceState(DeviceId.SmartPlug, "Smart Plug", "Peripherals & Monitors", isOn = true),
            DeviceId.PrivacyGuard to DeviceState(DeviceId.PrivacyGuard, "Privacy Guard", "Mic & Webcam Shutter", isOn = false),
        ),
        settings = DeskSettings(),
    )

    @Test
    fun defaultPowerIs143W() {
        assertEquals(143, powerDrawWatts(defaultState()))
    }

    @Test
    fun leaveSceneTurnsOffAndEnablesPrivacy() {
        val result = applyScene(defaultState(), Scene.Leave, now)
        val s = result.state
        assertFalse(s.isOn(DeviceId.Soundbar))
        assertFalse(s.isOn(DeviceId.DeskLight))
        assertFalse(s.isOn(DeviceId.SmartPlug))
        assertTrue(s.isOn(DeviceId.PrivacyGuard))
        assertEquals(5, s.settings.brightness)
        assertEquals(Scene.Leave, s.settings.activeScene)
        assertEquals(
            listOf(Topics.SOUNDBAR, Topics.LIGHTS, Topics.PLUG, Topics.WIDGET_EVENT, Topics.PRIVACY, Topics.SCENES),
            result.messages.map(MqttMessage::topic),
        )
        assertEquals("""{"notice":"Privacy Mode Activated: Mic & Cam Cutoff"}""", result.messages[3].json)
        assertEquals("""{"preset":"LEAVE"}""", result.messages.last().json)
    }

    @Test
    fun leaveSceneRespectsPrivacyOnLeaveOff() {
        val start = defaultState().let { it.copy(settings = it.settings.copy(privacyOnLeave = false)) }
        val result = applyScene(start, Scene.Leave, now)
        assertFalse(result.state.isOn(DeviceId.PrivacyGuard))
        assertTrue(result.messages.none { it.topic == Topics.WIDGET_EVENT })
    }

    @Test
    fun meetSetsBrightness100() {
        val result = applyScene(defaultState(), Scene.Meet, now)
        assertEquals(100, result.state.settings.brightness)
        assertTrue(result.state.isOn(DeviceId.DeskLight))
        assertFalse(result.state.isOn(DeviceId.PrivacyGuard))
    }

    @Test
    fun workSetsBrightness85() {
        val result = applyScene(defaultState(), Scene.Work, now)
        assertEquals(85, result.state.settings.brightness)
        assertEquals("""{"preset":"WORK"}""", result.messages.last().json)
    }

    @Test
    fun togglePayloadFormat() {
        assertEquals(
            """{"state":"ON","timestamp":"5:21:34 PM","source":"android-desk-widget"}""",
            Payloads.toggle(true, now),
        )
    }

    @Test
    fun formattingIgnoresTurkishLocale() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        assertEquals("22.4°C", Formatting.temperature(22.44))
        assertEquals("22.5°C", Formatting.temperature(22.46))
        assertEquals("49%", Formatting.humidity(48.6))
        assertEquals("""{"value":85}""", Payloads.value(85))
        assertEquals(
            """{"state":"OFF","timestamp":"5:21:34 PM","source":"android-desk-widget"}""",
            Payloads.toggle(false, now),
        )
    }

    @Test
    fun sceneSkipsOfflineDevice() {
        val start = defaultState().let {
            it.copy(devices = it.devices + (DeviceId.SmartPlug to it.devices.getValue(DeviceId.SmartPlug).copy(online = false)))
        }
        val result = applyScene(start, Scene.Leave, now)
        assertTrue(result.state.isOn(DeviceId.SmartPlug)) // unreachable: keeps its state
        assertTrue(result.messages.none { it.topic == Topics.PLUG })
        assertFalse(result.state.isOn(DeviceId.Soundbar))
    }

    @Test
    fun leaveClearsPlugTimer() {
        val start = defaultState().let {
            it.copy(settings = it.settings.copy(plugTimer = PlugTimer.M30, plugTimerEndsAtMillis = 123L))
        }
        val s = applyScene(start, Scene.Leave, now).state.settings
        assertEquals(PlugTimer.Off, s.plugTimer)
        assertNull(s.plugTimerEndsAtMillis)
    }
}
