package com.chali2n.thelemicpanels

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/** One panel refresh. Runs off the main thread so the Era Legis fetch is safe. */
class RefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val kind = PanelKind.fromKey(inputData.getString(KEY_PANEL)) ?: return Result.failure()
        WidgetRender.updateAll(applicationContext, kind)
        return Result.success()
    }

    companion object {
        const val KEY_PANEL = "panel"
    }
}
