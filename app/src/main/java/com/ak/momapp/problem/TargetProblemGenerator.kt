package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * Target builder: a target number and a spread of cards; she taps the
 * cards that add up to it. [Problem.answer] holds the target, [cards]
 * the shuffled spread, [pickCount] how many to choose. Any picked set
 * of the right size that sums to the target counts — the planted
 * solution just guarantees one exists. [Problem.revealText] shows it.
 */
class TargetProblemGenerator(private val random: Random) {

    fun generate(difficulty: Difficulty, language: AppLanguage): Problem {
        val pick = when (difficulty) {
            Difficulty.EASY -> 3
            Difficulty.MEDIUM -> if (random.nextBoolean()) 3 else 4
            Difficulty.HARD -> 4
        }
        val spread = when (difficulty) {
            Difficulty.EASY -> 6
            Difficulty.MEDIUM -> 7
            Difficulty.HARD -> 8
        }
        val solution = List(pick) { cardValue(difficulty) }
        val target = solution.sum()
        val decoys = List(spread - pick) { cardValue(difficulty) }
        val cards = (solution + decoys).shuffled(random)

        val text = when (language) {
            AppLanguage.ENGLISH -> "Pick $pick cards that add up to $target."
            AppLanguage.ROMANIAN -> "Alege $pick cartonașe care adunate dau $target."
        }
        // No hints: the task restates itself, and naming a solution card
        // would give half the answer away.
        return Problem(
            text = text,
            answer = target,
            difficulty = difficulty,
            kind = ProblemKind.TARGET,
            cards = cards,
            pickCount = pick,
            revealText = solution.joinToString(" + "),
        )
    }

    private fun cardValue(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> random.nextInt(2, 11)
        Difficulty.MEDIUM -> random.nextInt(4, 21)
        Difficulty.HARD -> random.nextInt(8, 41)
    }
}
