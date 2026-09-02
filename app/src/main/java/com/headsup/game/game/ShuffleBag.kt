package com.headsup.game.game

import kotlin.random.Random

/**
 * A no-repeat shuffle over a playlist's track IDs.
 *
 * Every track is drawn exactly once, in random order, before any track can
 * repeat. Once the bag is empty it automatically refills with the full track
 * list, reshuffled. The bag's remaining contents are persisted between games
 * (see [ShuffleBagStore]), so "no repeats until all have been played" holds
 * across app launches, not just within one round.
 */
class ShuffleBag(
    allTrackIds: List<String>,
    persistedRemaining: List<String>? = null,
    persistedAll: List<String>? = null,
    private val random: Random = Random.Default,
) {
    private val all: List<String> = allTrackIds.distinct()
    private val remaining: ArrayDeque<String>

    init {
        val allSet = all.toSet()
        val base: MutableList<String> = if (persistedRemaining == null) {
            all.shuffled(random).toMutableList()
        } else {
            // Drop tracks that were removed from the playlist since we last synced.
            persistedRemaining.filter { it in allSet }.toMutableList()
        }
        // Tracks added to the playlist since the last sync join the current cycle
        // at random positions, so they can come up before the bag refills.
        if (persistedRemaining != null) {
            val known = (persistedRemaining + (persistedAll ?: emptyList())).toSet()
            val newTracks = all.filter { it !in known }
            for (track in newTracks) {
                base.add(random.nextInt(base.size + 1), track)
            }
        }
        remaining = ArrayDeque(base)
    }

    val totalCount: Int get() = all.size
    val remainingCount: Int get() = remaining.size

    /** Draws the next track ID, refilling and reshuffling when the bag is empty. */
    fun draw(): String? {
        if (all.isEmpty()) return null
        if (remaining.isEmpty()) {
            remaining.addAll(all.shuffled(random))
        }
        return remaining.removeFirst()
    }

    fun snapshotRemaining(): List<String> = remaining.toList()
    fun snapshotAll(): List<String> = all
}
