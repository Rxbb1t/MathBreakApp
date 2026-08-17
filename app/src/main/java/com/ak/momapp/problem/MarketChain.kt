package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * A morning behind a market stall.
 *
 * Step 1 is always the stock left on the table and step 5 always reaches
 * back to it. What happens between them is drawn: the apples might be
 * counted as full bags or as the ones left loose, priced plainly or with
 * a delivery charge on top, and the money read as change from a note or
 * as how many ten-euro notes it would cover.
 *
 * The stock never falls below twenty, because the finale multiplies it
 * and then subtracts, and a tiny stock is the one way that could go
 * below zero.
 */
internal object MarketChain : ChallengeChain {

    override fun build(random: Random, language: AppLanguage): DailyChallenge {
        val stall = drawStall(random)
        val opening = stockStep(stall)
        val second = pick(SECOND, opening.answer, random)
        val third = pick(THIRD, second.answer, random)
        val fourth = pick(FOURTH, third.answer, random)
        val finale = finaleStep(opening.answer, fourth.answer, random)
        return DailyChallenge(
            intro = if (en(language)) {
                "Five steps, and each answer feeds the next. Hold on to the Stock."
            } else {
                "Cinci pași, iar fiecare răspuns îl hrănește pe următorul. Ține minte Marfa."
            },
            stages = listOf(opening, second, third, fourth, finale).map { it.problem(language) },
        )
    }

    private data class Stall(val perCrate: Int, val crates: Int, val sold: Int) {
        val stock: Int get() = perCrate * crates - sold
    }

    private fun drawStall(random: Random): Stall {
        val perCrate = random.nextInt(8, 16)
        val crates = random.nextInt(4, 8)
        // Two bounds at once. Under twenty, so the Romanian reads
        // "vinde 19 mere" and needs no "de"; and never so many that
        // fewer than twenty apples survive.
        val sold = random.nextInt(5, minOf(20, perCrate * crates - STOCK_MIN + 1))
        return Stall(perCrate, crates, sold)
    }

    private fun stockStep(stall: Stall) = ChainStep(
        answer = stall.stock,
        text = { language ->
            if (en(language)) {
                "Step 1, the Stock. A crate holds ${stall.perCrate} apples. " +
                    "The stall takes in ${stall.crates} crates and sells ${stall.sold} " +
                    "apples. How many apples are left?"
            } else {
                "Pasul 1, Marfa. Într-o ladă intră ${stall.perCrate} mere. " +
                    "Taraba primește ${stall.crates} lăzi și vinde ${stall.sold} mere. " +
                    "Câte mere rămân?"
            }
        },
        hint = { language ->
            if (en(language)) {
                "Fill the crates first and count what came in, then take the sold ones " +
                    "off. Two steps, in that order."
            } else {
                "Umple întâi lăzile și numără cât a intrat, apoi scade ce s-a vândut. " +
                    "Doi pași, în ordinea asta."
            }
        },
        notes = ::marketNotes,
    )

    // ── Step 2: the stock, bagged up ─────────────────────────────────────

