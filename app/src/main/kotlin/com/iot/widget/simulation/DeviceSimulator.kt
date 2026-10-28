package com.iot.widget.simulation

import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.log.EventLog
import com.iot.widget.data.log.LogLevel
import com.iot.widget.di.ApplicationScope
import com.iot.widget.domain.devices.BuiltIn
import com.iot.widget.domain.devices.isOnDashboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Simulation layer · what real devices would do on their own (spec §9a):
 *  - Restart: the device drops offline for 5 s, then comes back (dashboard cards react because
 *    they read the same `online` flag; the camera tiles of step 5 will too),
 *  - background chatter: every minute there is a 50 % chance that a device not on the dashboard
 *    reports something — "telemetry ok", "low battery", or "connection timeout" when offline
 *    (so at most one event a minute, about one every two minutes on average).
 *
 * It runs on the app-wide scope, so a restart finishes even if the user leaves the screen.
 * Replace with real device status (MQTT "status" topics) later.
 */
@Singleton
class DeviceSimulator @Inject constructor(
    private val devices: DeviceRepository,
    private val eventLog: EventLog,
    private val clock: Clock,
    private val random: Random,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _restarting = MutableStateFlow<Set<String>>(emptySet())

    /** Ids of devices currently restarting (the UI shows "RESTARTING"). */
    val restarting: StateFlow<Set<String>> = _restarting.asStateFlow()

    private var eventsJob: Job? = null

    /** Starts a restart; returns false if that device is already restarting (no overlapping cycles). */
    fun restart(id: String): Boolean {
        var started = false
        // update { } is atomic: two quick taps cannot both see "not restarting"
        _restarting.update { current ->
            if (id in current) current else current + id.also { started = true }
        }
        if (!started) return false
        scope.launch {
            try {
                val name = devices.byId(id)?.name ?: id
                devices.setOnline(id, online = false, atMillis = clock.millis())
                eventLog.device(id, name, LogLevel.WARN, "restarting…")
                delay(RESTART_MS)
                // deleted during the restart: nothing comes back
                if (devices.byId(id) != null) {
                    devices.setOnline(id, online = true, atMillis = clock.millis())
                    eventLog.device(id, name, LogLevel.INFO, "back online")
                }
            } finally {
                _restarting.update { it - id }
            }
        }
        return true
    }

    /**
     * Called at app start: finishes restarts that the app did not live to complete (the process
     * was killed during the 5 s), then starts the background chatter. Calling it again does nothing.
     */
    fun start() {
        if (eventsJob != null) return
        eventsJob = scope.launch {
            recoverInterruptedRestarts()
            while (true) {
                delay(EVENT_INTERVAL_MS)
                if (random.nextDouble() < 0.5) emitRandomEvent()
            }
        }
    }

    /** Only a restart takes a device offline, so any offline device (except those seeded offline) comes back. */
    private suspend fun recoverInterruptedRestarts() {
        devices.devices.first()
            .filter { !it.online && it.id !in OFFLINE_BY_DESIGN && it.id !in _restarting.value }
            .forEach {
                devices.setOnline(it.id, online = true, atMillis = clock.millis())
                eventLog.device(it.id, it.name, LogLevel.INFO, "back online")
            }
    }

    private suspend fun emitRandomEvent() {
        val candidates = devices.devices.first().filterNot { it.isOnDashboard }
        if (candidates.isEmpty()) return
        val device = candidates[random.nextInt(candidates.size)]
        val battery = device.info.batteryPct
        when {
            !device.online -> eventLog.device(device.id, device.name, LogLevel.ERROR, "connection timeout")
            battery != null && battery <= LOW_BATTERY -> eventLog.device(device.id, device.name, LogLevel.WARN, "low battery ($battery%)")
            else -> eventLog.device(device.id, device.name, LogLevel.INFO, "telemetry ok")
        }
    }

    companion object {
        const val RESTART_MS = 5_000L
        const val EVENT_INTERVAL_MS = 60_000L
        const val LOW_BATTERY = 20

        /** Seeded as offline on purpose, to show that state in the demo. */
        val OFFLINE_BY_DESIGN = setOf(BuiltIn.MEETING_SENSOR)
    }
}
