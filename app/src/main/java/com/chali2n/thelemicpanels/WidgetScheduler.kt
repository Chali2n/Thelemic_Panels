package com.chali2n.thelemicpanels

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/** Drives refreshes: periodic per panel, plus an immediate one on tap or on add. */
object WidgetScheduler {
    const val ACTION_REFRESH = "com.chali2n.thelemicpanels.action.REFRESH"

    /**
     * The date panel changes every two hours, the lunar panel slowly, but Resh and
     * the planetary hours must drop a station or an hour as its time passes, so they
     * refresh every fifteen minutes (the shortest interval WorkManager allows).
     */
    fun onWidgetActive(context: Context) {
        val workManager = WorkManager.getInstance(context)
        schedule(workManager, PanelKind.DATE, 2, TimeUnit.HOURS)
        schedule(workManager, PanelKind.LUNAR, 6, TimeUnit.HOURS)
        schedule(workManager, PanelKind.RESH, 15, TimeUnit.MINUTES)
        schedule(workManager, PanelKind.HOURS, 15, TimeUnit.MINUTES)
    }

    private fun schedule(
        workManager: WorkManager,
        kind: PanelKind,
        interval: Long,
        unit: TimeUnit,
    ) {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(interval, unit)
            .setInputData(workDataOf(RefreshWorker.KEY_PANEL to kind.key))
            .build()
        workManager.enqueueUniquePeriodicWork(
            "panel-${kind.key}",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun refreshNow(context: Context, kind: PanelKind) {
        val request = OneTimeWorkRequestBuilder<RefreshWorker>()
            .setInputData(workDataOf(RefreshWorker.KEY_PANEL to kind.key))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
