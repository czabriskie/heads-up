package com.headsup.game.player

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.headsup.game.model.Track
import com.headsup.game.network.SpotifyApi
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.first

private val Context.chorusDataStore: DataStore<Preferences> by preferencesDataStore(name = "chorus_cache")

/**
 * Finds where to start each song so the guesser hears its most recognizable
 * part. Tries Spotify's audio analysis to locate the chorus; when that isn't
 * available (Spotify deprecated the endpoint for apps created after Nov 2024)
 * it falls back to [ChorusLocator.heuristicStartMs].
 *
 * Analysis-based positions are persisted per track; heuristic results are only
 * cached in memory so a transient failure doesn't stick forever.
 */
class ChorusFinder(
    private val api: SpotifyApi,
    context: Context,
) {
    private val appContext = context.applicationContext
    private val memoryCache = ConcurrentHashMap<String, Long>()

    private fun keyFor(trackId: String) = longPreferencesKey("chorus_$trackId")

    suspend fun startPositionMs(track: Track): Long {
        val trackId = track.id ?: return 0
        memoryCache[trackId]?.let { return it }

        val persisted = appContext.chorusDataStore.data.first()[keyFor(trackId)]
        if (persisted != null) {
            memoryCache[trackId] = persisted
            return persisted
        }

        return try {
            val analysis = api.getAudioAnalysis(trackId)
            val position = ChorusLocator.chorusStartMs(track.durationMs, analysis.sections)
            memoryCache[trackId] = position
            appContext.chorusDataStore.edit { it[keyFor(trackId)] = position }
            position
        } catch (e: Exception) {
            val position = ChorusLocator.heuristicStartMs(track.durationMs)
            memoryCache[trackId] = position
            position
        }
    }

    /** Warms the cache for an upcoming track so the round never waits on the network. */
    suspend fun prefetch(track: Track) {
        startPositionMs(track)
    }
}
