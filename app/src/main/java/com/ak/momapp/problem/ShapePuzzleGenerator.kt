package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * Equation puzzles where shapes stand in for numbers:
 *
 *     🍎 + 🍎 = 10
 *     🍎 + 🍐 = 12
 *     🍐 × 🍎 = ?
 *
 * Built constructively. Values are picked first, then each given line
 * reveals exactly one new shape, so every puzzle is solvable top to
 * bottom. EASY uses 2 shapes, MEDIUM and HARD use 3 with bigger values
 * and multiplication in the given lines.
 */
class ShapePuzzleGenerator(private val random: Random) {

    fun generate(difficulty: Difficulty, language: AppLanguage): Problem {
        val shapes = SHAPES.shuffled(random)
        return when (difficulty) {
            Difficulty.EASY -> easyPuzzle(shapes, language)
            Difficulty.MEDIUM -> mediumPuzzle(shapes, language)
            Difficulty.HARD -> hardPuzzle(shapes, language)
        }
    }

    private fun easyPuzzle(shapes: List<String>, language: AppLanguage): Problem {
        val (shapeA, shapeB) = shapes
        val (a, b) = distinctValues(2, from = 2, until = 10)
        val lines = listOf(
            "$shapeA + $shapeA = ${2 * a}",
            "$shapeA + $shapeB = ${a + b}",
        )
        val finals = buildList {
            add("$shapeB + $shapeB = ?" to 2 * b)
            add("$shapeA × $shapeB = ?" to a * b)
            add("$shapeB + $shapeA + $shapeA = ?" to b + 2 * a)
            if (b > a) add("$shapeB − $shapeA = ?" to b - a)
        }
        return build(
            difficulty = Difficulty.EASY,
            lines = lines,
            finals = finals,
            values = listOf(shapeA to a, shapeB to b),
            language = language,
        )
    }

    private fun mediumPuzzle(shapes: List<String>, language: AppLanguage): Problem {
        val (shapeA, shapeB, shapeC) = shapes
        val (a, b, c) = distinctValues(3, from = 2, until = 13)
        val lines = listOf(
            "$shapeA + $shapeA = ${2 * a}",
            "$shapeA × $shapeB = ${a * b}",
            "$shapeB + $shapeC = ${b + c}",
        )
        val finals = buildList {
            add("$shapeA + $shapeB + $shapeC = ?" to a + b + c)
            add("$shapeA × $shapeC = ?" to a * c)
            add("$shapeA × $shapeC + $shapeB = ?" to a * c + b)
            if (c * b > a) add("$shapeC × $shapeB − $shapeA = ?" to c * b - a)
        }
        return build(
            difficulty = Difficulty.MEDIUM,
            lines = lines,
            finals = finals,
            values = listOf(shapeA to a, shapeB to b, shapeC to c),
            language = language,
        )
    }

    private fun hardPuzzle(shapes: List<String>, language: AppLanguage): Problem {
        val (shapeA, shapeB, shapeC) = shapes
        val (a, b, c) = distinctValues(3, from = 3, until = 16)
        val lines = listOf(
            "$shapeA + $shapeA + $shapeA = ${3 * a}",
            "$shapeA × $shapeB = ${a * b}",
            "$shapeB × $shapeC = ${b * c}",
        )
        val finals = buildList {
            add("$shapeA × $shapeC + $shapeB = ?" to a * c + b)
            if (a * b > c) add("$shapeA × $shapeB − $shapeC = ?" to a * b - c)
            if (c * a > b) add("$shapeC × $shapeA − $shapeB = ?" to c * a - b)
        }
        return build(
            difficulty = Difficulty.HARD,
            lines = lines,
            finals = finals,
            values = listOf(shapeA to a, shapeB to b, shapeC to c),
            language = language,
        )
    }

    private fun build(
        difficulty: Difficulty,
        lines: List<String>,
        finals: List<Pair<String, Int>>,
        values: List<Pair<String, Int>>,
        language: AppLanguage,
    ): Problem {
        val (finalLine, answer) = finals.random(random)
        val firstShape = values.first().first
        val firstLineHint = when (language) {
            AppLanguage.ENGLISH -> "Start at the top. The first line tells you $firstShape"
            AppLanguage.ROMANIAN -> "Începe de sus. Primul rând îți spune cât face $firstShape"
        }
        val valuesHint = values.joinToString("   ") { (shape, value) -> "$shape = $value" }
        val notes = if (difficulty == Difficulty.EASY) {
            emptyList()
        } else {
            when (language) {
                AppLanguage.ENGLISH -> listOf(
                    "The same shape stands for the same number everywhere in the puzzle.",
                    "Each given line reveals exactly one new shape. Carry what you know into the next line.",
                )
                AppLanguage.ROMANIAN -> listOf(
                    "Aceeași formă înseamnă același număr peste tot în joc.",
                    "Fiecare rând dat dezvăluie exact o formă nouă. Ia cu tine ce știi în rândul următor.",
                )
            }
        }
        return Problem(
            text = (lines + finalLine).joinToString("\n"),
            answer = answer,
            difficulty = difficulty,
            kind = ProblemKind.PUZZLE,
            hints = listOf(firstLineHint, valuesHint),
            notes = notes,
        )
    }

    /** Distinct values keep the deduction unambiguous and satisfying. */
    private fun distinctValues(count: Int, from: Int, until: Int): List<Int> =
        (from until until).shuffled(random).take(count)

    companion object {
        private val SHAPES = listOf("🍎", "🍐", "🍋", "🌸", "⭐", "🐟", "🍓", "🥕")
    }
}
