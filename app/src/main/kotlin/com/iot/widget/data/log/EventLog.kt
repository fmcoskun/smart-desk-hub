package com.iot.widget.data.log

import android.util.Log
import com.iot.widget.data.LogRepository
import com.iot.widget.di.ApplicationScope
import com.iot.widget.domain.logs.LogEntry
import com.iot.widget.domain.logs.LogSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** The level lives in the domain now (domain/logs); this alias keeps older imports working. */
typealias LogLevel = com.iot.widget.domain.logs.LogLevel

/**
 * Data layer · the app's event log, with two sources (spec §6.9):
 *  - client events: what users do ("Ayşe Yılmaz: signed in"),
 *  - device events: what (simulated) devices report ("Room Sensor: low battery").
 * [RoomEventLog] stores them (searchable in the Logs tab). Callers only know this interface,
 * which is why switching from the Logcat-only version needed no change anywhere else.
 */
interface EventLog {
    /** A user action. [deviceId]/[deviceName] tie it to a device (it then shows under that device's "View logs"). */
    fun record(actor: String, message: String, deviceId: String? = null, deviceName: String? = null)

    fun device(deviceId: String, deviceName: String, level: LogLevel, message: String)
}

/**
 * Stores each event in the `logs` table and mirrors it to Logcat. The insert runs on the
 * app-wide scope, so callers (ViewModels, workers) never wait for the database.
 */
class RoomEventLog @Inject constructor(
    private val logs: LogRepository,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) : EventLog {
    private val logcat = LogcatEventLog()

    override fun record(actor: String, message: String, deviceId: String?, deviceName: String?) {
        logcat.record(actor, message, deviceId, deviceName)
        scope.launch {
            logs.add(LogEntry(0, clock.millis(), LogSource.CLIENT, LogLevel.INFO, actor, deviceId, deviceName, message, null, null))
        }
    }

    override fun device(deviceId: String, deviceName: String, level: LogLevel, message: String) {
        logcat.device(deviceId, deviceName, level, message)
        scope.launch {
            logs.add(LogEntry(0, clock.millis(), LogSource.DEVICE, level, null, deviceId, deviceName, message, null, null))
        }
    }
}

/** Logcat only (`adb logcat -s SmartDesk`); used by RoomEventLog for its mirror lines. */
class LogcatEventLog @Inject constructor() : EventLog {
    override fun record(actor: String, message: String, deviceId: String?, deviceName: String?) {
        Log.i("SmartDesk", "[EVENT] $actor: $message" + (deviceName?.let { " ($it)" } ?: ""))
    }

    override fun device(deviceId: String, deviceName: String, level: LogLevel, message: String) {
        Log.i("SmartDesk", "[DEVICE] $level $deviceName: $message")
    }
}
