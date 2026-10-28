package com.iot.widget.automation

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.PlugTimer
import com.iot.widget.domain.automation.SunsetCalculator
import com.iot.widget.domain.automation.nextOccurrence
import dagger.Lazy
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * How a new job treats an already scheduled one with the same name:
 * - Replace: the user changed a setting → throw the old plan away.
 * - Keep: app start-up → leave whatever is already planned (it may be running right now).
 * - AfterCurrent: a worker schedules its own next run → queue it after itself, without
 *   cancelling the job that is still finishing.
 */
enum class SchedulePolicy { Replace, Keep, AfterCurrent }

/**
 * Automation layer · plans the background automations of spec §9b. An interface so screens and
 * workers can be tested with a fake; [WorkManagerAutomationScheduler] is the real one.
 */
interface AutomationScheduler {
    /** Starts the Smart Plug auto-off timer; returns the moment it will fire (epoch millis). */
    fun startPlugTimer(timer: PlugTimer, now: Instant): Long
    fun cancelPlugTimer()
    /** Plans both LED schedule jobs (turn on and turn off). */
    fun scheduleLight(settings: DeskSettings, policy: SchedulePolicy = SchedulePolicy.Replace)

    /** Plans only one of them; a running LED job uses this to plan its own next occurrence. */
    fun scheduleLightAction(turnOn: Boolean, settings: DeskSettings, policy: SchedulePolicy)
    fun scheduleSunset(settings: DeskSettings, policy: SchedulePolicy = SchedulePolicy.Replace)

    /** Called at app start: makes sure every enabled automation has a job (keeps existing ones). */
    fun syncAll(settings: DeskSettings)
}

/**
 * Plans each automation in one of two ways (spec §9c):
 *  - **Exact alarm** (user granted "Alarms & reminders"): AlarmManager wakes the phone at the
 *    exact moment, even in Doze; AutomationAlarmReceiver then runs the action directly.
 *  - **WorkManager** (no permission): a *unique* one-time job (by name), delayed until its
 *    moment. Survives app close and reboot, but Android may run it late while the phone is
 *    idle (Doze) or the app is rarely used — minutes, in deep idle even hours.
 * Whichever way is used, the other one is cleared, so an automation never fires twice.
 * After firing, each automation plans its own next occurrence.
 *
 * WorkManager and the alarms arrive as dagger.Lazy so they are looked up only when something
 * is planned. This keeps classes that merely *can* schedule (e.g. the dashboard's ViewModel)
 * constructible in tests that run without a WorkManager, such as Hilt's test application.
 */
class WorkManagerAutomationScheduler @Inject constructor(
    private val workManagerLazy: Lazy<WorkManager>,
    private val alarmsLazy: Lazy<ExactAlarms>,
    private val clock: Clock,
) : AutomationScheduler {

    private val workManager: WorkManager get() = workManagerLazy.get()
    private val alarms: ExactAlarms get() = alarmsLazy.get()

    override fun startPlugTimer(timer: PlugTimer, now: Instant): Long {
        val minutes = requireNotNull(timer.minutes) { "use cancelPlugTimer() for PlugTimer.Off" }
        val endsAt = now.toEpochMilli() + minutes * 60_000L
        plan<PlugTimerWorker>(PLUG_TIMER, endsAt - now.toEpochMilli(), workDataOf(PlugTimerWorker.KEY_ENDS_AT to endsAt), endsAt, SchedulePolicy.Replace)
        return endsAt
    }

    override fun cancelPlugTimer() = cancel(PLUG_TIMER)

    override fun scheduleLight(settings: DeskSettings, policy: SchedulePolicy) {
        if (!settings.scheduleEnabled) {
            cancel(LIGHT_ON)
            cancel(LIGHT_OFF)
            return
        }
        scheduleLightAction(turnOn = true, settings, policy)
        scheduleLightAction(turnOn = false, settings, policy)
    }

    override fun scheduleLightAction(turnOn: Boolean, settings: DeskSettings, policy: SchedulePolicy) {
        if (!settings.scheduleEnabled) return
        val now = ZonedDateTime.now(clock)
        val next = if (turnOn) {
            nextOccurrence(now, settings.scheduleOn, settings.scheduleOnDays)
        } else {
            nextOccurrence(now, settings.scheduleOff, settings.scheduleOffDays)
        }
        val action = if (turnOn) LightScheduleWorker.ACTION_ON else LightScheduleWorker.ACTION_OFF
        plan<LightScheduleWorker>(if (turnOn) LIGHT_ON else LIGHT_OFF, delayTo(next), workDataOf(LightScheduleWorker.KEY_ACTION to action), null, policy)
    }

    override fun scheduleSunset(settings: DeskSettings, policy: SchedulePolicy) {
        if (!settings.autoDim) {
            cancel(SUNSET_DIM)
            return
        }
        val now = ZonedDateTime.now(clock)
        val today = SunsetCalculator.sunset(now.toLocalDate(), now.zone)
        val next = if (today.isAfter(now)) today else SunsetCalculator.sunset(now.toLocalDate().plusDays(1), now.zone)
        plan<SunsetDimWorker>(SUNSET_DIM, delayTo(next), Data.EMPTY, null, policy)
    }

    override fun syncAll(settings: DeskSettings) {
        scheduleLight(settings, SchedulePolicy.Keep)
        scheduleSunset(settings, SchedulePolicy.Keep)
        val endsAt = settings.plugTimerEndsAtMillis
        if (settings.plugTimer != PlugTimer.Off && endsAt != null) {
            // a timer that expired while the phone was off fires right away (delay 0)
            val delay = (endsAt - clock.millis()).coerceAtLeast(0)
            plan<PlugTimerWorker>(PLUG_TIMER, delay, workDataOf(PlugTimerWorker.KEY_ENDS_AT to endsAt), endsAt, SchedulePolicy.Keep)
        }
    }

    private fun delayTo(moment: ZonedDateTime): Long = Duration.between(clock.instant(), moment.toInstant()).toMillis()

    private fun cancel(name: String) {
        workManager.cancelUniqueWork(name)
        alarms.cancel(name)
    }

    private inline fun <reified W : androidx.work.ListenableWorker> plan(
        name: String,
        delayMillis: Long,
        input: Data,
        endsAt: Long?,
        policy: SchedulePolicy,
    ) {
        if (alarms.canSchedule() && alarms.set(name, clock.millis() + delayMillis, endsAt)) {
            // Alarm path. Drop any WorkManager job of the same name, except when the caller IS
            // that job (AfterCurrent): cancelling it would cancel the code that is running.
            if (policy != SchedulePolicy.AfterCurrent) workManager.cancelUniqueWork(name)
            return
        }
        alarms.cancel(name)
        val request = OneTimeWorkRequestBuilder<W>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()
        val existing = when (policy) {
            SchedulePolicy.Replace -> ExistingWorkPolicy.REPLACE
            SchedulePolicy.Keep -> ExistingWorkPolicy.KEEP
            SchedulePolicy.AfterCurrent -> ExistingWorkPolicy.APPEND_OR_REPLACE
        }
        workManager.enqueueUniqueWork(name, existing, request)
    }

    companion object {
        const val PLUG_TIMER = "plug-timer"
        const val LIGHT_ON = "light-on"
        const val LIGHT_OFF = "light-off"
        const val SUNSET_DIM = "sunset-dim"
    }
}
