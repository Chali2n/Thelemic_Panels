# Thelemic_Panels

Four Android home-screen widgets for Augusta, Maine (44.3106, -69.7795, `America/New_York`).

1. **Thelemic Date**: today's date in Thelemic style, with the Sun and Moon by sign and degree, the Latin weekday, and the year of the New Aeon. Updates every 2 hours.
2. **Lunar Calendar**: the Moon's phase, how much of it is lit, and its sign, plus the next New and Full Moons. Updates every 6 hours.
3. **Liber Resh**: today's four adoration times for Augusta: sunrise, noon, sunset and midnight.
4. **Planetary Hours**: the planet ruling the current hour, with a countdown to the next ones, using the traditional Chaldean order.

## Building

Needs JDK 17 and an Android SDK with `platforms;android-35` and `build-tools;35.0.0`.

```sh
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # astronomy checks against a reference reading
```

## How it works

- **Astronomy runs on the device** via the Kotlin port of Astronomy Engine
  (`io.github.cosinekitty:astronomy`): sunrise, sunset, solar noon, the Moon's ecliptic
  longitude, phase angle and illumination, the next New and Full Moon, and sign ingresses.
  Nothing is scraped, so all four panels work offline.
- **Thelemic Date** is the one networked panel. It reads `plain` from
  `https://date.eralegis.info/now?format=json&tz=America/New_York&location=44.31:-69.78`
  so the line stays exactly as Era Legis writes it, character for character.
- Each panel caches its last good render and repaints from that cache if a refresh fails, so
  a panel never blanks.
- Countdowns are `Chronometer` views set to count down, so they tick every second without the
  app being awake.
- Planetary days run sunrise to sunrise. Each day and night is split into twelve unequal
  hours; hour 1 of the day belongs to the weekday's planet and the rest follow the Chaldean
  order: ♄ ♃ ♂ ☉ ♀ ☿ ☽.

### Refresh cadence

Thelemic Date every 2 hours and Lunar Calendar every 6 hours, as specified. **Liber Resh and
Planetary Hours refresh every 15 minutes instead of daily/6-hourly**, because they are ordered
lists that must drop an adoration or an hour as soon as its time passes; a static render would
still be showing yesterday's sunrise the next morning. The work is local arithmetic with no
network involved. Tapping any panel refreshes it immediately, and resizing re-renders it with
more or fewer rows.

### Widget sizes

| Panel | Size | Shows |
|---|---|---|
| Thelemic Date | 4×2 | the four-part line and the Era Legis button |
| Lunar Calendar | 4×3 | headline, phase and sign cells, next New and Full Moons |
| Liber Resh | 4×4 | next adoration with a live countdown, then the following ones |
| Planetary Hours | 4×3 | next hour with a live countdown, then the two after it |