    /** The always-applicable variant is the full-bag count; see [pick]. */
    private val SECOND: List<StepSpec> = listOf(
        // What will not fill another bag. The bag size is drawn from the
        // ones that leave something behind: a size that divides the stock
        // exactly would hand the next step a zero to multiply.
        { stock, random ->
            val sizes = BAG_SIZES.filter { stock % it != 0 }
            if (sizes.isEmpty()) {
                null
            } else {
                val bag = sizes.random(random)
                ChainStep(
                    answer = stock % bag,
                    text = { language ->
                        if (en(language)) {
                            "Step 2. Those apples go into bags of $bag. Once every full bag " +
                                "is packed, how many are still loose? That leftover is Y."
                        } else {
                            "Pasul 2. Merele se pun în pungi de câte $bag. După ce se umplu " +
                                "toate pungile pline, câte rămân pe dinafară? Ce rămâne e Y."
                        }
                    },
                    hint = { language ->
                        if (en(language)) {
                            "The Stock was $stock apples. Take out as many whole bags as " +
                                "will come out, and read what will not fill another one."
                        } else {
                            "Marfa a fost de ${de(stock)} mere. Scoate atâtea pungi întregi " +
                                "câte ies și citește cât nu mai umple încă una."
                        }
                    },
                    notes = ::groupingNotes,
                )
            }
        },
        // How many bags actually got filled.
        { stock, random ->
            val bag = BAG_SIZES.random(random)
            ChainStep(
                answer = stock / bag,
                text = { language ->
                    if (en(language)) {
                        "Step 2. Those apples go into bags of $bag. How many full bags does " +
                            "that make? That count is Y."
                    } else {
                        "Pasul 2. Merele se pun în pungi de câte $bag. Câte pungi pline ies? " +
                            "Numărul lor e Y."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "The Stock was $stock apples. Full bags only: the ones left over at " +
                            "the end do not make a bag."
                    } else {
                        "Marfa a fost de ${de(stock)} mere. Doar pungi pline: cele rămase la " +
                            "urmă nu fac o pungă."
                    }
                },
                notes = ::groupingNotes,
            )
        },
    )

    // ── Step 3: turned into money ────────────────────────────────────────

    /** Both variants accept anything; see [pick]. */
    private val THIRD: List<StepSpec> = listOf(
        // Straight price each.
        { count, random ->
            val price = random.nextInt(2, 10)
            ChainStep(
                answer = count * price,
                text = { language ->
                    if (en(language)) {
                        "Step 3. Each one of those sells for $price euro. What do they come " +
                            "to altogether? Call it Z."
                    } else {
                        "Pasul 3. Fiecare dintre ele se vinde cu $price euro. Cât fac la un " +
                            "loc? Să-i zicem Z."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "Y was $count. They all cost the same, so this is one multiplication."
                    } else {
                        "Y a fost $count. Toate costă la fel, deci e o singură înmulțire."
                    }
                },
                notes = ::moneyNotes,
            )
        },
        // Price each, and a flat charge on the whole lot.
        { count, random ->
            val price = random.nextInt(2, 8)
            val carriage = random.nextInt(5, 16)
            ChainStep(
                answer = count * price + carriage,
                text = { language ->
                    if (en(language)) {
                        "Step 3. Each one of those sells for $price euro, and the buyer pays " +
                            "$carriage euro more to have the lot carried home. What is the " +
                            "bill? Call it Z."
                    } else {
                        "Pasul 3. Fiecare dintre ele se vinde cu $price euro, iar " +
                            "cumpărătorul mai plătește $carriage euro ca să i le ducă acasă. " +
                            "Cât face nota? Să-i zicem Z."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "Y was $count. Multiply first and add the carrying on at the end: " +
                            "it is charged once, not once each."
                    } else {
                        "Y a fost $count. Întâi înmulțește și adaugă transportul la final: " +
                            "se plătește o singură dată, nu pentru fiecare."
                    }
                },
                notes = ::moneyNotes,
            )
        },
    )

    // ── Step 4: the money, read back as a count ──────────────────────────

