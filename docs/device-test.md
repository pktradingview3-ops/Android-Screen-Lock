# Device test: vivo Y31 (Phase 0)

Automated checks already passing in CI (Android 11 / API 30 emulator, 22 tests): all screens and buttons work, layouts draw correctly, and the static lock-screen image is set successfully. The checks below are what only a real phone can confirm.

Before testing: confirm the app version at the bottom of the first screen says **0.2.0 (build 4)**. If it says an older version, install the newer APK.

Goal: decide whether live time shows on the lock screen, and whether a static snapshot works.

## Setup
1. Install the debug APK from the GitHub Actions artifact (`timewall-debug-apk`). Allow install from unknown sources if asked.
2. Note the Android version and Funtouch OS version: Settings > About phone.
3. Open TimeWall, choose a layout (L1 is a good first test), and set 12-hour format.

## Test A: live wallpaper, "Home and lock" (expected to work)
- [ ] Tap "Set live wallpaper (recommended)". The system preview shows TimeWall Clock.
- [ ] Set wallpaper for Home and Lock screen. Lock the phone.
- [ ] Time on the lock screen is correct.
- [ ] Wait until the next minute (lock screen open). Time changes correctly.
- [ ] Change the name or layout in the app, then check the lock screen again. Update should appear.

## Test B: live wallpaper, "Lock screen only" (may not be available)
- [ ] If the option exists, set Lock screen only. Lock the phone. Does the TimeWall time show?
- Result: ____

## Test C: static snapshot (fallback)
- [ ] Tap "Set lock screen snapshot (static)". Status message appears.
- [ ] Lock the phone. The image shows with the time at the moment of tapping (it should NOT update).

## Test D: battery and stability
- [ ] Leave the phone locked for 60 minutes. Note battery %: ____
- [ ] After reboot, live wallpaper still works.
- [ ] Battery > App battery usage for TimeWall: if the time stops updating in background, set to Unrestricted and retest.

## Test E: app lock (v0.2.0)
Setup: install the new APK over the old one (version 0.2.0). Settings > App lock > Turn on app lock, PIN 4 to 6 digits.
- [ ] Open TimeWall: PIN screen appears before any layout.
- [ ] Wrong PIN shows the attempts-left message. Right PIN opens the app.
- [ ] Five wrong PINs show the countdown (30 s). The correct PIN is refused during the countdown.
- [ ] Open the app, press Home, wait 10 s, return: no PIN asked.
- [ ] Press Home, wait more than 30 s, return: PIN asked.
- [ ] Recents screen: TimeWall preview is blank (FLAG_SECURE). Screenshot is blocked.
- [ ] Fingerprint: enroll a fingerprint in Settings. In TimeWall, turn on "Fingerprint too". Lock, return, and check the prompt appears and unlocks. Use PIN from the prompt and check that works too.
- [ ] Turn off app lock: wrong PIN is refused, right PIN turns it off.
- [ ] Kill TimeWall from recents (or reboot), reopen: PIN asked when lock is on.
- [ ] Live wallpaper still shows the time while app lock is on (app lock does not touch it).
- Result: ____

## Result summary
- Live on lock screen alone: yes / no
- Live on Home + Lock: yes / no
- Static snapshot works: yes / no
- App lock works on vivo Y31 (PIN / lockout / fingerprint / 30 s grace): yes / no
- Notes:
