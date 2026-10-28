package com.iot.widget.simulation

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SensorSimulatorTest {

    @Test
    fun emitsEveryIntervalWithinDrift() = runTest {
        val readings = SensorSimulator(Random(42)).readings(intervalMs = 5_000).take(3).toList()

        // runTest uses virtual time: 3 readings at 5 s intervals = 15 s, without really waiting
        assertEquals(15_000L, currentTime)
        var previous = SensorReading(22.4, 48.0)
        readings.forEach {
            assertTrue(abs(it.temperatureC - previous.temperatureC) <= 0.2)
            assertTrue(abs(it.humidityPct - previous.humidityPct) <= 1.0)
            previous = it
        }
    }


    @Test
    fun sensorContinuesAcrossSubscriptions() = runTest {
        val sim = SensorSimulator(Random(42))
        val first = sim.readings(intervalMs = 5_000).take(3).toList()
        // leaving and re-opening the Dashboard resumes from the last reading, not from 22.4 °C
        assertEquals(first.last(), sim.latest)
    }
}
