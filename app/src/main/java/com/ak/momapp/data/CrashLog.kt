package com.ak.momapp.data

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The last crash, written to a file so it survives the process dying.
 *
 * The app sends nothing anywhere and is not about to start, so a crash on
 * someone else's phone is invisible unless they can hand it over
 * themselves. This keeps the most recent one on disk and lets the Settings
 * screen show it with a Copy button, which is the whole reporting pipeline:
 * she copies, pastes it into a message, and that is a bug report.
 *
 * Deliberately plain files rather than DataStore. This runs while the app
 * is already falling over, so it wants the shortest path to disk that
 * exists, with nothing asynchronous and no coroutines to schedule.
 */
object CrashLog {

    private const val FILE_NAME = "last_error.txt"

    private val stamp: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

    /**
     * Records uncaught exceptions from every thread, then hands the crash
     * on to whoever was handling it before.
     *
     * CHAINING IS NOT OPTIONAL. Swallowing the exception here would leave
     * the process alive with a half-dead UI and no crash dialog, which is
     * a worse experience than the crash and a much harder one to explain.
     * Writing the report is wrapped so that a failure to save cannot
     * replace the original error with a confusing one about saving it.
     */
    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(appContext, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    /** The stored report, or null when nothing has gone wrong yet. */
    fun read(context: Context): String? =
        file(context).takeIf { it.exists() }?.runCatching { readText() }?.getOrNull()?.ifBlank { null }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    /**
     * What she would be pasting into a report.
     *
     * The device and version lines matter as much as the stack trace: "it
     * crashes" is unactionable, and the two things always asked back are
     * which version and which phone.
     */
    fun describeDevice(context: Context): String {
        val version = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
        }.getOrDefault("unknown")
        return buildString {
            appendLine("Brain Break $version")
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            append("${Build.MANUFACTURER} ${Build.MODEL}")
        }
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        file(context).writeText(
            report(describeDevice(context), thread.name, error, Instant.now()),
        )
    }

    /**
     * The report text, kept free of Android types so its shape can be
     * tested. What matters is that the trace survives intact: a report
     * that lost the exception type or its cause would look fine on screen
     * and be worthless to whoever received it.
     */
    fun report(header: String, threadName: String, error: Throwable, now: Instant): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }
        return buildString {
            appendLine(stamp.format(now))
            appendLine(header)
            appendLine("Thread: $threadName")
            appendLine()
            append(trace.toString().trim())
        }
    }

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)
}
