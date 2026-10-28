package com.iot.widget.domain

import com.iot.widget.data.mqtt.MqttMessage
import java.time.ZonedDateTime

/** The new desk state after a scene, plus the MQTT messages to send, in order. */
data class SceneResult(val state: DeskState, val messages: List<MqttMessage>)

/**
 * Domain layer · Work / Meet / Leave presets (reference app.js `applyScene`).
 *
 * A pure function: same input → same output, no side effects. The ViewModel decides *when*
 * to save and publish; this only decides *what*. That split is what makes it easy to test.
 *
 * The dimmer message is not in the list: like the reference, it is sent by the ViewModel's
 * 150 ms debounce, so it arrives after "office/scenes/set".
 */
fun applyScene(state: DeskState, scene: Scene, now: ZonedDateTime): SceneResult {
    val allOn = scene != Scene.Leave
    // Leave turns Privacy Guard on only when "Enable on Leave scene" is on (spec §9b)
    val privacyOn = when (scene) {
        Scene.Work, Scene.Meet -> false
        Scene.Leave -> if (state.settings.privacyOnLeave) true else state.isOn(DeviceId.PrivacyGuard)
    }
    val brightness = when (scene) {
        Scene.Work -> 85
        Scene.Meet -> 100
        Scene.Leave -> MIN_BRIGHTNESS // the reference sets 0; the slider clamps it to its minimum
    }

    // An offline device cannot be reached: it keeps its state and gets no message (spec §9a)
    fun reachable(id: DeviceId) = state.devices[id]?.online != false
    val targets = listOf(
        DeviceId.Soundbar to allOn,
        DeviceId.DeskLight to allOn,
        DeviceId.SmartPlug to allOn,
        DeviceId.PrivacyGuard to privacyOn,
    ).filter { (id, _) -> reachable(id) }

    // an unreachable light keeps its brightness too
    val newBrightness = if (reachable(DeviceId.DeskLight)) brightness else state.settings.brightness
    val next = targets
        .fold(state) { s, (id, on) -> s.withDevice(id, on) }
        .let { it.copy(settings = it.settings.copy(brightness = newBrightness, activeScene = scene)) }

    val messages = buildList {
        targets.forEach { (id, on) ->
            if (id == DeviceId.PrivacyGuard && on) add(MqttMessage(Topics.WIDGET_EVENT, Payloads.notice(Payloads.PRIVACY_NOTICE)))
            add(MqttMessage(Topics.toggleTopic(id), Payloads.toggle(on, now)))
        }
        add(MqttMessage(Topics.SCENES, Payloads.preset(scene)))
    }
    return SceneResult(next, messages)
}
