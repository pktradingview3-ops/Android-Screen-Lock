package com.timewall.app.security

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppSessionTest {

    private var now = 0L

    @Before
    fun setUp() {
        now = 1_000_000L
        AppSession.clock = { now }
        AppSession.onProcessStart(false)
    }

    @After
    fun tearDown() {
        AppSession.onProcessStart(false)
    }

    @Test
    fun processStart_locksOnlyWhenLockEnabled() {
        AppSession.onProcessStart(true)
        assertTrue(AppSession.locked.value)
        AppSession.onProcessStart(false)
        assertFalse(AppSession.locked.value)
    }

    @Test
    fun coldStart_withLockEnabled_isLocked() {
        AppSession.onProcessStart(true)
        AppSession.onAppStarted(lockEnabled = true)
        assertTrue(AppSession.locked.value)
    }

    @Test
    fun returnWithinGrace_staysUnlocked() {
        AppSession.onProcessStart(true)
        AppSession.onAppStarted(true)
        AppSession.unlock()

        AppSession.onAppStopped()
        now += AppSession.GRACE_MS // exactly at the limit is still inside grace
        AppSession.onAppStarted(true)
        assertFalse(AppSession.locked.value)
    }

    @Test
    fun returnAfterGrace_locks() {
        AppSession.onProcessStart(true)
        AppSession.onAppStarted(true)
        AppSession.unlock()

        AppSession.onAppStopped()
        now += AppSession.GRACE_MS + 1
        AppSession.onAppStarted(true)
        assertTrue(AppSession.locked.value)
    }

    @Test
    fun returnAfterGrace_withLockDisabled_staysUnlocked() {
        AppSession.onProcessStart(false)
        AppSession.onAppStopped()
        now += 10 * AppSession.GRACE_MS
        AppSession.onAppStarted(false)
        assertFalse(AppSession.locked.value)
    }

    @Test
    fun lockNow_locksOnlyWhenEnabled() {
        AppSession.lockNow(lockEnabled = false)
        assertFalse(AppSession.locked.value)
        AppSession.lockNow(lockEnabled = true)
        assertTrue(AppSession.locked.value)
    }

    @Test
    fun unlock_clearsLock() {
        AppSession.onProcessStart(true)
        AppSession.unlock()
        assertFalse(AppSession.locked.value)
    }
}
