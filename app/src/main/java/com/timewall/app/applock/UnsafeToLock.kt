package com.timewall.app.applock

/**
 * Apps that must never be protected, on any device.
 *
 * Locking the **home screen** means every press of Home demands a PIN, which makes the
 * phone unusable. Locking **Settings** removes the only place the accessibility service
 * can be turned back off. Both are one tap away in the picker and neither can be undone
 * from inside the app, so they are refused in two places: the picker disables the toggle,
 * and the accessibility service ignores these packages even if an older build stored them.
 */
internal val UNSAFE_TO_LOCK = setOf(
    // Settings: the only way to disable this service again.
    "com.android.settings",
    // System UI: dialogs, the status bar, the power menu.
    "com.android.systemui",
    // Home screens. The live launcher is also detected by CATEGORY_HOME at runtime,
    // so an OEM rename is still caught; these are the known names.
    "com.android.launcher",
    "com.android.launcher2",
    "com.android.launcher3",
    "com.google.android.apps.nexuslauncher",
    "com.vivo.simplelauncher",
    "com.bbk.launcher2",
    "com.oppo.launcher",
    // Installers / store: needed to repair or remove a bad install.
    "com.android.vending",
    "com.google.android.packageinstaller",
    "com.android.packageinstaller",
)
