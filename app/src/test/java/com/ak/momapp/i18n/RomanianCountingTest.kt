package com.ak.momapp.i18n

import com.ak.momapp.problem.DailyChallengeGenerator
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.Problem
import com.ak.momapp.problem.ProblemGenerator
import java.time.LocalDate
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Romanian counts "12 mere" but "45 **de** mere": from twenty upward the
 * noun takes "de" in front of it. Miss it and the sentence doesn't read
 * as broken grammar so much as broken app, which is exactly the kind of
 * thing that survives a code review and gets caught by the person using
 * it.
 *
 * The generators interpolate numbers into sentences, so the rule can only
 * be checked on the finished text.
 */
class RomanianCountingTest {

    /**
     * A number of twenty or more followed straight by a word. Any hit is
     * either a genuine miss or one of the exceptions below.
     */
    private val countedNoun = Regex("\\b(\\d+) ([\\p{L}]+)")

    /**
     * A number can be followed by plenty of words that it isn't counting,
     * and those are fine bare. Three groups:
     *
     *  - "de" itself, which is the correct case;
     *  - unit abbreviations, which are written without it ("30 m");
     *  - closed-class words -- verbs, prepositions, conjunctions,
     *    adverbs -- which follow a numeral without being counted by it
     *    ("rămân 45 și mai are…", "duci 25 vecinilor").
     *
     * The last group is a snapshot of what the templates currently
     * produce. A new template with a new non-noun follower will fail here
     * once and want adding; that is the price of catching the real thing,
     * and it errs toward a false alarm rather than a silent miss.
     */
    private val allowed = setOf(
        "de",
        // Units, written without "de".
        "m", "cm", "dm", "mm", "km", "g", "kg", "l", "ml", "min", "h", "s", "euro",
        // Verbs and copulas.
        "e", "este", "sunt", "au", "are", "rămân", "face", "fac", "costă",
        "întreabă", "dă", "duci", "ia", "iau", "pune", "vinde", "adaugă",
        // Prepositions, conjunctions, pronouns, articles.
        "din", "și", "la", "în", "după", "se", "nu", "pe", "cu", "le", "îi",
        "i", "o", "un", "ca", "dar", "sau", "că", "care", "mai", "doar",
        "pentru", "peste", "sub", "prin", "spre", "până", "iar", "deci",
        // Adverbials that follow a number without being counted.
        "cadou", "singur", "singură", "gratis", "vecinilor", "fiecare",
        "toamna", "iarna", "primăvara", "vara",
        "luni", "marți", "miercuri", "joi", "vineri", "sâmbătă", "duminică",
    )

    private fun offences(text: String): List<String> =
        countedNoun.findAll(text)
            .filter { it.groupValues[1].toInt() >= 20 }
            .filter { it.groupValues[2].lowercase() !in allowed }
            .map { it.value }
            .toList()

    private fun offences(problem: Problem): List<String> =
        (listOf(problem.text) + problem.hints + problem.notes + problem.solution)
            .flatMap(::offences)

    @Test
    fun `Romanian problems put de in front of counted nouns`() {
        val bad = sortedSetOf<String>()
        for (difficulty in Difficulty.entries) {
            val generator = ProblemGenerator(Random(7))
            repeat(SAMPLE) {
                bad += offences(generator.generate(difficulty, AppLanguage.ROMANIAN))
            }
        }
        assertTrue(
            "counted nouns missing \"de\":\n" + bad.joinToString("\n") { "  $it" },
            bad.isEmpty(),
        )
    }

    @Test
    fun `the Romanian daily challenge puts de in front of counted nouns`() {
        val bad = sortedSetOf<String>()
        val start = LocalDate.of(2026, 1, 1)
        repeat(400) { day ->
            DailyChallengeGenerator()
                .generate(start.plusDays(day.toLong()), AppLanguage.ROMANIAN)
                .stages
                .forEach { bad += offences(it) }
        }
        assertTrue(
            "counted nouns missing \"de\":\n" + bad.joinToString("\n") { "  $it" },
            bad.isEmpty(),
        )
    }

    /** The detector has to be able to see a mistake, or it proves nothing. */
    @Test
    fun `the check catches a missing de`() {
        assertTrue(offences("Am cumpărat 45 mere de la piață.").isNotEmpty())
        assertTrue(offences("Am cumpărat 45 de mere de la piață.").isEmpty())
        assertTrue("under twenty needs no de", offences("Am cumpărat 12 mere.").isEmpty())
        assertTrue("units are written without de", offences("Un gard de 30 m.").isEmpty())
    }

    private companion object {
        const val SAMPLE = 4_000
    }
}
