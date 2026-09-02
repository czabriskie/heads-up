package com.headsup.game.network

import com.headsup.game.model.DevicesResponse
import com.headsup.game.model.PlayRequest
import com.headsup.game.model.PlaylistPage
import com.headsup.game.model.TokenResponse
import com.headsup.game.model.TrackPage
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

    @GET("v1/me/playlists")
    suspend fun getMyPlaylists(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): PlaylistPage

    @GET("v1/playlists/{id}/tracks")
    suspend fun getPlaylistTracks(
        @Path("id") playlistId: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: String =
            "items(track(id,name,uri,is_local,duration_ms,artists(name))),next,total",
    ): TrackPage

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
