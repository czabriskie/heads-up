package com.headsup.game.game

import org.junit.Assert.assertEquals
import org.junit.Test

class TiltGestureFilterTest {

    private val events = mutableListOf<String>()
    private val filter = TiltGestureFilter(
        onTiltDown = { events += "down" },
        onTiltUp = { events += "up" },
    )

    private fun feed(z: Float, times: Int = 20) = repeat(times) { filter.onSample(z) }

    @Test
    fun `starting flat on a table fires nothing`() {
        feed(9.8f) // screen up, lying flat
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `tilting down from vertical fires one correct`() {
        feed(0f)     // held to the forehead
        feed(-9.8f)  // screen toward the floor, held there
        assertEquals(listOf("down"), events)
    }

    @Test
    fun `tilting up from vertical fires one pass`() {
        feed(0f)
        feed(9.8f)
        assertEquals(listOf("up"), events)
    }

    @Test
    fun `holding a tilt fires only once`() {
        feed(0f)
        feed(-9.8f, times = 200)
        assertEquals(listOf("down"), events)
    }

    @Test
    fun `must return to neutral before the next gesture`() {
        feed(0f)
        feed(-9.8f)
        feed(-5f)    // eased off, but not back to vertical
        feed(-9.8f)  // tilting down again must not double-count
        assertEquals(listOf("down"), events)
        feed(0f)     // back to the forehead
        feed(9.8f)
        assertEquals(listOf("down", "up"), events)
    }

    @Test
    fun `small wobbles below the trigger do nothing`() {
        feed(0f)
        feed(5f)
        feed(-5f)
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `reset requires neutral again`() {
        feed(0f)
        filter.reset()
        feed(-9.8f)
        assertEquals(emptyList<String>(), events)
    }
}
