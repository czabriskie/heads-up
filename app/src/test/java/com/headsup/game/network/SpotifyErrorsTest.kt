package com.headsup.game.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SpotifyErrorsTest {

    private fun httpError(code: Int, body: String) = HttpException(
        Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType()))
    )

    @Test
    fun `extracts the message from a Spotify error body`() {
        assertEquals("Insufficient client scope", spotifyErrorMessage("""{"error":{"status":403,"message":"Insufficient client scope"}}"""))
    }

    @Test
    fun `tolerates bodies with no message or bad json`() {
        assertNull(spotifyErrorMessage("""{"error":{"status": 403}}"""))
        assertNull(spotifyErrorMessage("not json"))
        assertNull(spotifyErrorMessage(""))
        assertNull(spotifyErrorMessage(null))
    }

    @Test
    fun `403 explains the ownership rule and includes the detail`() {
        val text = describeError(httpError(403, """{"error":{"status":403,"message":"Forbidden"}}"""))
        assertTrue(text, text.contains("403"))
        assertTrue(text, text.contains("own or collaborate"))
        assertTrue(text, text.endsWith("(Forbidden)"))
    }

    @Test
    fun `other codes and non-http errors are described plainly`() {
        assertEquals("Spotify returned HTTP 500.", describeError(httpError(500, "")))
        assertEquals("boom", describeError(IOException("boom")))
    }
}
