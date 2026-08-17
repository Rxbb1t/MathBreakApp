package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * A rug, a mat, and a tape measure.
 *
 * Step 1 is always the border and step 5 always reaches back to it. In
 * between, the border might become a square's side or half its own
 * length, that length might be squared or run out as a strip, and the
 * area might be read in hundreds or in whole tiles.
 *
 * Both sides of the rug are drawn with the same parity, which makes the
 * border a multiple of four, which is what lets the side step divide
 * exactly. Without it the square would have a fractional side and the
 * chain would need a decimal point the keypad does not have.
 *
 * The bounds also keep the area from running away. Squaring grows fast,
 * so the square step declines a length over fifty and the strip step
 * never runs narrower than six, which together hold every area inside a
 * band the last step can still multiply by hand.
 */
internal object WorkshopChain : ChallengeChain {

    override fun build(random: Random, language: AppLanguage): DailyChallenge {
        val rug = drawRug(random)
        val opening = borderStep(rug)
        val second = pick(SECOND, opening.answer, random)
        val third = pick(THIRD, second.answer, random)
        val fourth = pick(FOURTH, third.answer, random)
        val finale = finaleStep(opening.answer, fourth.answer, random)
        return DailyChallenge(
            intro = if (en(language)) {
                "Five steps, and each answer feeds the next. Hold on to the Border."
            } else {
                "Cinci pași, iar fiecare răspuns îl hrănește pe următorul. Ține minte Conturul."
            },
            stages = listOf(opening, second, third, fourth, finale).map { it.problem(language) },
        )
    }

    private data class Rug(val length: Int, val width: Int) {
        val border: Int get() = 2 * (length + width)
    }

    private fun drawRug(random: Random): Rug {
        // Same parity on both sides, so their sum is even, so twice their
        // sum divides by four and the side step comes out whole.
        val even = random.nextBoolean()
        val length = drawSide(random, even)
        var width = drawSide(random, even)
        // A square rug would make steps 1 and 2 the same question asked
        // twice, so the two sides are always different.
        while (width == length) width = drawSide(random, even)
        return Rug(maxOf(length, width), minOf(length, width))
    }

    private fun drawSide(random: Random, even: Boolean): Int {
        val half = random.nextInt(SIDE_MIN / 2, SIDE_MAX / 2 + 1)
        return if (even) half * 2 else half * 2 + 1
    }

    private fun borderStep(rug: Rug) = ChainStep(
        answer = rug.border,
        text = { language ->
            if (en(language)) {
                "Step 1, the Border. A rug is ${rug.length} cm long and ${rug.width} cm " +
                    "wide. How far is it all the way round?"
            } else {
                "Pasul 1, Conturul. Un covor are ${rug.length} cm lungime și ${rug.width} cm " +
                    "lățime. Cât e de jur împrejur?"
            }
        },
        hint = { language ->
            if (en(language)) {
                "A rectangle has two long sides and two short ones. Add one of each " +
                    "first, then double it."
            } else {
                "Un dreptunghi are două laturi lungi și două scurte. Adună întâi una din " +
                    "fiecare, apoi dublează."
            }
        },
        notes = ::borderNotes,
    )

    // ── Step 2: the border, turned into a length ─────────────────────────

    /** Both variants accept anything; the border is always a multiple of four. */
    private val SECOND: List<StepSpec> = listOf(
        // A square mat with the same distance round.
        { border, _ ->
            ChainStep(
                answer = border / 4,
                text = { language ->
                    if (en(language)) {
                        "Step 2. A square mat has exactly that same distance round. " +
                            "How long is one of its sides? That length is Y."
                    } else {
                        "Pasul 2. Un preș pătrat are exact același contur. " +
                            "Cât are una dintre laturile lui? Acea lungime e Y."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "The Border was $border cm. A square's four sides are all the same, " +
                            "so they share it out equally."
                    } else {
                        "Conturul a fost de $border cm. Cele patru laturi ale unui pătrat " +
                            "sunt egale, deci își împart conturul în mod egal."
                    }
                },
                notes = ::squareNotes,
            )
        },
        // One long side and one short side together.
        { border, _ ->
            ChainStep(
                answer = border / 2,
                text = { language ->
                    if (en(language)) {
                        "Step 2. Half the Border is one long side and one short side put " +
                            "together. How long is that? Call it Y."
                    } else {
                        "Pasul 2. Jumătate din Contur înseamnă o latură lungă și una scurtă " +
                            "la un loc. Cât fac împreună? Să-i zicem Y."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "The Border was $border cm, and it goes round both sides twice. " +
                            "Half of it covers each of them once."
                    } else {
                        "Conturul a fost de $border cm și trece de două ori pe lângă fiecare " +
                            "latură. Jumătate din el le acoperă pe fiecare o dată."
                    }
                },
                notes = ::borderNotes,
            )
        },
    )

