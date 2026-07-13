package com.ak.momapp.ui.problem

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * A soft two-note "ding" for correct answers, synthesized on the fly so
 * the app ships no audio asset. Silent and vibrate modes always win;
 * the chime only plays when the ringer is on.
 */
object SuccessChime {

    private const val SAMPLE_RATE = 22_050
    private const val AMPLITUDE = 0.22

    // E5 then A5: a gentle upward "ta-ding".
    private val NOTES = listOf(659.25 to 0.11, 880.0 to 0.30)

    // Rendered once, on first use.
    private val pcm: ShortArray by lazy { render() }

    fun play(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        // Short-lived static track per play; misses are rare and cheap.
        thread(name = "success-chime") {
            val samples = pcm
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            try {
                track.write(samples, 0, samples.size)
                track.play()
                // Let the tail ring out before releasing the track.
                Thread.sleep(samples.size * 1_000L / SAMPLE_RATE + 50)
            } catch (_: Exception) {
                // A failed chime must never take the app down.
            } finally {
                track.release()
            }
        }
    }

    /** Two overlapping sine notes with a quick attack and a long decay. */
    private fun render(): ShortArray {
        val totalSeconds = NOTES.maxOf { (_, start) -> start } + 0.45
        val samples = DoubleArray((totalSeconds * SAMPLE_RATE).toInt())
        for ((frequency, startSeconds) in NOTES) {
            val start = (startSeconds * SAMPLE_RATE).toInt()
            for (i in start until samples.size) {
                val t = (i - start).toDouble() / SAMPLE_RATE
                val attack = min(1.0, t / 0.012)
                val decay = Math.exp(-t * 7.0)
                samples[i] += sin(2 * PI * frequency * t) * attack * decay
            }
        }
        return ShortArray(samples.size) { i ->
            (samples[i].coerceIn(-1.0, 1.0) * AMPLITUDE * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
