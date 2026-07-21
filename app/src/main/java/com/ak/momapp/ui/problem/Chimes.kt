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

/** The app's little sounds, all riding the one "Sound" setting. */
enum class ChimeSound {
    /** Correct answer: a soft rising C-E-G major triad, cheery but low-key. */
    SUCCESS,

    /** Wrong answer: one soft low "boop", warm rather than punishing. */
    TRY_AGAIN,

    /** Daily challenge finished: a short sparkly upward arpeggio. */
    FANFARE,
}

/**
 * Soft feedback sounds, synthesized on the fly so the app ships no audio
 * assets. Silent and vibrate modes always win; nothing plays unless the
 * ringer is on.
 *
 * Each sound is rendered once to a small WAV in the cache dir and loaded
 * into a single shared SoundPool, which is built for rapid retriggering.
 * A fresh AudioTrack per play (the original approach) stuttered when
 * answers came in quick succession.
 */
object Chimes {

    private const val SAMPLE_RATE = 22_050

    private class Recipe(
        /** Bump the version suffix when a sound changes, or the cached WAV wins. */
        val cacheName: String,
        /** Frequency in Hz to start time in seconds. */
        val notes: List<Pair<Double, Double>>,
        val amplitude: Double,
        val decay: Double,
        val tailSeconds: Double,
        /** Fade-in of each note, in seconds. Longer is softer, less "in your face". */
        val attack: Double = 0.012,
    )

    private val RECIPES = mapOf(
        ChimeSound.SUCCESS to Recipe(
            cacheName = "chime_success_v2.wav",
            notes = listOf(523.25 to 0.0, 659.25 to 0.10, 783.99 to 0.20),
            amplitude = 0.15,
            decay = 5.0,
            tailSeconds = 0.5,
            attack = 0.035,
        ),
        ChimeSound.TRY_AGAIN to Recipe(
            cacheName = "chime_try_again_v2.wav",
            notes = listOf(392.0 to 0.0),
            amplitude = 0.13,
            decay = 10.0,
            tailSeconds = 0.35,
        ),
        ChimeSound.FANFARE to Recipe(
            cacheName = "chime_fanfare_v2.wav",
            notes = listOf(659.25 to 0.0, 880.0 to 0.13, 1108.73 to 0.26, 1318.51 to 0.39),
            amplitude = 0.20,
            decay = 6.0,
            tailSeconds = 0.55,
        ),
    )

    private val lock = Any()
    private var soundPool: SoundPool? = null
    private val sampleIds = mutableMapOf<ChimeSound, Int>()
    private val loadedIds = mutableSetOf<Int>()
    private val pendingPlays = mutableSetOf<ChimeSound>()

    /** Start rendering and loading the sounds so the first play is instant. */
    fun preload(context: Context) {
        synchronized(lock) {
            if (soundPool == null) load(context.applicationContext)
        }
    }

    fun play(context: Context, sound: ChimeSound) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        synchronized(lock) {
            if (soundPool == null) load(context.applicationContext)
            val id = sampleIds[sound]
            if (id != null && id in loadedIds) {
                soundPool?.play(id, 1f, 1f, 1, 0, 1f)
            } else {
                // Still loading: play as soon as the sample lands.
                pendingPlays += sound
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
                if (status != 0) return@synchronized
                loadedIds += id
                val sound = sampleIds.entries.firstOrNull { it.value == id }?.key
                if (sound != null && pendingPlays.remove(sound)) {
                    loadedPool.play(id, 1f, 1f, 1, 0, 1f)
                }
            }
        }
        soundPool = pool
        // Rendering and file I/O stay off the main thread.
        thread(name = "chime-load") {
            for ((sound, recipe) in RECIPES) {
                try {
                    val file = File(context.cacheDir, recipe.cacheName)
                    if (!file.exists()) writeWav(file, render(recipe))
                    val id = pool.load(file.path, 1)
                    synchronized(lock) { sampleIds[sound] = id }
                } catch (_: Exception) {
                    // A failed chime must never take the app down.
                }
            }
        }
    }

    /** Overlapping sine notes with a quick attack and a long decay. */
    private fun render(recipe: Recipe): ShortArray {
        val totalSeconds = recipe.notes.maxOf { (_, start) -> start } + recipe.tailSeconds
        val samples = DoubleArray((totalSeconds * SAMPLE_RATE).toInt())
        for ((frequency, startSeconds) in recipe.notes) {
            val start = (startSeconds * SAMPLE_RATE).toInt()
            for (i in start until samples.size) {
                val t = (i - start).toDouble() / SAMPLE_RATE
                val attack = min(1.0, t / recipe.attack)
                val decay = Math.exp(-t * recipe.decay)
                samples[i] += sin(2 * PI * frequency * t) * attack * decay
            }
        }
        // A short fade at the very end so the buffer never stops mid-swing,
        // which would be heard as a click.
        val fade = min((0.02 * SAMPLE_RATE).toInt(), samples.size)
        for (i in 0 until fade) {
            samples[samples.size - 1 - i] *= i.toDouble() / fade
        }
        return ShortArray(samples.size) { i ->
            // Volume is applied first, then we clip -- so overlapping notes
            // (a chord) stay a clean summed wave instead of a flat-topped,
            // buzzy one. With these amplitudes the clip effectively never
            // triggers; it's only a last-resort guard.
            ((samples[i] * recipe.amplitude).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
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
