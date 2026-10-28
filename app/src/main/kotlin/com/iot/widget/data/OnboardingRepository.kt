package com.iot.widget.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Data layer · has the welcome tour been shown on this device? (spec §5: once per device,
 * not per user). Stored next to the session in the same DataStore file.
 */
interface OnboardingRepository {
    val seen: Flow<Boolean>
    suspend fun markSeen()

    /** "Show welcome tour" in the User Guide: the tour appears again. */
    suspend fun reset()
}

class DataStoreOnboardingRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : OnboardingRepository {

    override val seen: Flow<Boolean> = dataStore.data.map { it[KEY_SEEN] == true }

    override suspend fun markSeen() {
        dataStore.edit { it[KEY_SEEN] = true }
    }

    override suspend fun reset() {
        dataStore.edit { it.remove(KEY_SEEN) }
    }

    private companion object {
        val KEY_SEEN = booleanPreferencesKey("onboarding_seen")
    }
}
