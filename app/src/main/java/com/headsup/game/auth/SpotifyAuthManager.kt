package com.headsup.game.auth

import android.net.Uri
import com.headsup.game.BuildConfig
import com.headsup.game.network.SpotifyAccountsApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SpotifyAuthManager(
    private val tokenStore: TokenStore,
    private val accountsApi: SpotifyAccountsApi,
) {
    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI
    private val refreshMutex = Mutex()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    val isAuthorized: Flow<Boolean> = tokenStore.tokens.map { it != null }

    val isConfigured: Boolean get() = clientId.isNotBlank()

    private val scopes = listOf(
        "playlist-read-private",
        "playlist-read-collaborative",
        "user-read-playback-state",
        "user-modify-playback-state",
    )

    /** Builds the authorize URL and persists the PKCE verifier for the redirect leg. */
    suspend fun buildAuthorizeUri(): Uri {
        val verifier = Pkce.generateCodeVerifier()
        val state = Pkce.generateState()
        tokenStore.savePendingAuth(verifier, state)
        _authError.value = null
        return Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", Pkce.codeChallenge(verifier))
            .appendQueryParameter("state", state)
            .appendQueryParameter("scope", scopes.joinToString(" "))
            .build()
    }

    /** Handles the headsup://callback redirect. */
    suspend fun handleCallback(uri: Uri) {
        val pending = tokenStore.consumePendingAuth()
        if (pending == null) {
            _authError.value = "Sign-in session expired, please try again."
            return
        }
        val (verifier, expectedState) = pending

        val error = uri.getQueryParameter("error")
        if (error != null) {
            _authError.value = "Spotify sign-in failed: $error"
            return
        }
        if (uri.getQueryParameter("state") != expectedState) {
            _authError.value = "Sign-in state mismatch, please try again."
            return
        }
        val code = uri.getQueryParameter("code")
        if (code == null) {
            _authError.value = "Spotify did not return an authorization code."
            return
        }
        try {
            val tokens = accountsApi.exchangeCode(
                code = code,
                redirectUri = redirectUri,
                clientId = clientId,
                codeVerifier = verifier,
            )
            val refresh = tokens.refreshToken
            if (refresh == null) {
                _authError.value = "Spotify did not return a refresh token."
                return
            }
            tokenStore.save(tokens.accessToken, refresh, tokens.expiresIn)
        } catch (e: Exception) {
            _authError.value = "Could not complete sign-in: ${e.message}"
        }
    }

    /**
     * Returns a non-expired access token, refreshing if needed.
     * Returns null when signed out (or when the refresh token was revoked).
     */
    suspend fun getValidAccessToken(): String? = refreshMutex.withLock {
        val stored = tokenStore.current() ?: return null
        if (System.currentTimeMillis() < stored.expiresAt) return stored.accessToken
        return try {
            val refreshed = accountsApi.refreshToken(
                refreshToken = stored.refreshToken,
                clientId = clientId,
            )
            tokenStore.save(
                accessToken = refreshed.accessToken,
                refreshToken = refreshed.refreshToken ?: stored.refreshToken,
                expiresInSec = refreshed.expiresIn,
            )
            refreshed.accessToken
        } catch (e: retrofit2.HttpException) {
            // Refresh token revoked or invalid: force sign-in again.
            tokenStore.clear()
            null
        } catch (e: Exception) {
            // Network hiccup: fall back to the stored (possibly stale) token.
            stored.accessToken
        }
    }

    suspend fun signOut() {
        tokenStore.clear()
    }
}
