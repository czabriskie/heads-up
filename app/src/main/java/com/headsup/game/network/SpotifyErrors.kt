package com.headsup.game.network

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.HttpException

/** Pulls the `error.message` out of a Spotify error body, if there is one. */
internal fun spotifyErrorMessage(body: String?): String? = try {
    body?.takeIf { it.isNotBlank() }
        ?.let { Json.parseToJsonElement(it).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content }
} catch (_: Exception) {
    null
}

/** Human-readable description of an API failure for the UI. */
fun describeError(e: Throwable): String {
    if (e !is HttpException) return e.message ?: e.javaClass.simpleName
    val code = e.code()
    val detail = spotifyErrorMessage(e.response()?.errorBody()?.string())
    val hint = when (code) {
        401 -> "Spotify session expired. Sign out and back in."
        403 -> "Spotify refused access (HTTP 403). Only playlists you own or collaborate on can be loaded."
        429 -> "Spotify rate limit hit. Wait a minute and try again."
        else -> "Spotify returned HTTP $code."
    }
    return if (detail != null) "$hint ($detail)" else hint
}

/** Debug-only: logs Spotify's error body for non-2xx responses without consuming it. */
internal object SpotifyErrorBodyLogger : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (!response.isSuccessful) {
            val body = response.peekBody(4096).string()
            Log.w("SpotifyApi", "${response.code} ${chain.request().method} ${chain.request().url.encodedPath}: $body")
        }
        return response
    }
}
