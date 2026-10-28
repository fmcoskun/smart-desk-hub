package com.iot.widget.data.mqtt

import kotlinx.coroutines.flow.StateFlow

/**
 * Data layer · one MQTT message: a topic (an address like "office/desk/lights") and a JSON
 * payload. The app builds these in the domain layer and hands them to an MqttPublisher.
 * [actor] names who sent it in the event log when that is not the signed-in user
 * (see [Actors]); null means "whoever is signed in".
 */
data class MqttMessage(val topic: String, val json: String, val actor: String? = null)

/** Log actors for messages no person sent. */
object Actors {
    /** Rules the app runs by itself, e.g. the screen-lock privacy rule. */
    const val SYSTEM = "System"

    /** Scheduled automations (plug timer, LED schedule, sunset dim). */
    const val SCHEDULER = "Scheduler"
}

/**
 * Data layer · where every MQTT message goes. Today [LoggingMqttPublisher] only logs them;
 * a real broker client implements this same interface later, and nothing else changes.
 */
interface MqttPublisher {
    /** The most recent message, shown in the dashboard's "MQTT Bridge Console". */
    val lastMessage: StateFlow<MqttMessage?>

    fun publish(message: MqttMessage)
}
