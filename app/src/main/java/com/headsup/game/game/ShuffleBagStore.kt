package com.headsup.game.game

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.shuffleDataStore: DataStore<Preferences> by preferencesDataStore(name = "shuffle_bags")

@Serializable
data class PersistedBag(
    val all: List<String> = emptyList(),
    val remaining: List<String> = emptyList(),
)

/** Persists each playlist's shuffle-bag state so no-repeat survives app restarts. */
class ShuffleBagStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private fun keyFor(playlistId: String) = stringPreferencesKey("bag_$playlistId")

    suspend fun load(playlistId: String): PersistedBag? {
        val raw = context.shuffleDataStore.data.first()[keyFor(playlistId)] ?: return null
        return try {
            json.decodeFromString<PersistedBag>(raw)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun save(playlistId: String, bag: ShuffleBag) {
        val persisted = PersistedBag(all = bag.snapshotAll(), remaining = bag.snapshotRemaining())
        context.shuffleDataStore.edit { prefs ->
            prefs[keyFor(playlistId)] = json.encodeToString(persisted)
        }
    }

    suspend fun clear(playlistId: String) {
        context.shuffleDataStore.edit { it.remove(keyFor(playlistId)) }
    }
}
