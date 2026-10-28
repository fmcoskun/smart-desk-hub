package com.iot.widget.data

import com.iot.widget.data.mqtt.LoggingMqttPublisher
import com.iot.widget.data.mqtt.MqttMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoggingMqttPublisherTest {

    @Test
    fun publisherLogsExactFormat() {
        val lines = mutableListOf<String>()
        val publisher = LoggingMqttPublisher(logger = { lines += it })
        assertNull(publisher.lastMessage.value)

        val message = MqttMessage("office/desk/lights", """{"state":"ON"}""")
        publisher.publish(message)

        assertEquals(listOf("""[MQTT PUB] office/desk/lights -> {"state":"ON"}"""), lines)
        assertEquals(message, publisher.lastMessage.value)
    }
}
