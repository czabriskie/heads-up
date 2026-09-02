package com.headsup.game.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.headsup.game.AppContainer
import com.headsup.game.model.SimplePlaylist
import kotlinx.coroutines.launch

@Composable
fun PlaylistScreen(
    container: AppContainer,
    onPlaylistSelected: (SimplePlaylist) -> Unit,
) {
    val viewModel: PlaylistViewModel = viewModel(factory = PlaylistViewModel.Factory(container))
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Pick a playlist", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = { scope.launch { container.authManager.signOut() } }) {
                Text("Sign out")
            }
        }
        Spacer(Modifier.height(8.dp))

        when (val s = state) {
            is PlaylistUiState.Loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            is PlaylistUiState.Error -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.load() }) { Text("Retry") }
            }
            is PlaylistUiState.Loaded -> {
                if (s.playlists.isEmpty()) {
                    Text(
                        if (s.hiddenCount > 0)
                            "None of your playlists can be used: Spotify only lets this app read " +
                                "playlists you created or collaborate on. Make one in Spotify first."
                        else "You have no playlists yet — create one in Spotify first."
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(s.playlists, key = { it.id }) { playlist ->
                            PlaylistRow(playlist) { onPlaylistSelected(playlist) }
                        }
                        if (s.hiddenCount > 0) {
                            item(key = "hidden-note") {
                                Text(
                                    "${s.hiddenCount} followed playlist${if (s.hiddenCount == 1) "" else "s"} hidden: " +
                                        "Spotify only lets this app read playlists you created or collaborate on.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(playlist: SimplePlaylist, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val imageUrl = playlist.images?.firstOrNull()?.url
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                playlist.tracks?.let {
                    Text(
                        "${it.total} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
