# Device test: vivo Y31 (Phase 0)

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

## Result summary
- Live on lock screen alone: yes / no
- Live on Home + Lock: yes / no
- Static snapshot works: yes / no
- Notes:
