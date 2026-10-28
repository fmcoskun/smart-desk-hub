package com.iot.widget.data

import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.DeskState
import com.iot.widget.domain.DeviceId
import kotlinx.coroutines.flow.Flow

/**
 * Data layer · the single source of truth for the desk (spec §9a). Screens and background
 * workers read and change the desk only through this interface.
 *
 * It is an interface so the storage can be swapped: today Room on the phone, later a real
 * REST/MQTT backend, and in tests a simple in-memory fake — callers do not change.
 */
interface DeskRepository {
    /** Live desk state; emits again after every change, from any screen or worker. */
    val state: Flow<DeskState>

    /** Inserts the sample devices and default settings if they are not there yet. */
    suspend fun ensureSeeded()

    suspend fun current(): DeskState

    /** Switches a device; switching the plug off also cancels its timer. */
    suspend fun setDeviceOn(id: DeviceId, on: Boolean)

    suspend fun updateSettings(transform: (DeskSettings) -> DeskSettings)

    /**
     * Reads the desk, applies [transform] and saves the result in ONE database transaction, so
     * a background worker cannot write in between and be overwritten (a "lost update"). Only
     * the switches and the settings are saved: device names and online state stay as they are
     * in the database. Returns (before, after). Used by scenes, which change everything at once.
     */
    suspend fun updateState(transform: (DeskState) -> DeskState): Pair<DeskState, DeskState>

    /**
     * End of the plug's auto-off timer: switches the plug off and clears the timer, but only if
     * [plannedEnd] is still the saved end time (the user may have re-set or cancelled it since).
     * Check and change happen in one SQL step. Returns true if the plug was on and went off.
     */
    suspend fun turnOffPlugIfPlanned(plannedEnd: Long): Boolean
}
