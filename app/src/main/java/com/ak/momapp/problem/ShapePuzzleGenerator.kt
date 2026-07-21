package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import kotlin.random.nextInt

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

    /**
     * The third shape and the multiplication-led given lines arrive on
     * their own ramps rather than all at once on a band boundary, so the
     * puzzle grows a piece at a time.
     */
    fun generate(level: Level, language: AppLanguage): Problem {
        val shapes = SHAPES.shuffled(random)
        val thirdShape = random.nextDouble() < level.ramp(THIRD_SHAPE_FROM, THIRD_SHAPE_BY)
        return when {
            !thirdShape -> twoShapePuzzle(shapes, level, language)
            random.nextDouble() < level.ramp(Level.MEDIUM_ANCHOR, Level.HARD_ANCHOR) ->
                hardPuzzle(shapes, level, language)
            else -> mediumPuzzle(shapes, level, language)
        }
    }

    private fun twoShapePuzzle(shapes: List<String>, level: Level, language: AppLanguage): Problem {
        val (shapeA, shapeB) = shapes
        val (a, b) = distinctValues(2, level.span(2..9, 2..12, 3..15))
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
            level = level,
            lines = lines,
            finals = finals,
            values = listOf(shapeA to a, shapeB to b),
            language = language,
        )
    }

    private fun mediumPuzzle(shapes: List<String>, level: Level, language: AppLanguage): Problem {
        val (shapeA, shapeB, shapeC) = shapes
        val (a, b, c) = distinctValues(3, level.span(2..9, 2..12, 3..15))
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
            level = level,
            lines = lines,
            finals = finals,
            values = listOf(shapeA to a, shapeB to b, shapeC to c),
            language = language,
        )
    }

    private fun hardPuzzle(shapes: List<String>, level: Level, language: AppLanguage): Problem {
        val (shapeA, shapeB, shapeC) = shapes
        val (a, b, c) = distinctValues(3, level.span(3..9, 3..15, 3..15))
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
            level = level,
            lines = lines,
            finals = finals,
            values = listOf(shapeA to a, shapeB to b, shapeC to c),
            language = language,
        )
    }

    private fun build(
        level: Level,
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
        // A two-shape puzzle carries its own instructions in its shape.
        val notes = if (values.size < 3) {
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
        // The puzzle is built so that given line i reveals values[i], so
        // reading it back top to bottom IS the solution. The last step
        // swaps every shape for its number, which is the move she has to
        // make herself.
        val solution = buildList {
            lines.forEachIndexed { index, line ->
                val (shape, value) = values[index]
                add(
                    if (language == AppLanguage.ROMANIAN) "$line   deci $shape = $value"
                    else "$line   so $shape = $value",
                )
            }
            val substituted = values
                .fold(finalLine) { line, (shape, value) -> line.replace(shape, "$value") }
                .replace("?", "$answer")
            add(
                if (language == AppLanguage.ROMANIAN) "Pune numerele în ultimul rând: $substituted"
                else "Put the numbers into the last line: $substituted",
            )
        }
        return Problem(
            text = (lines + finalLine).joinToString("\n"),
            answer = answer,
            level = level,
            kind = ProblemKind.PUZZLE,
            hints = listOf(firstLineHint, valuesHint),
            notes = notes,
            solution = solution,
        )
    }

    /** Distinct values keep the deduction unambiguous and satisfying. */
    private fun distinctValues(count: Int, from: IntRange): List<Int> =
        from.shuffled(random).take(count)

    companion object {
        private val SHAPES = listOf("🍎", "🍐", "🍋", "🌸", "⭐", "🐟", "🍓", "🥕")

        /** Where a third shape starts turning up, and where it is the norm. */
        private const val THIRD_SHAPE_FROM = 20
        private const val THIRD_SHAPE_BY = Level.MEDIUM_ANCHOR
    }
}
