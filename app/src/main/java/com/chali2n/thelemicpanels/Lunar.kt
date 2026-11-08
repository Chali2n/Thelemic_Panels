package com.chali2n.thelemicpanels

import java.time.LocalDate
import java.util.Locale

/** Lunar Calendar panel: phase, illumination, Moon sign, next lunations. */
object Lunar {
    const val EMPTY = "No upcoming moon dates found."

    fun panel(nowMs: Long): Panel {
        val phase = Astro.phaseLabel(nowMs)
        val longitude = Astro.moonLongitude(nowMs)
        val sign = Astro.signName(longitude)
        val litText = String.format(Locale.US, "%.1f%% lit", Astro.illuminatedFraction(nowMs) * 100.0)

        val today = TimeFmt.date(nowMs)
        val nextNew = Astro.nextNewMoon(nowMs)
        val nextFull = Astro.nextFullMoon(nowMs)

        val lunation: Pair<String, Long>? = when {
            nextNew != null && TimeFmt.date(nextNew) == today -> "New Moon" to nextNew
            nextFull != null && TimeFmt.date(nextFull) == today -> "Full Moon" to nextFull
            else -> null
        }
        val ingress = ingressAfter(nowMs, today)

        val summary: List<Seg> = when {
            lunation != null -> listOf(
                Seg.strong(lunation.first),
                Seg.muted(" at "),
                Seg.strong(TimeFmt.clock(lunation.second)),
                Seg.muted(" today."),
            )

            ingress != null -> {
                val (signIndex, at) = ingress
                val whenText = if (TimeFmt.local(at).hour >= 18) "tonight." else "today."
                listOf(
                    Seg.muted("Moon enters "),
                    Seg.strong(Astro.SIGNS[signIndex]),
                    Seg.muted(" at "),
                    Seg.strong(TimeFmt.clock(at)),
                    Seg.muted(" $whenText"),
                )
            }

            else -> listOf(
                Seg.strong(phase),
                Seg.muted(" Moon in "),
                Seg.strong(sign),
                Seg.muted(" all day."),
            )
        }

        val events: List<Tile> = listOfNotNull(
            nextNew?.let { at ->
                at to Tile(top = TimeFmt.dayAndClock(nowMs, at), mid = "New Moon")
            },
            nextFull?.let { at ->
                val name = Astro.fullMoonName(TimeFmt.local(at).monthValue)
                at to Tile(top = TimeFmt.dayAndClock(nowMs, at), mid = "Full Moon • $name Moon")
            },
        ).sortedBy { it.first }.map { it.second }

        return Panel(
            kind = PanelKind.LUNAR,
            summary = summary,
            tiles = listOf(
                Tile(top = phase, mid = litText),
                Tile(top = "Moon Sign", mid = sign),
            ) + events,
            emptyText = if (events.isEmpty()) EMPTY else null,
            updatedAtMs = nowMs,
        )
    }

    /** A sign change still to come before midnight tonight, if any. */
    private fun ingressAfter(nowMs: Long, today: LocalDate): Pair<Int, Long>? {
        val end = Astro.startOfDay(today.plusDays(1))
        if (nowMs >= end) return null
        return Astro.signChangeBetween(nowMs, end)
    }
}
