package com.headsup.game.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "spotify_auth")

data class StoredTokens(
    val accessToken: String,
    val refreshToken: String,
    /** Epoch millis after which the access token must be refreshed. */
    val expiresAt: Long,
)

class TokenStore(private val context: Context) {

    private object Keys {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val EXPIRES_AT = longPreferencesKey("expires_at")
        val PENDING_VERIFIER = stringPreferencesKey("pending_verifier")
        val PENDING_STATE = stringPreferencesKey("pending_state")
    }

    val tokens: Flow<StoredTokens?> = context.authDataStore.data.map { prefs ->
        val access = prefs[Keys.ACCESS]
        val refresh = prefs[Keys.REFRESH]
        val expiresAt = prefs[Keys.EXPIRES_AT]
        if (access != null && refresh != null && expiresAt != null) {
            StoredTokens(access, refresh, expiresAt)
        } else null
    }

    suspend fun current(): StoredTokens? = tokens.first()

    suspend fun save(accessToken: String, refreshToken: String, expiresInSec: Long) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.ACCESS] = accessToken
            prefs[Keys.REFRESH] = refreshToken
            // Refresh one minute early so in-flight requests don't race expiry.
            prefs[Keys.EXPIRES_AT] = System.currentTimeMillis() + (expiresInSec - 60) * 1000
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }

    suspend fun savePendingAuth(verifier: String, state: String) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.PENDING_VERIFIER] = verifier
            prefs[Keys.PENDING_STATE] = state
        }
    }

    suspend fun consumePendingAuth(): Pair<String, String>? {
        val prefs = context.authDataStore.data.first()
        val verifier = prefs[Keys.PENDING_VERIFIER] ?: return null
        val state = prefs[Keys.PENDING_STATE] ?: return null
        context.authDataStore.edit {
            it.remove(Keys.PENDING_VERIFIER)
            it.remove(Keys.PENDING_STATE)
        }
        return verifier to state
    }
}
