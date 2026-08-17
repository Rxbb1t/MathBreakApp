package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * The original chain, and still the most abstract of the four.
 *
 * Step 1 is always the Anchor hunt and step 5 always reaches back to it.
 * Between them the Anchor might be worked into a remainder or measured
 * against the next whole ten, that small number shifted along or
 * doubled, and the result filtered by reaching for a prime or by
 * counting what divides it.
 */
internal object AnchorChain : ChallengeChain {

    override fun build(random: Random, language: AppLanguage): DailyChallenge {
        val hunt = drawHunt(random)
        val opening = anchorStep(hunt)
        val second = pick(SECOND, opening.answer, random)
        val third = pick(THIRD, second.answer, random)
        val fourth = pick(FOURTH, third.answer, random)
        val finale = finaleStep(opening.answer, fourth.answer, random)
        return DailyChallenge(
            intro = if (en(language)) {
                "Five steps, and each answer feeds the next. Hold on to your Anchor."
            } else {
                "Cinci pași, iar fiecare răspuns îl hrănește pe următorul. Ține minte Ancora."
            },
            stages = listOf(opening, second, third, fourth, finale).map { it.problem(language) },
        )
    }

    private data class Hunt(
        val anchor: Int,
        val low: Int,
        val high: Int,
        val factorA: Int,
        val factorB: Int,
    )

    /**
     * Draws the hunt so it has exactly ONE solution: the window is
     * narrower than the gap between consecutive multiples of both
     * factors, and it holds the anchor. Anything else would be unfair in
     * a way she cannot argue with, because the app would reject a number
     * that satisfies everything it asked for.
     */
    private fun drawHunt(random: Random): Hunt {
        val (factorA, factorB) = FACTOR_PAIRS.random(random)
        // Both factors divide their least common multiple, so every
        // multiple of the stride is a multiple of both and nothing else is.
        val stride = leastCommonMultiple(factorA, factorB)
        val steps = ceilingDivide(ANCHOR_MIN, stride)..(ANCHOR_MAX / stride)
        val anchor = stride * random.nextInt(steps.first, steps.last + 1)

        val spread = minOf(stride - 1, MAX_WINDOW)
        val below = random.nextInt(1, spread)
        val above = random.nextInt(1, spread - below + 1)
        return Hunt(anchor, anchor - below, anchor + above, factorA, factorB)
    }

    private fun anchorStep(hunt: Hunt) = ChainStep(
        answer = hunt.anchor,
        text = { language ->
            if (en(language)) {
                "Step 1, your Anchor. Which number between ${hunt.low} and ${hunt.high} " +
                    "is a multiple of ${hunt.factorA} and a multiple of ${hunt.factorB}?"
            } else {
                "Pasul 1, Ancora ta. Care număr dintre ${hunt.low} și ${hunt.high} " +
                    "e multiplu de ${hunt.factorA} și multiplu de ${hunt.factorB}?"
            }
        },
        hint = { language ->
            if (en(language)) {
                "A multiple is a number that divides exactly, with nothing left over. " +
                    "Only one number in that stretch works for both."
            } else {
                "Un multiplu se împarte exact, fără rest. " +
                    "Un singur număr din acel interval merge pentru amândouă."
            }
        },
        notes = ::multipleNotes,
    )

    // ── Step 2: the Anchor, worked down to a single figure ───────────────

