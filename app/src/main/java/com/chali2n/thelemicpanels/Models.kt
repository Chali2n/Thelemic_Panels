package com.chali2n.thelemicpanels

/** How a run of text inside a panel summary should be emphasised. */
enum class Emphasis { NORMAL, MUTED, STRONG }

/** One run of summary text. Panels are built from a list of these. */
data class Seg(val text: String, val emphasis: Emphasis = Emphasis.NORMAL) {
    companion object {
        fun normal(text: String) = Seg(text)
        fun muted(text: String) = Seg(text, Emphasis.MUTED)
        fun strong(text: String) = Seg(text, Emphasis.STRONG)
    }
}

/**
 * One row or cell inside a panel.
 *
 * [top] is the small muted line (a clock time, a date, or a cell caption),
 * [mid] is the prominent line, [bottom] the small muted line beneath it.
 */
data class Tile(
    val top: String,
    val mid: String,
    val bottom: String? = null,
)

/**
 * Light and dark renderings of the same panel. A skin changes only layout and
 * palette, so both skins share one computation and one cache entry per kind.
 */
enum class PanelSkin { LIGHT, DARK }

enum class PanelKind(val key: String) {
    DATE("date"),
    LUNAR("lunar"),
    RESH("resh"),
    HOURS("hours");

    companion object {
        fun fromKey(key: String?): PanelKind? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Everything a widget needs to draw itself. Deliberately made of plain data so it
 * can be written to the cache and replayed when a refresh fails.
 */
data class Panel(
    val kind: PanelKind,
    val summary: List<Seg>,
    val tiles: List<Tile> = emptyList(),
    val emptyText: String? = null,
    val cta: String? = null,
    val ctaUrl: String? = null,
    val updatedAtMs: Long = 0L,
)
