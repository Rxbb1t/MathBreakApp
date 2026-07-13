package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * Number hunt: a spread of distinct number cards and a rule — tap every
 * card that fits it (all the evens, all the multiples of 5) or the one
 * best card (the largest prime). [Problem.cards] holds the shuffled
 * spread, [Problem.correctCards] the values that must be tapped and
 * [Problem.revealText] lists them. Tap-answered, so no hints — but the
 * notebook teaches the trick (last digit, digit sum, the small primes).
 */
class NumberHuntGenerator(private val random: Random) {

    private enum class Rule { EVEN, ODD, MULTIPLE, LARGEST_PRIME }

    fun generate(difficulty: Difficulty, language: AppLanguage): Problem {
        // Primes wait until MEDIUM; EASY hunts stay purely visual rules.
        val rule = when (difficulty) {
            Difficulty.EASY -> listOf(Rule.EVEN, Rule.ODD, Rule.MULTIPLE).random(random)
            else -> Rule.entries.random(random)
        }
        return when (rule) {
            Rule.LARGEST_PRIME -> largestPrime(difficulty, language)
            Rule.MULTIPLE -> ruleHunt(difficulty, multipleOf(difficulty, language))
            Rule.EVEN -> ruleHunt(difficulty, HuntRule.even(language))
            Rule.ODD -> ruleHunt(difficulty, HuntRule.odd(language))
        }
    }

    /** A tappable rule: its prompt, its test, and its notebook notes. */
    private class HuntRule(
        val prompt: String,
        val fits: (Int) -> Boolean,
        val notes: List<String>,
    ) {
        companion object {
            fun even(language: AppLanguage) = HuntRule(
                prompt = when (language) {
                    AppLanguage.ENGLISH -> "Tap all the even numbers."
                    AppLanguage.ROMANIAN -> "Atinge toate numerele pare."
                },
                fits = { it % 2 == 0 },
                notes = listOf(parityNote(language)),
            )

            fun odd(language: AppLanguage) = HuntRule(
                prompt = when (language) {
                    AppLanguage.ENGLISH -> "Tap all the odd numbers."
                    AppLanguage.ROMANIAN -> "Atinge toate numerele impare."
                },
                fits = { it % 2 != 0 },
                notes = listOf(parityNote(language)),
            )

            private fun parityNote(language: AppLanguage) = when (language) {
                AppLanguage.ENGLISH ->
                    "Even numbers end in 0, 2, 4, 6 or 8; odd ones in 1, 3, 5, 7 or 9. " +
                        "Only the last digit matters, however long the number is."
                AppLanguage.ROMANIAN ->
                    "Numerele pare se termină în 0, 2, 4, 6 sau 8; cele impare în 1, 3, 5, 7 sau 9. " +
                        "Contează doar ultima cifră, oricât de lung e numărul."
            }
        }
    }

    private fun multipleOf(difficulty: Difficulty, language: AppLanguage): HuntRule {
        // EASY sticks to the last-digit divisors; the digit-sum trick for
        // 3 and 9 arrives with the levels that can enjoy it.
        val k = when (difficulty) {
            Difficulty.EASY -> listOf(5, 10).random(random)
            Difficulty.MEDIUM -> listOf(3, 5, 10).random(random)
            Difficulty.HARD -> listOf(3, 9).random(random)
        }
        return HuntRule(
            prompt = when (language) {
                AppLanguage.ENGLISH -> "Tap all the multiples of $k."
                AppLanguage.ROMANIAN -> "Atinge toți multiplii lui $k."
            },
            fits = { it % k == 0 },
            notes = listOfNotNull(
                when (language) {
                    AppLanguage.ENGLISH ->
                        "Multiples of 5 end in 0 or 5; multiples of 10 end in 0."
                    AppLanguage.ROMANIAN ->
                        "Multiplii lui 5 se termină în 0 sau 5; multiplii lui 10 se termină în 0."
                }.takeIf { k == 5 || k == 10 },
                when (language) {
                    AppLanguage.ENGLISH ->
                        "Add the digits: when the sum divides by 3, the number does too — " +
                            "same trick for 9. 87 → 8 + 7 = 15, so 87 divides by 3."
                    AppLanguage.ROMANIAN ->
                        "Adună cifrele: dacă suma se împarte la 3, și numărul se împarte — " +
                            "același truc merge pentru 9. 87 → 8 + 7 = 15, deci 87 se împarte la 3."
                }.takeIf { k == 3 || k == 9 },
            ),
        )
    }

