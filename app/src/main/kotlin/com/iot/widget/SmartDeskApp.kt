package com.iot.widget

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.iot.widget.automation.AutomationScheduler
import com.iot.widget.automation.ScreenLockHandler
import com.iot.widget.automation.ScreenLockReceiver
import com.iot.widget.data.DeskRepository
import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.LogRepository
import com.iot.widget.data.RecordingRepository
import com.iot.widget.data.SampleLogSeeder
import com.iot.widget.data.SessionRepository
import com.iot.widget.data.UserRepository
import com.iot.widget.di.ApplicationScope
import com.iot.widget.domain.cameras.Clips
import com.iot.widget.domain.cameras.SampleRecordings
import com.iot.widget.domain.logs.SampleLogs
import com.iot.widget.simulation.DeviceSimulator
import com.iot.widget.widget.SmartDeskWidget
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * The Application class: created once per process, before any screen (and also when
 * WorkManager wakes the app up to run a background job).
 *
 * @HiltAndroidApp makes Hilt generate the app-wide dependency container here; every
 * @Inject-ed class in the app is created from it.
 *
 * Configuration.Provider: WorkManager (background jobs) asks the app for its configuration.
 * We hand it Hilt's worker factory so background workers can receive injected
 * dependencies (repository, MQTT publisher...) just like screens do.
 */
@OptIn(FlowPreview::class) // debounce
@HiltAndroidApp
class SmartDeskApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var repository: DeskRepository
    @Inject lateinit var scheduler: AutomationScheduler
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @Inject lateinit var screenLockHandler: ScreenLockHandler
    @Inject lateinit var deviceSimulator: DeviceSimulator
    @Inject lateinit var logs: LogRepository
    @Inject lateinit var devices: DeviceRepository
    @Inject lateinit var users: UserRepository
    @Inject lateinit var clock: Clock
    @Inject lateinit var recordings: RecordingRepository
    @Inject lateinit var logSeeder: SampleLogSeeder
    @Inject lateinit var session: SessionRepository

    override fun onCreate() {
        super.onCreate()
        // Make sure every enabled automation has a planned job. Uses "keep" so a job that
        // WorkManager is about to run (the reason this process may have started) is left alone.
        appScope.launch { scheduler.syncAll(repository.current().settings) }

        // Log housekeeping: drop entries past the retention limits, then give a brand-new
        // install a week of sample history (spec §6.9)
        appScope.launch {
            val now = ZonedDateTime.now(clock)
            logs.prune(now.toInstant().toEpochMilli())
            val deviceNames = devices.devices.first().associate { it.id to it.name }
            val userNames = SampleLogs.publishers(users.all())
            logSeeder.seedOnce { SampleLogs.generate(now, deviceNames, userNames) }
            // and three days of sample camera recordings (spec §6.7), regenerated once they are
            // all older than yesterday, so Today and Yesterday are never empty in the demo
            val yesterday = now.toLocalDate().minusDays(1).atStartOfDay(now.zone).toInstant().toEpochMilli()
            recordings.seedIfEmptyOrStale(yesterday, Clips.RECORDINGS.keys) { SampleRecordings.generate(now) }
        }

        // Home-screen widgets follow every desk or sign-in change, from any source (app, widget,
        // automations). Debounced: a scene changes several rows at once. Only if one is placed.
        appScope.launch {
            // the user's role decides whether the widget may switch, so it is part of the key
            combine(repository.state, session.currentUser) { desk, user -> Triple(desk, user?.id, user?.role) }
                .distinctUntilChanged()
                .debounce(WIDGET_REFRESH_MS)
                .collect {
                    val manager = GlanceAppWidgetManager(this@SmartDeskApp)
                    if (manager.getGlanceIds(SmartDeskWidget::class.java).isNotEmpty()) SmartDeskWidget().updateAll(this@SmartDeskApp)
                }
        }

        // Simulated devices occasionally report events (spec §9a); real devices would do this over MQTT
        deviceSimulator.start()

        // "Enable when screen locks": screen-off can only be received by a receiver registered
        // in code; see ScreenLockReceiver for when Android actually delivers it
        ContextCompat.registerReceiver(
            this,
            ScreenLockReceiver { appScope.launch { screenLockHandler.onScreenOff() } },
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private companion object {
        const val WIDGET_REFRESH_MS = 300L
    }
}
