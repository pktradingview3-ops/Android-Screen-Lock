package com.timewall.app.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    // Lower iteration count keeps the test fast. Production uses PinHasher.ITERATIONS.
    private val iterations = 1_000

    @Test
    fun hash_isDeterministicForSameSaltAndPin() {
        val salt = ByteArray(16) { it.toByte() }
        assertArrayEquals(
            PinHasher.hash("1234", salt, iterations),
            PinHasher.hash("1234", salt, iterations),
        )
    }

    @Test
    fun hash_is256Bits() {
        assertEquals(32, PinHasher.hash("1234", PinHasher.newSalt(), iterations).size)
    }

    @Test
    fun hash_differsForDifferentSalts() {
        val a = PinHasher.hash("1234", ByteArray(16) { 1 }, iterations)
        val b = PinHasher.hash("1234", ByteArray(16) { 2 }, iterations)
        assertNotEquals(a.toList(), b.toList())
    }

    @Test
    fun hash_differsForDifferentPins() {
        val salt = ByteArray(16) { 7 }
        assertNotEquals(
            PinHasher.hash("1234", salt, iterations).toList(),
            PinHasher.hash("1235", salt, iterations).toList(),
        )
    }

    @Test
    fun matches_acceptsCorrectPinOnly() {
        val salt = PinHasher.newSalt()
        val expected = PinHasher.hash("246810", salt, iterations)
        assertTrue(PinHasher.matches("246810", salt, iterations, expected))
        assertFalse(PinHasher.matches("246811", salt, iterations, expected))
        assertFalse(PinHasher.matches("", salt, iterations, expected))
    }

    @Test
    fun newSalt_is16BytesAndRandom() {
        val a = PinHasher.newSalt()
        val b = PinHasher.newSalt()
        assertEquals(16, a.size)
        assertNotEquals(a.toList(), b.toList())
    }
}