    private fun ruleHunt(difficulty: Difficulty, rule: HuntRule): Problem {
        val spread = spreadFor(difficulty)
        // Always at least two to find and two to leave alone.
        val matchCount = random.nextInt(2, spread - 1)
        val range = valueRange(difficulty)
        val matches = distinctValues(matchCount, range, rule.fits)
        val decoys = distinctValues(spread - matchCount, range) { !rule.fits(it) }
        val correct = matches.toSet()

        return Problem(
            text = rule.prompt,
            answer = matches.sum(),
            difficulty = difficulty,
            kind = ProblemKind.SELECT,
            notes = rule.notes,
            cards = (matches + decoys).shuffled(random),
            correctCards = correct,
            revealText = matches.sorted().joinToString(", "),
        )
    }

    private fun largestPrime(difficulty: Difficulty, language: AppLanguage): Problem {
        val (primes, traps) = when (difficulty) {
            Difficulty.HARD -> PRIMES_HARD to TRAPS_HARD
            else -> PRIMES_MEDIUM to TRAPS_MEDIUM
        }
        val spread = spreadFor(difficulty)
        // Three real primes so "largest" is a real question, the rest
        // composite lookalikes.
        val planted = primes.shuffled(random).take(3)
        val decoys = traps.shuffled(random).take(spread - planted.size)
        val answer = planted.max()

        val text = when (language) {
            AppLanguage.ENGLISH -> "Tap the largest prime number."
            AppLanguage.ROMANIAN -> "Atinge cel mai mare număr prim."
        }
        return Problem(
            text = text,
            answer = answer,
            difficulty = difficulty,
            kind = ProblemKind.SELECT,
            notes = primeNotes(language),
            cards = (planted + decoys).shuffled(random),
            correctCards = setOf(answer),
            revealText = answer.toString(),
        )
    }

    private fun primeNotes(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "A prime divides only by 1 and by itself. The primes up to 30: " +
                "2, 3, 5, 7, 11, 13, 17, 19, 23, 29.",
            "Watch for lookalikes: 91 = 7 × 13, 87 = 3 × 29, 119 = 7 × 17, " +
                "121 = 11 × 11 — none of them prime.",
            "To test a number, try dividing by the small primes in turn: 2, 3, 5, 7, 11, 13. " +
                "If none of them fits, it's prime.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Un număr prim se împarte doar la 1 și la el însuși. Numerele prime până la 30: " +
                "2, 3, 5, 7, 11, 13, 17, 19, 23, 29.",
            "Atenție la impostori: 91 = 7 × 13, 87 = 3 × 29, 119 = 7 × 17, " +
                "121 = 11 × 11 — niciunul nu e prim.",
            "Ca să verifici un număr, împarte-l pe rând la numerele prime mici: 2, 3, 5, 7, 11, 13. " +
                "Dacă niciunul nu se potrivește, e prim.",
        )
    }

    private fun spreadFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 6
        Difficulty.MEDIUM -> 8
        Difficulty.HARD -> 9
    }

    private fun valueRange(difficulty: Difficulty): IntRange = when (difficulty) {
        Difficulty.EASY -> 2..49
        Difficulty.MEDIUM -> 10..99
        Difficulty.HARD -> 100..999
    }

    private fun distinctValues(count: Int, range: IntRange, fits: (Int) -> Boolean): List<Int> {
        val values = mutableSetOf<Int>()
        while (values.size < count) {
            val value = random.nextInt(range.first, range.last + 1)
            if (fits(value)) values.add(value)
        }
        return values.toList()
    }

    companion object {
        private val PRIMES_MEDIUM = listOf(11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97)
        private val TRAPS_MEDIUM = listOf(21, 27, 33, 39, 49, 51, 57, 63, 69, 77, 87, 91, 93)
        private val PRIMES_HARD = listOf(101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199)
        private val TRAPS_HARD = listOf(111, 117, 119, 121, 123, 129, 133, 141, 143, 147, 153, 159, 161, 169, 171, 177, 183, 187, 189)
    }
}
