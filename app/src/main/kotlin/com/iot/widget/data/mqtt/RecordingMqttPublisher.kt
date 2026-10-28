package com.iot.widget.data.mqtt

import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.LogRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.domain.Topics
import com.iot.widget.domain.logs.LogEntry
import com.iot.widget.domain.logs.LogLevel
import com.iot.widget.domain.logs.LogSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * Data layer · a *decorator*: it is an MqttPublisher that wraps another MqttPublisher. It passes
 * every message on unchanged and additionally writes it to the event log (spec §6.9):
 *  - a CLIENT entry "published <topic>" with the payload and the device the topic belongs to.
 *    The actor is the message's own [MqttMessage.actor] ("Scheduler" for automations, "System"
 *    for the screen-lock rule); otherwise the signed-in user, or "System" when signed out.
 *    (Someone is nearly always signed in, so an alarm firing in the background must say so
 *    itself, or the log would blame the user.)
 *  - for the four switch topics, ~200 ms later a DEVICE entry "ack ON/OFF" from that device,
 *    the confirmation a real device would send back (WARN "no ack (offline)" if it is offline).
 * Because callers only see the MqttPublisher interface, no screen had to change for this.
 */
class RecordingMqttPublisher(
    private val inner: MqttPublisher,
    private val logs: LogRepository,
    private val session: SessionRepository,
    private val devices: DeviceRepository,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val ackDelayMs: Long = ACK_DELAY_MS,
) : MqttPublisher {

    override val lastMessage: StateFlow<MqttMessage?> get() = inner.lastMessage

    override fun publish(message: MqttMessage) {
        inner.publish(message)
        scope.launch {
            val actor = message.actor ?: session.currentUser.first()?.name ?: SYSTEM
            // the device id lets the device page's "View logs" find this entry too
            val target = TOPIC_DEVICES[message.topic]
            val targetName = target?.let { devices.byId(it)?.name ?: it }
            logs.add(
                LogEntry(0, clock.millis(), LogSource.CLIENT, LogLevel.INFO, actor, target, targetName,
                    "published ${message.topic}", message.topic, message.json),
            )
            val deviceId = SWITCH_TOPICS[message.topic] ?: return@launch
            delay(ackDelayMs)
            val device = devices.byId(deviceId)
            val name = device?.name ?: deviceId
            if (device?.online == false) {
                // an offline (or restarting) device cannot confirm anything
                logs.add(LogEntry(0, clock.millis(), LogSource.DEVICE, LogLevel.WARN, null, deviceId, name, "no ack (offline)", null, null))
                return@launch
            }
            val state = if (message.json.contains("\"state\":\"ON\"")) "ON" else "OFF"
            logs.add(LogEntry(0, clock.millis(), LogSource.DEVICE, LogLevel.INFO, null, deviceId, name, "ack $state", null, null))
        }
    }

    companion object {
        const val ACK_DELAY_MS = 200L
        const val SYSTEM = Actors.SYSTEM

        /** Switch topic → the dashboard device that acknowledges it. */
        val SWITCH_TOPICS = mapOf(
            Topics.SOUNDBAR to "soundbar",
            Topics.LIGHTS to "desk_light",
            Topics.PLUG to "smart_plug",
            Topics.PRIVACY to "privacy_guard",
        )

        /** Every device topic → its device (switches plus their settings); scenes have none. */
        val TOPIC_DEVICES = SWITCH_TOPICS + mapOf(
            Topics.SOUNDBAR_OUTPUT to "soundbar",
            Topics.SOUNDBAR_VOLUME to "soundbar",
            Topics.LIGHTS_DIM to "desk_light",
            Topics.LIGHTS_CT to "desk_light",
            Topics.LIGHTS_AUTODIM to "desk_light",
            Topics.LIGHTS_SCHEDULE to "desk_light",
            Topics.PLUG_TIMER to "smart_plug",
            Topics.PRIVACY_ON_LEAVE to "privacy_guard",
        )
    }
}
