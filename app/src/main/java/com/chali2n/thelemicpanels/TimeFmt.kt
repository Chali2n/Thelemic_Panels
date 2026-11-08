package com.chali2n.thelemicpanels

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** All user-facing date and time formatting for the panels. */
object TimeFmt {
    val ZONE: ZoneId = ZoneId.of("America/New_York")

    private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val MONTH_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    fun local(ms: Long): ZonedDateTime = Instant.ofEpochMilli(ms).atZone(ZONE)

    fun date(ms: Long): LocalDate = local(ms).toLocalDate()

    /** "4:19 PM" */
    fun clock(ms: Long): String = CLOCK.format(local(ms))

    /** "in 4 min", "in 2 h 5 min", "now" */
    fun relative(nowMs: Long, targetMs: Long): String {
        val minutes = (targetMs - nowMs) / 60_000L
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "in $minutes min"
            else -> {
                val hours = minutes / 60
                val rest = minutes % 60
                if (rest == 0L) "in $hours h" else "in $hours h $rest min"
            }
        }
    }

    /** "Today 3:12 AM", "Tomorrow 2:02 AM", "Nov 24 9:53 AM" */
    fun dayAndClock(nowMs: Long, targetMs: Long): String {
        val today = date(nowMs)
        val day = date(targetMs)
        val label = when (day) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> MONTH_DAY.format(day)
        }
        return "$label ${clock(targetMs)}"
    }
}
