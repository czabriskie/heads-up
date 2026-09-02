# Heads Up: Music 🎵

A Heads Up–style party game for Android, written in Kotlin with Jetpack Compose — but instead of guessing words, you guess **songs from your own Spotify playlist**.

## How it plays

1. Connect your Spotify account and pick one of your playlists.
2. Hand the phone to the guesser: they hold it against their forehead in landscape, screen facing everyone else.
3. A song from the playlist starts playing out loud through the Spotify app. Friends see the title and artist on screen; the guesser has to name the song.
4. **Tilt the phone down** (screen toward the floor) → correct ✓, next song.
   **Tilt the phone up** (screen toward the ceiling) → pass, next song.
5. When the round timer (60/90/120s) runs out you get a scoreboard of every song and how you did.

### True no-repeat shuffle

The playlist is shuffled into a "bag": every song is drawn exactly once, in random order, before any song can come up again. The bag is **persisted per playlist** — close the app, play tomorrow, and you still won't hear a repeat until the whole playlist has been used up, at which point it reshuffles automatically. Songs added to the playlist join the current cycle; removed songs are dropped. "Reset shuffle" on the game screen starts a fresh cycle on demand.

## Requirements

- Android 8.0+ (API 26)
- The **Spotify app** installed on the same phone
- **Spotify Premium** (the Web API's playback control endpoints require it)

## Setup

### 1. Register a Spotify app

1. Go to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) and create an app.
2. Add this Redirect URI to the app settings: `headsup://callback`
3. Under APIs used, select **Web API**.
4. Copy the **Client ID** (no client secret is needed — the app uses the PKCE flow).

### 2. Configure the project

Add your client ID to `local.properties` (never committed):

```properties
SPOTIFY_CLIENT_ID=your_client_id_here
```

(Alternatively set a `SPOTIFY_CLIENT_ID` Gradle property or environment variable.)

### 3. Build & run

```bash
./gradlew :app:installDebug
```

Or open the project in Android Studio and run the `app` configuration.

### 4. First playback

Playback goes through Spotify Connect, targeting the Spotify app on the phone. If Spotify hasn't played anything recently it may not register as a Connect device — if the game warns "No Spotify device found", open Spotify, play any song for a second, and return to the game.

## Architecture

Single-module Compose app, MVVM, no DI framework.

| Piece | Where | What it does |
|---|---|---|
| PKCE OAuth | `auth/` | Custom Tab sign-in to Spotify, token exchange/refresh, DataStore persistence |
| Web API client | `network/`, `model/` | Retrofit + kotlinx.serialization: playlists, tracks, Connect playback |
| No-repeat shuffle | `game/ShuffleBag.kt` + `ShuffleBagStore.kt` | The persistent shuffle bag (unit-tested) |
| Tilt gestures | `game/TiltDetector.kt` | Gravity-sensor state machine: face-down = correct, face-up = pass |
| Playback | `player/SpotifyPlayer.kt` | Plays each drawn track on the phone's Spotify app, pauses at round end |
| UI | `ui/` | Login → playlist picker → game (setup / countdown / play / results) |

Run the shuffle-logic tests with:

```bash
./gradlew :app:testDebugUnitTest
```
