package com.timewall.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.timewall.app.security.AppLockStore
import com.timewall.app.security.AppSession

class TimeWallApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // A PIN-protected app always starts locked, including after the system kills the process.
        AppSession.onProcessStart(AppLockStore.from(this).isEnabled)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                AppSession.onAppStarted(AppLockStore.from(this@TimeWallApplication).isEnabled)
            }

            override fun onStop(owner: LifecycleOwner) {
                AppSession.onAppStopped()
            }
        })
    }
}
