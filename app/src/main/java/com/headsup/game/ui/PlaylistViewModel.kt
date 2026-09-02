package com.headsup.game.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.headsup.game.AppContainer
import com.headsup.game.model.SimplePlaylist
import com.headsup.game.network.SpotifyApi
import com.headsup.game.network.describeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PlaylistUiState {
    data object Loading : PlaylistUiState
    data class Loaded(
        val playlists: List<SimplePlaylist>,
        /** Followed playlists dropped because Spotify won't serve their tracks to this app. */
        val hiddenCount: Int = 0,
    ) : PlaylistUiState
    data class Error(val message: String) : PlaylistUiState
}

class PlaylistViewModel(private val api: SpotifyApi) : ViewModel() {

    private val _state = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val state: StateFlow<PlaylistUiState> = _state

    init {
        load()
    }

    fun load() {
        _state.value = PlaylistUiState.Loading
        viewModelScope.launch {
            try {
                val userId = api.getMe().id
                val playlists = mutableListOf<SimplePlaylist>()
                var offset = 0
                while (true) {
                    val page = api.getMyPlaylists(limit = 50, offset = offset)
                    playlists += page.items.filterNotNull()
                    if (page.next == null || page.items.isEmpty()) break
                    offset += page.items.size
                }
                val readable = playlists.filter { it.isReadableBy(userId) }
                _state.value = PlaylistUiState.Loaded(
                    playlists = readable,
                    hiddenCount = playlists.size - readable.size,
                )
            } catch (e: Exception) {
                _state.value = PlaylistUiState.Error("Couldn't load playlists: ${describeError(e)}")
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlaylistViewModel(container.spotifyApi) as T
    }
}
