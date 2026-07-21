package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Target builder: a target number and a spread of cards; she taps the
 * cards that add up to it. [Problem.answer] holds the target, [cards]
 * the shuffled spread, [pickCount] how many to choose. Any picked set
 * of the right size that sums to the target counts. The planted
 * solution just guarantees one exists. [Problem.revealText] shows it.
 */
class TargetProblemGenerator(private val random: Random) {

    fun generate(level: Level, language: AppLanguage): Problem {
        // A fourth card to find arrives gradually rather than on the day
        // she is renamed to Normal: rare at the bottom of the band, usual
        // by the top of it.
        val pick = if (random.nextDouble() < level.ramp(FOURTH_CARD_FROM, FOURTH_CARD_BY)) 4 else 3
        val spread = level.between(6, 7, 8)
        val solution = List(pick) { cardValue(level) }
        val target = solution.sum()
        val decoys = List(spread - pick) { cardValue(level) }
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
            level = level,
            kind = ProblemKind.TARGET,
            cards = cards,
            pickCount = pick,
            revealText = solution.joinToString(" + "),
        )
    }

    /** Card values grow with the level: single digits low, up to forties high. */
    private fun cardValue(level: Level): Int =
        random.nextInt(level.span(2..10, 4..20, 8..40))

    companion object {
        /** Where the fourth card starts appearing, and where it takes over. */
        private const val FOURTH_CARD_FROM = Level.EASY_TOP
        private const val FOURTH_CARD_BY = Level.HARD_ANCHOR
    }
}
