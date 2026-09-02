package com.headsup.game.ui

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.headsup.game.AppContainer
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authError by container.authManager.authError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🎵 Heads Up: Music", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "Hold the phone to your forehead while a song from your playlist plays. " +
                "Your friends see the title — guess the song! " +
                "Tilt down for correct, tilt up to pass.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        if (!container.authManager.isConfigured) {
            Text(
                "No Spotify client ID configured.\n" +
                    "Add SPOTIFY_CLIENT_ID=<your id> to local.properties and rebuild " +
                    "(see README.md).",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        } else {
            Button(onClick = {
                scope.launch {
                    val uri = container.authManager.buildAuthorizeUri()
                    CustomTabsIntent.Builder().build().launchUrl(context, uri)
                }
            }) {
                Text("Connect Spotify")
            }
            authError?.let { error ->
                Spacer(Modifier.height(16.dp))
                Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}
