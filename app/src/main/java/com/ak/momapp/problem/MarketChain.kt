package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * A morning behind a market stall.
 *
 *  1. THE STOCK. Crates in, some sold, what is left. Everything else
 *     hangs off that.
 *  2. THE LOOSE ONES. Bag up what will bag up, and see what will not.
 *  3. WHAT THEY COME TO. The loose ones at a price each.
 *  4. THE CHANGE. Paid with a note, so the answer is a subtraction.
 *  5. THE FINISH. Back to the stock from step one.
 *
 * The note is always bigger than the bill, so the change is never
 * negative, and the loose count is never zero, so step 3 is never a
 * multiplication by nothing.
 */
internal object MarketChain : ChallengeChain {

    override fun build(random: Random, language: AppLanguage): DailyChallenge {
        val state = draw(random)
        return DailyChallenge(
            intro = if (en(language)) {
                "Five steps, and each answer feeds the next. Hold on to the Stock."
            } else {
                "Cinci pași, iar fiecare răspuns îl hrănește pe următorul. Ține minte Marfa."
            },
            stages = listOf(
                stockStep(state, language),
                looseStep(state, language),
                worthStep(state, language),
                changeStep(state, language),
                finaleStep(state, language),
            ),
        )
    }

    private fun draw(random: Random): State {
        val perCrate = random.nextInt(8, 16)
        val crates = random.nextInt(4, 8)
        // Two bounds at once. Under twenty, so the Romanian reads
        // "vinde 19 mere" and needs no "de"; and never so many that fewer
        // than twenty apples survive, because the finale multiplies the
        // stock and then subtracts, and a tiny stock could take that
        // below zero.
        val sold = random.nextInt(5, minOf(20, perCrate * crates - STOCK_MIN + 1))
        val stock = perCrate * crates - sold

        // Redrawn against the stock so the leftovers are never nothing:
        // a bag size that divides the stock exactly would leave step 3
        // multiplying by zero and step 4 handing back the whole note.
        val bag = BAG_SIZES.filter { stock % it != 0 }.random(random)
        val loose = stock % bag

        val price = random.nextInt(2, 10)
        val worth = loose * price
        // The smallest note that still leaves change to count.
        val note = NOTES.first { it > worth }

        return State(
            perCrate = perCrate,
            crates = crates,
            sold = sold,
            bag = bag,
            price = price,
            note = note,
            tail = TAILS.random(random),
        )
    }

    private fun stockStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 1, the Stock. A crate holds ${state.perCrate} apples. " +
                "The stall takes in ${state.crates} crates and sells ${state.sold} apples. " +
                "How many apples are left?"
        } else {
            "Pasul 1, Marfa. Într-o ladă intră ${state.perCrate} mere. " +
                "Taraba primește ${state.crates} lăzi și vinde ${state.sold} mere. " +
                "Câte mere rămân?"
        },
        answer = state.stock,
        hint = if (en(language)) {
            "Fill the crates first and count what came in, then take the sold ones off. " +
                "Two steps, in that order."
        } else {
            "Umple întâi lăzile și numără cât a intrat, apoi scade ce s-a vândut. " +
                "Doi pași, în ordinea asta."
        },
        notes = marketNotes(language),
        language = language,
    )

    private fun looseStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 2. Those apples go into bags of ${state.bag}. Once every full bag is " +
                "packed, how many are still loose? That leftover is Y."
        } else {
            "Pasul 2. Merele se pun în pungi de câte ${state.bag}. După ce se umplu toate " +
                "pungile pline, câte rămân pe dinafară? Ce rămâne e Y."
        },
        answer = state.loose,
        hint = if (en(language)) {
            "The Stock was ${state.stock} apples. Take out as many whole bags as will " +
                "come out, and read what will not fill another one."
        } else {
            "Marfa a fost de ${de(state.stock)} mere. Scoate atâtea pungi întregi câte ies " +
                "și citește cât nu mai umple încă una."
        },
        notes = groupingNotes(language),
        language = language,
    )

    private fun worthStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 3. The loose ones are sold off at ${state.price} euro each. " +
                "What do they come to? Call it Z."
        } else {
            "Pasul 3. Cele rămase se vând cu câte ${state.price} euro bucata. " +
                "Cât fac la un loc? Să-i zicem Z."
        },
        answer = state.worth,
        hint = if (en(language)) {
            "Y was ${state.loose}. They all cost the same, so this is one multiplication."
        } else {
            "Y a fost ${state.loose}. Toate costă la fel, deci e o singură înmulțire."
        },
        notes = moneyNotes(language),
        language = language,
    )

    private fun changeStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 4. The buyer pays with a note of ${state.note} euro. " +
                "How much change comes back? Call it W."
        } else {
            "Pasul 4. Cumpărătorul plătește cu o bancnotă de ${de(state.note)} euro. " +
                "Cât primește rest? Să-i zicem W."
        },
        answer = state.change,
        hint = if (en(language)) {
            "Z was ${state.worth} euro. Change is what is left of the note once the bill " +
                "has been taken out of it."
        } else {
            "Z a fost de ${de(state.worth)} euro. Restul e ce rămâne din bancnotă după ce " +
                "scoți din ea cât face marfa."
        },
        notes = moneyNotes(language),
        language = language,
    )

    private fun finaleStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 5, the finish. Multiply W by the Stock from step 1, " +
                "then take away ${state.tail}. What are you left with?"
        } else {
            "Pasul 5, finalul. Înmulțește W cu Marfa de la pasul 1, " +
                "apoi scade ${state.tail}. Cu cât rămâi?"
        },
        answer = state.finale,
        hint = if (en(language)) {
            "W was ${state.change} and the Stock, all the way back at step 1, " +
                "was ${state.stock} apples."
        } else {
            "W a fost ${state.change}, iar Marfa, tocmai de la pasul 1, " +
                "a fost de ${de(state.stock)} mere."
        },
        notes = chainNotes(language),
        language = language,
    )

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
                "Restul e o scădere, nu o împărțire: bancnota minus cât face marfa.",
                "O verificare scurtă: restul plus cât face marfa trebuie să dea bancnota.",
            )
        }

    private data class State(
        val perCrate: Int,
        val crates: Int,
        val sold: Int,
        val bag: Int,
        val price: Int,
        val note: Int,
        val tail: Int,
    ) {
        val stock: Int get() = perCrate * crates - sold

        /** Never zero: the bag size was drawn from the ones that do not divide the stock. */
        val loose: Int get() = stock % bag
        val worth: Int get() = loose * price

        /** Never negative: the note was drawn as the first one bigger than the bill. */
        val change: Int get() = note - worth
        val finale: Int get() = change * stock - tail
    }

    /**
     * The stock never falls below this. The finale multiplies it and then
     * subtracts, and the change can be as little as 1, so a small stock is
     * the one way that last step could go negative.
     */
    private const val STOCK_MIN = 20

    /** Bag sizes small enough that something is nearly always left over. */
    private val BAG_SIZES = listOf(4, 5, 6, 7, 8, 9)

    /**
     * Real notes, in order, so the first one bigger than the bill is also
     * the one a person would actually hand over. The largest possible bill
     * is 8 loose at 9 euro, so 100 always covers it.
     */
    private val NOTES = listOf(50, 100)

    /** Under the smallest possible product, which is change 1 times stock 20. */
    private val TAILS = listOf(4, 5, 6)
}
