package com.iot.widget.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.iot.widget.domain.logs.LogEntry
import com.iot.widget.domain.logs.LogLevel
import com.iot.widget.domain.logs.LogSource
import com.iot.widget.testutil.FakeLogRepository
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** The sample week of logs is added exactly once per install, whatever happens around it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SampleLogSeederTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clock = fixedClock()
    private val logs = FakeLogRepository(clock = clock)
    private val seeder by lazy {
        SampleLogSeeder(logs, PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "s.preferences_pb") }, clock)
    }

    @After
    fun tearDown() = scope.cancel()

    private fun entry(ago: Long, message: String) =
        LogEntry(0, clock.millis() - ago, LogSource.CLIENT, LogLevel.INFO, "System", null, null, message, null, null)

    private val sample = List(5) { entry(2 * 86_400_000L, "sample $it") }

    @Test
    fun logsSeededOnceEvenAfterPrune() = runBlocking {
        seeder.seedOnce { sample }
        assertEquals(5, logs.flow.value.size)
        logs.flow.value = emptyList() // 30 days later everything was pruned
        seeder.seedOnce { sample }
        assertTrue(logs.flow.value.isEmpty()) // no fake "last week" again
    }

    @Test
    fun logsSeededEvenIfEventCameFirst() = runBlocking {
        logs.add(entry(1_000, "screen locked")) // an event written at start, before the seeder ran
        seeder.seedOnce { sample }
        assertEquals(6, logs.flow.value.size)
    }

    @Test
    fun olderInstallWithHistoryIsNotSeededAgain() = runBlocking {
        logs.add(entry(3 * 86_400_000L, "old sample entry")) // installed before the flag existed
        seeder.seedOnce { sample }
        assertEquals(1, logs.flow.value.size)
    }
}
