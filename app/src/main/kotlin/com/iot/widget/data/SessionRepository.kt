package com.iot.widget.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iot.widget.domain.users.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Data layer · who is signed in. Survives app restarts; no passwords (demo accounts). */
interface SessionRepository {
    /** The signed-in user, or null when signed out (also when that user was disabled or deleted). */
    val currentUser: Flow<User?>

    /** Returns the signed-in user, or null for unknown or inactive users. */
    suspend fun signIn(userId: String): User?
    suspend fun signOut()
}

/**
 * DataStore = a small key-value file written asynchronously and safely (the modern replacement
 * for SharedPreferences). Only the user *id* is stored; the user itself always comes live from
 * the users table, so a role change shows up immediately.
 */
@Singleton
class DataStoreSessionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val users: UserRepository,
    private val clock: Clock,
) : SessionRepository {

    override val currentUser: Flow<User?> =
        combine(dataStore.data.map { it[KEY_USER_ID] }, users.users) { id, all ->
            id to all.firstOrNull { it.id == id && it.active }
        }.onEach { (id, user) ->
            // the stored account was disabled or deleted: forget it, so a restart shows Login
            if (id != null && user == null) signOut()
        }.map { it.second }

    override suspend fun signIn(userId: String): User? {
        val user = users.byId(userId)
        if (user == null || !user.active) return null
        dataStore.edit { it[KEY_USER_ID] = userId }
        users.markLoggedIn(userId, clock.millis())
        return user
    }

    override suspend fun signOut() {
        dataStore.edit { it.remove(KEY_USER_ID) }
    }

    /** The raw stored id (tests check that a disabled account is really forgotten). */
    suspend fun storedUserId(): String? = dataStore.data.first()[KEY_USER_ID]

    private companion object {
        val KEY_USER_ID = stringPreferencesKey("current_user_id")
    }
}
