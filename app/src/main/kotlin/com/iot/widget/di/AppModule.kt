package com.iot.widget.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.work.WorkManager
import com.iot.widget.automation.AndroidExactAlarms
import com.iot.widget.automation.AndroidNotifier
import com.iot.widget.automation.AutomationScheduler
import com.iot.widget.automation.ExactAlarms
import com.iot.widget.automation.Notifier
import com.iot.widget.automation.WorkManagerAutomationScheduler
import com.iot.widget.data.DataStoreOnboardingRepository
import com.iot.widget.data.DataStoreSessionRepository
import com.iot.widget.data.GuideRepository
import com.iot.widget.data.RawGuideRepository
import com.iot.widget.data.OnboardingRepository
import com.iot.widget.data.DeskRepository
import com.iot.widget.data.RoomDeskRepository
import com.iot.widget.data.RoomUserRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.data.UserRepository
import com.iot.widget.data.db.AppDatabase
import com.iot.widget.data.db.DeskDao
import com.iot.widget.data.db.UserDao
import com.iot.widget.data.db.DeviceDao
import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.RoomDeviceRepository
import com.iot.widget.data.log.EventLog
import com.iot.widget.data.log.RoomEventLog
import com.iot.widget.data.LogRepository
import com.iot.widget.data.RecordingRepository
import com.iot.widget.data.RoomRecordingRepository
import com.iot.widget.data.RoomLogRepository
import com.iot.widget.data.db.LogDao
import com.iot.widget.data.db.RecordingDao
import com.iot.widget.data.mqtt.LoggingMqttPublisher
import com.iot.widget.data.mqtt.MqttPublisher
import com.iot.widget.data.mqtt.RecordingMqttPublisher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Hilt module: tells Hilt how to build objects it cannot construct by itself
 * (interfaces, library classes). InstallIn(SingletonComponent) makes these bindings available
 * app-wide; only the ones also marked @Singleton are created once and shared. Unscoped ones
 * (Clock, Random, the DAO, WorkManager) are simply fetched again each time they are injected.
 *
 * Clock and Random are injected instead of calling `Clock.systemDefaultZone()` or
 * `Random.nextInt()` directly: tests replace them with fixed ones, so time- and
 * randomness-dependent code (timers, sensor simulation) becomes testable.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun random(): Random = Random.Default

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun deskDao(db: AppDatabase): DeskDao = db.deskDao()

    @Provides
    fun userDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun deviceDao(db: AppDatabase): DeviceDao = db.deviceDao()

    @Provides
    fun logDao(db: AppDatabase): LogDao = db.logDao()

    @Provides
    fun recordingDao(db: AppDatabase): RecordingDao = db.recordingDao()

    /** One DataStore file per app ("session"); creating two for the same file is an error, hence @Singleton. */
    @Provides
    @Singleton
    fun sessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("session") }

    /** Logcat publisher wrapped by the decorator that also writes every message to the event log. */
    @Provides
    @Singleton
    fun mqttPublisher(
        logs: LogRepository,
        session: SessionRepository,
        devices: DeviceRepository,
        clock: Clock,
        @ApplicationScope scope: CoroutineScope,
    ): MqttPublisher = RecordingMqttPublisher(LoggingMqttPublisher(), logs, session, devices, clock, scope)

    @Provides
    fun workManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
}

/** @Binds = "when someone asks for the interface, give them this implementation". */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    abstract fun deskRepository(impl: RoomDeskRepository): DeskRepository

    @Binds
    abstract fun userRepository(impl: RoomUserRepository): UserRepository

    @Binds
    abstract fun deviceRepository(impl: RoomDeviceRepository): DeviceRepository

    @Binds
    abstract fun sessionRepository(impl: DataStoreSessionRepository): SessionRepository

    @Binds
    abstract fun onboardingRepository(impl: DataStoreOnboardingRepository): OnboardingRepository

    @Binds
    abstract fun guideRepository(impl: RawGuideRepository): GuideRepository

    @Binds
    abstract fun eventLog(impl: RoomEventLog): EventLog

    @Binds
    abstract fun logRepository(impl: RoomLogRepository): LogRepository

    @Binds
    abstract fun recordingRepository(impl: RoomRecordingRepository): RecordingRepository

    @Binds
    abstract fun automationScheduler(impl: WorkManagerAutomationScheduler): AutomationScheduler

    @Binds
    abstract fun notifier(impl: AndroidNotifier): Notifier

    @Binds
    abstract fun exactAlarms(impl: AndroidExactAlarms): ExactAlarms
}

/** Marks the app-wide CoroutineScope (outlives every screen; used for start-up work). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
