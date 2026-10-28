package com.iot.widget.data.mqtt

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data layer · MqttPublisher that writes each message to Logcat as
 * `[MQTT PUB] topic -> json` (filter with `adb logcat -s SmartDesk`).
 *
 * TODO(MQTT): replace with a real client and keep this class's contract:
 *  - HiveMQ:  client.publishWith().topic(message.topic).payload(message.json.toByteArray()).send()
 *  - Paho:    client.publish(message.topic, MqttMessage(message.json.toByteArray()))
 * Connect once (e.g. from SmartDeskApp) and publish off the main thread.
 *
 * The logger is a constructor parameter so tests can capture the lines without Android's Log.
 */
class LoggingMqttPublisher(
    private val logger: (String) -> Unit = { Log.i(TAG, it) },
) : MqttPublisher {

    // MutableStateFlow is private; the outside world only gets the read-only StateFlow
    private val _lastMessage = MutableStateFlow<MqttMessage?>(null)
    override val lastMessage: StateFlow<MqttMessage?> = _lastMessage.asStateFlow()

    override fun publish(message: MqttMessage) {
        logger("[MQTT PUB] ${message.topic} -> ${message.json}")
        _lastMessage.value = message
    }

    companion object {
        const val TAG = "SmartDesk"
    }
}
