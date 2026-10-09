package com.timewall.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.timewall.app.security.AppLockManager
import com.timewall.app.security.AppLockStore
import com.timewall.app.security.AppSession
import com.timewall.app.security.VerifyOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppLockViewModel(application: Application) : AndroidViewModel(application) {

    private val store = AppLockStore.from(application)
    private val manager = AppLockManager(store)

    private val _enabled = MutableStateFlow(store.isEnabled)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _biometric = MutableStateFlow(store.biometricEnabled)
    val biometric: StateFlow<Boolean> = _biometric.asStateFlow()

    fun lockoutRemainingMs(): Long = manager.lockoutRemainingMs()

    suspend fun verify(pin: String): VerifyOutcome = manager.verify(pin)

    suspend fun setPin(pin: String): Result<Unit> {
        val result = runCatching {
            val saved = withContext(Dispatchers.Default) { store.savePin(pin) }
            check(saved) { "PIN could not be saved to storage" }
        }
        if (result.isSuccess) {
            _enabled.value = true
        }
        return result
    }

    suspend fun disableLock() {
        withContext(Dispatchers.Default) { store.clearAll() }
        _enabled.value = false
        _biometric.value = false
        AppSession.unlock()
    }

    fun setBiometric(on: Boolean) {
        store.biometricEnabled = on
        _biometric.value = store.biometricEnabled
    }

    fun lockNow() {
        AppSession.lockNow(_enabled.value)
    }
}
