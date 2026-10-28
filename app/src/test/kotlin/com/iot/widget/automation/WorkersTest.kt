package com.iot.widget.automation

import com.iot.widget.domain.withDevice
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.PlugTimer
import com.iot.widget.domain.Topics
import com.iot.widget.domain.isOn
import com.iot.widget.testutil.FakeDeskRepository
import com.iot.widget.testutil.FakeExactAlarms
import com.iot.widget.testutil.FakeNotifier
import com.iot.widget.testutil.FakeScheduler
import com.iot.widget.testutil.RecordingPublisher
import com.iot.widget.testutil.defaultDeskState
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkersTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = fixedClock()
    private val publisher = RecordingPublisher()
    private val notifier = FakeNotifier()
    private val scheduler = FakeScheduler()

    /** Builds workers with our fakes instead of Hilt (what HiltWorkerFactory does in the app). */
    private fun factory(repo: FakeDeskRepository) = object : WorkerFactory() {
        override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
            when (workerClassName) {
                PlugTimerWorker::class.java.name -> PlugTimerWorker(appContext, workerParameters, AutomationActions(appContext, repo, publisher, notifier, scheduler, clock))
                LightScheduleWorker::class.java.name -> LightScheduleWorker(appContext, workerParameters, AutomationActions(appContext, repo, publisher, notifier, scheduler, clock))
                SunsetDimWorker::class.java.name -> SunsetDimWorker(appContext, workerParameters, AutomationActions(appContext, repo, publisher, notifier, scheduler, clock))
                else -> error(workerClassName)
            }
    }

    private suspend inline fun <reified W : ListenableWorker> run(repo: FakeDeskRepository, vararg input: Pair<String, Any?>) =
        TestListenableWorkerBuilder<W>(context)
            .setWorkerFactory(factory(repo))
            .setInputData(workDataOf(*input))
            .build()
            .startWork()
            .get()

    @Test
    fun plugTimerTurnsPlugOff() = runTest {
        val repo = FakeDeskRepository(defaultDeskState(DeskSettings(plugTimer = PlugTimer.M30, plugTimerEndsAtMillis = 1_000L)))
        val result = run<PlugTimerWorker>(repo, PlugTimerWorker.KEY_ENDS_AT to 1_000L)

        assertEquals(ListenableWorker.Result.success(), result)
        assertFalse(repo.flow.value.isOn(DeviceId.SmartPlug))
        assertEquals(PlugTimer.Off, repo.flow.value.settings.plugTimer)
        assertEquals(listOf(Topics.PLUG, Topics.PLUG_TIMER), publisher.topics())
        assertEquals("""{"value":"off"}""", publisher.messages.last().json)
        assertEquals(listOf("Smart Plug turned off by timer"), notifier.texts)
    }

    @Test
    fun staleTimerDoesNothing() = runTest {
        // the user re-set the timer to 1h: the old 30m job carries a different end time
        val repo = FakeDeskRepository(defaultDeskState(DeskSettings(plugTimer = PlugTimer.H1, plugTimerEndsAtMillis = 9_999L)))
        run<PlugTimerWorker>(repo, PlugTimerWorker.KEY_ENDS_AT to 1_000L)

        assertTrue(repo.flow.value.isOn(DeviceId.SmartPlug))
        assertTrue(publisher.messages.isEmpty())
        assertTrue(notifier.texts.isEmpty())
    }

    @Test
    fun lightScheduleTurnsOnAndReschedules() = runTest {
        val repo = FakeDeskRepository(defaultDeskState().let { it.copy(devices = it.devices + (DeviceId.DeskLight to it.devices.getValue(DeviceId.DeskLight).copy(isOn = false))) })
        run<LightScheduleWorker>(repo, LightScheduleWorker.KEY_ACTION to LightScheduleWorker.ACTION_ON)

        assertTrue(repo.flow.value.isOn(DeviceId.DeskLight))
        assertEquals(listOf(Topics.LIGHTS), publisher.topics())
        assertEquals(listOf("Desk Halo LED turned on by schedule"), notifier.texts)
        assertEquals(listOf("lightAction:on:${SchedulePolicy.AfterCurrent}"), scheduler.calls)
    }

    @Test
    fun lightScheduleRespectsDisabled() = runTest {
        val repo = FakeDeskRepository(defaultDeskState(DeskSettings(scheduleEnabled = false)))
        run<LightScheduleWorker>(repo, LightScheduleWorker.KEY_ACTION to LightScheduleWorker.ACTION_OFF)

        assertTrue(repo.flow.value.isOn(DeviceId.DeskLight))
        assertTrue(publisher.messages.isEmpty())
    }

    @Test
    fun sunsetDimsOnlyWhenLightOnAndAutoDim() = runTest {
        val on = FakeDeskRepository(defaultDeskState(DeskSettings(autoDim = true, brightness = 80)))
        run<SunsetDimWorker>(on)
        assertEquals(30, on.flow.value.settings.brightness)
        assertEquals("""{"brightness":30}""", publisher.messages.single().json)
        assertEquals(listOf("Desk Halo LED dimmed at sunset"), notifier.texts)

        publisher.messages.clear()
        val off = FakeDeskRepository(defaultDeskState(DeskSettings(autoDim = false, brightness = 80)))
        run<SunsetDimWorker>(off)
        assertEquals(80, off.flow.value.settings.brightness)
        assertTrue(publisher.messages.isEmpty())
    }

    @Test
    fun lightOnRunDoesNotChainTheOffJob() = runTest {
        // Real WorkManager: the "on" job re-plans only itself; the pending "off" job stays single
        val config = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        val workManager = WorkManager.getInstance(context)
        val real = WorkManagerAutomationScheduler({ workManager }, { FakeExactAlarms(allowed = false) }, clock)
        val repo = FakeDeskRepository()
        real.scheduleLight(repo.flow.value.settings)

        val worker = TestListenableWorkerBuilder<LightScheduleWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    LightScheduleWorker(appContext, workerParameters, AutomationActions(appContext, repo, publisher, notifier, real, clock))
            })
            .setInputData(workDataOf(LightScheduleWorker.KEY_ACTION to LightScheduleWorker.ACTION_ON))
            .build()
        worker.startWork().get()

        val pendingOff = workManager.getWorkInfosForUniqueWork(WorkManagerAutomationScheduler.LIGHT_OFF).get()
            .filter { !it.state.isFinished }
        assertEquals(1, pendingOff.size)
    }

    @Test
    fun startPlugTimerEnqueuesUniqueDelayedWork() {
        val config = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        val workManager = WorkManager.getInstance(context)
        val real = WorkManagerAutomationScheduler({ workManager }, { FakeExactAlarms(allowed = false) }, clock)

        val endsAt = real.startPlugTimer(PlugTimer.M30, clock.instant())

        assertEquals(clock.millis() + 30 * 60_000L, endsAt)
        val infos = workManager.getWorkInfosForUniqueWork(WorkManagerAutomationScheduler.PLUG_TIMER).get()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
        assertEquals(30 * 60_000L, infos.single().initialDelayMillis)

        real.cancelPlugTimer()
        assertEquals(
            WorkInfo.State.CANCELLED,
            workManager.getWorkInfosForUniqueWork(WorkManagerAutomationScheduler.PLUG_TIMER).get().single().state,
        )
    }


    private fun offline(state: com.iot.widget.domain.DeskState, id: DeviceId) =
        state.copy(devices = state.devices + (id to state.devices.getValue(id).copy(online = false)))

    @Test
    fun scheduleSkipsOfflineLight() = runTest {
        val start = defaultDeskState(DeskSettings(scheduleEnabled = true))
        val repo = FakeDeskRepository(offline(start.withDevice(DeviceId.DeskLight, false), DeviceId.DeskLight))
        run<LightScheduleWorker>(repo, LightScheduleWorker.KEY_ACTION to LightScheduleWorker.ACTION_ON)
        assertFalse(repo.flow.value.isOn(DeviceId.DeskLight))
        assertTrue(publisher.topics().isEmpty())
        assertTrue(notifier.texts.isEmpty())
    }

    @Test
    fun sunsetDimSkipsOfflineLight() = runTest {
        val repo = FakeDeskRepository(offline(defaultDeskState(DeskSettings(autoDim = true, brightness = 85)), DeviceId.DeskLight))
        run<SunsetDimWorker>(repo)
        assertEquals(85, repo.flow.value.settings.brightness)
        assertTrue(publisher.topics().isEmpty())
    }
}
