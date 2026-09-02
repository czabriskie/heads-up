package com.headsup.game.network

import com.headsup.game.model.AudioAnalysis
import com.headsup.game.model.DevicesResponse
import com.headsup.game.model.PlayRequest
import com.headsup.game.model.PlaylistPage
import com.headsup.game.model.TokenResponse
import com.headsup.game.model.TrackPage
import com.headsup.game.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** https://accounts.spotify.com — token exchange for the PKCE flow. */
interface SpotifyAccountsApi {

    @FormUrlEncoded
    @POST("api/token")
    suspend fun exchangeCode(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("code_verifier") codeVerifier: String,
    ): TokenResponse

    @FormUrlEncoded
    @POST("api/token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String,
    ): TokenResponse
}

/** https://api.spotify.com — playlists, tracks, and Connect playback control. */
interface SpotifyApi {

    /** Current user's profile; `id` is returned without any extra scope. */
    @GET("v1/me")
    suspend fun getMe(): UserProfile

    @GET("v1/me/playlists")
    suspend fun getMyPlaylists(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): PlaylistPage

    /**
     * Get Playlist Items. Replaces the deprecated `/playlists/{id}/tracks`
     * endpoint (2026 Web API changes): entries are under `item`, `is_local`
     * moved to the wrapper, and `limit` is capped at 50. Spotify only serves
     * this for playlists the user owns or collaborates on; anything else 403s.
     */
    @GET("v1/playlists/{id}/items")
    suspend fun getPlaylistTracks(
        @Path("id") playlistId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("additional_types") additionalTypes: String = "track",
        @Query("fields") fields: String =
            "items(is_local,item(id,type,name,uri,duration_ms,artists(name))),next,total",
    ): TrackPage

    /**
     * Section-level audio analysis, used to locate the chorus. Deprecated by
     * Spotify for apps created after Nov 2024 (returns 403) — callers must
     * handle failure and fall back to a heuristic.
     */
    @GET("v1/audio-analysis/{id}")
    suspend fun getAudioAnalysis(
        @Path("id") trackId: String,
    ): AudioAnalysis

    @GET("v1/me/player/devices")
    suspend fun getDevices(): DevicesResponse

    @PUT("v1/me/player/play")
    suspend fun play(
        @Body body: PlayRequest,
        @Query("device_id") deviceId: String? = null,
    ): Response<Unit>

    @PUT("v1/me/player/pause")
    suspend fun pause(
        @Query("device_id") deviceId: String? = null,
    ): Response<Unit>
}
