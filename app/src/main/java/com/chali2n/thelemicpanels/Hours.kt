package com.chali2n.thelemicpanels

import java.time.LocalDate

/** Planetary Hours, traditional Chaldean system, sunrise to sunrise. */
object Hours {
    const val EMPTY = "No planetary hours held right now."

    data class Hour(
        val startMs: Long,
        val glyph: String,
        val planet: String,
        val label: String,
        val previousGlyph: String,
        val previousPlanet: String,
    )

    /** Chaldean order, the order hours step through. */
    private val CHALDEAN = listOf(
        "♄" to "Saturn",
        "♃" to "Jupiter",
        "♂" to "Mars",
        "☉" to "Sun",
        "♀" to "Venus",
        "☿" to "Mercury",
        "☽" to "Moon",
    )

    /** java.time DayOfWeek (Mon = 1 … Sun = 7) to that day's ruler in [CHALDEAN]. */
    private val DAY_RULER = mapOf(1 to 6, 2 to 2, 3 to 5, 4 to 1, 5 to 4, 6 to 0, 7 to 3)

    /** All twenty-four hours of the planetary day that begins at [date]'s sunrise. */
    fun hoursForDay(date: LocalDate): List<Hour>? {
        val rise = Astro.sunrise(date) ?: return null
        val set = Astro.sunset(date) ?: return null
        val nextRise = Astro.sunrise(date.plusDays(1)) ?: return null

        val dayLength = (set - rise) / 12.0
        val nightLength = (nextRise - set) / 12.0
        val startIndex = DAY_RULER.getValue(date.dayOfWeek.value)

        return (0 until 24).map { i ->
            val start = if (i < 12) {
                rise + (dayLength * i).toLong()
            } else {
                set + (nightLength * (i - 12)).toLong()
            }
            val planet = CHALDEAN[(startIndex + i) % 7]
            val previous = CHALDEAN[(startIndex + i + 6) % 7]
            val label = if (i < 12) "Day hour ${i + 1}" else "Night hour ${i - 11}"
            Hour(
                startMs = start,
                glyph = planet.first,
                planet = planet.second,
                label = label,
                previousGlyph = previous.first,
                previousPlanet = previous.second,
            )
        }
    }

    /**
     * Hours still to come, from the planetary day that contains now through the
     * day after it, so the list holds roughly thirty to forty-eight items.
     */
    fun schedule(nowMs: Long): List<Hour> {
        val today = TimeFmt.date(nowMs)
        val sunriseToday = Astro.sunrise(today)
        val first = if (sunriseToday != null && nowMs >= sunriseToday) today else today.minusDays(1)

        val chain = hoursForDay(first).orEmpty() + hoursForDay(first.plusDays(1)).orEmpty()
        return chain.filter { it.startMs > nowMs }
    }

    fun panel(nowMs: Long): Panel {
        val upcoming = schedule(nowMs)
        if (upcoming.isEmpty()) {
            return Panel(
                kind = PanelKind.HOURS,
                summary = listOf(Seg.muted(EMPTY)),
                emptyText = EMPTY,
                updatedAtMs = nowMs,
            )
        }

        val tiles = upcoming.take(3).map { hour ->
            Tile(
                top = TimeFmt.clock(hour.startMs),
                mid = "${hour.glyph} ${hour.planet}",
                bottom = "${hour.label} • follows ${hour.previousGlyph} ${hour.previousPlanet}",
            )
        }

        return Panel(
            kind = PanelKind.HOURS,
            summary = summaryFor(nowMs, upcoming),
            tiles = tiles,
            updatedAtMs = nowMs,
        )
    }

    private fun summaryFor(nowMs: Long, upcoming: List<Hour>): List<Seg> {
        val out = mutableListOf<Seg>()
        val next = upcoming[0]
        out += Seg.strong("${next.glyph} ${next.planet}")
        out += Seg.muted(" ${TimeFmt.relative(nowMs, next.startMs)}")

        if (upcoming.size > 1) {
            val second = upcoming[1]
            out += Seg.muted(" – then ")
            out += Seg.strong("${second.glyph} ${second.planet}")
            out += Seg.muted(" at ${TimeFmt.clock(second.startMs)}")
        }
        if (upcoming.size > 2) {
            val third = upcoming[2]
            out += Seg.muted(" and ")
            out += Seg.strong("${third.glyph} ${third.planet}")
            out += Seg.muted(" at ${TimeFmt.clock(third.startMs)}")
        }
        out += Seg.muted(".")
        return out
    }
}
