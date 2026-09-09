package com.headsup.game.game

import com.headsup.game.model.Artist
import com.headsup.game.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundLogTest {

    private fun track(name: String) = Track(
        id = name,
        name = name,
        uri = "spotify:track:$name",
        artists = listOf(Artist(name = "$name Band")),
    )

    @Test
    fun `scored songs land on the results list in order`() {
        val log = RoundLog()
        log.start()

        log.show(track("one"))
        log.score(correct = true)
        log.show(track("two"))
        log.score(correct = false)

        assertEquals(listOf("one", "two"), log.results.map { it.trackName })
        assertEquals(
            listOf(GuessOutcome.CORRECT, GuessOutcome.PASS),
            log.results.map { it.outcome },
        )
        assertEquals(1, log.correctCount)
        assertEquals(1, log.passCount)
    }

    @Test
    fun `the song on screen when the round ends is listed as unanswered`() {
        val log = RoundLog()
        log.start()

        log.show(track("guessed"))
        log.score(correct = true)
        log.show(track("buzzer"))
        log.finish()

        assertEquals(listOf("guessed", "buzzer"), log.results.map { it.trackName })
        assertEquals(GuessOutcome.UNANSWERED, log.results.last().outcome)
        // Unanswered songs are listed but don't count for or against the score.
        assertEquals(1, log.correctCount)
        assertEquals(0, log.passCount)
    }

    @Test
    fun `artist names come along for the unanswered song`() {
        val log = RoundLog()
        log.start()

        log.show(track("buzzer"))
        log.finish()

        assertEquals("buzzer Band", log.results.single().artistNames)
    }

    @Test
    fun `finishing after the last song was scored adds nothing`() {
        val log = RoundLog()
        log.start()

        log.show(track("one"))
        log.score(correct = true)
        log.finish()

        assertEquals(1, log.results.size)
        assertEquals(GuessOutcome.CORRECT, log.results.single().outcome)
    }

    @Test
    fun `a second gesture before the next song is drawn is ignored`() {
        val log = RoundLog()
        log.start()

        log.show(track("one"))
        assertTrue(log.score(correct = true))
        assertFalse(log.score(correct = false))

        assertEquals(1, log.results.size)
        assertEquals(1, log.correctCount)
        assertEquals(0, log.passCount)
    }

    @Test
    fun `starting a new round clears the previous one`() {
        val log = RoundLog()
        log.start()
        log.show(track("old"))
        log.score(correct = true)
        log.show(track("stale"))

        log.start()

        assertEquals(emptyList<GuessResult>(), log.results)
        assertEquals(0, log.correctCount)
        // The song left in hand from the old round doesn't leak into the new one.
        log.finish()
        assertEquals(emptyList<GuessResult>(), log.results)
    }

    @Test
    fun `finishing twice does not duplicate the unanswered song`() {
        val log = RoundLog()
        log.start()
        log.show(track("buzzer"))

        log.finish()
        log.finish()

        assertEquals(1, log.results.size)
    }
}
