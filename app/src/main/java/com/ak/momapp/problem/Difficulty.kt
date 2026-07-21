package com.ak.momapp.problem

/**
 * The three names she sees: Easy, Normal, Hard.
 *
 * This is a BAND over [Level]'s 0–100 scale, not the scale itself. It is
 * what the settings picker offers, what the Exercises rows show, and what
 * decides a problem's extra thinking time. What it no longer does is set
 * how big the numbers in a problem are. That follows the points
 * underneath, so the difficulty can move without the label having to.
 */
enum class Difficulty {
    EASY, MEDIUM, HARD;

    fun harder(): Difficulty = entries.getOrElse(ordinal + 1) { this }

    fun easier(): Difficulty = entries.getOrElse(ordinal - 1) { this }
}
