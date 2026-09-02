package com.headsup.game.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val scope: String? = null,
)

@Serializable
data class SpotifyImage(
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class PlaylistTracksRef(
    val total: Int = 0,
)

@Serializable
data class SimplePlaylist(
    val id: String,
    val name: String,
    val images: List<SpotifyImage>? = null,
    val tracks: PlaylistTracksRef? = null,
)

@Serializable
data class PlaylistPage(
    val items: List<SimplePlaylist?> = emptyList(),
    val next: String? = null,
    val total: Int = 0,
)

@Serializable
data class Artist(
    val name: String,
)

@Serializable
data class Track(
    val id: String? = null,
    val name: String,
    val uri: String,
    val type: String = "track",
    val artists: List<Artist> = emptyList(),
    @SerialName("is_local") val isLocal: Boolean = false,
    @SerialName("duration_ms") val durationMs: Long = 0,
) {
    val artistNames: String get() = artists.joinToString(", ") { it.name }
}

/** One entry of a playlist. `item` is the current field; `track` is the pre-2026 name. */
@Serializable
data class PlaylistTrackItem(
    val item: Track? = null,
    val track: Track? = null,
    @SerialName("is_local") val isLocal: Boolean = false,
) {
    /** The playable Spotify track, or null for local files, episodes, and removed tracks. */
    val playableTrack: Track?
        get() = (item ?: track)?.takeIf { it.id != null && it.type == "track" && !it.isLocal && !isLocal }
}

@Serializable
data class TrackPage(
    val items: List<PlaylistTrackItem> = emptyList(),
    val next: String? = null,
    val total: Int = 0,
)

@Serializable
data class Device(
    val id: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
    val name: String = "",
    val type: String = "",
)

@Serializable
data class DevicesResponse(
    val devices: List<Device> = emptyList(),
)

@Serializable
data class AudioSection(
    val start: Double = 0.0,
    val duration: Double = 0.0,
    val loudness: Double = 0.0,
)

@Serializable
data class AudioAnalysis(
    val sections: List<AudioSection> = emptyList(),
)

@Serializable
data class PlayRequest(
    val uris: List<String>,
    @SerialName("position_ms") val positionMs: Long = 0,
)
