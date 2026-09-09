package com.headsup.game.game

import com.headsup.game.model.Track

/** How a round ended up treating one song. */
enum class GuessOutcome {
    CORRECT,
    PASS,

    /** Shown to the room, but the round ended before anyone tilted. */
    UNANSWERED,
}

data class GuessResult(
    val trackName: String,
    val artistNames: String,
    val outcome: GuessOutcome,
)

/**
 * The running list of songs a round has shown.
 *
 * A song is drawn from the shuffle bag and put on screen before anyone can
 * score it, so the log keeps that song "in hand" until a tilt scores it or the
 * round ends. Ending the round logs the song still in hand as
 * [GuessOutcome.UNANSWERED]: it was shown, everyone read it off the phone, and
 * it can't come back this cycle, so it belongs on the results list even though
 * nobody guessed it.
 */
class RoundLog {

    private val entries = mutableListOf<GuessResult>()
    private var inHand: Track? = null

    val results: List<GuessResult> get() = entries.toList()
    val correctCount: Int get() = entries.count { it.outcome == GuessOutcome.CORRECT }
    val passCount: Int get() = entries.count { it.outcome == GuessOutcome.PASS }

    /** Clears the log for a new round. */
    fun start() {
        entries.clear()
        inHand = null
    }

    /** Notes that [track] is now facing the room. */
    fun show(track: Track) {
        inHand = track
    }

    /**
     * Scores the song on screen. Returns false when there is nothing to score —
     * a second tilt landing before the next song is drawn, which would
     * otherwise log the same song twice.
     */
    fun score(correct: Boolean): Boolean {
        val track = inHand ?: return false
        inHand = null
        entries += track.toResult(if (correct) GuessOutcome.CORRECT else GuessOutcome.PASS)
        return true
    }

    /** Ends the round, logging the song still on screen as unanswered. */
    fun finish() {
        val track = inHand ?: return
        inHand = null
        entries += track.toResult(GuessOutcome.UNANSWERED)
    }

    private fun Track.toResult(outcome: GuessOutcome) =
        GuessResult(trackName = name, artistNames = artistNames, outcome = outcome)
}
