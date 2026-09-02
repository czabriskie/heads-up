package com.headsup.game.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShuffleBagTest {

    private val tracks = (1..10).map { "track$it" }

    @Test
    fun `draws every track exactly once before any repeat`() {
        val bag = ShuffleBag(tracks, random = Random(42))
        val drawn = (1..tracks.size).map { bag.draw()!! }
        assertEquals(tracks.toSet(), drawn.toSet())
        assertEquals(tracks.size, drawn.distinct().size)
    }

    @Test
    fun `refills and reshuffles after exhaustion`() {
        val bag = ShuffleBag(tracks, random = Random(42))
        repeat(tracks.size) { bag.draw() }
        assertEquals(0, bag.remainingCount)

        val secondCycle = (1..tracks.size).map { bag.draw()!! }
        assertEquals(tracks.toSet(), secondCycle.toSet())
    }

    @Test
    fun `refill never starts with the track just drawn`() {
        // Many seeds, small playlist: a naive reshuffle would repeat often.
        repeat(500) { seed ->
            val bag = ShuffleBag(listOf("a", "b", "c"), random = Random(seed))
            repeat(2) { bag.draw() }
            val lastOfCycle = bag.draw()!!
            assertTrue(bag.draw()!! != lastOfCycle)
        }
    }

    @Test
    fun `refill after restart still avoids the last drawn track`() {
        repeat(200) { seed ->
            val first = ShuffleBag(listOf("a", "b", "c"), random = Random(seed))
            repeat(3) { first.draw() }
            val restored = ShuffleBag(
                listOf("a", "b", "c"),
                persistedRemaining = first.snapshotRemaining(),
                persistedAll = first.snapshotAll(),
                persistedLastDrawn = first.snapshotLastDrawn(),
                random = Random(seed + 1000),
            )
            assertTrue(restored.draw()!! != first.snapshotLastDrawn())
        }
    }

    @Test
    fun `empty playlist draws null`() {
        val bag = ShuffleBag(emptyList())
        assertNull(bag.draw())
    }

    @Test
    fun `persisted remaining is honored across restarts`() {
        val first = ShuffleBag(tracks, random = Random(1))
        val drawnBefore = (1..4).map { first.draw()!! }

        // Simulate app restart: rebuild from the persisted snapshot.
        val second = ShuffleBag(
            allTrackIds = tracks,
            persistedRemaining = first.snapshotRemaining(),
            persistedAll = first.snapshotAll(),
            random = Random(2),
        )
        val drawnAfter = (1..6).map { second.draw()!! }

        // The full cycle across the restart still covers all tracks with no repeats.
        assertEquals(tracks.toSet(), (drawnBefore + drawnAfter).toSet())
        assertEquals(tracks.size, (drawnBefore + drawnAfter).distinct().size)
    }

    @Test
    fun `tracks removed from playlist are dropped from the bag`() {
        val first = ShuffleBag(tracks, random = Random(1))
        val shrunk = tracks - "track3"
        val second = ShuffleBag(
            allTrackIds = shrunk,
            persistedRemaining = first.snapshotRemaining(),
            persistedAll = first.snapshotAll(),
        )
        val drawn = (1..second.remainingCount).map { second.draw()!! }
        assertTrue("track3" !in drawn)
    }

    @Test
    fun `tracks added to playlist join the current cycle`() {
        val first = ShuffleBag(tracks, random = Random(1))
        repeat(5, { first.draw() })

        val grown = tracks + "trackNew"
        val second = ShuffleBag(
            allTrackIds = grown,
            persistedRemaining = first.snapshotRemaining(),
            persistedAll = first.snapshotAll(),
            random = Random(2),
        )
        assertEquals(6, second.remainingCount)
        val drawn = (1..second.remainingCount).map { second.draw()!! }
        assertTrue("trackNew" in drawn)
    }

    @Test
    fun `duplicate track ids are collapsed`() {
        val bag = ShuffleBag(listOf("a", "b", "a", "c", "b"))
        assertEquals(3, bag.totalCount)
        val drawn = (1..3).map { bag.draw()!! }
        assertEquals(setOf("a", "b", "c"), drawn.toSet())
    }
}
