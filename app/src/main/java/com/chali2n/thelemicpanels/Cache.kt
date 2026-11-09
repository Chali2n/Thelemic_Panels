package com.chali2n.thelemicpanels

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Last good panel per kind, so a failed refresh repaints the previous values
 * instead of blanking the widget.
 */
object Cache {
    private const val PREFS = "thelemic_panels"
    private const val KEY_PREFIX = "panel_"

    fun save(context: Context, panel: Panel) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + panel.kind.key, encode(panel))
            .apply()
    }

    fun load(context: Context, kind: PanelKind): Panel? {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PREFIX + kind.key, null) ?: return null
        return runCatching { decode(stored, kind) }.getOrNull()
    }

    private fun encode(panel: Panel): String {
        val summary = JSONArray()
        panel.summary.forEach { seg ->
            summary.put(JSONObject().put("t", seg.text).put("e", seg.emphasis.name))
        }
        val tiles = JSONArray()
        panel.tiles.forEach { tile ->
            tiles.put(
                JSONObject().apply {
                    put("top", tile.top)
                    put("mid", tile.mid)
                    tile.bottom?.let { put("bottom", it) }
                },
            )
        }
        return JSONObject().apply {
            put("summary", summary)
            put("tiles", tiles)
            panel.emptyText?.let { put("empty", it) }
            panel.cta?.let { put("cta", it) }
            panel.ctaUrl?.let { put("ctaUrl", it) }
            put("updatedAt", panel.updatedAtMs)
        }.toString()
    }

    private fun decode(raw: String, kind: PanelKind): Panel {
        val json = JSONObject(raw)
        val summaryJson = json.optJSONArray("summary") ?: JSONArray()
        val summary = (0 until summaryJson.length()).map { index ->
            val seg = summaryJson.getJSONObject(index)
            Seg(
                text = seg.getString("t"),
                emphasis = runCatching { Emphasis.valueOf(seg.getString("e")) }.getOrDefault(Emphasis.NORMAL),
            )
        }
        val tilesJson = json.optJSONArray("tiles") ?: JSONArray()
        val tiles = (0 until tilesJson.length()).map { index ->
            val tile = tilesJson.getJSONObject(index)
            Tile(
                top = tile.getString("top"),
                mid = tile.getString("mid"),
                bottom = if (tile.has("bottom")) tile.getString("bottom") else null,
            )
        }
        return Panel(
            kind = kind,
            summary = summary,
            tiles = tiles,
            emptyText = if (json.has("empty")) json.getString("empty") else null,
            cta = if (json.has("cta")) json.getString("cta") else null,
            ctaUrl = if (json.has("ctaUrl")) json.getString("ctaUrl") else null,
            updatedAtMs = json.optLong("updatedAt", 0L),
        )
    }
}
