package com.chali2n.thelemicpanels

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.RemoteViews

/** Computes, caches and paints the four widgets. */
object WidgetRender {
    private const val FIXED_HEADER_DP = 100
    private const val FIXED_HEADER_WITH_STATS_DP = 150
    private const val ROW_DP = 60

    private class RowIds(
        val row: Int,
        val top: Int,
        val countdown: Int,
        val mid: Int,
        val detail: Int,
    )

    private class EventIds(val row: Int, val top: Int, val mid: Int)

    private val reshRows = listOf(
        RowIds(R.id.row1, R.id.row1_time, R.id.row1_cd, R.id.row1_name, R.id.row1_detail),
        RowIds(R.id.row2, R.id.row2_time, R.id.row2_cd, R.id.row2_name, R.id.row2_detail),
        RowIds(R.id.row3, R.id.row3_time, R.id.row3_cd, R.id.row3_name, R.id.row3_detail),
        RowIds(R.id.row4, R.id.row4_time, R.id.row4_cd, R.id.row4_name, R.id.row4_detail),
    )

    private val hoursRows = listOf(
        RowIds(R.id.row1, R.id.row1_time, R.id.row1_cd, R.id.row1_name, R.id.row1_detail),
        RowIds(R.id.row2, R.id.row2_time, R.id.row2_cd, R.id.row2_name, R.id.row2_detail),
        RowIds(R.id.row3, R.id.row3_time, R.id.row3_cd, R.id.row3_name, R.id.row3_detail),
    )

    private val lunarEvents = listOf(
        EventIds(R.id.event1, R.id.event1_top, R.id.event1_mid),
        EventIds(R.id.event2, R.id.event2_top, R.id.event2_mid),
    )

    fun providerFor(kind: PanelKind): Class<*> = when (kind) {
        PanelKind.DATE -> ThelemicDateWidget::class.java
        PanelKind.LUNAR -> LunarWidget::class.java
        PanelKind.RESH -> ReshWidget::class.java
        PanelKind.HOURS -> PlanetaryHoursWidget::class.java
    }

    private fun layoutFor(kind: PanelKind): Int = when (kind) {
        PanelKind.DATE -> R.layout.widget_date
        PanelKind.LUNAR -> R.layout.widget_lunar
        PanelKind.RESH -> R.layout.widget_resh
        PanelKind.HOURS -> R.layout.widget_hours
    }