    // ── Step 3: that length, turned into an area ─────────────────────────

    /** The always-applicable variant is the strip; see [pick]. */
    private val THIRD: List<StepSpec> = listOf(
        // A square on that side. Declined past fifty, where squaring
        // starts producing numbers the last step cannot carry.
        { side, _ ->
            if (side > MAX_SQUARE_SIDE) {
                null
            } else {
                ChainStep(
                    answer = side * side,
                    text = { language ->
                        if (en(language)) {
                            "Step 3. A square is cut with sides of Y. How much floor does it " +
                                "cover, in square centimetres? Call it Z."
                        } else {
                            "Pasul 3. Se taie un pătrat cu laturile de Y. Ce suprafață " +
                                "acoperă, în centimetri pătrați? Să-i zicem Z."
                        }
                    },
                    hint = { language ->
                        if (en(language)) {
                            "Y was $side cm. Area is one side along multiplied by the other " +
                                "side down, and on a square those are the same number."
                        } else {
                            "Y a fost de $side cm. Aria e o latură înmulțită cu cealaltă, " +
                                "iar la un pătrat cele două sunt același număr."
                        }
                    },
                    notes = ::areaNotes,
                )
            }
        },
        // A runner: that long, and a few centimetres wide.
        { side, random ->
            val width = random.nextInt(STRIP_MIN_WIDTH, STRIP_MAX_WIDTH + 1)
            ChainStep(
                answer = side * width,
                text = { language ->
                    if (en(language)) {
                        "Step 3. A runner is cut Y long and $width cm wide. How much floor " +
                            "does it cover, in square centimetres? Call it Z."
                    } else {
                        "Pasul 3. Se taie o fâșie lungă de Y și lată de $width cm. Ce " +
                            "suprafață acoperă, în centimetri pătrați? Să-i zicem Z."
                    }
                },
                hint = { language ->
                    if (en(language)) {
                        "Y was $side cm. Area is the length multiplied by the width, " +
                            "whichever way round you do it."
                    } else {
                        "Y a fost de $side cm. Aria e lungimea înmulțită cu lățimea, " +
                            "în orice ordine ai face-o."
                    }
                },
                notes = ::areaNotes,
            )
        },
    )

    // ── Step 4: the area, read back as a count ───────────────────────────

    /**
     * The tile variant always has a size that fits, because the strip is
     * never narrower than six and the smallest square is bigger still,
     * so no area this chain can produce falls under a hundred.
     */
    private val FOURTH: List<StepSpec> = listOf(
        { area, _ ->
            if (area < 100) {
                null
            } else {
                ChainStep(
                    answer = area / 100,
                    text = { language ->
                        if (en(language)) {
                            "Step 4. How many whole hundreds fit inside Z? Call that count W."
                        } else {
                            "Pasul 4. Câte sute întregi încap în Z? Numărul lor e W."
                        }
                    },
                    hint = { language ->
                        if (en(language)) {
                            "Z was $area. Whole hundreds only, so whatever is left below a " +
                                "hundred is left behind rather than rounded up."
                        } else {
                            "Z a fost $area. Doar sute întregi, deci ce rămâne sub o sută " +
                                "rămâne pe dinafară, nu se rotunjește în sus."
                        }
                    },
                    notes = ::groupingNotes,
                )
            }
        },
        { area, random ->
            val sizes = TILE_SIZES.filter { it <= area }
            if (sizes.isEmpty()) {
                null
            } else {
                val tile = sizes.random(random)
                ChainStep(
                    answer = area / tile,
                    text = { language ->
                        if (en(language)) {
                            "Step 4. Tiles of $tile square centimetres are laid over it. How " +
                                "many whole tiles fit inside Z? Call that count W."
                        } else {
                            "Pasul 4. Peste ea se pun plăci de câte ${de(tile)} centimetri " +
                                "pătrați. Câte plăci întregi încap în Z? Numărul lor e W."
                        }
                    },
                    hint = { language ->
                        if (en(language)) {
                            "Z was $area. Whole tiles only: a piece of floor too small for " +
                                "another whole tile is left bare."
                        } else {
                            "Z a fost $area. Doar plăci întregi: o bucată de podea prea mică " +
                                "pentru încă o placă întreagă rămâne goală."
                        }
                    },
                    notes = ::groupingNotes,
                )
            }
        },
    )

