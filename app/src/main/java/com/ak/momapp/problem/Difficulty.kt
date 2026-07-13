package com.ak.momapp.problem

enum class Difficulty {
    EASY, MEDIUM, HARD;

    fun harder(): Difficulty = entries.getOrElse(ordinal + 1) { this }

    fun easier(): Difficulty = entries.getOrElse(ordinal - 1) { this }
}

/**
 * Adaptive difficulty: 3 correct answers in a row bump the level up,
 * 5 misses (wrong problems or skips) in a row bump it down. Clamped at
 * EASY and [maxLevel]. Each answer breaks the other kind's streak, so
 * mixed results keep the level put.
 */
class DifficultyTracker(
    start: Difficulty,
    correctInARow: Int = 0,
    missesInARow: Int = 0,
    /** The Relaxed preset stops the climb at MEDIUM; everyone else at HARD. */
    private val maxLevel: Difficulty = Difficulty.HARD,
) {

    var current: Difficulty = minOf(start, maxLevel)
        private set

    var correctInARow: Int = correctInARow
        private set

    var missesInARow: Int = missesInARow
        private set

    fun recordCorrect() {
        missesInARow = 0
        correctInARow++
        if (correctInARow >= CORRECT_STREAK_TO_LEVEL_UP) {
            current = minOf(current.harder(), maxLevel)
            correctInARow = 0
        }
    }

    fun recordIncorrect() {
        correctInARow = 0
        missesInARow++
        if (missesInARow >= MISS_STREAK_TO_LEVEL_DOWN) {
            current = current.easier()
            missesInARow = 0
        }
    }

    companion object {
        const val CORRECT_STREAK_TO_LEVEL_UP = 3
        const val MISS_STREAK_TO_LEVEL_DOWN = 5
    }
}
