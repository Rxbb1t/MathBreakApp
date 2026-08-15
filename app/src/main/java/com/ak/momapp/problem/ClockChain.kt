package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * A journey, told in minutes.
 *
 *  1. THE JOURNEY. Two times on a clock face, and the minutes between
 *     them. Everything else hangs off that.
 *  2. THE SPARE MINUTES. Take the whole hours out and see what is left.
 *  3. THE BLOCKS. Read those spare minutes as five-minute blocks, which
 *     is how anybody actually reads a clock.
 *  4. THE STOPS. Each block costs a few minutes.
 *  5. THE FINISH. Back to the journey from step one.
 *
 * Every bound below exists to keep an answer whole and non-negative:
 * the spare minutes are drawn first and never fall under five, so step 3
 * always has at least one block to find, and the journey is always longer
 * than the parting subtraction so step 5 cannot go below zero.
 */
internal object ClockChain : ChallengeChain {

    override fun build(random: Random, language: AppLanguage): DailyChallenge {
        val state = draw(random)
        return DailyChallenge(
            intro = if (en(language)) {
                "Five steps, and each answer feeds the next. Hold on to the Journey."
            } else {
                "Cinci pași, iar fiecare răspuns îl hrănește pe următorul. Ține minte Drumul."
            },
            stages = listOf(
                journeyStep(state, language),
                spareStep(state, language),
                blockStep(state, language),
                stopStep(state, language),
                finaleStep(state, language),
            ),
        )
    }

    private fun draw(random: Random): State {
        val wholeHours = random.nextInt(1, 3)
        // Spare minutes first, so they can never be zero and step 3 always
        // has a block to find. Whole hours are added on top.
        //
        // The one-hour journeys stop at 40 spare minutes so the total never
        // lands between 101 and 119. Romanian counts those without "de"
        // ("105 minute", not "105 de minute"), and the app's de() helper
        // does not know that; keeping the draw out of the band means it
        // never has to.
        val spareCeiling = if (wholeHours == 1) SPARE_MAX_SHORT else SPARE_MAX
        val spare = random.nextInt(SPARE_MIN, spareCeiling + 1)
        val trip = wholeHours * 60 + spare
        // Late enough to be a sensible hour, early enough that the longest
        // journey still lands the same day.
        val departure = random.nextInt(6, 19) * 60 + random.nextInt(0, 60)
        return State(
            departureMinutes = departure,
            trip = trip,
            spare = spare,
            perStop = random.nextInt(3, 10),
            tail = TAILS.random(random),
        )
    }

