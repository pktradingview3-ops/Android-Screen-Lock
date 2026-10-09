# TimeWall — Vivo Clock Wallpaper & Live Wallpaper

**Built for Vivo phones.** A black wallpaper with a large, bold clock for Android.
**Five clock layouts** (vertical and horizontal), 12/24-hour format, an optional
date line, and optional custom name text. Built with Kotlin and Jetpack Compose as
a proper `WallpaperService` live wallpaper, with a static lock-screen snapshot as a
fallback. Optional **app lock** with PIN and fingerprint.

It is a clock wallpaper and a live wallpaper — nothing more. It uses only
Android's official wallpaper APIs. It does **not** replace or control the system
lock screen, and it needs no accessibility service, no overlay, no device-admin
permission and no root.

## Built and tested on Vivo

This app is developed against a real **Vivo Y31 5G (model V2521, Android 15,
Funtouch OS)** — not just an emulator. Every release is built, installed and
tested on that device, and the device-specific behaviour is documented rather
than guessed.

| Item | Value |
|---|---|
| Primary test device | **Vivo Y31 5G (V2521)**, Android 15, Funtouch OS |
| Also validated on | Android 11 / API 30 emulator (CI) |
| Install method | `adb install -r -t` — upgrades in place, no uninstall |
| minSdk | 26 (Android 8.0), so it runs on older Vivo models too |

### Vivo / Funtouch OS notes

- **Wallpaper picker.** The app opens the **system** live-wallpaper preview; on
  Funtouch OS the menu names differ between versions, so the app shows
  step-by-step help instead of trying to automate it.
- **Lock screen.** Funtouch does not run a live wallpaper on the lock screen by
  itself, which is exactly why the app also offers the static snapshot mode. See
  "What it can and cannot do" below.
- **Battery.** If Funtouch kills the wallpaper service in the background, set
  TimeWall's battery usage to **Unrestricted**. The PDR lists this as a known
  device risk.
- **Lock pattern.** The app-lock PIN is separate from the phone's own lock
  pattern and never touches it.

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
- **Lock other apps (v0.3.0)** — pick any installed app and it asks for your PIN
  when it opens. Same PIN, same lockout, same fingerprint as the app lock. An
  unlocked app stays open for 30 s after you unlock it, so switching away and back
  does not ask again.
- **No network** — no accounts, no analytics, no ads, no crash reporting. One
  permission: `SET_WALLPAPER`.
- **Live preview** in the editor uses the same renderer as the wallpaper, so what
  you see is what gets drawn.

### Locking other apps — what the permission is used for

Per-app locking needs Android's **accessibility service**, the same mechanism every
app locker uses. Android gives no other way for one app to notice that another app
was opened. The scope here is deliberately the smallest that still works:

| Setting | Value | Why |
|---|---|---|
| Event types | `typeWindowStateChanged` only | The single event that says which app came to the foreground |
| `canRetrieveWindowContent` | **false** | The service never reads what is on the screen — only the package name that rides along with the event |
| Input control | none | No key filtering, no gestures |
| Network | none | Nothing leaves the device. Still no `INTERNET` permission |

Turn it on at **Lock apps → Open accessibility settings → TimeWall app lock**. The
app shows a red warning card until it is enabled, because nothing locks without it.

**Some apps cannot be locked, on purpose.** The home screen, Settings, System UI,
the Play Store and the package installers are refused (greyed out in the list, with
the reason shown). Locking the home screen would make every press of Home demand a
PIN, and locking Settings would remove the only place this service can be turned off
again — either one locks you out of your own phone. The accessibility service
ignores these packages too, so a package protected by an older build is not locked
either.

If you prefer not to grant it, simply leave it off — the wallpaper and the app's
own lock work exactly as before.

> **Scope of the app lock:** the PIN-protected lock guards **TimeWall's own
> screens**. Per-app locking (v0.3.0) extends the same PIN to whichever other apps
> you pick, via the accessibility service described above. Neither one touches the
> system lock screen.

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

### Why the debug keystore is committed

`debug.keystore` (standard alias `androiddebugkey`, password `android`) is checked
in on purpose, and `app/build.gradle.kts` points the debug signing config at it.

Without a shared debug key, **every machine and every CI run generates its own**,
so an APK built on one machine cannot update an app installed from another — the
install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, and the only way forward
is an uninstall, which wipes the user's settings.

With it committed, any build from any machine signs with the same key and
`adb install -r` upgrades in place.

This is a **debug** key. It is not a secret, and it must never sign a release
build.

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
