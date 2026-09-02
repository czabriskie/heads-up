package com.headsup.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.headsup.game.ui.HeadsUpApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer.get(this)
        handleAuthRedirect(intent)
        setContent {
            HeadsUpApp(container)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthRedirect(intent)
    }

    private fun handleAuthRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "headsup" && uri.host == "callback") {
            lifecycleScope.launch {
                container.authManager.handleCallback(uri)
            }
        }
    }
}
