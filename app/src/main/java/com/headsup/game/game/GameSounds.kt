package com.headsup.game.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Short synthesized cues for the round. Generated in code so the app ships no
 * audio assets, and played as game audio without requesting focus so Spotify
 * keeps playing underneath.
 *
 *  - [playTick]     countdown "3, 2, 1"
 *  - [playStart]    rising four-note fanfare when the round begins
 *  - [playCorrect]  rising two-note chime on tilt down
 *  - [playPass]     falling two-note tone on tilt up
 *  - [playTimeUp]   three-note descending buzzer when the round ends
 */
class GameSounds {

    private val tick = buildTrack(listOf(Note(1000f, 90)))
    private val start = buildTrack(listOf(Note(523f, 90), Note(659f, 90), Note(784f, 90), Note(1047f, 260)))
    private val correct = buildTrack(listOf(Note(880f, 90), Note(1318f, 160)))
    private val pass = buildTrack(listOf(Note(392f, 110), Note(261f, 200)))
    private val timeUp = buildTrack(
        listOf(Note(220f, 220), Note(196f, 220), Note(165f, 520)),
        harmonics = BUZZER_HARMONICS,
    )

    fun playTick() = replay(tick)
    fun playStart() = replay(start)
    fun playCorrect() = replay(correct)
    fun playPass() = replay(pass)
    fun playTimeUp() = replay(timeUp)

    fun release() {
        listOf(tick, start, correct, pass, timeUp).forEach { it.release() }
    }

    private fun replay(track: AudioTrack) {
        runCatching {
            track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    private data class Note(val hz: Float, val ms: Int)

    /**
     * @param harmonics relative amplitudes of the 1st, 2nd, 3rd... partials.
     * A single 1f is a pure sine; adding overtones gives a reedier buzz.
     */
    private fun buildTrack(notes: List<Note>, harmonics: List<Float> = listOf(1f)): AudioTrack {
        val totalFrames = notes.sumOf { it.ms * SAMPLE_RATE / 1000 }
        val pcm = ShortArray(totalFrames)
        val gain = AMPLITUDE / harmonics.sum()
        var offset = 0
        for (note in notes) {
            val frames = note.ms * SAMPLE_RATE / 1000
            val fade = min(frames / 4, SAMPLE_RATE / 100) // <=10 ms attack/decay to avoid clicks
            for (i in 0 until frames) {
                val envelope = when {
                    i < fade -> i.toFloat() / fade
                    i > frames - fade -> (frames - i).toFloat() / fade
                    else -> 1f
                }
                var sample = 0.0
                harmonics.forEachIndexed { k, amp ->
                    sample += amp * sin(2.0 * PI * note.hz * (k + 1) * i / SAMPLE_RATE)
                }
                pcm[offset + i] = (sample * envelope * gain * Short.MAX_VALUE).toInt().toShort()
            }
            offset += frames
        }
        val bytes = pcm.size * 2
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(bytes)
            .build()
            .also { it.write(pcm, 0, pcm.size) }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val AMPLITUDE = 0.6f
        val BUZZER_HARMONICS = listOf(1f, 0.6f, 0.4f, 0.3f, 0.2f)
    }
}
