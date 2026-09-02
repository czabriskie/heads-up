# Project Handoff — Heads Up: Music

_Last updated: 2026-09-02 (device-tested)_

## What this is

An Android party game (Kotlin, Jetpack Compose, single module) that plays Heads Up with songs instead of words. The guesser holds the phone on their forehead in landscape; a song from one of their own Spotify playlists plays out loud through the Spotify app; everyone else sees the title/artist on screen; the guesser names the song. Tilt down = correct, tilt up = pass. Rounds are 60/90/120s with a results scoreboard.

Two signature features:

1. **True no-repeat shuffle** — each playlist is a persistent "shuffle bag": every song is drawn exactly once, in random order, before any repeat. State survives app restarts (DataStore, per playlist). Playlist edits are merged into the current cycle. Manual "Reset shuffle" available.
2. **Optional playback** — a "Play songs through Spotify" switch on the setup screen (default on). Off = title/artist only, no Spotify calls during the round, so friends hum or describe the song instead; the chorus switch is disabled while it's off.
3. **Start at the chorus** — songs start at their most recognizable part: the loudest audio-analysis section in the 15–65% window of the track, falling back to 30%-in when analysis is unavailable. Positions are cached and prefetched one track ahead. Toggleable on the setup screen (default on).

## Current status

| Item | Status |
|---|---|
| Full app code (auth → playlists → game → results) | ✅ Written |
| Compiles / debug APK assembles | ✅ Verified (Gradle 8.9, AGP 8.5.2, Kotlin 2.0.20, JDK 17+) |
| Unit tests (`ShuffleBagTest`, `ChorusLocatorTest`, 14 tests) | ✅ Passing (`./gradlew :app:testDebugUnitTest`) |
| Run on a real device | ✅ Galaxy S23 (SM-S911U): sign-in, playlist load, Spotify Connect playback, round loop all verified |
| Spotify client ID | ✅ Configured locally in `local.properties` (never committed) |

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
- `game/` — `ShuffleBag.kt` (pure no-repeat logic + playlist-diff merging; unit-tested), `ShuffleBagStore.kt` (persistence), `TiltDetector.kt` (gravity-sensor state machine; must pass through a neutral zone between gestures).
- `player/` — `SpotifyPlayer.kt` (Spotify Connect play/pause; resolves a device on 404 and retries once; maps 403 → "needs Premium"), `ChorusLocator.kt` (pure chorus-picking logic; unit-tested), `ChorusFinder.kt` (analysis fetch + DataStore/memory cache + prefetch).
- `ui/` — `HeadsUpApp.kt` (auth-gated NavHost), `LoginScreen`, `PlaylistScreen`/`PlaylistViewModel`, `GameScreen` (setup/countdown/playing/results phases, landscape lock + keep-screen-on during play), `GameViewModel` (round timer, scoring, bag draws, playback, chorus prefetch).

## Key decisions & constraints

- **Web API + Spotify Connect, not the App Remote SDK.** Keeps the repo free of Spotify's binary AAR (not on Maven Central) and the auth simple. Cost: requires Premium and an awake Spotify app as the Connect device. If playback proves flaky in device testing, the fallback plan is Spotify's App Remote SDK (vendored AAR).
- **Playlist items endpoint (2026 API changes).** `/v1/playlists/{id}/tracks` now returns 403; the app uses `/v1/playlists/{id}/items` (entries under `item`, `is_local` on the wrapper, `limit` max 50). Spotify only serves items for playlists the user owns or collaborates on, so the picker fetches `/v1/me` and hides playlists that aren't owned by the user or collaborative, with a footnote saying how many were hidden. If one still 403s, the error message explains why.
- **Debug HTTP logging.** Debug builds log request lines and non-2xx bodies under logcat tags `okhttp.OkHttpClient` and `SpotifyApi` (no headers, so no bearer token). Release builds log nothing.
- **Audio-analysis deprecation.** Spotify returns 403 on `/v1/audio-analysis` for apps created after Nov 2024. `ChorusFinder` treats any failure as "use the 30% heuristic" and only persists analysis-derived positions, so transient failures don't stick. Expect the heuristic path on a fresh client ID.
- **Tilt thresholds** (`TiltDetector`: trigger |z| > 7, re-arm |z| < 4, low-pass α = 0.35) worked in a first S23 round but haven't been tuned. The filter is seeded from the first sensor sample; starting it at 0 caused a spurious gesture at round start.
- **Gesture sounds** (`game/GestureSounds.kt`) are synthesized sine chimes played via `AudioTrack` as `USAGE_GAME` without audio focus, so they layer over Spotify instead of pausing it. Rising A5→E6 = correct, falling G4→C4 = pass.
- **App icon** is a hand-drawn vector adaptive icon (`res/drawable/ic_launcher_foreground.xml`: white eighth note + green question mark on GameBlue). No raster mipmaps; minSdk 26 so adaptive-only is fine.
- Debug-only build config; no minification, signing, or CI set up.

## Sensible next steps

1. More device time: tilt feel across several rounds, the no-device hint flow, and behaviour when Spotify is backgrounded.
2. Tune tilt thresholds / flash duration (600ms) from real play.
3. Maybe: team scores across rounds, haptics on gesture, countdown beeps, a sound on/off switch, a "song was already guessed this round" guard if rounds outlast playlists.
4. CI (GitHub Actions: `./gradlew testDebugUnitTest assembleDebug`) and a release signing config if this goes beyond personal use.

## Repo state

- Development happened on `claude/spotify-shuffle-game-kotlin-pv6a02`; it lands on `main` via PR.
- `local.properties` is gitignored — the client ID never gets committed.
- `README.md` covers player-facing setup; this file is the developer handoff.
