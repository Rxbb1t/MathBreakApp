package com.ak.momapp.i18n

import com.ak.momapp.problem.DailyChallengeGenerator
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.Problem
import com.ak.momapp.problem.ProblemGenerator
import com.ak.momapp.problem.toLevel
import java.time.LocalDate
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every problem is authored twice, and the two halves sit a few lines
 * apart in the same file. That makes it easy for a Romanian word to ride
 * along into the English text -- "covrigi" did exactly that, in the daily
 * challenge, and survived until she met it on screen.
 *
 * The check runs over generated OUTPUT rather than the source, so it sees
 * what she would actually read: text, hints, helper-sheet notes and
 * worked solutions alike.
 */
class LanguagePurityTest {

    /** Letters that exist in Romanian and not in English. */
    private val romanianLetters = Regex("[ăâîșțĂÂÎȘȚşţ]")

    /**
     * Romanian words that survive without their diacritics, so the letter
     * check alone would miss them. Mostly food and market words, because
     * that is where the app's stories live.
     */
    private val romanianWords = Regex(
        "\\b(" +
            "covrig|covrigi|covrigii|covrigul|" +
            "lei|leu|bani|piata|piaţa|" +
            "mamaliga|sarmale|placinta|gogosi|cozonac|mici|zacusca|" +
            "branza|smantana|paine|lapte|oua|ardei|varza|rosii|cartofi|" +
            "bunica|bunicul|matusa|unchiul|" +
            "tava|tavi|prajitura|prajituri" +
            ")\\b",
        RegexOption.IGNORE_CASE,
    )

    private fun offendingFields(problem: Problem): List<String> =
        (listOf(problem.text, problem.revealText, problem.answerUnit) +
            problem.hints + problem.notes + problem.solution)
            .filter { romanianLetters.containsMatchIn(it) || romanianWords.containsMatchIn(it) }

    @Test
    fun `no Romanian reaches the English problems`() {
        val offenders = sortedSetOf<String>()
        for (difficulty in Difficulty.entries) {
            val generator = ProblemGenerator(Random(3))
            repeat(SAMPLE) {
                offenders += offendingFields(generator.generate(difficulty.toLevel(), AppLanguage.ENGLISH))
            }
        }
        assertTrue(
            "Romanian in the English problems:\n" + offenders.joinToString("\n") { "  $it" },
            offenders.isEmpty(),
        )
    }

    /**
     * The daily challenge has its own generator and its own copy of the
     * EN/RO pairs, which is exactly why it was the half that drifted.
     */
    @Test
    fun `no Romanian reaches the English daily challenge`() {
        val offenders = sortedSetOf<String>()
        val start = LocalDate.of(2026, 1, 1)
        repeat(400) { day ->
            DailyChallengeGenerator()
                .generate(start.plusDays(day.toLong()), AppLanguage.ENGLISH)
                .stages
                .forEach { offenders += offendingFields(it) }
        }
        assertTrue(
            "Romanian in the English daily challenge:\n" + offenders.joinToString("\n") { "  $it" },
            offenders.isEmpty(),
        )
    }

    /**
     * The other direction, so the test can't pass by the word lists being
     * wrong: the Romanian problems really are Romanian, and would trip
     * every check above.
     */
    @Test
    fun `the Romanian problems are actually Romanian`() {
        val generator = ProblemGenerator(Random(3))
        val flagged = (1..SAMPLE).count {
            offendingFields(generator.generate(Difficulty.MEDIUM.toLevel(), AppLanguage.ROMANIAN)).isNotEmpty()
        }
        assertTrue(
            "the detector found almost no Romanian in the Romanian problems: $flagged/$SAMPLE",
            flagged > SAMPLE / 2,
        )
    }

    private companion object {
        const val SAMPLE = 4_000
    }
}
