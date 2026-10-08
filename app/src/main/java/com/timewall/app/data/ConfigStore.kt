package com.timewall.app.data

import android.content.Context
import android.content.SharedPreferences
import com.timewall.app.domain.LayoutId
import com.timewall.app.domain.WallpaperConfig

/**
 * Local-only settings storage. SharedPreferences is used (not DataStore) because the live
 * wallpaper engine must read the current config synchronously on the UI thread.
 * Nothing leaves the device.
 */
object ConfigStore {

    private const val PREFS_NAME = "timewall_config"
    private const val KEY_LAYOUT = "layout"
    private const val KEY_USE_24H = "use_24h"
    private const val KEY_SHOW_DATE = "show_date"
    private const val KEY_NAME = "name"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(prefs: SharedPreferences): WallpaperConfig = WallpaperConfig(
        layout = LayoutId.fromName(prefs.getString(KEY_LAYOUT, null)),
        use24h = prefs.getBoolean(KEY_USE_24H, false),
        showDate = prefs.getBoolean(KEY_SHOW_DATE, true),
        name = prefs.getString(KEY_NAME, "") ?: "",
    )

    fun write(prefs: SharedPreferences, config: WallpaperConfig) {
        prefs.edit()
            .putString(KEY_LAYOUT, config.layout.name)
            .putBoolean(KEY_USE_24H, config.use24h)
            .putBoolean(KEY_SHOW_DATE, config.showDate)
            .putString(KEY_NAME, config.displayName)
            .apply()
    }
}
