package com.chali2n.thelemicpanels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Fixture: a past reading for Augusta (Sunday 8 November 2026), not today's sky.
 * Sunset 4:19 PM, solar midnight 11:22 PM, sunrise 6:27 AM, night hour 1 Jupiter
 * following Saturn, Moon 11 Scorpio waning crescent under 1% lit, New Moon Nov 9
 * 02:02, Full Moon Nov 24 09:53.
 *
 * Every assertion is pinned to that instant, so the suite never depends on the
 * date it is run. The panels compute from the device clock and ignore all of it.
 */
class AstronomyTest {
    private val ny: ZoneId = ZoneId.of("America/New_York")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(ny).toInstant().toEpochMilli()

    private val now: Long = at(2026, 11, 8, 16, 19)

    private fun assertNear(expected: LocalDateTime, actualMs: Long?, toleranceMinutes: Long = 3) {
        assertNotNull("expected a time, got none", actualMs)
        val expectedMs = expected.atZone(ny).toInstant().toEpochMilli()
        val deltaMinutes = Math.abs(actualMs!! - expectedMs) / 60_000
        assertTrue(
            "expected about $expected, got ${TimeFmt.local(actualMs)} (off by $deltaMinutes min)",
            deltaMinutes <= toleranceMinutes,
        )
    }

    @Test
    fun sunriseSunsetAndNoonMatchTheAlmanac() {
        val day = LocalDate.of(2026, 11, 8)
        assertNear(LocalDateTime.of(2026, 11, 8, 6, 27), Astro.sunrise(day))
        assertNear(LocalDateTime.of(2026, 11, 8, 16, 19), Astro.sunset(day))
        assertNear(LocalDateTime.of(2026, 11, 8, 11, 23), Astro.solarNoon(day))
    }

    @Test
    fun solarMidnightIsTheMidpointOfConsecutiveNoons() {
        val noonToday = Astro.solarNoon(LocalDate.of(2026, 11, 8))!!
        val noonTomorrow = Astro.solarNoon(LocalDate.of(2026, 11, 9))!!
        val midnight = noonToday + (noonTomorrow - noonToday) / 2
        assertNear(LocalDateTime.of(2026, 11, 8, 23, 23), midnight)
    }

    @Test
    fun moonSitsInScorpioAroundElevenDegrees() {
        val longitude = Astro.moonLongitude(now)
        assertEquals("Scorpio", Astro.signName(longitude))
        assertTrue("degree was ${Astro.degreeInSign(longitude)}", Astro.degreeInSign(longitude) in 10..12)
    }

    @Test
    fun theMoonIsABarelyLitWaningCrescent() {
        val lit = Astro.illuminatedFraction(now) * 100.0
        assertTrue("illumination was $lit%", lit > 0.0 && lit < 2.0)
        assertEquals("Waning Crescent", Astro.phaseLabel(now))
        // The day of the New Moon itself takes the principal name.
        assertEquals("New Moon", Astro.phaseLabel(at(2026, 11, 9, 12, 0)))
    }

    @Test
    fun nextLunationsMatchTheReference() {
        val newMoon = Astro.nextNewMoon(now)
        assertNotNull(newMoon)
        assertNear(LocalDateTime.of(2026, 11, 9, 2, 2), newMoon, toleranceMinutes = 6)

        val fullMoon = Astro.nextFullMoon(now)
        assertNotNull(fullMoon)
        assertNear(LocalDateTime.of(2026, 11, 24, 9, 53), fullMoon, toleranceMinutes = 6)
        assertEquals("2026-11-24", TimeFmt.date(fullMoon!!).toString())
        assertEquals("Beaver", Astro.fullMoonName(TimeFmt.local(fullMoon).monthValue))
    }

    @Test
    fun lunarPanelReadsLikeTheReferenceWidget() {
        val panel = Lunar.panel(now)
        assertEquals(
            "Waning Crescent Moon in Scorpio all day.",
            panel.summary.joinToString("") { it.text },
        )
        assertEquals(4, panel.tiles.size)
        assertEquals("Waning Crescent", panel.tiles[0].top)
        assertEquals("Moon Sign", panel.tiles[1].top)
        assertEquals("Scorpio", panel.tiles[1].mid)
        assertEquals("New Moon", panel.tiles[2].mid)
        assertEquals("Full Moon • Beaver Moon", panel.tiles[3].mid)
        assertEquals(TimeFmt.dayAndClock(now, Astro.nextNewMoon(now)!!), panel.tiles[2].top)
        assertEquals(TimeFmt.dayAndClock(now, Astro.nextFullMoon(now)!!), panel.tiles[3].top)
    }

    @Test
    fun reshStationsFollowSunset() {
        val upcoming = Resh.schedule(now)
        assertTrue("expected upcoming stations", upcoming.size >= 4)

        assertEquals("Tum", upcoming[0].name)
        assertEquals("Sunset", upcoming[0].label)
        assertNear(LocalDateTime.of(2026, 11, 8, 16, 19), upcoming[0].atMs)

        assertEquals("Khephra", upcoming[1].name)
        assertNear(LocalDateTime.of(2026, 11, 8, 23, 23), upcoming[1].atMs)

        assertEquals("Ra", upcoming[2].name)
        assertNear(LocalDateTime.of(2026, 11, 9, 6, 27), upcoming[2].atMs)
    }

