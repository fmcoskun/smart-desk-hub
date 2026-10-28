package com.iot.widget.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.iot.widget.domain.logs.LogEntry
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

/**
 * Data layer · adds the demo's "last week" of logs exactly once per install.
 *
 * A flag in DataStore remembers that it happened. Checking "is the table empty?" instead
 * would go wrong twice: after 30 days of pruning the table is empty again (and a fake week
 * would reappear as if it were real), and an event written at start-up (a screen lock, a
 * restart) would make the table non-empty before the sample was ever added.
 */
class SampleLogSeeder @Inject constructor(
    private val logs: LogRepository,
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
) {
    suspend fun seedOnce(make: () -> List<LogEntry>) {
        if (dataStore.data.first()[KEY_SEEDED] == true) return
        // Installs from before this flag already have their sample week: entries older than a
        // few minutes mean the history is there. Only fresh start-up events → add the sample.
        if (!logs.hasEntriesBefore(clock.millis() - FRESH_MS)) logs.addAll(make())
        dataStore.edit { it[KEY_SEEDED] = true }
    }

    private companion object {
        val KEY_SEEDED = booleanPreferencesKey("logs_seeded")
        const val FRESH_MS = 10 * 60_000L
    }
}
