package com.iot.widget.control

import com.iot.widget.automation.AutomationScheduler
import com.iot.widget.data.DeskRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.data.mqtt.MqttMessage
import com.iot.widget.data.mqtt.MqttPublisher
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.Payloads
import com.iot.widget.domain.PlugTimer
import com.iot.widget.domain.Scene
import com.iot.widget.domain.SceneResult
import com.iot.widget.domain.Topics
import com.iot.widget.domain.applyScene
import com.iot.widget.domain.isReachable
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.can
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Control layer · the one place that switches desk devices and runs scenes, used by the
 * Dashboard and by the home-screen widget. Keeping the rules here means the widget can never
 * behave differently from the app:
 *  - only users with CONTROL_DEVICES (not Guest, not signed out),
 *  - an offline device gets no command,
 *  - the state is saved first, then the MQTT messages are published,
 *  - switching the plug off (by hand or by Leave) cancels its auto-off job and tells the plug.
 */
class DeskCommands @Inject constructor(
    private val repo: DeskRepository,
    private val publisher: MqttPublisher,
    private val scheduler: AutomationScheduler,
    private val session: SessionRepository,
    private val clock: Clock,
) {
    suspend fun canControl(): Boolean = session.currentUser.first()?.can(Permission.CONTROL_DEVICES) == true

    /** Returns false when nothing happened (not allowed, or the device is unreachable). */
    suspend fun toggle(id: DeviceId, on: Boolean): Boolean {
        if (!canControl()) return false
        val before = repo.current()
        if (!before.isReachable(id)) return false
        repo.setDeviceOn(id, on)
        // switching the plug off cancels its auto-off job (the repository clears the setting)
        val timerCancelled = id == DeviceId.SmartPlug && !on && before.settings.plugTimer != PlugTimer.Off
        if (id == DeviceId.SmartPlug && !on) scheduler.cancelPlugTimer()
        if (id == DeviceId.PrivacyGuard && on) publish(Topics.WIDGET_EVENT, Payloads.notice(Payloads.PRIVACY_NOTICE))
        publish(Topics.toggleTopic(id), Payloads.toggle(on, now()))
        // tell the device its timer is gone too, as the timer worker does
        if (timerCancelled) publish(Topics.PLUG_TIMER, Payloads.value(PlugTimer.Off.key))
        return true
    }

    /**
     * Applies a scene (read + apply + save in one transaction, so a worker cannot write in
     * between) and publishes its messages. [publishDim]: send the LED brightness now — the
     * Dashboard says false because it sends it through its own 150 ms debounce.
     * Returns null when not allowed.
     */
    suspend fun scene(scene: Scene, publishDim: Boolean): SceneResult? {
        if (!canControl()) return null
        var sceneResult: SceneResult? = null
        val (before, _) = repo.updateState { current -> applyScene(current, scene, now()).also { sceneResult = it }.state }
        val result = checkNotNull(sceneResult)
        val timerCancelled = before.settings.plugTimer != PlugTimer.Off && result.state.settings.plugTimer == PlugTimer.Off
        if (timerCancelled) scheduler.cancelPlugTimer() // Leave switched the plug off
        result.messages.forEach(publisher::publish)
        if (timerCancelled) publish(Topics.PLUG_TIMER, Payloads.value(PlugTimer.Off.key))
        // the scene did not touch an unreachable light, so it gets no dim message either
        if (publishDim && result.state.isReachable(DeviceId.DeskLight)) {
            publish(Topics.LIGHTS_DIM, Payloads.brightness(result.state.settings.brightness))
        }
        return result
    }

    private fun publish(topic: String, json: String) = publisher.publish(MqttMessage(topic, json))

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock)
}