    private fun journeyStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 1, the Journey. A train leaves at ${clockFace(state.departureMinutes)} " +
                "and pulls in at ${clockFace(state.arrivalMinutes)}. How many minutes was it?"
        } else {
            "Pasul 1, Drumul. Un tren pleacă la ${clockFace(state.departureMinutes)} " +
                "și ajunge la ${clockFace(state.arrivalMinutes)}. Câte minute a durat?"
        },
        answer = state.trip,
        hint = if (en(language)) {
            "Count up to the next full hour first, then the whole hours, then the rest. " +
                "Adding those three together is easier than one big subtraction."
        } else {
            "Numără întâi până la ora fixă următoare, apoi orele întregi, apoi restul. " +
                "Aduni cele trei bucăți, e mai ușor decât o scădere mare."
        },
        notes = clockNotes(language),
        language = language,
    )

    private fun spareStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 2. Take the whole hours out of the Journey. How many spare minutes " +
                "are left over? That leftover is Y."
        } else {
            "Pasul 2. Scoate orele întregi din Drum. Câte minute rămân pe dinafară? " +
                "Ce rămâne e Y."
        },
        answer = state.spare,
        hint = if (en(language)) {
            "The Journey was ${state.trip} minutes. An hour is 60 minutes, so take out " +
                "as many whole 60s as will come out and read what is left."
        } else {
            "Drumul a fost de ${de(state.trip)} minute. O oră are 60 de minute, deci scoate " +
                "atâtea ore întregi câte ies și citește ce rămâne."
        },
        notes = remainderNotes(language),
        language = language,
    )

    private fun blockStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 3. A clock face is marked in fives. How many whole five-minute blocks " +
                "fit inside Y? That count is Z."
        } else {
            "Pasul 3. Cadranul ceasului e împărțit din 5 în 5. Câte grupe întregi de " +
                "cinci minute încap în Y? Numărul lor e Z."
        },
        answer = state.blocks,
        hint = if (en(language)) {
            "Y was ${state.spare} minutes. Whole blocks only: anything left over after " +
                "the last full five does not count as a block."
        } else {
            "Y a fost de ${de(state.spare)} minute. Doar grupe întregi: ce rămâne după " +
                "ultima grupă de cinci nu se pune la socoteală."
        },
        notes = groupingNotes(language),
        language = language,
    )

    private fun stopStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 4. The train makes one stop for every block, and each stop costs " +
                "${state.perStop} minutes. How many minutes is that altogether? Call it W."
        } else {
            "Pasul 4. Trenul oprește o dată pentru fiecare grupă, iar fiecare oprire ia " +
                "câte ${state.perStop} minute. Câte minute fac în total? Să le zicem W."
        },
        answer = state.stopped,
        hint = if (en(language)) {
            "Z was ${state.blocks}. Each of those costs the same, so this is one " +
                "multiplication rather than a long addition."
        } else {
            "Z a fost ${state.blocks}. Fiecare costă la fel, deci e o singură înmulțire, " +
                "nu o adunare lungă."
        },
        notes = groupingNotes(language),
        language = language,
    )

    private fun finaleStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 5, the finish. Add W to the Journey from step 1, " +
                "then take away ${state.tail}. What are you left with?"
        } else {
            "Pasul 5, finalul. Adună W la Drumul de la pasul 1, " +
                "apoi scade ${state.tail}. Cu cât rămâi?"
        },
        answer = state.finale,
        hint = if (en(language)) {
            "W was ${state.stopped} and the Journey, all the way back at step 1, " +
                "was ${state.trip} minutes."
        } else {
            "W a fost ${state.stopped}, iar Drumul, tocmai de la pasul 1, " +
                "a fost de ${de(state.trip)} minute."
        },
        notes = chainNotes(language),
        language = language,
    )

    private fun clockNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "An hour is 60 minutes, so a clock rolls over at 60 rather than at 100. " +
                    "From 9:50, ten minutes later is 10:00, not 9:60.",
                "To cross an hour, count to the next full hour and then carry on. " +
                    "From 9:50 to 11:20 is 10 minutes, then an hour, then 20.",
                "Times written like 14:05 are the same clock, just counted past twelve.",
            )
        } else {
            listOf(
                "O oră are 60 de minute, deci ceasul trece mai departe la 60, nu la 100. " +
                    "De la 9:50, peste zece minute e 10:00, nu 9:60.",
                "Ca să treci peste o oră, numără până la ora fixă și apoi mai departe. " +
                    "De la 9:50 la 11:20 sunt 10 minute, apoi o oră, apoi 20.",
                "Orele scrise ca 14:05 sunt același ceas, numărat doar mai departe de douăsprezece.",
            )
        }

    private data class State(
        val departureMinutes: Int,
        val trip: Int,
        val spare: Int,
        val perStop: Int,
        val tail: Int,
    ) {
        val arrivalMinutes: Int get() = departureMinutes + trip

        /** At least one, because the spare minutes never fall under five. */
        val blocks: Int get() = spare / 5
        val stopped: Int get() = blocks * perStop

        /** The journey alone already clears the tail, so this stays positive. */
        val finale: Int get() = trip + stopped - tail
    }

    /** Never under five, so step 3 always finds a whole block. */
    private const val SPARE_MIN = 5
    private const val SPARE_MAX = 59

    /** Caps a one-hour journey at 100 minutes; see the draw. */
    private const val SPARE_MAX_SHORT = 40

    /** Comfortably under the shortest possible journey, which is 65 minutes. */
    private val TAILS = listOf(10, 15, 20, 25)
}