    @Test
    fun planetaryHoursStartWithJupiterAfterSaturn() {
        val upcoming = Hours.schedule(now)
        assertTrue("expected upcoming hours", upcoming.size >= 24)

        val first = upcoming[0]
        assertEquals("Jupiter", first.planet)
        assertEquals("♃", first.glyph)
        assertEquals("Night hour 1", first.label)
        assertEquals("Saturn", first.previousPlanet)
        assertNear(LocalDateTime.of(2026, 11, 8, 16, 19), first.startMs)

        assertEquals("Mars", upcoming[1].planet)
        assertEquals("Night hour 2", upcoming[1].label)
        assertEquals("Jupiter", upcoming[1].previousPlanet)
    }

    @Test
    fun onePlanetaryDayHasTwentyFourHoursInChaldeanOrder() {
        // Sunday 8 November 2026: the day ruler is the Sun.
        val hours = Hours.hoursForDay(LocalDate.of(2026, 11, 8))!!
        assertEquals(24, hours.size)
        assertEquals("Sun", hours[0].planet)
        assertEquals("Day hour 1", hours[0].label)
        assertEquals("Saturn", hours[11].planet)
        assertEquals("Day hour 12", hours[11].label)
        assertEquals("Jupiter", hours[12].planet)
        assertEquals("Night hour 1", hours[12].label)

        val order = listOf("Saturn", "Jupiter", "Mars", "Sun", "Venus", "Mercury", "Moon")
        hours.forEachIndexed { index, hour ->
            assertEquals("hour ${index + 1}", order[(3 + index) % 7], hour.planet)
        }
    }

    @Test
    fun planetaryDaysChainWithoutAGap() {
        val order = listOf("Saturn", "Jupiter", "Mars", "Sun", "Venus", "Mercury", "Moon")
        var date = LocalDate.of(2026, 10, 28)
        repeat(6) {
            val today = Hours.hoursForDay(date)!!
            val tomorrow = Hours.hoursForDay(date.plusDays(1))!!
            today.zipWithNext { a, b ->
                assertTrue("hours of $date must advance", b.startMs > a.startMs)
            }
            assertTrue(
                "gap between $date and its successor",
                tomorrow[0].startMs > today[23].startMs,
            )
            val lastIndex = order.indexOf(today[23].planet)
            assertEquals(
                "hour 1 of ${date.plusDays(1)} must follow hour 24 of $date",
                order[(lastIndex + 1) % 7],
                tomorrow[0].planet,
            )
            date = date.plusDays(1)
        }
    }

    @Test
    fun theDaylightSavingDayIsTwentyFiveHoursLong() {
        // DST ended at 2 AM on 1 November 2026, so the planetary day that began at
        // sunrise on 31 October runs 25 hours before the next sunrise.
        val beforeShift = Astro.sunrise(LocalDate.of(2026, 10, 31))!!
        val afterShift = Astro.sunrise(LocalDate.of(2026, 11, 1))!!

        // The day is one solar day in absolute time, but only 23 hours by the clock.
        val spanHours = (afterShift - beforeShift) / 3_600_000.0
        assertTrue("planetary day was $spanHours h", spanHours in 23.9..24.1)
        val wallClockMinutes = ChronoUnit.MINUTES.between(
            TimeFmt.local(beforeShift).toLocalDateTime(),
            TimeFmt.local(afterShift).toLocalDateTime(),
        )
        assertTrue("wall clock span was $wallClockMinutes min", wallClockMinutes in 22 * 60 + 30..23 * 60 + 30)
        assertEquals(ZoneOffset.ofHours(-4), TimeFmt.local(beforeShift).offset)
        assertEquals(ZoneOffset.ofHours(-5), TimeFmt.local(afterShift).offset)

        val hours = Hours.hoursForDay(LocalDate.of(2026, 10, 31))!!
        assertEquals(24, hours.size)
        hours.zipWithNext { a, b -> assertTrue("hours must advance across DST", b.startMs > a.startMs) }
    }

    @Test
    fun signIngressIsFoundAndBracketed() {
        val change = Astro.signChangeBetween(at(2026, 11, 8, 0, 0), at(2026, 11, 12, 0, 0))
        assertNotNull("the Moon must change sign within four days", change)
        val (newSign, moment) = change!!
        assertEquals("Sagittarius", Astro.SIGNS[newSign])
        assertEquals(newSign, Astro.signIndex(Astro.moonLongitude(moment)))
        assertTrue(
            "the sign two minutes earlier must differ",
            Astro.signIndex(Astro.moonLongitude(moment - 120_000)) != newSign,
        )
    }

    @Test
    fun timeFormattingMatchesTheReferenceWidgets() {
        assertEquals("in 4 min", TimeFmt.relative(now, now + 4 * 60_000))
        assertEquals("in 2 h 5 min", TimeFmt.relative(now, now + 125 * 60_000))
        assertEquals("now", TimeFmt.relative(now, now))
        assertEquals("Tomorrow 2:02 AM", TimeFmt.dayAndClock(now, at(2026, 11, 9, 2, 2)))
        assertEquals("Nov 24 9:53 AM", TimeFmt.dayAndClock(now, at(2026, 11, 24, 9, 53)))
        assertEquals("Today 4:19 PM", TimeFmt.dayAndClock(now, at(2026, 11, 8, 16, 19)))
        assertEquals("0.5% lit", String.format(Locale.US, "%.1f%% lit", 0.5))
    }

    @Test
    fun panelsBuildWithoutTouchingTheNetwork() {
        assertTrue(Resh.panel(now).tiles.isNotEmpty())
        assertTrue(Hours.panel(now).tiles.isNotEmpty())
        assertEquals(PanelKind.LUNAR, Lunar.panel(now).kind)
    }
}
