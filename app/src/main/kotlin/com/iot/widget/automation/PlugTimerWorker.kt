package com.iot.widget.automation

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Automation layer · runs the Smart Plug auto-off when WorkManager is used (no exact-alarm
 * permission). The logic itself is in [AutomationActions.plugTimerFired].
 *
 * A CoroutineWorker's doWork() is a suspend function WorkManager calls on a background thread.
 * @HiltWorker + @AssistedInject: Hilt supplies our dependencies, WorkManager supplies the two
 * @Assisted ones (context and parameters).
 */
@HiltWorker
class PlugTimerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val actions: AutomationActions,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        actions.plugTimerFired(inputData.getLong(KEY_ENDS_AT, -1))
        return Result.success()
    }

    companion object {
        const val KEY_ENDS_AT = "ends_at"
    }
}
