package com.headsup.game.network

import com.headsup.game.BuildConfig
import com.headsup.game.auth.SpotifyAuthManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object SpotifyApiFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val converterFactory = json.asConverterFactory("application/json".toMediaType())

    fun createAccountsApi(): SpotifyAccountsApi =
        Retrofit.Builder()
            .baseUrl("https://accounts.spotify.com/")
            .client(OkHttpClient())
            .addConverterFactory(converterFactory)
            .build()
            .create(SpotifyAccountsApi::class.java)

    fun createApi(authManager: SpotifyAuthManager): SpotifyApi {
        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { authManager.getValidAccessToken() }
            val request = if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    // Logs request lines and response bodies (tag "okhttp.OkHttpClient").
                    // Headers are skipped so the bearer token never lands in logcat.
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                    addInterceptor(SpotifyErrorBodyLogger)
                }
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.spotify.com/")
            .client(client)
            .addConverterFactory(converterFactory)
            .build()
            .create(SpotifyApi::class.java)
    }
}