    /** The always-applicable variant is the remainder; see [pick]. */
    private val SECOND: List<StepSpec> = listOf(
        // Multiply, then keep only what will not divide out. The divisor
        // is drawn from the ones that leave something behind: a remainder
        // of zero would turn this into a yes-or-no and give the next step
        // nothing to work on.
        { anchor, random ->
            val multiplier = random.nextInt(2, 5)
            val product = anchor * multiplier
            val divisors = DIVISORS.filter { product % it != 0 }
            if (divisors.isEmpty()) {
                null
            } else {
                val divisor = divisors.random(random)
                ChainStep(
                    answer = product % divisor,
                    text = { language ->
                        if (en(language)) {
                            "Step 2. Multiply your Anchor by $multiplier, then divide by " +
                                "$divisor and keep only the remainder. That remainder is Y."
                        } else {
                            "Pasul 2. Înmulțește Ancora cu $multiplier, apoi împarte la " +
                                "$divisor și păstrează doar restul. Acel rest e Y."
                        }
                    },
                    hint = { language ->
                        if (en(language)) {
                            "Your Anchor was $anchor. The remainder is what is left once " +
                                "every whole lot of $divisor has been taken out."
                        } else {
                            "Ancora ta a fost $anchor. Restul e ce rămâne după ce scoți " +
                                "toate grupele întregi de $divisor."
                        }
                    },
                    notes = ::remainderNotes,
                )
            }
        },
        // How far it sits below the next whole ten. Nothing to measure
        // when the Anchor is already sitting on one.
        { anchor, _ ->
            if (anchor % 10 == 0) {
                null
            } else {
                ChainStep(
                    answer = 10 - anchor % 10,
                    text = { language ->
                        if (en(language)) {
                            "Step 2. Count up from your Anchor to the next whole ten above " +
                                "it. How many steps is that? Call it Y."
                        } else {
                            "Pasul 2. Numără de la Ancora ta până la următoarea zece " +
                                "întreagă de deasupra. Câți pași sunt? Să-i zicem Y."
                        }
                    },
                    hint = { language ->
                        if (en(language)) {
                            "Your Anchor was $anchor. Only the units figure matters here: " +
                                "the tens are already behind you."
                        } else {
                            "Ancora ta a fost $anchor. Contează doar cifra unităților: " +
                                "zecile sunt deja în urmă."
                        }
                    },
                    notes = ::tenNotes,
                )
            }
        },
    )

    // ── Step 3: that figure, grown into a small number ───────────────────

    /** Both variants accept anything a single figure can be. */
    private val THIRD: List<StepSpec> = listOf(
        // Shift it along, then stop reading it as a quantity.
        { y, random ->
            val addend = random.nextInt(ADDEND_MIN, ADDEND_MAX + 1)
            ChainStep(
                answer = digitsOf(y + addend),
                text = { language ->
                    if (en(language)) {
                        "Step 3. Add $addend to Y. Now stop reading that as a quantity and " +
                            "add its two digits together. That digit sum is Z."
                    } else {
                        "Pasul 3. Adună $addend la Y. Acum nu-l mai citi ca pe o cantitate " +
                            "și adună-i cele două cifre. Suma cifrelor e Z."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "Y was $y. A digit sum is the tens digit and the units digit added " +
                            "together, so 34 gives 7."
                    } else {
                        "Y a fost $y. Suma cifrelor e cifra zecilor plus cifra unităților, " +
                            "deci 34 dă 7."
                    }
                },
                notes = ::digitNotes,
            )
        },
        // Double it and push it along.
        { y, random ->
            val extra = random.nextInt(3, 10)
            ChainStep(
                answer = 2 * y + extra,
                text = { language ->
                    if (en(language)) {
                        "Step 3. Double Y, then add $extra to what you get. Call that Z."
                    } else {
                        "Pasul 3. Dublează Y, apoi adaugă $extra la cât îți iese. " +
                            "Să-i zicem Z."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "Y was $y. Double it first and add afterwards: the other way round " +
                            "gives a different number."
                    } else {
                        "Y a fost $y. Întâi dublează și abia apoi adună: invers iese " +
                            "alt număr."
                    }
                },
                notes = ::doubleNotes,
            )
        },
    )

    // ── Step 4: that number, put through a filter ────────────────────────

    /** Both variants accept anything from two upward. */
    private val FOURTH: List<StepSpec> = listOf(
        { z, _ ->
            ChainStep(
                answer = nextPrimeAbove(z) - z,
                text = { language ->
                    if (en(language)) {
                        "Step 4. Find the smallest prime number bigger than Z, then take Z " +
                            "away from it. That difference is W."
                    } else {
                        "Pasul 4. Găsește cel mai mic număr prim mai mare decât Z, apoi " +
                            "scade Z din el. Acea diferență e W."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "Z was $z. A prime divides by nothing but itself and one, and the " +
                            "one you want is the first prime above Z, not below it."
                    } else {
                        "Z a fost $z. Un număr prim nu se împarte decât la el însuși și la " +
                            "unu, iar cel căutat e primul prim de deasupra lui Z."
                    }
                },
                notes = ::primeNotes,
            )
        },
        { z, _ ->
            if (z < 2) {
                null
            } else {
                ChainStep(
                    answer = divisorCount(z),
                    text = { language ->
                        if (en(language)) {
                            "Step 4. How many whole numbers divide Z exactly, counting 1 and " +
                                "Z themselves? That count is W."
                        } else {
                            "Pasul 4. Câte numere întregi se împart exact în Z, punând la " +
                                "socoteală și pe 1, și pe Z? Numărul lor e W."
                        }
                    },
                    hint = { language ->
                        if (en(language)) {
                            "Z was $z. Walk up from 1 and keep the ones that leave no " +
                                "remainder. Every number has at least two."
                        } else {
                            "Z a fost $z. Urcă de la 1 și păstrează-le pe cele care nu lasă " +
                                "rest. Orice număr are cel puțin două."
                        }
                    },
                    notes = ::divisorNotes,
                )
            }
        },
    )

