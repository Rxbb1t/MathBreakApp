package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * A journey, told in minutes.
 *
 * Step 1 is always the journey itself and step 5 always reaches back to
 * it. The three steps between them are drawn from the pools below, so
 * the same story asks a different question each time: the same journey
 * might be cut into spare minutes or measured against three hours, then
 * read as clock blocks or as the two figures on a display.
 *
 * Every bound exists to keep an answer whole and non-negative. The trip
 * is always longer than the parting subtraction, so step 5 cannot go
 * below zero whichever middle steps were dealt.
 */
internal object ClockChain : ChallengeChain {

    override fun build(random: Random, language: AppLanguage): DailyChallenge {
        val (departure, trip) = drawJourney(random)
        val opening = journeyStep(departure, trip)
        val second = pick(SECOND, opening.answer, random)
        val third = pick(THIRD, second.answer, random)
        val fourth = pick(FOURTH, third.answer, random)
        val finale = finaleStep(opening.answer, fourth.answer, random)
        return DailyChallenge(
            intro = if (en(language)) {
                "Five steps, and each answer feeds the next. Hold on to the Journey."
            } else {
                "Cinci pași, iar fiecare răspuns îl hrănește pe următorul. Ține minte Drumul."
            },
            stages = listOf(opening, second, third, fourth, finale).map { it.problem(language) },
        )
    }

    private fun drawJourney(random: Random): Pair<Int, Int> {
        val wholeHours = random.nextInt(1, 3)
        // Spare minutes first, so they can never be zero and the block
        // step always has a whole block to find. Whole hours on top.
        //
        // The one-hour journeys stop at 40 spare minutes so the total
        // never lands between 101 and 119. Romanian counts those without
        // "de" ("105 minute", not "105 de minute"), and the app's de()
        // helper does not know that; keeping the draw out of the band
        // means it never has to.
        val spareCeiling = if (wholeHours == 1) SPARE_MAX_SHORT else SPARE_MAX
        val spare = random.nextInt(SPARE_MIN, spareCeiling + 1)
        // Late enough to be a sensible hour, early enough that the
        // longest journey still lands the same day.
        val departure = random.nextInt(6, 19) * 60 + random.nextInt(0, 60)
        return departure to (wholeHours * 60 + spare)
    }

    private fun journeyStep(departure: Int, trip: Int) = ChainStep(
        answer = trip,
        text = { language ->
            if (en(language)) {
                "Step 1, the Journey. A train leaves at ${clockFace(departure)} " +
                    "and pulls in at ${clockFace(departure + trip)}. How many minutes was it?"
            } else {
                "Pasul 1, Drumul. Un tren pleacă la ${clockFace(departure)} " +
                    "și ajunge la ${clockFace(departure + trip)}. Câte minute a durat?"
            }
        },
        hint = { language ->
            if (en(language)) {
                "Count up to the next full hour first, then the whole hours, then the " +
                    "rest. Adding those three together is easier than one big subtraction."
            } else {
                "Numără întâi până la ora fixă următoare, apoi orele întregi, apoi restul. " +
                    "Aduni cele trei bucăți, e mai ușor decât o scădere mare."
            }
        },
        notes = ::clockNotes,
    )

    // ── Step 2: the Journey, cut down to under an hour ───────────────────

    /** The always-applicable variant is [spareMinutes]; see [pick]. */
    private val SECOND: List<StepSpec> = listOf(
        // What is left once the whole hours come out.
        { trip, _ ->
            val spare = trip % 60
            ChainStep(
                answer = spare,
                text = { language ->
                    if (en(language)) {
                        "Step 2. Take the whole hours out of the Journey. How many spare " +
                            "minutes are left over? That leftover is Y."
                    } else {
                        "Pasul 2. Scoate orele întregi din Drum. Câte minute rămân pe " +
                            "dinafară? Ce rămâne e Y."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "The Journey was $trip minutes. An hour is 60 minutes, so take out " +
                            "as many whole hours as will come out and read what is left."
                    } else {
                        "Drumul a fost de ${de(trip)} minute. O oră are 60 de minute, deci " +
                            "scoate atâtea ore întregi câte ies și citește ce rămâne."
                    }
                },
                notes = ::remainderNotes,
            )
        },
        // How far short of three hours it fell. Only the longer journeys
        // land close enough for the answer to stay under an hour.
        { trip, _ ->
            val short = 180 - trip
            if (short !in SPARE_MIN..SPARE_MAX) {
                null
            } else {
                ChainStep(
                    answer = short,
                    text = { language ->
                        if (en(language)) {
                            "Step 2. Three hours would have been 180 minutes. By how many " +
                                "minutes did the Journey fall short of that? Call it Y."
                        } else {
                            "Pasul 2. Trei ore ar fi însemnat 180 de minute. Cu câte minute " +
                                "a fost Drumul sub atât? Să-i zicem Y."
                        }
                    },
                    hint = { language ->
                        if (en(language)) {
                            "The Journey was $trip minutes. Take that away from 180 and read " +
                                "the difference."
                        } else {
                            "Drumul a fost de ${de(trip)} minute. Scade-l din 180 și citește " +
                                "diferența."
                        }
                    },
                    notes = ::clockNotes,
                )
            }
        },
    )

