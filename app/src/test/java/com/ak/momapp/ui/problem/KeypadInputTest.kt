package com.ak.momapp.ui.problem

import org.junit.Assert.assertEquals
import org.junit.Test

class KeypadInputTest {

    @Test
    fun `digits append`() {
        assertEquals("1", applyKey("", KeypadKey.D1))
        assertEquals("13", applyKey("1", KeypadKey.D3))
    }

    @Test
    fun `backspace removes the last digit and stops at empty`() {
        assertEquals("1", applyKey("13", KeypadKey.BACKSPACE))
        assertEquals("", applyKey("1", KeypadKey.BACKSPACE))
        assertEquals("", applyKey("", KeypadKey.BACKSPACE))
    }

    @Test
    fun `clear empties everything`() {
        assertEquals("", applyKey("1234", KeypadKey.CLEAR))
    }

    @Test
    fun `a leading zero is replaced rather than kept`() {
        assertEquals("5", applyKey("0", KeypadKey.D5))
        assertEquals("0", applyKey("", KeypadKey.D0))
    }

    @Test
    fun `input stops at a sane length`() {
        val long = "123456789"
        assertEquals(long, applyKey(long, KeypadKey.D1))
    }

    /**
     * Every digit key maps to the figure printed on it. Guards the
     * ordinal trick in applyKey: reordering the enum would otherwise
     * silently make the 7 key type a 4.
     */
    @Test
    fun `every digit key types its own figure`() {
        listOf(
            KeypadKey.D0, KeypadKey.D1, KeypadKey.D2, KeypadKey.D3, KeypadKey.D4,
            KeypadKey.D5, KeypadKey.D6, KeypadKey.D7, KeypadKey.D8, KeypadKey.D9,
        ).forEachIndexed { digit, key ->
            assertEquals(digit.toString(), key.digit)
            // Appended to a non-empty, non-zero string so nothing rewrites it.
            assertEquals("3$digit", applyKey("3", key))
        }
    }
}
