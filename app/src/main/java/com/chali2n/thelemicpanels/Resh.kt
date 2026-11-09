package com.chali2n.thelemicpanels

/** Liber Resh: the four solar adorations for Augusta. */
object Resh {
    const val EMPTY = "No adorations listed right now."

    data class Station(val name: String, val label: String, val atMs: Long)

    /**
     * Every station from now through the solar midnight that ends tomorrow,
     * so the list cannot empty out before the next refresh.
     */
    fun schedule(nowMs: Long): List<Station> {
        val today = TimeFmt.date(nowMs)
        val days = (0..2).map { today.plusDays(it.toLong()) }
        val noons = days.map { Astro.solarNoon(it) }
        val stations = mutableListOf<Station>()

        for (i in 0 until 2) {
            val day = days[i]
            val rise = Astro.sunrise(day) ?: continue
            val noon = noons[i] ?: continue
            val set = Astro.sunset(day) ?: continue
            val nextNoon = noons[i + 1] ?: continue

            stations += Station("Ra", "Sunrise", rise)
            stations += Station("Ahathoor", "Solar noon", noon)
            stations += Station("Tum", "Sunset", set)
            // Solar midnight is the midpoint of consecutive solar noons, taken in
            // absolute time so the DST switch cannot skew it.
            stations += Station("Khephra", "Solar midnight", noon + (nextNoon - noon) / 2)
        }

        return stations.sortedBy { it.atMs }.filter { it.atMs > nowMs }
    }

    fun panel(nowMs: Long): Panel {
        val upcoming = schedule(nowMs)
        if (upcoming.isEmpty()) {
            return Panel(
                kind = PanelKind.RESH,
                summary = listOf(Seg.muted(EMPTY)),
                emptyText = EMPTY,
                updatedAtMs = nowMs,
            )
        }

        val tiles = upcoming.take(4).map { station ->
            Tile(
                top = TimeFmt.clock(station.atMs),
                mid = station.name,
                bottom = station.label,
            )
        }

        return Panel(
            kind = PanelKind.RESH,
            summary = summaryFor(nowMs, upcoming),
            tiles = tiles,
            updatedAtMs = nowMs,
        )
    }

    private fun summaryFor(nowMs: Long, upcoming: List<Station>): List<Seg> {
        val out = mutableListOf<Seg>()
        val next = upcoming[0]
        out += Seg.strong(next.name)
        out += Seg.muted(" ${TimeFmt.relative(nowMs, next.atMs)}")

        if (upcoming.size > 1) {
            val second = upcoming[1]
            out += Seg.muted(" – then ")
            out += Seg.strong(second.name)
            out += Seg.muted(" at ${TimeFmt.clock(second.atMs)}")
        }
        if (upcoming.size > 2) {
            val third = upcoming[2]
            out += Seg.muted(" and ")
            out += Seg.strong(third.name)
            out += Seg.muted(" at ${TimeFmt.clock(third.atMs)}")
        }
        out += Seg.muted(".")
        return out
    }
}
