package com.headsup.game.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.headsup.game.AppContainer

private val SpotifyGreen = Color(0xFF1DB954)

@Composable
fun HeadsUpApp(container: AppContainer) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SpotifyGreen,
            secondary = SpotifyGreen,
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val authorized by container.authManager.isAuthorized.collectAsState(initial = null)
            when (authorized) {
                null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                false -> LoginScreen(container)
                true -> AuthorizedNavHost(container)
            }
        }
    }
}

@Composable
private fun AuthorizedNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "playlists") {
        composable("playlists") {
            PlaylistScreen(
                container = container,
                onPlaylistSelected = { playlist ->
                    val encodedName = Uri.encode(playlist.name)
                    navController.navigate("game/${playlist.id}?name=$encodedName")
                },
            )
        }
        composable(
            route = "game/{playlistId}?name={name}",
            arguments = listOf(
                navArgument("playlistId") { type = NavType.StringType },
                navArgument("name") {
                    type = NavType.StringType
                    defaultValue = "Playlist"
                },
            ),
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId").orEmpty()
            val playlistName = backStackEntry.arguments?.getString("name").orEmpty()
            GameScreen(
                container = container,
                playlistId = playlistId,
                playlistName = playlistName,
                onExit = { navController.popBackStack() },
            )
        }
    }
}