    private fun finaleStep(anchor: Int, w: Int, random: Random): ChainStep {
        val tail = TAILS.random(random)
        return ChainStep(
            answer = w * anchor - tail,
            text = { language ->
                if (en(language)) {
                    "Step 5, the finish. Multiply W by your Anchor from step 1, " +
                        "then subtract $tail. What are you left with?"
                } else {
                    "Pasul 5, finalul. Înmulțește W cu Ancora de la pasul 1, " +
                        "apoi scade $tail. Cu cât rămâi?"
                }
            },
            hint = { language ->
                if (en(language)) {
                    "W was $w and your Anchor, all the way back at step 1, was $anchor."
                } else {
                    "W a fost $w, iar Ancora, tocmai de la pasul 1, a fost $anchor."
                }
            },
            notes = ::chainNotes,
        )
    }

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

    private fun tenNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "The whole tens are 10, 20, 30 and so on. From 47 the next one up is 50, " +
                    "which is 3 steps away.",
                "Only the units figure decides the distance, because the tens are already " +
                    "counted.",
                "A number already sitting on a ten is nought steps from itself, not ten.",
            )
        } else {
            listOf(
                "Zecile întregi sunt 10, 20, 30 și așa mai departe. De la 47, următoarea e " +
                    "50, adică la 3 pași.",
                "Doar cifra unităților hotărăște distanța, fiindcă zecile sunt deja numărate.",
                "Un număr care stă deja pe o zece e la zero pași de el însuși, nu la zece.",
            )
        }

    private fun doubleNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "Doubling is adding a number to itself: double 7 is 14.",
                "Doubling then adding is not the same as adding then doubling. " +
                    "Double 5 plus 3 is 13; double 8 is 16.",
            )
        } else {
            listOf(
                "A dubla înseamnă a aduna numărul cu el însuși: dublul lui 7 e 14.",
                "Să dublezi și apoi să aduni nu e tot una cu a aduna și apoi a dubla. " +
                    "Dublul lui 5 plus 3 face 13; dublul lui 8 face 16.",
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

    private fun divisorNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "The numbers that divide 12 exactly are 1, 2, 3, 4, 6 and 12: six of them.",
                "Every number has at least two, itself and 1. A prime has exactly those two " +
                    "and nothing else.",
                "They come in pairs that multiply back: for 12, 2 with 6 and 3 with 4.",
            )
        } else {
            listOf(
                "Numerele care se împart exact în 12 sunt 1, 2, 3, 4, 6 și 12: șase la număr.",
                "Orice număr are cel puțin două, pe el însuși și pe 1. Un prim are exact " +
                    "acestea două și nimic altceva.",
                "Vin în perechi care se înmulțesc înapoi: la 12, 2 cu 6 și 3 cu 4.",
            )
        }

    /**
     * Factor pairs for the hunt. Neither number divides the other, so
     * "a multiple of both" asks something that "a multiple of the bigger
     * one" would not.
     */
    private val FACTOR_PAIRS = listOf(3 to 4, 3 to 5, 4 to 5, 3 to 8, 4 to 6, 5 to 6, 4 to 9)

    /** The anchor stays double-digit: big enough to work at, small enough to hold. */
    private const val ANCHOR_MIN = 24
    private const val ANCHOR_MAX = 96

    /**
     * The widest the hunt's window can get. Every stride is at least 12,
     * so this stays under it and the single-answer guarantee holds.
     */
    private const val MAX_WINDOW = 11

    /** Candidate divisors for the remainder step. None of them is zero, by being written here. */
    private val DIVISORS = listOf(6, 7, 8, 9)

    /**
     * The shift. Chosen so the result is always two digits: the incoming
     * figure is at most 9, so the sum lands between 11 and 49.
     */
    private const val ADDEND_MIN = 10
    private const val ADDEND_MAX = 40

    /** The parting subtraction, comfortably under the smallest possible product. */
    private val TAILS = listOf(10, 15, 20)
}
