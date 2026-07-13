package com.ak.momapp.problem

import org.junit.Assert.assertEquals
import org.junit.Test

class DifficultyTrackerTest {

    @Test
    fun `three correct in a row bump difficulty up`() {
        val tracker = DifficultyTracker(Difficulty.EASY)
        repeat(2) { tracker.recordCorrect() }
        assertEquals(Difficulty.EASY, tracker.current)
        tracker.recordCorrect()
        assertEquals(Difficulty.MEDIUM, tracker.current)
    }

    @Test
    fun `two correct then a miss does not bump up`() {
        val tracker = DifficultyTracker(Difficulty.EASY)
        repeat(2) { tracker.recordCorrect() }
        tracker.recordIncorrect()
        assertEquals(Difficulty.EASY, tracker.current)
        // The miss also reset the streak: two more correct still aren't enough.
        repeat(2) { tracker.recordCorrect() }
        assertEquals(Difficulty.EASY, tracker.current)
    }

    @Test
    fun `four misses keep the level, five in a row lower it`() {
        val tracker = DifficultyTracker(Difficulty.HARD)
        repeat(4) {
            tracker.recordIncorrect()
            assertEquals(Difficulty.HARD, tracker.current)
        }
        tracker.recordIncorrect()
        assertEquals(Difficulty.MEDIUM, tracker.current)
    }

    @Test
    fun `a correct answer breaks the miss streak`() {
        val tracker = DifficultyTracker(Difficulty.MEDIUM)
        repeat(4) { tracker.recordIncorrect() }
        tracker.recordCorrect()
        repeat(4) { tracker.recordIncorrect() }
        // Never five misses in a row, so the level never dropped.
        assertEquals(Difficulty.MEDIUM, tracker.current)
    }

    @Test
    fun `difficulty is clamped at both ends`() {
        val bottom = DifficultyTracker(Difficulty.EASY)
        repeat(30) { bottom.recordIncorrect() }
        assertEquals(Difficulty.EASY, bottom.current)

        val top = DifficultyTracker(Difficulty.HARD)
        repeat(9) { top.recordCorrect() }
        assertEquals(Difficulty.HARD, top.current)
    }

    @Test
    fun `three correct on medium reach hard`() {
        val tracker = DifficultyTracker(Difficulty.MEDIUM)
        repeat(3) { tracker.recordCorrect() }
        assertEquals(Difficulty.HARD, tracker.current)
    }

    @Test
    fun `tracker resumes from a persisted correct streak`() {
        val tracker = DifficultyTracker(Difficulty.EASY, correctInARow = 2)
        tracker.recordCorrect()
        assertEquals(Difficulty.MEDIUM, tracker.current)
        assertEquals(0, tracker.correctInARow)
    }

    @Test
    fun `tracker resumes from a persisted miss streak`() {
        val tracker = DifficultyTracker(Difficulty.HARD, missesInARow = 4)
        tracker.recordIncorrect()
        assertEquals(Difficulty.MEDIUM, tracker.current)
        assertEquals(0, tracker.missesInARow)
    }

    @Test
    fun `full climb from easy to hard takes six correct answers`() {
        val tracker = DifficultyTracker(Difficulty.EASY)
        repeat(5) { tracker.recordCorrect() }
        assertEquals(Difficulty.MEDIUM, tracker.current)
        tracker.recordCorrect()
        assertEquals(Difficulty.HARD, tracker.current)
    }

    @Test
    fun `full descent from hard to easy takes ten misses`() {
        val tracker = DifficultyTracker(Difficulty.HARD)
        repeat(9) { tracker.recordIncorrect() }
        assertEquals(Difficulty.MEDIUM, tracker.current)
        tracker.recordIncorrect()
        assertEquals(Difficulty.EASY, tracker.current)
    }

    @Test
    fun `a capped tracker never climbs past its max level`() {
        val tracker = DifficultyTracker(Difficulty.EASY, maxLevel = Difficulty.MEDIUM)
        repeat(3) { tracker.recordCorrect() }
        assertEquals(Difficulty.MEDIUM, tracker.current)
        repeat(9) { tracker.recordCorrect() }
        assertEquals(Difficulty.MEDIUM, tracker.current)
        // The streak still resets at the cap, it just has nowhere to go.
        assertEquals(0, tracker.correctInARow)
    }

    @Test
    fun `a start above the cap is pulled down to it`() {
        val tracker = DifficultyTracker(Difficulty.HARD, maxLevel = Difficulty.MEDIUM)
        assertEquals(Difficulty.MEDIUM, tracker.current)
    }
}
