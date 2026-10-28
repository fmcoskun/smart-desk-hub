package com.iot.widget.automation

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Automation layer · the Desk Halo LED schedule ("Turn on 08:30 weekdays", "Turn off 19:00")
 * when WorkManager is used. One job per action; the logic is in [AutomationActions].
 */
@HiltWorker
class LightScheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val actions: AutomationActions,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        actions.lightScheduleFired(turnOn = inputData.getString(KEY_ACTION) == ACTION_ON)
        return Result.success()
    }

    companion object {
        const val KEY_ACTION = "action"
        const val ACTION_ON = "on"
        const val ACTION_OFF = "off"
    }
}
