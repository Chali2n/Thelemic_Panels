package com.chali2n.thelemicpanels

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.moonPhase
import io.github.cosinekitty.astronomy.searchHourAngle
import io.github.cosinekitty.astronomy.searchMoonPhase
import io.github.cosinekitty.astronomy.searchRiseSet
import io.github.cosinekitty.astronomy.sunPosition
import java.time.LocalDate

/**
 * On-device astronomy for Augusta, Maine.
 *
 * Everything here is pure computation on [LocalDate]/epoch millis, so it runs
 * identically on the JVM in unit tests and on the device.
 */
object Astro {
    const val LATITUDE = 44.3106
    const val LONGITUDE = -69.7795

    /** Era Legis wants the rounded location. */
    const val ERA_LEGIS_LOCATION = "44.31:-69.78"

    private val observer = Observer(LATITUDE, LONGITUDE, 0.0)

    val SIGNS = listOf(
        "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
        "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces",
    )

    private fun time(ms: Long): Time = Time.fromMillisecondsSince1970(ms)

    private fun millis(time: Time?): Long? = time?.toMillisecondsSince1970()

    /** Epoch millis of local midnight starting [date]. */
    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(TimeFmt.ZONE).toInstant().toEpochMilli()

    fun sunrise(date: LocalDate): Long? =
        millis(searchRiseSet(Body.Sun, observer, Direction.Rise, time(startOfDay(date)), 2.0))

    fun sunset(date: LocalDate): Long? =
        millis(searchRiseSet(Body.Sun, observer, Direction.Set, time(startOfDay(date)), 2.0))

    /** The Sun's meridian transit for [date]. */
    fun solarNoon(date: LocalDate): Long? {
        val info = searchHourAngle(Body.Sun, observer, 0.0, time(startOfDay(date)))
        return millis(info.time)
    }

    /** Apparent geocentric ecliptic longitude of the Moon, degrees [0, 360). */
    fun moonLongitude(ms: Long): Double = eclipticGeoMoon(time(ms)).lon

    /** Ecliptic longitude of the Sun, degrees [0, 360). */
    fun sunLongitude(ms: Long): Double = sunPosition(time(ms)).elon

    /** Moon phase angle: 0 = new, 90 = first quarter, 180 = full, 270 = last quarter. */
    fun phaseAngle(ms: Long): Double = moonPhase(time(ms))

    /** Illuminated fraction of the Moon's disc, 0.0 to 1.0. */
    fun illuminatedFraction(ms: Long): Double =
        illumination(Body.Moon, time(ms)).phaseFraction

    /** Next time the Moon reaches [targetLon], searching forward from [fromMs]. */
    fun nextMoonPhase(targetLon: Double, fromMs: Long, limitDays: Double = 45.0): Long? =
        millis(searchMoonPhase(targetLon, time(fromMs), limitDays))

    fun nextNewMoon(fromMs: Long): Long? = nextMoonPhase(0.0, fromMs)

    fun nextFullMoon(fromMs: Long): Long? = nextMoonPhase(180.0, fromMs)

    fun normalize(degrees: Double): Double {
        val wrapped = degrees % 360.0
        return if (wrapped < 0) wrapped + 360.0 else wrapped
    }

    /** Zodiac index 0..11 for an ecliptic longitude. */
    fun signIndex(longitude: Double): Int = (normalize(longitude) / 30.0).toInt().coerceIn(0, 11)

    fun signName(longitude: Double): String = SIGNS[signIndex(longitude)]

    /** Whole degrees into the sign: 16 for 16.x Scorpii. */
    fun degreeInSign(longitude: Double): Int =
        (normalize(longitude) % 30.0).toInt().coerceIn(0, 29)

    private val PRINCIPAL_PHASES = listOf(
        0.0 to "New Moon",
        90.0 to "First Quarter",
        180.0 to "Full Moon",
        270.0 to "Last Quarter",
    )

    /**
     * The phase name as the reference source gives it: the four principal names
     * apply on the day of the event itself, otherwise the Moon is called by how
     * much and which way it is lit. That keeps a 0.5% Moon the day before New
     * Moon a Waning Crescent rather than a New Moon.
     */
    fun phaseLabel(ms: Long): String {
        val day = TimeFmt.date(ms)
        val dayStart = startOfDay(day)
        val dayEnd = startOfDay(day.plusDays(1))

        PRINCIPAL_PHASES.forEach { (angle, label) ->
            val moment = nextMoonPhase(angle, dayStart)
            if (moment != null && moment < dayEnd) return label
        }

        val waxing = phaseAngle(ms) < 180.0
        val lit = illuminatedFraction(ms)
        return when {
            waxing -> if (lit < 0.5) "Waxing Crescent" else "Waxing Gibbous"
            else -> if (lit < 0.5) "Waning Crescent" else "Waning Gibbous"
        }
    }

    /** Traditional name of the Full Moon in [month] (1-12). */
    fun fullMoonName(month: Int): String = when (month) {
        1 -> "Wolf"
        2 -> "Snow"
        3 -> "Worm"
        4 -> "Pink"
        5 -> "Flower"
        6 -> "Strawberry"
        7 -> "Buck"
        8 -> "Sturgeon"
        9 -> "Harvest"
        10 -> "Hunter's"
        11 -> "Beaver"
        else -> "Cold"
    }

    /**
     * The first sign change between [fromMs] and [toMs], as the new sign index and
     * the moment it happened. Sampled every ten minutes, then bisected to a second.
     */
    fun signChangeBetween(fromMs: Long, toMs: Long): Pair<Int, Long>? {
        val step = 10 * 60_000L
        var current = fromMs
        var currentSign = signIndex(moonLongitude(fromMs))
        while (current < toMs) {
            val next = minOf(current + step, toMs)
            val nextSign = signIndex(moonLongitude(next))
            if (nextSign != currentSign) {
                var low = current
                var high = next
                while (high - low > 1_000L) {
                    val mid = (low + high) / 2
                    if (signIndex(moonLongitude(mid)) == currentSign) low = mid else high = mid
                }
                return signIndex(moonLongitude(high)) to high
            }
            current = next
            currentSign = nextSign
        }
        return null
    }
}
