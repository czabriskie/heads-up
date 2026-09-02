package com.headsup.game.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.headsup.game.AppContainer
import com.headsup.game.game.ShuffleBag
import com.headsup.game.game.ShuffleBagStore
import com.headsup.game.model.Track
import com.headsup.game.network.SpotifyApi
import com.headsup.game.network.describeError
import com.headsup.game.player.ChorusFinder
import com.headsup.game.player.SpotifyPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuessResult(
    val trackName: String,
    val artistNames: String,
    val correct: Boolean,
)

enum class Flash { CORRECT, PASS }

sealed interface GameUiState {
    data object Loading : GameUiState

    data class Error(val message: String) : GameUiState

    data class Ready(
        val playlistName: String,
        val totalTracks: Int,
        val remainingInBag: Int,
        val roundSeconds: Int,
        val playSongs: Boolean,
        val startAtChorus: Boolean,
    ) : GameUiState

    data class Countdown(val secondsLeft: Int) : GameUiState

    data class Playing(
        val track: Track,
        val secondsLeft: Int,
        val correctCount: Int,
        val passCount: Int,
        val flash: Flash? = null,
        val playbackWarning: String? = null,
    ) : GameUiState

    data class Finished(val results: List<GuessResult>) : GameUiState
}

class GameViewModel(
    private val playlistId: String,
    private val playlistName: String,
    private val api: SpotifyApi,
    private val player: SpotifyPlayer,
    private val bagStore: ShuffleBagStore,
    private val chorusFinder: ChorusFinder,
) : ViewModel() {

    private val _state = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val state: StateFlow<GameUiState> = _state

    private var tracksById: Map<String, Track> = emptyMap()
    private var bag: ShuffleBag? = null
    private var roundSeconds = 60
    /** Whether to play each song through Spotify; off = title-and-artist only. */
    private var playSongs = true
    private var startAtChorus = true
    private var timerJob: Job? = null
    private var flashJob: Job? = null
    private val results = mutableListOf<GuessResult>()

    init {
        loadPlaylist()
    }

    private fun loadPlaylist() {
        _state.value = GameUiState.Loading
        viewModelScope.launch {
            try {
                val tracks = mutableListOf<Track>()
                var offset = 0
                while (true) {
                    val page = api.getPlaylistTracks(playlistId, limit = 50, offset = offset)
                    tracks += page.items.mapNotNull { it.playableTrack }
                    if (page.next == null || page.items.isEmpty()) break
                    offset += page.items.size
                }
                if (tracks.isEmpty()) {
                    _state.value = GameUiState.Error("This playlist has no playable tracks.")
                    return@launch
                }
                tracksById = tracks.associateBy { it.id!! }
                val persisted = bagStore.load(playlistId)
                val newBag = ShuffleBag(
                    allTrackIds = tracks.map { it.id!! },
                    persistedRemaining = persisted?.remaining,
                    persistedAll = persisted?.all,
                    persistedLastDrawn = persisted?.lastDrawn,
                )
                bag = newBag
                bagStore.save(playlistId, newBag)
                _state.value = GameUiState.Ready(
                    playlistName = playlistName,
                    totalTracks = newBag.totalCount,
                    remainingInBag = newBag.remainingCount,
                    roundSeconds = roundSeconds,
                    playSongs = playSongs,
                    startAtChorus = startAtChorus,
                )
                prefetchUpcoming()
            } catch (e: Exception) {
                _state.value = GameUiState.Error("Couldn't load playlist: ${describeError(e)}")
            }
        }
    }

    fun setRoundSeconds(seconds: Int) {
        roundSeconds = seconds
        _state.update { if (it is GameUiState.Ready) it.copy(roundSeconds = seconds) else it }
    }

    fun setPlaySongs(enabled: Boolean) {
        playSongs = enabled
        _state.update { if (it is GameUiState.Ready) it.copy(playSongs = enabled) else it }
        if (enabled) prefetchUpcoming()
    }

    fun setStartAtChorus(enabled: Boolean) {
        startAtChorus = enabled
        _state.update { if (it is GameUiState.Ready) it.copy(startAtChorus = enabled) else it }
        if (enabled) prefetchUpcoming()
    }

    /** Warms the chorus-position cache for the next track so playback starts instantly. */
    private fun prefetchUpcoming() {
        if (!playSongs || !startAtChorus) return
        val upNextId = bag?.peek() ?: return
        val upNext = tracksById[upNextId] ?: return
        viewModelScope.launch { chorusFinder.prefetch(upNext) }
    }

    /** Puts every track back in the bag, starting a fresh no-repeat cycle. */
    fun resetBag() {
        viewModelScope.launch {
            bagStore.clear(playlistId)
            loadPlaylist()
        }
    }

    fun startGame() {
        if (_state.value !is GameUiState.Ready) return
        results.clear()
        viewModelScope.launch {
            for (n in 3 downTo 1) {
                _state.value = GameUiState.Countdown(n)
                delay(1000)
            }
            nextTrack(initial = true)
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var secondsLeft = roundSeconds
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
                _state.update {
                    if (it is GameUiState.Playing) it.copy(secondsLeft = secondsLeft) else it
                }
            }
            finishGame()
        }
    }

    fun onCorrect() = recordAndAdvance(correct = true)

    fun onPass() = recordAndAdvance(correct = false)

    private fun recordAndAdvance(correct: Boolean) {
        val current = _state.value as? GameUiState.Playing ?: return
        results += GuessResult(current.track.name, current.track.artistNames, correct)
        _state.value = current.copy(
            correctCount = current.correctCount + if (correct) 1 else 0,
            passCount = current.passCount + if (correct) 0 else 1,
            flash = if (correct) Flash.CORRECT else Flash.PASS,
        )
        flashJob?.cancel()
        flashJob = viewModelScope.launch {
            delay(600)
            _state.update { if (it is GameUiState.Playing) it.copy(flash = null) else it }
        }
        viewModelScope.launch { nextTrack(initial = false) }
    }

    private suspend fun nextTrack(initial: Boolean) {
        val currentBag = bag ?: return
        val trackId = currentBag.draw() ?: return
        bagStore.save(playlistId, currentBag)
        val track = tracksById[trackId] ?: return

        val previous = _state.value as? GameUiState.Playing
        _state.value = GameUiState.Playing(
            track = track,
            secondsLeft = if (initial) roundSeconds else previous?.secondsLeft ?: roundSeconds,
            correctCount = previous?.correctCount ?: 0,
            passCount = previous?.passCount ?: 0,
            flash = previous?.flash,
        )

        if (!playSongs) return
        val startMs = if (startAtChorus) chorusFinder.startPositionMs(track) else 0L
        prefetchUpcoming()
        when (val result = player.play(track.uri, positionMs = startMs)) {
            is SpotifyPlayer.PlayResult.Success -> Unit
            is SpotifyPlayer.PlayResult.NoDevice -> setPlaybackWarning(
                "No Spotify device found — open Spotify, play any song for a second, then come back."
            )
            is SpotifyPlayer.PlayResult.Error -> setPlaybackWarning(result.message)
        }
    }

    private fun setPlaybackWarning(message: String) {
        _state.update {
            if (it is GameUiState.Playing) it.copy(playbackWarning = message) else it
        }
    }

    private fun finishGame() {
        timerJob?.cancel()
        if (playSongs) viewModelScope.launch { player.pause() }
        _state.value = GameUiState.Finished(results.toList())
    }

    fun endGameEarly() {
        if (_state.value is GameUiState.Playing) finishGame()
    }

    fun backToReady() {
        val currentBag = bag ?: return loadPlaylist()
        _state.value = GameUiState.Ready(
            playlistName = playlistName,
            totalTracks = currentBag.totalCount,
            remainingInBag = currentBag.remainingCount,
            roundSeconds = roundSeconds,
            playSongs = playSongs,
            startAtChorus = startAtChorus,
        )
        prefetchUpcoming()
    }

    override fun onCleared() {
        timerJob?.cancel()
    }

    class Factory(
        private val container: AppContainer,
        private val playlistId: String,
        private val playlistName: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GameViewModel(
                playlistId = playlistId,
                playlistName = playlistName,
                api = container.spotifyApi,
                player = container.player,
                bagStore = container.shuffleBagStore,
                chorusFinder = container.chorusFinder,
            ) as T
    }
}
