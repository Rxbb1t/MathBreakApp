package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * The original chain, and still the most abstract of the four.
 *
 *  1. THE ANCHOR. A small hunt rather than a sum: the one number in a
 *     window that both given numbers divide. Everything else hangs off it.
 *  2. THE OPERATOR SHIFT. Multiply the anchor, then take a remainder. The
 *     product is big, the answer is tiny, and getting from one to the
 *     other is the work.
 *  3. THE WORKING MEMORY TWIST. Shift that remainder along, then stop
 *     treating the result as a quantity and start treating it as digits.
 *  4. THE FILTER. Apply a rule to the digit sum: reach up to the next
 *     prime and measure the gap.
 *  5. THE GRAND FINALE. Back to the anchor from step one, which by now
 *     she has had to hold for four steps.
 */
internal object AnchorChain : ChallengeChain {

    override fun build(random: Random, language: AppLanguage): DailyChallenge {
        val state = draw(random)
        return DailyChallenge(
            intro = if (en(language)) {
                "Five steps, and each answer feeds the next. Hold on to your Anchor."
            } else {
                "Cinci pași, iar fiecare răspuns îl hrănește pe următorul. Ține minte Ancora."
            },
            stages = listOf(
                anchorStep(state, language),
                operatorStep(state, language),
                memoryStep(state, language),
                filterStep(state, language),
                finaleStep(state, language),
            ),
        )
    }

    /**
     * Draws one day's numbers, every bound chosen so the chain cannot
     * produce a step without an answer.
     *
     * Three things are guaranteed here rather than checked later:
     *
     *  - step 1 has exactly ONE solution, because the window is narrower
     *    than the gap between consecutive multiples of both factors;
     *  - step 2 never divides by zero and never leaves a remainder of
     *    zero, because the divisor is drawn from the candidates that do
     *    not divide the product exactly, and that list can never be empty;
     *  - step 5 never goes negative, because the smallest possible product
     *    already clears the largest possible subtraction.
     */
    private fun draw(random: Random): State {
        val (factorA, factorB) = FACTOR_PAIRS.random(random)
        // Both factors divide their least common multiple, so every
        // multiple of the stride is a multiple of both and nothing else is.
        val stride = leastCommonMultiple(factorA, factorB)
        val steps = ceilingDivide(ANCHOR_MIN, stride)..(ANCHOR_MAX / stride)
        val anchor = stride * random.nextInt(steps.first, steps.last + 1)

        // A window narrower than the stride can hold only one multiple of
        // it, and it holds the anchor, so the hunt has a single answer.
        val spread = minOf(stride - 1, MAX_WINDOW)
        val below = random.nextInt(1, spread)
        val above = random.nextInt(1, spread - below + 1)

        val multiplier = random.nextInt(2, 5)
        val product = anchor * multiplier
        // The divisor has to leave something behind: a remainder of zero
        // turns the step into a yes-or-no and gives step 3 nothing to
        // shift. At least one candidate always qualifies, because dividing
        // exactly by all of them at once needs a multiple of 504 and the
        // product cannot reach that.
        val divisor = DIVISORS.filter { product % it != 0 }.random(random)

        return State(
            anchor = anchor,
            low = anchor - below,
            high = anchor + above,
            factorA = factorA,
            factorB = factorB,
            multiplier = multiplier,
            divisor = divisor,
            addend = random.nextInt(ADDEND_MIN, ADDEND_MAX + 1),
            tail = TAILS.random(random),
        )
    }

