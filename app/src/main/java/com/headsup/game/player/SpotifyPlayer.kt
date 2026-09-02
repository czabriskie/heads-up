package com.headsup.game.player

import com.headsup.game.model.PlayRequest
import com.headsup.game.network.SpotifyApi

/**
 * Plays tracks through Spotify Connect: the phone's own Spotify app acts as
 * the playback device. Requires Spotify Premium and the Spotify app installed.
 */
class SpotifyPlayer(private val api: SpotifyApi) {

    private var deviceId: String? = null

    sealed interface PlayResult {
        data object Success : PlayResult
        data object NoDevice : PlayResult
        data class Error(val message: String) : PlayResult
    }

    private suspend fun resolveDevice(): String? {
        val devices = api.getDevices().devices
        val chosen = devices.firstOrNull { it.isActive }
            ?: devices.firstOrNull { it.type.equals("Smartphone", ignoreCase = true) }
            ?: devices.firstOrNull()
        deviceId = chosen?.id
        return deviceId
    }

    suspend fun play(trackUri: String, positionMs: Long = 0): PlayResult {
        return try {
            val body = PlayRequest(uris = listOf(trackUri), positionMs = positionMs)
            var response = api.play(body, deviceId)
            if (response.code() == 404) {
                // NO_ACTIVE_DEVICE — re-resolve and retry once with an explicit device.
                val device = resolveDevice() ?: return PlayResult.NoDevice
                response = api.play(body, device)
            }
            when {
                response.isSuccessful -> PlayResult.Success
                response.code() == 404 -> PlayResult.NoDevice
                response.code() == 403 ->
                    PlayResult.Error("Playback control needs Spotify Premium.")
                else -> PlayResult.Error("Spotify playback failed (HTTP ${response.code()}).")
            }
        } catch (e: Exception) {
            PlayResult.Error("Spotify playback failed: ${e.message}")
        }
    }

    suspend fun pause() {
        try {
            api.pause(deviceId)
        } catch (e: Exception) {
            // Best-effort: losing a pause at game end is harmless.
        }
    }
}