    /**
     * Between them these cover every bill. Change needs a note bigger
     * than the bill and whole notes need a bill of at least ten, and no
     * bill can miss both: the smallest bills are far under fifty and the
     * largest are far over ten.
     */
    private val FOURTH: List<StepSpec> = listOf(
        // Change from the smallest note that covers it.
        { bill, _ ->
            val note = NOTES.firstOrNull { it > bill }
            if (note == null) {
                null
            } else {
                ChainStep(
                    answer = note - bill,
                    text = { language ->
                        if (en(language)) {
                            "Step 4. The buyer pays with a note of $note euro. How much " +
                                "change comes back? Call it W."
                        } else {
                            "Pasul 4. Cumpărătorul plătește cu o bancnotă de ${de(note)} euro. " +
                                "Cât primește rest? Să-i zicem W."
                        }
                    },
                    hint = { language ->
                        if (en(language)) {
                            "Z was $bill euro. Change is what is left of the note once the " +
                                "bill has been taken out of it."
                        } else {
                            "Z a fost de ${de(bill)} euro. Restul e ce rămâne din bancnotă " +
                                "după ce scoți din ea cât face nota."
                        }
                    },
                    notes = ::moneyNotes,
                )
            }
        },
        // How much of it is whole ten-euro notes.
        { bill, _ ->
            if (bill < 10) {
                null
            } else {
                ChainStep(
                    answer = bill / 10,
                    text = { language ->
                        if (en(language)) {
                            "Step 4. The takings are counted out in ten-euro notes. How many " +
                                "whole tens does Z make? Call it W."
                        } else {
                            "Pasul 4. Încasările se numără în bancnote de zece euro. Câte " +
                                "grupe întregi de zece face Z? Să-i zicem W."
                        }
                    },
                    hint = { language ->
                        if (en(language)) {
                            "Z was $bill euro. Whole notes only, so the coins left over on " +
                                "top of the last ten are not another note."
                        } else {
                            "Z a fost de ${de(bill)} euro. Doar bancnote întregi, deci " +
                                "monedele rămase peste ultima zecime nu fac încă o bancnotă."
                        }
                    },
                    notes = ::groupingNotes,
                )
            }
        },
    )

    private fun finaleStep(stock: Int, w: Int, random: Random): ChainStep {
        val tail = TAILS.random(random)
        return ChainStep(
            answer = w * stock - tail,
            text = { language ->
                if (en(language)) {
                    "Step 5, the finish. Multiply W by the Stock from step 1, " +
                        "then take away $tail. What are you left with?"
                } else {
                    "Pasul 5, finalul. Înmulțește W cu Marfa de la pasul 1, " +
                        "apoi scade $tail. Cu cât rămâi?"
                }
            },
            hint = { language ->
                if (en(language)) {
                    "W was $w and the Stock, all the way back at step 1, was $stock apples."
                } else {
                    "W a fost $w, iar Marfa, tocmai de la pasul 1, a fost de ${de(stock)} mere."
                }
            },
            notes = ::chainNotes,
        )
    }

    private fun marketNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "Same-sized boxes multiply: 6 crates of 12 is 6 lots of 12, which is 72.",
                "What came in and what went out are different directions. " +
                    "Add the one, take away the other.",
                "Work out the whole load before taking anything off it, or the two " +
                    "steps get tangled.",
            )
        } else {
            listOf(
                "Cutiile la fel de mari se înmulțesc: 6 lăzi de câte 12 fac 6 grupe de 12, adică 72.",
                "Ce a intrat și ce a ieșit merg în direcții diferite. " +
                    "Pe una o aduni, pe cealaltă o scazi.",
                "Află întâi tot ce a intrat și abia apoi scade, altfel se încurcă pașii.",
            )
        }

    private fun moneyNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "Everything at the same price multiplies: 7 things at 4 euro is 28 euro.",
                "Change is a subtraction, not a division: the note take away the bill.",
                "A quick check: the change plus the bill has to come back to the note.",
            )
        } else {
            listOf(
                "Tot ce are același preț se înmulțește: 7 bucăți a câte 4 euro fac 28 de euro.",
                "Restul e o scădere, nu o împărțire: bancnota minus cât face nota de plată.",
                "O verificare scurtă: restul plus nota de plată trebuie să dea bancnota.",
            )
        }

    /**
     * The stock never falls below this. The finale multiplies it and
     * then subtracts, and the count coming out of step 4 can be as
     * little as 1, so a small stock is the one way it could go negative.
     */
    private const val STOCK_MIN = 20

    /** Bag sizes small enough that both bag variants have work to do. */
    private val BAG_SIZES = listOf(4, 5, 6, 7, 8, 9)

    /**
     * Real notes, in order, so the first one bigger than the bill is
     * also the one a person would actually hand over.
     */
    private val NOTES = listOf(50, 100, 200)

    /** Under the smallest possible product, which is 1 times a stock of 20. */
    private val TAILS = listOf(4, 5, 6)
}