    // ── Step 3: those minutes, read as a small count ─────────────────────

    /** The always-applicable variant is [fiveBlocks]; see [pick]. */
    private val THIRD: List<StepSpec> = listOf(
        // Whole five-minute blocks, which is how a clock face is marked.
        { minutes, _ ->
            ChainStep(
                answer = minutes / 5,
                text = { language ->
                    if (en(language)) {
                        "Step 3. A clock face is marked in fives. How many whole " +
                            "five-minute blocks fit inside Y? That count is Z."
                    } else {
                        "Pasul 3. Cadranul ceasului e împărțit din 5 în 5. Câte grupe " +
                            "întregi de cinci minute încap în Y? Numărul lor e Z."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "Y was $minutes minutes. Whole blocks only: anything left over " +
                            "after the last full five does not count as a block."
                    } else {
                        "Y a fost de ${de(minutes)} minute. Doar grupe întregi: ce rămâne " +
                            "după ultima grupă de cinci nu se pune la socoteală."
                    }
                },
                notes = ::groupingNotes,
            )
        },
        // The two figures on a digital display, added. Nothing to add up
        // if the display is showing a single figure.
        { minutes, _ ->
            if (minutes < 10) {
                null
            } else {
                ChainStep(
                    answer = digitsOf(minutes),
                    text = { language ->
                        if (en(language)) {
                            "Step 3. The display shows Y as two figures. Stop reading it as " +
                                "a length of time and add those two figures together. That " +
                                "sum is Z."
                        } else {
                            "Pasul 3. Afișajul arată Y cu două cifre. Nu-l mai citi ca pe o " +
                                "durată și adună cele două cifre. Suma lor e Z."
                        }
                    },
                    hint = { language ->
                        if (en(language)) {
                            "Y was $minutes minutes. The two figures are the tens and the " +
                                "units, so a display of 34 gives 7."
                        } else {
                            "Y a fost de ${de(minutes)} minute. Cele două cifre sunt zecile " +
                                "și unitățile, deci un afișaj de 34 dă 7."
                        }
                    },
                    notes = ::digitNotes,
                )
            }
        },
    )

    // ── Step 4: that count, turned back into minutes ─────────────────────

    /** The always-applicable variant is [stops]; see [pick]. */
    private val FOURTH: List<StepSpec> = listOf(
        // One stop per block, each costing the same.
        { count, random ->
            val perStop = random.nextInt(3, 10)
            ChainStep(
                answer = count * perStop,
                text = { language ->
                    if (en(language)) {
                        "Step 4. The train makes one stop for every block, and each stop " +
                            "costs $perStop minutes. How many minutes is that altogether? " +
                            "Call it W."
                    } else {
                        "Pasul 4. Trenul oprește o dată pentru fiecare grupă, iar fiecare " +
                            "oprire ia câte $perStop minute. Câte minute fac în total? " +
                            "Să le zicem W."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "Z was $count. Each of those costs the same, so this is one " +
                            "multiplication rather than a long addition."
                    } else {
                        "Z a fost $count. Fiecare costă la fel, deci e o singură înmulțire, " +
                            "nu o adunare lungă."
                    }
                },
                notes = ::groupingNotes,
            )
        },
        // The way back: twice as many, and a few more on top.
        { count, random ->
            val extra = random.nextInt(3, 10)
            ChainStep(
                answer = 2 * count + extra,
                text = { language ->
                    if (en(language)) {
                        "Step 4. The return trip makes twice as many stops as Z, and then " +
                            "$extra more on top. How many stops is that? Call it W."
                    } else {
                        "Pasul 4. La întoarcere trenul oprește de două ori mai mult decât Z, " +
                            "iar apoi încă $extra opriri pe deasupra. Câte opriri fac? " +
                            "Să le zicem W."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "Z was $count. Double it first, then add the extra ones."
                    } else {
                        "Z a fost $count. Întâi dublează, apoi adaugă opririle în plus."
                    }
                },
                notes = ::groupingNotes,
            )
        },
    )

    private fun finaleStep(trip: Int, w: Int, random: Random): ChainStep {
        val tail = TAILS.random(random)
        return ChainStep(
            answer = trip + w - tail,
            text = { language ->
                if (en(language)) {
                    "Step 5, the finish. Add W to the Journey from step 1, " +
                        "then take away $tail. What are you left with?"
                } else {
                    "Pasul 5, finalul. Adună W la Drumul de la pasul 1, " +
                        "apoi scade $tail. Cu cât rămâi?"
                }
            },
            hint = { language ->
                if (en(language)) {
                    "W was $w and the Journey, all the way back at step 1, was $trip minutes."
                } else {
                    "W a fost $w, iar Drumul, tocmai de la pasul 1, a fost de ${de(trip)} minute."
                }
            },
            notes = ::chainNotes,
        )
    }

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

    /** Never under five, so the block step always finds a whole block. */
    private const val SPARE_MIN = 5
    private const val SPARE_MAX = 59

    /** Caps a one-hour journey at 100 minutes; see the draw. */
    private const val SPARE_MAX_SHORT = 40

    /** Comfortably under the shortest possible journey, which is 65 minutes. */
    private val TAILS = listOf(10, 15, 20, 25)
}
