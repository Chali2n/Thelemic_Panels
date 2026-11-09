# Thelemic_Panels

Four Android home-screen widgets for Augusta, Maine (44.3106, -69.7795, `America/New_York`).

1. **Thelemic Date**: today's date in Thelemic style, with the Sun and Moon by sign and degree, the Latin weekday, and the year of the New Aeon. Updates every 2 hours.
2. **Lunar Calendar**: the Moon's phase, how much of it is lit, and its sign, plus the next New and Full Moons. Updates every 6 hours.
3. **Liber Resh**: today's four adoration times for Augusta: sunrise, noon, sunset and midnight.
4. **Planetary Hours**: the planet ruling the current hour, how long until the next begins, then the hours after it, using the traditional Chaldean order.

## Install

A debug build is committed here as
[thelemic-panels-1.4-dark-debug.apk](thelemic-panels-1.4-dark-debug.apk), so it can be sideloaded
without a toolchain: download it on the phone, tap it, and allow "install unknown apps" for
whichever app opens it. With a cable: `adb install -r thelemic-panels-1.4-dark-debug.apk`.
Android 8.0 (API 26) or newer. It installs over an earlier build as an update, since every build is
signed with the same debug key. The app screen shows the installed version at the bottom.

Then add the panels from the home-screen widget picker, or open the app and use the four
"Add …" buttons where the launcher supports pinning.

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
- Rows show the wall-clock time a station or hour begins, not a running countdown: the headline
  line above already says how long until the next one, and a static label would drift out of
  date between the 15-minute refreshes. Nothing in a panel ticks, so nothing is drawn while the
  screen is off.
- Planetary days run sunrise to sunrise. Each day and night is split into twelve unequal
  hours; hour 1 of the day belongs to the weekday's planet and the rest follow the Chaldean
  order: ♄ ♃ ♂ ☉ ♀ ☿ ☽.

### Dark glass, always

Every panel is a dark card: 26dp corners over a translucent near-black fill with near-white ink,
low chroma so the wallpaper's colour still reads through. There is no light variant to choose
between, and the panels do not follow the system theme — a dark panel stays dark while the phone
is in light mode. The widget picker lists exactly four entries under Thelemic Panels, and the app
screen is dark for the same reason.

The panels kept their original provider class names when the light ones were dropped, so widgets
already pinned to a home screen keep their ids and simply repaint dark instead of being removed by
the launcher.

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
| Liber Resh | 4×4 | how long until the next adoration, then the coming ones |
| Planetary Hours | 4×3 | how long until the next hour, then the two after it |

## Testing

`./gradlew :app:testDebugUnitTest` runs 17 JVM tests: 14 astronomy checks in
[AstronomyTest.kt](app/src/test/java/com/chali2n/thelemicpanels/AstronomyTest.kt) and 3 widget
checks in `WidgetInflateTest.kt`. The astronomy tests pin one past reading for Augusta, a Sunday
at 16:19, so they never drift with the calendar: sunrise 6:27 AM, sunset 4:19 PM, solar noon and solar midnight, the Moon at 11° Scorpii and under 1%
lit, the next New Moon, the named Full Moon, the four Resh stations, the whole 24-hour Chaldean
chain across a day change, the daylight-saving transition, sign ingresses, and the text each
panel renders. The widgets always read the device clock; nothing in them uses that fixture.

The tests caught one real bug: phases were banded by angle, so a 0.5% crescent the day before
New Moon was labelled "New Moon". The principal names now apply only on the day of the event,
and the rest of the month reads Waxing/Waning Crescent, Quarter or Gibbous.

[WidgetInflateTest.kt](app/src/test/java/com/chali2n/thelemicpanels/WidgetInflateTest.kt) runs
each panel's `RemoteViews`, both as a placeholder and with live data, through `apply()` under
Robolectric, which is what a launcher does; it also checks that every panel paints the dark ink,
so a missing colour in a layout fails the build, and that no panel ships a `Chronometer`.
That test found the cause of the first build's **"Couldn't add widget"**: each header row ended
in a bare `<View>` spacer, and `RemoteViews` only inflates classes annotated `@RemoteView`
(`View` and `Space` are not), so the whole layout threw `InflateException` in the launcher's
process. The spacer is now a `FrameLayout`.

## Limitations

- Third-party widgets cannot blur the wallpaper, so the cards are translucent rather than
  frosted like the reference screenshots. Over a bright photo the fill thins out and the ink
  reads less well.
- The committed APK is debug-signed. Installing a later build signed with a different key means
  uninstalling this one first.
- Liber Resh and Planetary Hours refresh on a 15-minute floor, the smallest interval WorkManager
  allows, so a station can linger for up to that long after its time passes.

## Layout

`app/src/main/java/com/chali2n/thelemicpanels/`

- `Astro.kt`: Astronomy Engine wrapper, phase naming, sign and degree helpers
- `Lunar.kt`, `Resh.kt`, `Hours.kt`, `Thelemic.kt`: one panel builder each
- `WidgetRender.kt`: computes, caches and paints the `RemoteViews`
- `Widgets.kt`: the four providers; `WidgetScheduler.kt` and `RefreshWorker.kt`: refresh timing
- `Cache.kt`: last good state per widget, so a failed refresh never blanks a panel
