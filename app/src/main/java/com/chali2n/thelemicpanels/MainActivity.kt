package com.chali2n.thelemicpanels

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

/** Host screen: pin the four panels to the home screen, or force a refresh. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WidgetScheduler.onWidgetActive(this)

        val manager = AppWidgetManager.getInstance(this)
        pin(R.id.pin_date, ThelemicDateWidget::class.java, manager)
        pin(R.id.pin_lunar, LunarWidget::class.java, manager)
        pin(R.id.pin_resh, ReshWidget::class.java, manager)
        pin(R.id.pin_hours, PlanetaryHoursWidget::class.java, manager)

        findViewById<Button>(R.id.refresh).setOnClickListener {
            PanelKind.entries.forEach { WidgetScheduler.refreshNow(this, it) }
            Toast.makeText(this, R.string.refreshing, Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.version).text = versionLine()
    }

    /** The installed build, so it's obvious which APK is on the phone. */
    private fun versionLine(): String {
        val version = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: return ""
        return getString(R.string.version_footer, version)
    }

    private fun pin(id: Int, provider: Class<*>, manager: AppWidgetManager) {
        val button = findViewById<Button>(id)
        if (!manager.isRequestPinAppWidgetSupported) {
            button.setOnClickListener {
                Toast.makeText(this, R.string.pin_unsupported, Toast.LENGTH_LONG).show()
            }
            return
        }
        button.setOnClickListener {
            manager.requestPinAppWidget(ComponentName(this, provider), null, null)
        }
    }
}
