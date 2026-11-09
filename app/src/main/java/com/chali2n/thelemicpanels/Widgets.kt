package com.chali2n.thelemicpanels

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

/** Base behaviour shared by every panel: schedule, refresh on tap, refresh on resize. */
abstract class PanelWidget : AppWidgetProvider() {
    protected abstract val kind: PanelKind

    /** Dark providers override this; the layout and palette follow from it. */
    protected open val skin: PanelSkin get() = PanelSkin.LIGHT

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        WidgetScheduler.onWidgetActive(context)
        WidgetScheduler.refreshNow(context, kind)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        WidgetScheduler.refreshNow(context, kind)
    }

    override fun onEnabled(context: Context) {
        WidgetScheduler.onWidgetActive(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetScheduler.ACTION_REFRESH) {
            WidgetScheduler.refreshNow(context, kind)
        }
    }
}

class ThelemicDateWidget : PanelWidget() {
    override val kind: PanelKind = PanelKind.DATE
}

class LunarWidget : PanelWidget() {
    override val kind: PanelKind = PanelKind.LUNAR
}

class ReshWidget : PanelWidget() {
    override val kind: PanelKind = PanelKind.RESH
}

class PlanetaryHoursWidget : PanelWidget() {
    override val kind: PanelKind = PanelKind.HOURS
}

class ThelemicDateDarkWidget : PanelWidget() {
    override val kind: PanelKind = PanelKind.DATE
    override val skin: PanelSkin = PanelSkin.DARK
}

class LunarDarkWidget : PanelWidget() {
    override val kind: PanelKind = PanelKind.LUNAR
    override val skin: PanelSkin = PanelSkin.DARK
}

class ReshDarkWidget : PanelWidget() {
    override val kind: PanelKind = PanelKind.RESH
    override val skin: PanelSkin = PanelSkin.DARK
}

class PlanetaryHoursDarkWidget : PanelWidget() {
    override val kind: PanelKind = PanelKind.HOURS
    override val skin: PanelSkin = PanelSkin.DARK
}
