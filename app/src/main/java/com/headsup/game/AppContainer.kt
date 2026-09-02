package com.headsup.game

import android.content.Context
import com.headsup.game.auth.SpotifyAuthManager
import com.headsup.game.auth.TokenStore
import com.headsup.game.game.ShuffleBagStore
import com.headsup.game.network.SpotifyApi
import com.headsup.game.network.SpotifyApiFactory
import com.headsup.game.player.SpotifyPlayer

/** Hand-rolled singleton graph — small enough that a DI framework isn't worth it. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val tokenStore: TokenStore by lazy { TokenStore(appContext) }
    val authManager: SpotifyAuthManager by lazy {
        SpotifyAuthManager(tokenStore, SpotifyApiFactory.createAccountsApi())
    }
    val spotifyApi: SpotifyApi by lazy { SpotifyApiFactory.createApi(authManager) }
    val player: SpotifyPlayer by lazy { SpotifyPlayer(spotifyApi) }
    val shuffleBagStore: ShuffleBagStore by lazy { ShuffleBagStore(appContext) }

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
    }
}
