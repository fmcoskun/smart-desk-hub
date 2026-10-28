package com.iot.widget.automation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.PlugTimer
import com.iot.widget.domain.isOn
import com.iot.widget.testutil.FakeDeskRepository
import com.iot.widget.testutil.FakeExactAlarms
import com.iot.widget.testutil.FakeNotifier
import com.iot.widget.testutil.RecordingPublisher
import com.iot.widget.testutil.defaultDeskState
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZonedDateTime

/** With the "Alarms & reminders" permission, automations use exact alarms instead of WorkManager (spec §9c). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExactAlarmSchedulingTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = fixedClock()
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
        workManager = WorkManager.getInstance(context)
    }

    private fun pendingWork(name: String) =
        workManager.getWorkInfosForUniqueWork(name).get().count { !it.state.isFinished }

    @Test
    fun plugTimerUsesExactAlarmWhenAllowed() {
        val alarms = FakeExactAlarms(allowed = true)
        val scheduler = WorkManagerAutomationScheduler({ workManager }, { alarms }, clock)

        val endsAt = scheduler.startPlugTimer(PlugTimer.M30, clock.instant())

        assertEquals(endsAt to endsAt, alarms.alarms[WorkManagerAutomationScheduler.PLUG_TIMER])
        assertEquals(0, pendingWork(WorkManagerAutomationScheduler.PLUG_TIMER))

        scheduler.cancelPlugTimer()
        assertTrue(alarms.alarms.isEmpty())
    }

    @Test
    fun fallsBackToWorkManagerWithoutPermission() {
        val alarms = FakeExactAlarms(allowed = false).apply { alarms["light-on"] = 1L to null }
        val scheduler = WorkManagerAutomationScheduler({ workManager }, { alarms }, clock)

        scheduler.scheduleLight(DeskSettings())

        assertTrue(alarms.alarms.isEmpty()) // a stale alarm from when the permission was granted is removed
        assertEquals(1, pendingWork(WorkManagerAutomationScheduler.LIGHT_ON))
        assertEquals(1, pendingWork(WorkManagerAutomationScheduler.LIGHT_OFF))
    }

    @Test
    fun lightScheduleAlarmsAtNextOccurrence() {
        val alarms = FakeExactAlarms(allowed = true)
        WorkManagerAutomationScheduler({ workManager }, { alarms }, clock).scheduleLight(DeskSettings())

        // clock = Tue 2026-10-27 17:21 Istanbul → next "on" Wed 08:30, next "off" today 19:00
        val zone = clock.zone
        assertEquals(ZonedDateTime.of(2026, 10, 28, 8, 30, 0, 0, zone).toInstant().toEpochMilli(), alarms.alarms.getValue("light-on").first)
        assertEquals(ZonedDateTime.of(2026, 10, 27, 19, 0, 0, 0, zone).toInstant().toEpochMilli(), alarms.alarms.getValue("light-off").first)
        assertEquals(0, pendingWork(WorkManagerAutomationScheduler.LIGHT_ON))
    }

    @Test
    fun alarmDispatchRunsTheSameActionAsTheWorker() = runTest {
        val repo = FakeDeskRepository(defaultDeskState(DeskSettings(plugTimer = PlugTimer.M30, plugTimerEndsAtMillis = 42L)))
        val alarms = FakeExactAlarms(allowed = true)
        val actions = AutomationActions(context, repo, RecordingPublisher(), FakeNotifier(),
            WorkManagerAutomationScheduler({ workManager }, { alarms }, clock), clock)

        actions.onAlarm(WorkManagerAutomationScheduler.PLUG_TIMER, endsAt = 42L)
        assertFalse(repo.flow.value.isOn(DeviceId.SmartPlug))

        actions.onAlarm(WorkManagerAutomationScheduler.LIGHT_OFF, endsAt = null)
        assertFalse(repo.flow.value.isOn(DeviceId.DeskLight))
        // the light action planned its own next occurrence as a new alarm
        assertTrue(alarms.alarms.containsKey(WorkManagerAutomationScheduler.LIGHT_OFF))
    }
}
