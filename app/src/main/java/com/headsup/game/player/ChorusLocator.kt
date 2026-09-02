package com.headsup.game.player

import com.headsup.game.model.AudioSection

/**
 * Pure logic for picking a recognizable start position in a track.
 *
 * With Spotify audio-analysis data: the chorus is almost always the loudest
 * section of a song, but the *final* chorus or an outro can be louder still,
 * so we only consider sections starting in the 15%–65% window — that reliably
 * lands on the first or second chorus rather than the tail of the song.
 *
 * Without analysis data: start 30% in, which is where a first chorus
 * typically sits in a 3–4 minute pop song.
 */
object ChorusLocator {

    /** Never start so late that less than this much of the song remains. */
    private const val MIN_TAIL_MS = 30_000L

    /** Songs this short are recognizable from the top; just play them whole. */
    private const val SHORT_TRACK_MS = 60_000L

    private const val WINDOW_START_FRACTION = 0.15
    private const val WINDOW_END_FRACTION = 0.65
    private const val HEURISTIC_FRACTION = 0.30

    fun chorusStartMs(durationMs: Long, sections: List<AudioSection>): Long {
        if (durationMs <= SHORT_TRACK_MS) return 0
        val windowStartMs = durationMs * WINDOW_START_FRACTION
        val windowEndMs = durationMs * WINDOW_END_FRACTION
        val best = sections
            .filter { it.start * 1000 in windowStartMs..windowEndMs }
            .maxByOrNull { it.loudness }
            ?: return heuristicStartMs(durationMs)
        return clamp((best.start * 1000).toLong(), durationMs)
    }

    fun heuristicStartMs(durationMs: Long): Long {
        if (durationMs <= SHORT_TRACK_MS) return 0
        return clamp((durationMs * HEURISTIC_FRACTION).toLong(), durationMs)
    }

    private fun clamp(positionMs: Long, durationMs: Long): Long =
        positionMs.coerceIn(0, maxOf(0, durationMs - MIN_TAIL_MS))
}
