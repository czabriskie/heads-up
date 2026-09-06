# Project Handoff — Heads Up: Music

_Last updated: 2026-09-06 (landscape layout fix; not yet device-tested)_

## What this is

An Android party game (Kotlin, Jetpack Compose, single module) that plays Heads Up with songs instead of words. The guesser holds the phone on their forehead in landscape; a song from one of their own Spotify playlists plays out loud through the Spotify app; everyone else sees the title/artist on screen; the guesser names the song. Tilt down = correct, tilt up = pass. Rounds are 60/90/120s with a results scoreboard.

Two signature features:

1. **True no-repeat shuffle** — each playlist is a persistent "shuffle bag": every song is drawn exactly once, in random order, before any repeat. State survives app restarts (DataStore, per playlist). Playlist edits are merged into the current cycle. When the bag refills, the new cycle never starts with the song just played (last draw is persisted too). Bags are per playlist by design; the same song can appear from two playlists that share it. Manual "Reset shuffle" available.
2. **Optional playback** — a "Play songs through Spotify" switch on the setup screen (default on). Off = title/artist only, no Spotify calls during the round, so friends hum or describe the song instead; the chorus switch is disabled while it's off.
3. **Start at the chorus** — songs start at their most recognizable part: the loudest audio-analysis section in the 15–65% window of the track, falling back to 30%-in when analysis is unavailable. Positions are cached and prefetched one track ahead. Toggleable on the setup screen (default on).

## Current status

| Item | Status |
|---|---|
| Full app code (auth → playlists → game → results) | ✅ Written |
| Compiles / debug APK assembles | ✅ Verified (Gradle 8.9, AGP 8.5.2, Kotlin 2.0.20, JDK 17+) |
| Unit tests (shuffle bag, chorus locator, tilt filter, models, API errors; 32 tests) | ✅ Passing (`./gradlew :app:testDebugUnitTest`) |
| Run on a real device | ✅ Galaxy S23 (SM-S911U): sign-in, playlist load, Spotify Connect playback, round loop all verified |
| Spotify client ID | ✅ Configured locally in `local.properties` (never committed) |
| Landscape setup/results layouts | ⚠️ Compiles and tests pass; never seen on a screen — see below |

## Open thread: landscape setup and results (branch `claude/landscape-mode-song-list-aja5q3`)

Reported from real play: after a round, the results screen showed no song list and the "Play again"
button was unreachable until the phone was rotated to portrait.

Cause: a round locks the phone to `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`, and the lock is released
while the phone is still physically sideways, so the results screen renders in landscape.
`ResultsContent` was a single `Column` — title, score, `LazyColumn(weight(1f))`, two buttons. The
unweighted children alone are taller than a landscape phone, so the weighted list was measured at
0dp and the buttons fell off the bottom. `ReadyContent` had the same shape and the same problem, hit
right after tapping "Play again".

Fix (commit `c4696e3`, `ui/GameScreen.kt`): both screens branch on
`LocalConfiguration.current.orientation`. Portrait keeps the single column (setup now scrolls, so
large font settings don't push the Start button off either); landscape splits into two panes —
score and buttons beside the scrolling song list on results, round-length and toggles beside the
instructions and Start button on setup. The shared pieces are factored into `ReadyHeader` /
`ReadyOptions` / `ReadyInstructions` / `ReadyActions` and `ResultsHeader` / `ResultsList` /
`ResultsActions` so both layouts stay in sync.

Verified only by build: `:app:compileDebugKotlin`, `:app:testDebugUnitTest` (32 tests), and
`:app:assembleDebug` all pass. That work happened in a cloud container with no device and no
emulator, so **nobody has looked at these layouts**. Worth checking on the S23:

1. Finish a round holding the phone sideways — the score and both buttons should sit to the left of
   a scrollable song list, with nothing clipped.
2. Scroll the song list in landscape when a round has more entries than fit.
3. Tap "Play again" from there — the setup screen's two panes should both be fully reachable.
4. Rotate to portrait on each screen; the old single-column layouts should be unchanged.
5. Check with the system font size cranked up, which is what the new portrait scrolling is for.

## To get it running

1. Create an app at the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard); set Redirect URI to exactly `headsup://callback`; select Web API.
2. Put the Client ID in `local.properties` at the repo root: `SPOTIFY_CLIENT_ID=...` (a Gradle property or env var of the same name also works — see `app/build.gradle.kts`). No client secret is used anywhere (PKCE flow).
3. `./gradlew :app:installDebug` with a phone attached, or run from Android Studio.
4. On the phone: Spotify app installed, logged in to **Premium** (Web API playback control requires it). If Spotify hasn't played recently it won't show up as a Connect device — play any song for a second first; the game shows a hint when it can't find a device.

## Architecture (all under `app/src/main/java/com/headsup/game/`)

