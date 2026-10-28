package com.iot.widget.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.iot.widget.data.db.AppDatabase
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Session = "who is signed in", stored with DataStore in a file that survives app restarts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val users = RoomUserRepository(db.userDao())
    private val clock = fixedClock()
    private val scopes = mutableListOf<CoroutineScope>()

    // DataStore allows one active instance per file; each "app start" gets its own scope
    private fun session(file: File): DataStoreSessionRepository {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        return DataStoreSessionRepository(store, users, clock)
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        db.close()
    }

    private fun file() = File(tmp.root, "session.preferences_pb")

    @Test
    fun signInActiveUserEmitsUserAndStampsLogin() = runBlocking {
        val s = session(file())
        assertTrue(s.signIn("ayse") != null)
        assertEquals("ayse", s.currentUser.first { it != null }!!.id)
        assertEquals(clock.millis(), users.byId("ayse")!!.lastLoginAtMillis)
    }

    @Test
    fun signInInactiveUserRefused() = runBlocking {
        val s = session(file())
        assertTrue(s.signIn("zeynep") == null)
        assertNull(s.currentUser.first())
    }

    @Test
    fun signOutEmitsNull() = runBlocking {
        val s = session(file())
        s.signIn("mehmet")
        s.signOut()
        assertNull(s.currentUser.first())
    }

    @Test
    fun sessionSurvivesRestart() = runBlocking {
        session(file()).signIn("mehmet")
        scopes.forEach { it.cancel() } // "process ends"
        scopes.clear()
        assertEquals("mehmet", session(file()).currentUser.first { it != null }!!.id)
    }

    @Test
    fun inactiveOrMissingUserSignsOut() = runBlocking {
        val s = session(file())
        s.signIn("mehmet")
        s.currentUser.test {
            assertEquals("mehmet", awaitItem()?.id)
            users.save(users.byId("mehmet")!!.copy(active = false))
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        // the stored id was cleared too: a restart does not bring Mehmet back
        assertNull(s.storedUserId())
    }


    @Test
    fun signInReturnsUser() = runBlocking {
        val s = session(file())
        assertEquals("Ayşe Yılmaz", s.signIn("ayse")?.name)
        assertNull(s.signIn("zeynep")) // disabled
        assertNull(s.signIn("nobody"))
    }
}
