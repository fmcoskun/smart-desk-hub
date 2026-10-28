package com.iot.widget.simulation

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Simulation · one reading of the simulated room sensor (feeds the Dashboard footer).
 */
data class SensorReading(val temperatureC: Double, val humidityPct: Double)

/**
 * Simulation layer · fake room sensor (the reference App.java `startSensorFeed`): every 5 s the
 * temperature drifts by ±0.2 °C and humidity by ±1 %. Replace with real sensor/MQTT data later.
 *
 * `flow { }` is a cold stream: the loop only runs while someone collects it, and stops when
 * they stop — so no work is done while the dashboard is not on screen. @Singleton keeps one
 * simulator for the whole app, and each new collection continues from [latest], so leaving
 * the Dashboard for a detail page does not reset the room to 22.4 °C / 48 %.
 */
@Singleton
class SensorSimulator @Inject constructor(private val random: Random) {

    /** The most recent reading (the starting values until the first tick). */
    @Volatile
    var latest: SensorReading = INITIAL
        private set

    fun readings(intervalMs: Long = 5_000): Flow<SensorReading> = flow {
        while (true) {
            delay(intervalMs) // suspends without blocking a thread
            val previous = latest
            val next = SensorReading(
                temperatureC = previous.temperatureC + (random.nextDouble() - 0.5) * 0.4,
                humidityPct = previous.humidityPct + (random.nextDouble() - 0.5) * 2,
            )
            latest = next
            emit(next)
        }
    }

    companion object {
        val INITIAL = SensorReading(22.4, 48.0)
    }
}
