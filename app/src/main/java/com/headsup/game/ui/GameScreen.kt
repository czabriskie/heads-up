package com.headsup.game.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.headsup.game.AppContainer
import com.headsup.game.game.TiltDetector

private val CorrectGreen = Color(0xFF1DB954)
private val PassOrange = Color(0xFFE07A00)
private val GameBlue = Color(0xFF1A3C6E)

@Composable
fun GameScreen(
    container: AppContainer,
    playlistId: String,
    playlistName: String,
    onExit: () -> Unit,
) {
    val viewModel: GameViewModel = viewModel(
        key = "game_$playlistId",
        factory = GameViewModel.Factory(container, playlistId, playlistName),
    )
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is GameUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is GameUiState.Error -> Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(s.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onExit) { Text("Back") }
        }
        is GameUiState.Ready -> ReadyContent(s, viewModel, onExit)
        is GameUiState.Countdown -> {
            GameModeEffects()
            Box(
                Modifier.fillMaxSize().background(GameBlue),
                contentAlignment = Alignment.Center,
            ) {
                Text("${s.secondsLeft}", fontSize = 160.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        is GameUiState.Playing -> {
            GameModeEffects()
            PlayingContent(s, viewModel)
        }
        is GameUiState.Finished -> ResultsContent(s, viewModel, onExit)
    }
}

/** Locks landscape and keeps the screen on while the phone is on someone's forehead. */
@Composable
private fun GameModeEffects() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
private fun ReadyContent(state: GameUiState.Ready, viewModel: GameViewModel, onExit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(state.playlistName, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "${state.remainingInBag} of ${state.totalTracks} songs left before the shuffle starts over",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Text("Round length", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60, 90, 120).forEach { seconds ->
                FilterChip(
                    selected = state.roundSeconds == seconds,
                    onClick = { viewModel.setRoundSeconds(seconds) },
                    label = { Text("${seconds}s") },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Switch(
                checked = state.playSongs,
                onCheckedChange = { viewModel.setPlaySongs(it) },
            )
            Text("Play songs through Spotify", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Switch(
                checked = state.startAtChorus,
                enabled = state.playSongs,
                onCheckedChange = { viewModel.setStartAtChorus(it) },
            )
            Text(
                "Start songs at the chorus",
                style = MaterialTheme.typography.bodyLarge,
                color = if (state.playSongs) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            (if (state.playSongs) "Make sure the Spotify app is open on this phone, then hold the phone "
            else "Songs won't play; your friends hum, sing, or describe them. Hold the phone ") +
                "to your forehead, screen facing your friends.\n\n" +
                "⬇️ Tilt down = correct   ⬆️ Tilt up = pass",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = { viewModel.startGame() }, modifier = Modifier.fillMaxWidth()) {
            Text("Start round", fontSize = 20.sp, modifier = Modifier.padding(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row {
            TextButton(onClick = { viewModel.resetBag() }) { Text("Reset shuffle") }
            TextButton(onClick = onExit) { Text("Back to playlists") }
        }
    }
}

@Composable
private fun PlayingContent(state: GameUiState.Playing, viewModel: GameViewModel) {
    val context = LocalContext.current
    val currentViewModel by rememberUpdatedState(viewModel)

    DisposableEffect(Unit) {
        val detector = TiltDetector(
            context = context,
            onTiltDown = { currentViewModel.onCorrect() },
            onTiltUp = { currentViewModel.onPass() },
        )
        detector.start()
        onDispose { detector.stop() }
    }

    val background by animateColorAsState(
        targetValue = when (state.flash) {
            Flash.CORRECT -> CorrectGreen
            Flash.PASS -> PassOrange
            null -> GameBlue
        },
        label = "gameBackground",
    )

    Box(Modifier.fillMaxSize().background(background)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("✓ ${state.correctCount}   ✗ ${state.passCount}", fontSize = 22.sp, color = Color.White)
            Text("${state.secondsLeft}s", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.flash) {
                Flash.CORRECT -> Text("CORRECT!", fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Flash.PASS -> Text("PASS", fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.Bold)
                null -> {
                    Text(
                        state.track.name,
                        fontSize = 44.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 52.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        state.track.artistNames,
                        fontSize = 26.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        state.playbackWarning?.let { warning ->
            Text(
                warning,
                color = Color.Yellow,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
        }
        TextButton(
            onClick = { viewModel.endGameEarly() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
        ) {
            Text("End", color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun ResultsContent(state: GameUiState.Finished, viewModel: GameViewModel, onExit: () -> Unit) {
    val correct = state.results.count { it.correct }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Time's up! 🎉", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "$correct correct out of ${state.results.size}",
            style = MaterialTheme.typography.titleLarge,
            color = CorrectGreen,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.results) { result ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (result.correct) "✓" else "✗",
                        fontSize = 22.sp,
                        color = if (result.correct) CorrectGreen else PassOrange,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(result.trackName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            result.artistNames,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.backToReady() }, modifier = Modifier.fillMaxWidth()) {
            Text("Play again")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text("Choose another playlist")
        }
    }
}
