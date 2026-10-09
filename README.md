# TimeWall — Android Clock Wallpaper & Live Wallpaper

A black wallpaper with a large, bold clock for Android. **Five clock layouts**
(vertical and horizontal), 12/24-hour format, an optional date line, and optional
custom name text. Built with Kotlin and Jetpack Compose as a proper
`WallpaperService` live wallpaper, with a static lock-screen snapshot as a
fallback. Optional **app lock** with PIN and fingerprint.

It is a clock wallpaper and a live wallpaper — nothing more. It uses only
Android's official wallpaper APIs. It does **not** replace or control the system
lock screen, and it needs no accessibility service, no overlay, no device-admin
permission and no root.

## Features

- **Five layouts** — `L1` Vertical stack left, `L2` Vertical stack center,
  `L3` Vertical condensed right, `L4` Horizontal single line,
  `L5` Horizontal wide dot. Each is a data-driven template, so a new layout is a
  new template rather than new screen code.
- **Real live time** — a `WallpaperService` redraws on the minute boundary
  (scheduled to the next minute + 50 ms, so it does not drift).
- **12-hour / 24-hour** format, midnight and noon handled as `12`.
- **Optional date line** — `FRI, 09 OCT 2026`, upper-cased in `Locale.ENGLISH`.
- **Optional name or text**, trimmed and capped at 20 characters.
- **App lock (v0.2.0)** — optional 4–6 digit PIN, optional fingerprint, auto-lock
  after 30 s in the background. PIN is stored only as a PBKDF2-HMAC-SHA256 hash
  (120,000 iterations, 16-byte random salt). Wrong PINs trigger a lockout that
  starts at 30 s and doubles, capped at 15 minutes. `FLAG_SECURE` blocks
  screenshots and Recents previews while the lock is on.
- **No network** — no accounts, no analytics, no ads, no crash reporting. One
  permission: `SET_WALLPAPER`.
- **Live preview** in the editor uses the same renderer as the wallpaper, so what
  you see is what gets drawn.

## What it can and cannot do

A wallpaper bitmap is a picture; once set, its time is frozen. So there are two
honest options, and the app offers both:

| Mode | Live clock? | Where |
|---|---|---|
| **Home and lock screen (live time)** | ✅ updates every minute | home + lock (system preview) |
| **Lock screen only (still image)** | ❌ frozen at the moment you tap | lock screen only |

Android cannot run a live wallpaper on the lock screen by itself on most OEM
builds, which is why the second option exists. The app says so in the UI rather
than pretending otherwise.

## Build

Requirements: **JDK 17**, **Android SDK 35**, Gradle 8.9 (wrapper included).

```bash
# unit tests + debug APK
./gradlew :app:testDebugUnitTest :app:assembleDebug

# APK lands at
app/build/outputs/apk/debug/app-debug.apk
```

Set `sdk.dir` in `local.properties`, or export `ANDROID_HOME`.

### Build from CI

Every push to an `arena/**` branch (and to `main`) runs GitHub Actions:
unit tests + debug APK in one job, and the instrumented UI/rendering tests on an
**Android 11 (API 30)** emulator in another — the same API level as the target
device. Download `timewall-debug-apk` from the workflow run.

## Install

```bash
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
```

The APK is debug-signed, so `-t` is required.

## Testing

- **31 unit tests (JVM)** — time formatting, config rules, PIN policy, PIN
  hashing, and the 30-second lock/grace logic.
- **Instrumented UI tests (emulator)** — every screen, button, chip, switch and
  text field; the app-lock flows (lock at launch, wrong PIN, lockout countdown,
  lock now); and a rendering test that each layout draws a black background with
  white text.
- A CI run that executes **zero** instrumented tests is treated as a failure.

Device checklist for the target phone: [`docs/device-test.md`](docs/device-test.md).

## Project layout

```
app/src/main/java/com/timewall/app/
├── ui/          Compose screens (layout picker, editor, app-lock screens, help)
├── domain/      layouts, time formatter, config models (pure Kotlin, unit-tested)
├── render/      Canvas renderer — one renderer used by preview, live wallpaper
│                and static export, so all three match
├── security/    app lock: PIN policy, PBKDF2 hashing, session/grace, biometrics
├── apply/       static export (render to Bitmap → WallpaperManager FLAG_LOCK)
├── data/        SharedPreferences persistence (no network, no database)
└── wallpaper/   WallpaperService + minute-aligned tick scheduler
```

Design decisions and the full product spec: [`docs/PDR.md`](docs/PDR.md).
Layout mockups: [`design/wallpaper-mockups/`](design/wallpaper-mockups/).

## Privacy

No accounts, no analytics, no crash reporting, no network calls. The only data
kept is local: the layout choice, time format, date toggle, name text, and — if
app lock is on — a salted PIN hash, the lockout counter and the fingerprint
toggle. There is no in-app PIN reset, because that would be a bypass; a forgotten
PIN is cleared with Android Settings → Apps → TimeWall → Clear data.

## License

MIT — see [LICENSE](LICENSE).
