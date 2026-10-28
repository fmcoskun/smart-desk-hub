package com.iot.widget.data

import com.iot.widget.data.mqtt.MqttMessage
import com.iot.widget.data.mqtt.RecordingMqttPublisher
import com.iot.widget.domain.logs.LogEntry
import com.iot.widget.domain.logs.LogLevel
import com.iot.widget.domain.logs.LogQuery
import com.iot.widget.domain.logs.LogSource
import com.iot.widget.testutil.FakeDeviceRepository
import com.iot.widget.testutil.FakeSessionRepository
import com.iot.widget.testutil.RecordingPublisher
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every MQTT publish becomes a CLIENT log entry; switch topics also get a simulated DEVICE ack. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordingMqttPublisherTest {

    /** Minimal in-memory log store. */
    private class MemoryLogs : LogRepository {
        val entries = mutableListOf<LogEntry>()
        override fun observe(query: LogQuery): Flow<LogPage> = flowOf(LogPage(entries, entries.size))
        override suspend fun add(entry: LogEntry): Long { entries += entry; return entries.size.toLong() }
        override suspend fun byId(id: Long) = null
        override suspend fun prune(nowMillis: Long) = Unit
        override suspend fun seedIfEmpty(entries: () -> List<LogEntry>) = Unit
        override suspend fun addAll(entries: List<LogEntry>) { this.entries += entries }
        override suspend fun hasEntriesBefore(millis: Long) = entries.any { it.timeMillis < millis }
        override suspend fun export(query: LogQuery) = entries
    }

    private val inner = RecordingPublisher()
    private val logs = MemoryLogs()

    private fun TestScope.publisher(signedIn: String? = "ayse") = RecordingMqttPublisher(
        inner, logs, FakeSessionRepository(startUserId = signedIn), FakeDeviceRepository(), fixedClock(), backgroundScope,
    )

    @Test
    fun publishIsRecordedWithActorTopicAndPayload() = runTest {
        publisher().publish(MqttMessage("office/scenes/set", """{"preset":"WORK"}"""))
        runCurrent()
        val e = logs.entries.single()
        assertEquals(LogSource.CLIENT, e.source)
        assertEquals("Ayşe Yılmaz", e.actor)
        assertEquals("published office/scenes/set", e.message)
        assertEquals("office/scenes/set", e.topic)
        assertEquals("""{"preset":"WORK"}""", e.payload)
    }

    @Test
    fun switchTopicGetsDeviceAckAfter200ms() = runTest {
        publisher().publish(MqttMessage("office/desk/plug_main", """{"state":"OFF","timestamp":"x","source":"y"}"""))
        runCurrent()
        assertEquals(1, logs.entries.size)
        advanceTimeBy(201)
        runCurrent()
        val ack = logs.entries.last()
        assertEquals(LogSource.DEVICE, ack.source)
        assertEquals(LogLevel.INFO, ack.level)
        assertEquals("smart_plug", ack.deviceId)
        assertEquals("Smart Plug", ack.deviceName)
        assertEquals("ack OFF", ack.message)
    }

    @Test
    fun nonSwitchTopicGetsNoAck() = runTest {
        publisher().publish(MqttMessage("office/desk/lights/dim", """{"brightness":40}"""))
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(1, logs.entries.size)
    }

    @Test
    fun systemActorWhenSignedOut() = runTest {
        publisher(signedIn = null).publish(MqttMessage("office/desk/lights", """{"state":"ON"}"""))
        runCurrent()
        assertEquals("System", logs.entries.first().actor)
    }

    @Test
    fun automationActorWinsOverSignedInUser() = runTest {
        // an alarm fires while Ayşe is still signed in: the log must say who really acted
        publisher().publish(MqttMessage("office/desk/plug_main", """{"state":"OFF"}""", actor = "Scheduler"))
        runCurrent()
        assertEquals("Scheduler", logs.entries.first().actor)
    }

    @Test
    fun clientEntryNamesTheTargetDevice() = runTest {
        val p = publisher()
        p.publish(MqttMessage("office/desk/plug_main", """{"state":"ON"}"""))
        p.publish(MqttMessage("office/desk/lights/dim", """{"brightness":40}"""))
        p.publish(MqttMessage("office/scenes/set", """{"preset":"WORK"}"""))
        runCurrent()
        val clients = logs.entries.filter { it.source == LogSource.CLIENT }
        assertEquals(listOf("smart_plug", "desk_light", null), clients.map { it.deviceId })
        assertEquals("Smart Plug", clients[0].deviceName)
    }

    @Test
    fun innerPublisherStillCalled() = runTest {
        val p = publisher()
        p.publish(MqttMessage("office/desk/lights", """{"state":"ON"}"""))
        assertEquals(listOf("office/desk/lights"), inner.topics())
        assertTrue(p.lastMessage.value?.topic == "office/desk/lights")
    }


    @Test
    fun noAckForOfflineDevice() = runTest {
        val devices = FakeDeviceRepository()
        devices.setOnline("smart_plug", online = false, atMillis = 0)
        val p = RecordingMqttPublisher(inner, logs, FakeSessionRepository(startUserId = "ayse"), devices, fixedClock(), backgroundScope)
        p.publish(MqttMessage("office/desk/plug_main", """{"state":"OFF"}"""))
        advanceTimeBy(201)
        runCurrent()
        val reply = logs.entries.last()
        assertEquals(LogSource.DEVICE, reply.source)
        assertEquals(LogLevel.WARN, reply.level)
        assertEquals("no ack (offline)", reply.message)
    }
}