    private fun finaleStep(border: Int, w: Int, random: Random): ChainStep {
        val tail = TAILS.random(random)
        return ChainStep(
            answer = w * border - tail,
            text = { language ->
                if (en(language)) {
                    "Step 5, the finish. Multiply W by the Border from step 1, " +
                        "then take away $tail. What are you left with?"
                } else {
                    "Pasul 5, finalul. Înmulțește W cu Conturul de la pasul 1, " +
                        "apoi scade $tail. Cu cât rămâi?"
                }
            },
            hint = { language ->
                if (en(language)) {
                    "W was $w and the Border, all the way back at step 1, was $border cm."
                } else {
                    "W a fost $w, iar Conturul, tocmai de la pasul 1, a fost de $border cm."
                }
            },
            notes = ::chainNotes,
        )
    }

    private fun borderNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "All the way round a rectangle is the long side twice and the short side " +
                    "twice: 8 by 3 goes round in 8 + 3 + 8 + 3, which is 22.",
                "Adding one long and one short first and then doubling is the same sum " +
                    "with less to carry.",
                "Going round is measured in plain centimetres. It is a length, not an area.",
            )
        } else {
            listOf(
                "De jur împrejurul unui dreptunghi înseamnă latura lungă de două ori și cea " +
                    "scurtă de două ori: la 8 pe 3 iese 8 + 3 + 8 + 3, adică 22.",
                "Aduni întâi o latură lungă și una scurtă, apoi dublezi: aceeași sumă, " +
                    "dar cu mai puțin de ținut minte.",
                "Conturul se măsoară în centimetri simpli. E o lungime, nu o arie.",
            )
        }

    private fun squareNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "A square has four sides the same length, so its border shares out into " +
                    "four equal pieces: a border of 28 gives sides of 7.",
                "Sharing equally is a division, and here it is always by four.",
            )
        } else {
            listOf(
                "Un pătrat are patru laturi egale, deci conturul lui se împarte în patru " +
                    "bucăți egale: un contur de 28 dă laturi de 7.",
                "Împărțirea egală e o împărțire, iar aici e mereu la patru.",
            )
        }

    private fun areaNotes(language: AppLanguage): List<String> =
        if (en(language)) {
            listOf(
                "Area is how much surface is covered, counted in little squares: " +
                    "a 6 by 4 rectangle holds 24 of them.",
                "On a square both sides are the same, so the area is that side times " +
                    "itself. A side of 7 covers 49.",
                "Area grows fast. Doubling a side does not double the area, it quadruples it.",
            )
        } else {
            listOf(
                "Aria e cât din suprafață e acoperit, numărat în pătrățele: " +
                    "un dreptunghi de 6 pe 4 cuprinde 24 de pătrățele.",
                "La un pătrat ambele laturi sunt egale, deci aria e latura înmulțită cu ea " +
                    "însăși. O latură de 7 acoperă 49.",
                "Aria crește repede. Dacă dublezi latura, aria nu se dublează, ci se face " +
                    "de patru ori.",
            )
        }

    /**
     * Sides big enough that every area clears a hundred, and small
     * enough that the finale stays a number she can hold.
     */
    private const val SIDE_MIN = 16
    private const val SIDE_MAX = 46

    /** Past this a squared side outgrows what the last step can multiply. */
    private const val MAX_SQUARE_SIDE = 50

    /** Never narrow enough to leave an area under a hundred. */
    private const val STRIP_MIN_WIDTH = 6
    private const val STRIP_MAX_WIDTH = 12

    private val TILE_SIZES = listOf(100, 200)

    /** Far under the smallest possible product, which is one whole hundred times a 68 border. */
    private val TAILS = listOf(10, 15, 20, 25)
}
