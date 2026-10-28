package com.iot.widget.automation

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Automation layer · "Auto-dim at sunset" when WorkManager is used; logic in [AutomationActions]. */
@HiltWorker
class SunsetDimWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val actions: AutomationActions,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        actions.sunsetFired()
        return Result.success()
    }
}