- `MainActivity.kt` — entry point; also receives the `headsup://callback` OAuth redirect (intent filter in the manifest, `singleTask`).
- `AppContainer.kt` — hand-rolled DI singleton; no framework.
- `auth/` — PKCE OAuth: `Pkce.kt` (verifier/challenge), `SpotifyAuthManager.kt` (authorize URL, code exchange, token refresh with mutex, error surface), `TokenStore.kt` (DataStore; also persists the in-flight verifier/state so process death during the browser round-trip doesn't break sign-in).
- `network/` — Retrofit + kotlinx.serialization. `SpotifyApi.kt` (playlists, tracks, devices, play/pause, audio-analysis), `SpotifyApiFactory.kt` (OkHttp interceptor injects a valid token via `getValidAccessToken()`).
- `model/Models.kt` — API DTOs.
- `game/` — `ShuffleBag.kt` (pure no-repeat logic + playlist-diff merging; unit-tested), `ShuffleBagStore.kt` (persistence), `TiltGestureFilter.kt` (pure gesture state machine; unit-tested) + `TiltDetector.kt` (sensor wrapper).
- `player/` — `SpotifyPlayer.kt` (Spotify Connect play/pause; resolves a device on 404 and retries once; maps 403 → "needs Premium"), `ChorusLocator.kt` (pure chorus-picking logic; unit-tested), `ChorusFinder.kt` (analysis fetch + DataStore/memory cache + prefetch).
- `ui/` — `HeadsUpApp.kt` (auth-gated NavHost), `LoginScreen`, `PlaylistScreen`/`PlaylistViewModel`, `GameScreen` (setup/countdown/playing/results phases, landscape lock + keep-screen-on during play; the setup and results screens have two-pane landscape layouts, since the phone is usually still sideways when a round ends), `GameViewModel` (round timer, scoring, bag draws, playback, chorus prefetch).

## Key decisions & constraints

- **Web API + Spotify Connect, not the App Remote SDK.** Keeps the repo free of Spotify's binary AAR (not on Maven Central) and the auth simple. Cost: requires Premium and an awake Spotify app as the Connect device. If playback proves flaky in device testing, the fallback plan is Spotify's App Remote SDK (vendored AAR).
- **Playlist items endpoint (2026 API changes).** `/v1/playlists/{id}/tracks` now returns 403; the app uses `/v1/playlists/{id}/items` (entries under `item`, `is_local` on the wrapper, `limit` max 50). Spotify only serves items for playlists the user owns or collaborates on, so the picker fetches `/v1/me` and hides playlists that aren't owned by the user or collaborative, with a footnote saying how many were hidden. If one still 403s, the error message explains why.
- **Debug HTTP logging.** Debug builds log request lines and non-2xx bodies under logcat tags `okhttp.OkHttpClient` and `SpotifyApi` (no headers, so no bearer token). Release builds log nothing.
- **Audio-analysis deprecation.** Spotify returns 403 on `/v1/audio-analysis` for apps created after Nov 2024. `ChorusFinder` treats any failure as "use the 30% heuristic" and only persists analysis-derived positions, so transient failures don't stick. Expect the heuristic path on a fresh client ID.
- **Tilt thresholds** (`TiltDetector`: trigger |z| > 7, re-arm |z| < 4, low-pass α = 0.35) worked in a first S23 round but haven't been tuned. The filter is seeded from the first sensor sample; starting it at 0 caused a spurious gesture at round start.
- **Sounds** (`game/GameSounds.kt`) are synthesized via `AudioTrack` as `USAGE_GAME` without audio focus, so they layer over Spotify instead of pausing it. Tick on each countdown second, C-major fanfare on start, rising A5→E6 = correct, falling G4→C4 = pass, three-note descending buzzer (with overtones) when the round ends. Triggered from `GameScreen` on state transitions.
- **App icon** is a hand-drawn vector adaptive icon (`res/drawable/ic_launcher_foreground.xml`: white eighth note + green question mark on GameBlue). No raster mipmaps; minSdk 26 so adaptive-only is fine.
- Debug-only build config; no minification, signing, or CI set up.

## Sensible next steps

1. Eyeball the landscape setup/results panes on a device (see the open thread above) — that branch is unmerged and unverified visually.
2. More device time: tilt feel across several rounds, the no-device hint flow, and behaviour when Spotify is backgrounded.
3. Tune tilt thresholds / flash duration (600ms) from real play.
4. Maybe: team scores across rounds, haptics on gesture, countdown beeps, a sound on/off switch, a last-5-seconds tick, a "song was already guessed this round" guard if rounds outlast playlists.
5. CI (GitHub Actions: `./gradlew testDebugUnitTest assembleDebug`) and a release signing config if this goes beyond personal use.

## Repo state

- Development happened on `claude/spotify-shuffle-game-kotlin-pv6a02`; it lands on `main` via PR.
- The landscape fix sits on `claude/landscape-mode-song-list-aja5q3` (no PR opened yet).
- Building here needs the Android SDK (`compileSdk 35`); a cloud session has to install one itself (`sdkmanager "platforms;android-35" "build-tools;35.0.0"` into a scratch dir, then `ANDROID_HOME=...`). Locally, Android Studio's SDK or a `sdk.dir` in `local.properties` covers it.
- `local.properties` is gitignored — the client ID never gets committed.
- `README.md` covers features, how to play, and Android Studio install; this file is the developer handoff.
