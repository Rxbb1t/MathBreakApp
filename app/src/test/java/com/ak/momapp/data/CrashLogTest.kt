package com.ak.momapp.data

import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The report is the entire bug-reporting pipeline for an app that sends
 * nothing anywhere, so what it contains is the whole question. Everything
 * missing from it is something nobody will ever be able to ask about.
 *
 * Only the text is covered here. That the handler chains to the one before
 * it, which is what keeps a crash from leaving a live process with a dead
 * screen, needs a real device and is checked there.
 */
class CrashLogTest {

    private fun report(error: Throwable) =
        CrashLog.report("Brain Break 1.6 (11)", "main", error, Instant.parse("2026-07-27T09:15:00Z"))

    @Test
    fun `the report names the failure and where it happened`() {
        val text = report(IllegalStateException("level went negative"))
        assertTrue("no exception type in:\n$text", "IllegalStateException" in text)
        assertTrue("no message in:\n$text", "level went negative" in text)
        assertTrue("no stack frame in:\n$text", "CrashLogTest" in text)
    }

    @Test
    fun `the report carries the version, device and thread`() {
        val text = report(RuntimeException("boom"))
        assertTrue("no version in:\n$text", "Brain Break 1.6 (11)" in text)
        assertTrue("no thread in:\n$text", "Thread: main" in text)
        assertTrue("no timestamp in:\n$text", "2026-07-27" in text)
    }

    /**
     * The cause is usually the interesting half. A wrapped exception that
     * printed only its wrapper would name the symptom and hide the fault.
     */
    @Test
    fun `a wrapped failure keeps its cause`() {
        val text = report(RuntimeException("outer", IllegalArgumentException("the real fault")))
        assertTrue("no cause in:\n$text", "the real fault" in text)
        assertTrue("cause not marked as one in:\n$text", "Caused by" in text)
    }

    @Test
    fun `an exception with no message still reports its type`() {
        val text = report(NullPointerException())
        assertTrue("no type in:\n$text", "NullPointerException" in text)
    }
}
