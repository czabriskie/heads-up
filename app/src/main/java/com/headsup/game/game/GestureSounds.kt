package com.headsup.game.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Short synthesized cues for the two gestures: a rising two-note chime for
 * "correct" and a falling one for "pass". Generated in code so the app ships
 * no audio assets, and played as game audio without requesting focus so
 * Spotify keeps playing underneath.
 */
class GestureSounds {

    private val correct = buildTrack(listOf(Note(880f, 90), Note(1318f, 160)))   // A5 -> E6
    private val pass = buildTrack(listOf(Note(392f, 110), Note(261f, 200)))      // G4 -> C4

    fun playCorrect() = replay(correct)

    fun playPass() = replay(pass)

    fun release() {
        correct.release()
        pass.release()
    }

    private fun replay(track: AudioTrack) {
        runCatching {
            track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    private data class Note(val hz: Float, val ms: Int)

    private fun buildTrack(notes: List<Note>): AudioTrack {
        val totalFrames = notes.sumOf { it.ms * SAMPLE_RATE / 1000 }
        val pcm = ShortArray(totalFrames)
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
                val sample = sin(2.0 * PI * note.hz * i / SAMPLE_RATE).toFloat()
                pcm[offset + i] = (sample * envelope * AMPLITUDE * Short.MAX_VALUE).toInt().toShort()
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
    }
}