    /** Recompute one panel and repaint every widget instance of it. */
    fun updateAll(context: Context, kind: PanelKind) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, providerFor(kind)))
        if (ids.isEmpty()) return

        val now = System.currentTimeMillis()
        val fresh = runCatching { compute(kind, now) }.getOrNull()
        if (fresh != null) Cache.save(context, fresh)
        val panel = fresh ?: Cache.load(context, kind) ?: placeholder(kind)

        ids.forEach { id -> manager.updateAppWidget(id, build(context, kind, panel, id)) }
    }

    private fun compute(kind: PanelKind, nowMs: Long): Panel = when (kind) {
        PanelKind.DATE -> Thelemic.panel(nowMs)
        PanelKind.LUNAR -> Lunar.panel(nowMs)
        PanelKind.RESH -> Resh.panel(nowMs)
        PanelKind.HOURS -> Hours.panel(nowMs)
    }

    internal fun placeholder(kind: PanelKind): Panel = Panel(
        kind = kind,
        summary = listOf(Seg.muted("—")),
        emptyText = if (kind == PanelKind.DATE) null else "Updating…",
        cta = if (kind == PanelKind.DATE) Thelemic.CTA_LABEL else null,
        ctaUrl = if (kind == PanelKind.DATE) Thelemic.CTA_URL else null,
    )

    internal fun build(
        context: Context,
        kind: PanelKind,
        panel: Panel,
        appWidgetId: Int,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, layoutFor(kind))
        bindSummary(context, views, panel)
        bindRefresh(context, views, kind)

        when (kind) {
            PanelKind.DATE -> bindDate(context, views, panel)
            PanelKind.LUNAR -> bindLunar(context, views, panel, appWidgetId)
            PanelKind.RESH -> bindRows(views, panel, capacity(context, appWidgetId, FIXED_HEADER_DP, 4))
            PanelKind.HOURS -> bindRows(views, panel, capacity(context, appWidgetId, FIXED_HEADER_DP, 3))
        }
        return views
    }

    private fun bindSummary(context: Context, views: RemoteViews, panel: Panel) {
        val builder = SpannableStringBuilder()
        val muted = context.getColor(R.color.ink_muted)
        panel.summary.forEach { seg ->
            val start = builder.length
            builder.append(seg.text)
            val end = builder.length
            if (end <= start) return@forEach
            when (seg.emphasis) {
                Emphasis.STRONG -> builder.setSpan(
                    StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )

                Emphasis.MUTED -> builder.setSpan(
                    ForegroundColorSpan(muted), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )

                Emphasis.NORMAL -> Unit
            }
        }
        views.setTextViewText(R.id.panel_summary, builder)
    }

    /** Tapping a panel refreshes it; the button on the date panel opens Era Legis. */
    private fun bindRefresh(context: Context, views: RemoteViews, kind: PanelKind) {
        val intent = Intent(context, providerFor(kind)).setAction(WidgetScheduler.ACTION_REFRESH)
        views.setOnClickPendingIntent(
            R.id.panel_root,
            PendingIntent.getBroadcast(
                context,
                100 + kind.ordinal,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    private fun bindDate(context: Context, views: RemoteViews, panel: Panel) {
        val label = panel.cta
        val url = panel.ctaUrl
        if (label == null || url == null) {
            views.setViewVisibility(R.id.panel_cta, View.GONE)
            return
        }
        views.setViewVisibility(R.id.panel_cta, View.VISIBLE)
        views.setTextViewText(R.id.panel_cta, label)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        views.setOnClickPendingIntent(
            R.id.panel_cta,
            PendingIntent.getActivity(
                context,
                200,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    private fun bindLunar(
        context: Context,
        views: RemoteViews,
        panel: Panel,
        appWidgetId: Int,
    ) {
        bindCell(views, R.id.stat1_top, R.id.stat1_mid, panel.tiles.getOrNull(0))
        bindCell(views, R.id.stat2_top, R.id.stat2_mid, panel.tiles.getOrNull(1))

        val events = panel.tiles.drop(2)
        val capacity = capacity(context, appWidgetId, FIXED_HEADER_WITH_STATS_DP, 2)
        lunarEvents.forEachIndexed { index, ids ->
            val tile = events.getOrNull(index)
            val visible = tile != null && index < capacity
            views.setViewVisibility(ids.row, if (visible) View.VISIBLE else View.GONE)
            if (tile == null || !visible) return@forEachIndexed
            views.setTextViewText(ids.top, tile.top)
            views.setTextViewText(ids.mid, tile.mid)
        }

        val empty = panel.emptyText
        views.setViewVisibility(R.id.panel_empty, if (empty != null) View.VISIBLE else View.GONE)
        if (empty != null) views.setTextViewText(R.id.panel_empty, empty)
    }

    private fun bindRows(views: RemoteViews, panel: Panel, capacity: Int) {
        val rows = if (panel.kind == PanelKind.RESH) reshRows else hoursRows
        val now = System.currentTimeMillis()

        rows.forEachIndexed { index, ids ->
            val tile = panel.tiles.getOrNull(index)
            val visible = tile != null && index < capacity
            views.setViewVisibility(ids.row, if (visible) View.VISIBLE else View.GONE)
            if (tile == null || !visible) return@forEachIndexed

            views.setTextViewText(ids.top, tile.top)
            views.setTextViewText(ids.mid, tile.mid)

            val detail = tile.bottom
            if (detail == null) {
                views.setViewVisibility(ids.detail, View.GONE)
            } else {
                views.setViewVisibility(ids.detail, View.VISIBLE)
                views.setTextViewText(ids.detail, detail)
            }

            val target = tile.liveCountdownTo
            if (target != null && target > now) {
                val base = SystemClock.elapsedRealtime() + (target - now)
                views.setChronometer(ids.countdown, base, " · in %s", true)
                views.setChronometerCountDown(ids.countdown, true)
                views.setViewVisibility(ids.countdown, View.VISIBLE)
            } else {
                views.setViewVisibility(ids.countdown, View.GONE)
            }
        }

        val empty = panel.emptyText
        views.setViewVisibility(R.id.panel_empty, if (empty != null) View.VISIBLE else View.GONE)
        if (empty != null) views.setTextViewText(R.id.panel_empty, empty)
    }

    private fun bindCell(views: RemoteViews, topId: Int, midId: Int, tile: Tile?) {
        if (tile == null) {
            views.setViewVisibility(topId, View.GONE)
            views.setViewVisibility(midId, View.GONE)
            return
        }
        views.setViewVisibility(topId, View.VISIBLE)
        views.setViewVisibility(midId, View.VISIBLE)
        views.setTextViewText(topId, tile.top)
        views.setTextViewText(midId, tile.mid)
    }

    /** How many [ROW_DP] rows fit in the height the launcher gave this instance. */
    private fun capacity(context: Context, appWidgetId: Int, fixedDp: Int, maxRows: Int): Int {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        if (heightDp <= 0) return minOf(2, maxRows)
        return ((heightDp - fixedDp) / ROW_DP).coerceIn(1, maxRows)
    }
}
