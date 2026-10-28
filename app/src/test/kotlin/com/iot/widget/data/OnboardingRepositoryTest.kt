package com.iot.widget.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** "Onboarding seen" is a device-wide flag that survives app restarts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnboardingRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    private fun repo(file: File): DataStoreOnboardingRepository {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }
        return DataStoreOnboardingRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
    }

    @After
    fun tearDown() = scopes.forEach { it.cancel() }

    @Test
    fun repositoryPersistsAndResets() = runBlocking {
        val file = File(tmp.root, "session.preferences_pb")
        val first = repo(file)
        assertFalse(first.seen.first())
        first.markSeen()
        assertTrue(first.seen.first())
        scopes.forEach { it.cancel() } // "app restart"
        val second = repo(file)
        assertTrue(second.seen.first())
        second.reset()
        assertFalse(second.seen.first())
    }
}
