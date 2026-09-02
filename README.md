# Heads Up: Music 🎵

A Heads Up–style party game for Android, written in Kotlin with Jetpack Compose — but instead of guessing words, you guess **songs from your own Spotify playlists**.

## Features

| Feature | Details |
|---|---|
| **Play with your own playlists** | Sign in with Spotify (PKCE OAuth, no client secret) and pick any playlist you created or collaborate on. |
| **Songs play out loud** | Each song plays through the Spotify app on the phone via Spotify Connect. |
| **Optional silent mode** | Turn off "Play songs through Spotify" before a round and friends hum, sing, or describe the song instead. No Spotify calls are made during a silent round. |
| **Start at the chorus** | Songs start at their most recognizable part instead of the intro (toggle on the setup screen). |
| **True no-repeat shuffle** | Every song in a playlist is drawn once, in random order, before any repeat. Progress is saved per playlist across rounds and app restarts. |
| **Tilt gestures** | Tilt the phone down for correct, up for pass. One gesture per tilt; the phone must return to your forehead in between. |
| **Sound cues** | A rising chime on correct and a falling tone on pass, layered over the music. |
| **Round timer and scoreboard** | 60, 90, or 120 second rounds, then a results screen listing every song and how you did. |
| **Reset shuffle** | Start a fresh no-repeat cycle for a playlist on demand. |

## How to play

1. Open the app and tap **Connect Spotify**. Approve access in the browser; you're bounced back into the app.
2. Pick a playlist. Only playlists you created or collaborate on are listed (see [Which playlists work](#which-playlists-work)).
3. On the setup screen choose a round length and whether songs should play and start at the chorus. Make sure the Spotify app is open on the phone if playback is on.
4. Tap **Start round**, then hand the phone to the guesser. They hold it against their forehead in landscape with the screen facing everyone else.
5. A song starts and the title and artist appear on screen for the other players. The guesser names the song.
   - **Tilt down** (screen toward the floor) = correct ✓
   - **Tilt up** (screen toward the ceiling) = pass ✗
6. When the timer runs out, the scoreboard shows every song from the round. Tap **Play again** for another round with the same playlist, or go back and pick a different one.

### Which playlists work

Spotify's Web API only lets third-party apps read the contents of playlists you **own or collaborate on**. Playlists you merely follow, including Spotify's own editorial playlists, are hidden from the picker with a note saying how many were skipped. To play one of those, add its songs to a playlist you create yourself.

### Songs start at the chorus

The app asks Spotify's audio analysis for the song's sections and jumps to the loudest section in the 15–65% window of the track, which almost always lands on a chorus. Positions are cached per track and prefetched for the upcoming song.

> Spotify no longer serves audio analysis to apps created after November 2024, so on a new client ID the game falls back to starting 30% into the song, never later than 30 seconds before the end. Songs of a minute or less play from the top.

### No-repeat shuffle

Each playlist has a persistent shuffle "bag". Every song is drawn exactly once, in random order, before any song can come up again, and the bag is saved on the phone so tomorrow's game continues where today's left off. When the bag empties it reshuffles, and the new cycle never starts with the song you just heard. Songs added to the playlist join the current cycle; removed songs are dropped. Bags are per playlist, so the same song can come up from two playlists that both contain it.

## Requirements

- Android 8.0+ (API 26)
- The **Spotify app** installed on the same phone
- **Spotify Premium** on the account you sign in with. Playback control requires it, and Spotify now requires Premium to register a developer app at all.

## Install with Android Studio

### 1. Register a Spotify app

1. Go to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) and sign in with your Premium account.
2. Click **Create app** (top right). If the button is missing or disabled, Spotify may have new app creation paused; see [Troubleshooting](#troubleshooting).
3. Fill in any name and description. Set **Redirect URI** to exactly:
   ```
   headsup://callback
   ```
4. Under "Which API/SDKs are you planning to use?" tick **Web API**, accept the terms, and save.
5. Open the app's **Settings** and copy the **Client ID**. No client secret is needed.

### 2. Get the code

```bash
git clone git@github.com:czabriskie/heads-up.git
cd heads-up
```

### 3. Add your client ID

Create a file named `local.properties` in the repo root (it's gitignored) containing:

```properties
SPOTIFY_CLIENT_ID=your_client_id_here
```

Android Studio adds its own `sdk.dir=...` line to the same file; keep both.

You can also supply the ID as a Gradle property or an environment variable named `SPOTIFY_CLIENT_ID`.

### 4. Open and run

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or newer). It bundles the JDK and Android SDK the project needs.
2. **File → Open** and choose the `heads-up` folder. Let Gradle sync finish; accept any SDK component downloads it asks for.
3. On your phone, enable **Developer options** (Settings → About phone → tap "Build number" seven times) and turn on **USB debugging**.
4. Plug the phone in, accept the "Allow USB debugging?" prompt, and pick the phone in the device dropdown.
5. Press **Run ▶** with the `app` configuration. The app installs and launches.

The app icon is a music note with a question mark on a blue background.

### Command line instead

If you already have a JDK 17 and the Android SDK, with `sdk.dir` in `local.properties`:

```bash
./gradlew :app:installDebug
```

## Troubleshooting

| Symptom | Fix |
|---|---|
| No **Create app** button on the Spotify dashboard | Make sure you're signed in with a Premium account. Spotify has also paused new app creation for weeks at a time; if the page says new integrations are on hold, wait or reuse an existing app's client ID with `headsup://callback` added as a redirect URI. |
| App shows a "not configured" message at launch | `SPOTIFY_CLIENT_ID` wasn't found. Check `local.properties` and rebuild. |
| "No Spotify device found" when a round starts | Spotify only registers as a Connect device after it has played recently. Open Spotify, play any song for a second, and come back. |
| "Playback control needs Spotify Premium" | The signed-in account isn't Premium. |
| A playlist errors with HTTP 403 | You don't own it. Copy its songs into your own playlist. |
| Gestures fire twice or not at all | Thresholds live in `TiltGestureFilter`. Tilt firmly and return the phone to your forehead between gestures. |

## Development

Single-module Compose app, MVVM, no DI framework.

| Piece | Where | What it does |
|---|---|---|
| PKCE OAuth | `auth/` | Custom Tab sign-in to Spotify, token exchange/refresh, DataStore persistence |
| Web API client | `network/`, `model/` | Retrofit + kotlinx.serialization: profile, playlists, playlist items, Connect playback. Debug builds log requests to logcat (tags `okhttp.OkHttpClient`, `SpotifyApi`). |
| No-repeat shuffle | `game/ShuffleBag.kt`, `ShuffleBagStore.kt` | The persistent shuffle bag |
| Tilt gestures | `game/TiltGestureFilter.kt`, `TiltDetector.kt` | Pure gesture state machine plus the gravity-sensor wrapper |
| Sound cues | `game/GestureSounds.kt` | Synthesized chimes played as game audio without taking audio focus |
| Chorus finder | `player/ChorusLocator.kt`, `ChorusFinder.kt` | Section picking logic plus analysis fetch, cache, and prefetch |
| Playback | `player/SpotifyPlayer.kt` | Plays each drawn track on the phone's Spotify app, pauses at round end |
| UI | `ui/` | Login → playlist picker → game (setup / countdown / play / results) |

### Tests

Unit tests cover the shuffle bag, chorus selection, tilt gesture filter, playlist and track parsing, and API error messages:

```bash
./gradlew :app:testDebugUnitTest
```

In Android Studio, right-click `app/src/test` and choose **Run tests**.

See `HANDOFF.md` for a developer-oriented status summary and the reasoning behind the main design decisions.
