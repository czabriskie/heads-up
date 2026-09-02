package com.headsup.game.auth

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

object Pkce {

    private val random = SecureRandom()

    fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        random.nextBytes(bytes)
        return base64Url(bytes)
    }

    fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return base64Url(digest)
    }

    fun generateState(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return base64Url(bytes)
    }

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
