package com.timewall.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.timewall.app.data.ConfigStore
import com.timewall.app.domain.WallpaperConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ConfigStore.prefs(application)
    private val _config = MutableStateFlow(ConfigStore.read(prefs))
    val config: StateFlow<WallpaperConfig> = _config.asStateFlow()

    /** Applies a change and saves it, so the live wallpaper picks it up immediately. */
    fun update(transform: (WallpaperConfig) -> WallpaperConfig) {
        val updated = transform(_config.value)
        _config.value = updated
        ConfigStore.write(prefs, updated)
    }
}
