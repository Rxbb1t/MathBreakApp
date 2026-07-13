package com.ak.momapp.ui.problem

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * A soft two-note "ding" for correct answers, synthesized on the fly so
 * the app ships no audio asset. Silent and vibrate modes always win;
 * the chime only plays when the ringer is on.
 *
 * The rendered chime is written once to a small WAV in the cache dir and
 * loaded into a single shared SoundPool, which is built for rapid
 * retriggering. A fresh AudioTrack per play (the old approach) stuttered
 * when answers came in quick succession.
 */
object SuccessChime {

    private const val SAMPLE_RATE = 22_050
    private const val AMPLITUDE = 0.22

    // Bump when the rendered sound changes so a stale cached WAV is ignored.
    private const val CACHE_FILE = "success_chime_v1.wav"

    // E5 then A5: a gentle upward "ta-ding".
    private val NOTES = listOf(659.25 to 0.11, 880.0 to 0.30)

    private val lock = Any()
    private var soundPool: SoundPool? = null
    private var soundId = 0
    private var loaded = false
    private var playWhenLoaded = false

    /** Start rendering and loading the chime so the first play is instant. */
    fun preload(context: Context) {
        synchronized(lock) {
            if (soundPool == null) load(context.applicationContext)
        }
    }

    fun play(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        synchronized(lock) {
            if (soundPool == null) load(context.applicationContext)
            if (loaded) {
                soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
            } else {
                // Still loading: play as soon as the sample lands.
                playWhenLoaded = true
            }
        }
    }

    // Must be called with the lock held.
    private fun load(context: Context) {
        val pool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        pool.setOnLoadCompleteListener { loadedPool, id, status ->
            synchronized(lock) {
                if (status == 0) {
                    loaded = true
                    if (playWhenLoaded) loadedPool.play(id, 1f, 1f, 1, 0, 1f)
                }
                playWhenLoaded = false
            }
        }
        soundPool = pool
        // Rendering and file I/O stay off the main thread.
        thread(name = "success-chime-load") {
            try {
                val file = File(context.cacheDir, CACHE_FILE)
                if (!file.exists()) writeWav(file, render())
                val id = pool.load(file.path, 1)
                synchronized(lock) { soundId = id }
            } catch (_: Exception) {
                // A failed chime must never take the app down.
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

    /** Minimal 16-bit mono PCM WAV container around the rendered samples. */
    private fun writeWav(file: File, samples: ShortArray) {
        val dataSize = samples.size * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)                      // fmt chunk size
        buffer.putShort(1)                     // PCM
        buffer.putShort(1)                     // mono
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(SAMPLE_RATE * 2)         // byte rate
        buffer.putShort(2)                     // block align
        buffer.putShort(16)                    // bits per sample
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)
        for (sample in samples) buffer.putShort(sample)
        file.writeBytes(buffer.array())
    }
}
