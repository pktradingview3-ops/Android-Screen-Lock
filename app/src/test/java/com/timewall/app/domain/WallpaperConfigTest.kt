package com.timewall.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperConfigTest {

    @Test
    fun displayName_trimsAndCapsLength() {
        val config = WallpaperConfig(name = "   ${"A".repeat(40)}   ")
        assertEquals(WallpaperConfig.MAX_NAME_LENGTH, config.displayName.length)
        assertFalse(config.displayName.startsWith(" "))
    }

    @Test
    fun displayName_emptyStaysEmpty() {
        assertEquals("", WallpaperConfig(name = "   ").displayName)
    }

    @Test
    fun dateVisible_requiresLayoutSupport() {
        // L2 and L5 have no date line in the approved mockups.
        assertFalse(WallpaperConfig(layout = LayoutId.L2, showDate = true).isDateVisible)
        assertFalse(WallpaperConfig(layout = LayoutId.L5, showDate = true).isDateVisible)
        assertTrue(WallpaperConfig(layout = LayoutId.L1, showDate = true).isDateVisible)
        assertFalse(WallpaperConfig(layout = LayoutId.L1, showDate = false).isDateVisible)
    }

    @Test
    fun layoutFromName_unknownFallsBackToL1() {
        assertEquals(LayoutId.L1, LayoutId.fromName(null))
        assertEquals(LayoutId.L1, LayoutId.fromName("NOPE"))
        assertEquals(LayoutId.L4, LayoutId.fromName("L4"))
    }
}
