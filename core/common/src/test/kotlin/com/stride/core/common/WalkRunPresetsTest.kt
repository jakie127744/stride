package com.stride.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class WalkRunPresetsTest {

    @Test
    fun `week 1 gets the getting-started preset`() {
        assertEquals("week1_2", recommendedPresetForWeek(1).id)
    }

    @Test
    fun `week before the table clamps to the first preset`() {
        assertEquals("week1_2", recommendedPresetForWeek(0).id)
    }

    @Test
    fun `week past the table clamps to the last preset`() {
        assertEquals("week7_8", recommendedPresetForWeek(20).id)
    }

    @Test
    fun `week 5 gets steady progress`() {
        assertEquals("week5_6", recommendedPresetForWeek(5).id)
    }
}