    private fun anchorStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 1, your Anchor. Which number between ${state.low} and ${state.high} " +
                "is a multiple of ${state.factorA} and a multiple of ${state.factorB}?"
        } else {
            "Pasul 1, Ancora ta. Care număr dintre ${state.low} și ${state.high} " +
                "e multiplu de ${state.factorA} și multiplu de ${state.factorB}?"
        },
        answer = state.anchor,
        hint = if (en(language)) {
            "A multiple is a number that divides exactly, with nothing left over. " +
                "Only one number in that stretch works for both."
        } else {
            "Un multiplu se împarte exact, fără rest. " +
                "Un singur număr din acel interval merge pentru amândouă."
        },
        notes = multipleNotes(language),
        language = language,
    )

    private fun operatorStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 2. Multiply your Anchor by ${state.multiplier}, then divide by " +
                "${state.divisor} and keep only the remainder. That remainder is Y."
        } else {
            "Pasul 2. Înmulțește Ancora cu ${state.multiplier}, apoi împarte la " +
                "${state.divisor} și păstrează doar restul. Acel rest e Y."
        },
        answer = state.remainder,
        hint = if (en(language)) {
            "Your Anchor was ${state.anchor}. The remainder is what is left once " +
                "every whole lot of ${state.divisor} has been taken out."
        } else {
            "Ancora ta a fost ${state.anchor}. Restul e ce rămâne după ce scoți " +
                "toate grupele întregi de ${state.divisor}."
        },
        notes = remainderNotes(language),
        language = language,
    )

    private fun memoryStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 3. Add ${state.addend} to Y. Now stop reading that as a quantity " +
                "and add its two digits together. That digit sum is Z."
        } else {
            "Pasul 3. Adună ${state.addend} la Y. Acum nu-l mai citi ca pe o cantitate " +
                "și adună-i cele două cifre. Suma cifrelor e Z."
        },
        answer = state.digitSum,
        hint = if (en(language)) {
            "Y was ${state.remainder}. A digit sum is the tens digit and the units " +
                "digit added together, so 34 gives 7."
        } else {
            "Y a fost ${state.remainder}. Suma cifrelor e cifra zecilor plus cifra " +
                "unităților, deci 34 dă 7."
        },
        notes = digitNotes(language),
        language = language,
    )

    private fun filterStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 4. Find the smallest prime number bigger than Z, then take Z away " +
                "from it. That difference is W."
        } else {
            "Pasul 4. Găsește cel mai mic număr prim mai mare decât Z, apoi scade Z " +
                "din el. Acea diferență e W."
        },
        answer = state.gap,
        hint = if (en(language)) {
            "Z was ${state.digitSum}. A prime divides by nothing but itself and one, " +
                "and the one you want is the first prime above Z, not below it."
        } else {
            "Z a fost ${state.digitSum}. Un număr prim nu se împarte decât la el " +
                "însuși și la unu, iar cel căutat e primul prim de deasupra lui Z."
        },
        notes = primeNotes(language),
        language = language,
    )

    private fun finaleStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 5, the finish. Multiply W by your Anchor from step 1, " +
                "then subtract ${state.tail}. What are you left with?"
        } else {
            "Pasul 5, finalul. Înmulțește W cu Ancora de la pasul 1, " +
                "apoi scade ${state.tail}. Cu cât rămâi?"
        },
        answer = state.finale,
        hint = if (en(language)) {
            "W was ${state.gap} and your Anchor, all the way back at step 1, " +
                "was ${state.anchor}."
        } else {
            "W a fost ${state.gap}, iar Ancora, tocmai de la pasul 1, " +
                "a fost ${state.anchor}."
        },
        notes = anchorChainNotes(language),
        language = language,
    )

    private fun multipleNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "A multiple of 7 is 7, 14, 21, 28 and so on: what you get counting up in sevens.",
                "To be a multiple of two numbers at once, it has to appear in both counts. " +
                    "10 and 4 both reach 20, so 20 is a multiple of both.",
                "The multiples of both come at regular gaps, so a short stretch of numbers " +
                    "holds at most one of them.",
            )
        } else {
            listOf(
                "Multiplii lui 7 sunt 7, 14, 21, 28 și așa mai departe: ce obții numărând din 7 în 7.",
                "Ca să fie multiplu de două numere deodată, trebuie să apară în ambele șiruri. " +
                    "Și 10, și 4 ajung la 20, deci 20 e multiplu de amândouă.",
                "Multiplii comuni vin la distanțe egale, așa că un interval scurt de numere " +
                    "conține cel mult unul.",
            )
        }

    private fun digitNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "A digit sum ignores what the number is worth and just adds the figures: " +
                    "52 becomes 5 + 2 = 7.",
                "Because it throws the place value away, a digit sum is always small. " +
                    "Two digits can never add up past 18.",
            )
        } else {
            listOf(
                "Suma cifrelor nu ține cont de cât valorează numărul, ci doar adună figurile. " +
                    "Suma cifrelor lui 52: 5 + 2 = 7.",
                "Fiindcă lasă deoparte valoarea de poziție, suma cifrelor e mereu mică. " +
                    "Două cifre nu pot depăși 18.",
            )
        }

    private fun primeNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "A prime divides by nothing except itself and 1. " +
                    "The primes up to thirty: 2, 3, 5, 7, 11, 13, 17, 19, 23, 29.",
                "1 is not prime, and 2 is the only even one.",
                "If a number is not on the list, walk upward until you meet one that is.",
            )
        } else {
            listOf(
                "Un număr prim nu se împarte decât la el însuși și la 1. " +
                    "Numerele prime până la treizeci: 2, 3, 5, 7, 11, 13, 17, 19, 23, 29.",
                "1 nu e prim, iar 2 e singurul par.",
                "Dacă un număr nu e pe listă, urcă până întâlnești unul care e.",
            )
        }

    private fun anchorChainNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "Where you have been: the Anchor, then Y the remainder, then Z the digit sum, " +
                    "then W the step up to the next prime.",
                "This last one reaches all the way back. W multiplies the Anchor, not Z.",
            )
        } else {
            listOf(
                "Pe unde ai trecut: Ancora, apoi Y restul, apoi Z suma cifrelor, " +
                    "apoi W pasul până la următorul prim.",
                "Ultimul se întoarce tocmai la început. W înmulțește Ancora, nu Z.",
            )
        }

    /**
     * Only the DRAWN values are stored. Every answer is derived from them,
     * which is the whole reason this is one object rather than five
     * variables passed along: a chain where step four's input is a field
     * somebody could set is a chain that can disagree with itself.
     */
    private data class State(
        val anchor: Int,
        val low: Int,
        val high: Int,
        val factorA: Int,
        val factorB: Int,
        val multiplier: Int,
        val divisor: Int,
        val addend: Int,
        val tail: Int,
    ) {
        /** Never zero; see the divisor's draw. */
        val remainder: Int get() = anchor * multiplier % divisor
        val shifted: Int get() = remainder + addend
        val digitSum: Int get() = digitsOf(shifted)
        val prime: Int get() = PRIMES.first { it > digitSum }
        val gap: Int get() = prime - digitSum
        val finale: Int get() = gap * anchor - tail
    }

    /**
     * Far enough to cover any digit sum this chain can reach. The largest
     * shifted value is 48, whose digits sum to 12, so 13 is always in
     * reach and the lookup can never run off the end.
     */
    private val PRIMES = listOf(2, 3, 5, 7, 11, 13, 17, 19, 23)

    /**
     * Factor pairs for step 1. Neither number divides the other, so
     * "a multiple of both" asks something that "a multiple of the bigger
     * one" would not.
     */
    private val FACTOR_PAIRS = listOf(3 to 4, 3 to 5, 4 to 5, 3 to 8, 4 to 6, 5 to 6, 4 to 9)

    /** The anchor stays double-digit: big enough to work at, small enough to hold. */
    private const val ANCHOR_MIN = 24
    private const val ANCHOR_MAX = 96

    /**
     * The widest step 1's window can get. Every stride is at least 12, so
     * this stays under it and the single-answer guarantee holds.
     */
    private const val MAX_WINDOW = 11

    /** Step 2's candidate divisors. None of them is zero, by being written here. */
    private val DIVISORS = listOf(6, 7, 8, 9)

    /**
     * Step 3's shift. Chosen so the result is always two digits: the
     * remainder is at most 8, so the sum lands between 11 and 48.
     */
    private const val ADDEND_MIN = 10
    private const val ADDEND_MAX = 40

    /** Step 5's parting subtraction, comfortably under the smallest product. */
    private val TAILS = listOf(10, 15, 20)
}
