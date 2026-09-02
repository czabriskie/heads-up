package com.headsup.game.player

import com.headsup.game.model.AudioSection
import org.junit.Assert.assertEquals
import org.junit.Test

class ChorusLocatorTest {

    // A typical ~3.5 minute song.
    private val durationMs = 210_000L

    private fun section(startSec: Double, loudness: Double) =
        AudioSection(start = startSec, duration = 20.0, loudness = loudness)

    @Test
    fun `picks the loudest section inside the chorus window`() {
        val sections = listOf(
            section(0.0, -20.0),    // intro
            section(40.0, -6.0),    // first chorus (loudest in window)
            section(70.0, -12.0),   // verse
            section(100.0, -8.0),   // second chorus
        )
        assertEquals(40_000L, ChorusLocator.chorusStartMs(durationMs, sections))
    }

    @Test
    fun `ignores loud sections outside the window`() {
        val sections = listOf(
            section(5.0, -2.0),     // loud intro, before 15% of 210s (31.5s)
            section(50.0, -8.0),    // in window
            section(190.0, -1.0),   // loudest of all, but past 65% (136.5s)
        )
        assertEquals(50_000L, ChorusLocator.chorusStartMs(durationMs, sections))
    }

    @Test
    fun `falls back to heuristic when no section is in the window`() {
        val sections = listOf(section(1.0, -5.0), section(200.0, -3.0))
        assertEquals(
            ChorusLocator.heuristicStartMs(durationMs),
            ChorusLocator.chorusStartMs(durationMs, sections),
        )
    }

    @Test
    fun `falls back to heuristic with no sections at all`() {
        assertEquals(63_000L, ChorusLocator.chorusStartMs(durationMs, emptyList()))
    }

    @Test
    fun `heuristic starts 30 percent in`() {
        assertEquals(63_000L, ChorusLocator.heuristicStartMs(durationMs))
    }

    @Test
    fun `short tracks start from the beginning`() {
        assertEquals(0L, ChorusLocator.chorusStartMs(45_000L, listOf(section(20.0, -5.0))))
        assertEquals(0L, ChorusLocator.heuristicStartMs(45_000L))
    }

    @Test
    fun `start position always leaves at least 30 seconds of song`() {
        // 80s track: chorus window ends at 52s, but starting at 51s would leave
        // only 29s of song — the position is clamped to duration - 30s = 50s.
        val songMs = 80_000L
        val sections = listOf(section(51.0, -3.0))
        assertEquals(50_000L, ChorusLocator.chorusStartMs(songMs, sections))
    }
}
