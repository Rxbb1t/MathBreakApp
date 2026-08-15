package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * A rug, a mat, and a tape measure.
 *
 *  1. THE BORDER. All the way round a rectangle. Everything else hangs
 *     off that.
 *  2. THE SIDE. A square with the same border, so one side is a quarter
 *     of it.
 *  3. THE AREA. That square's area, which is the one step that grows
 *     rather than shrinks.
 *  4. THE HUNDREDS. Read the area in whole hundreds.
 *  5. THE FINISH. Back to the border from step one.
 *
 * The two sides are drawn with the same parity, which makes the border a
 * multiple of four, which is what lets step 2 divide exactly. Without
 * that the square would have a fractional side and the whole chain would
 * need a decimal point the keypad does not have.
 */
internal object WorkshopChain : ChallengeChain {

    override fun build(random: Random, language: AppLanguage): DailyChallenge {
        val state = draw(random)
        return DailyChallenge(
            intro = if (en(language)) {
                "Five steps, and each answer feeds the next. Hold on to the Border."
            } else {
                "Cinci pași, iar fiecare răspuns îl hrănește pe următorul. Ține minte Conturul."
            },
            stages = listOf(
                borderStep(state, language),
                sideStep(state, language),
                areaStep(state, language),
                hundredsStep(state, language),
                finaleStep(state, language),
            ),
        )
    }

    private fun draw(random: Random): State {
        // Same parity on both sides, so their sum is even, so twice their
        // sum divides by four and step 2 comes out whole.
        val even = random.nextBoolean()
        val length = drawSide(random, even)
        var width = drawSide(random, even)
        // A square rug would make steps 1 and 2 the same question asked
        // twice, so the two sides are always different.
        while (width == length) width = drawSide(random, even)
        return State(
            length = maxOf(length, width),
            width = minOf(length, width),
            tail = TAILS.random(random),
        )
    }

    private fun drawSide(random: Random, even: Boolean): Int {
        val half = random.nextInt(SIDE_MIN / 2, SIDE_MAX / 2 + 1)
        return if (even) half * 2 else half * 2 + 1
    }

    private fun borderStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 1, the Border. A rug is ${state.length} cm long and ${state.width} cm wide. " +
                "How far is it all the way round?"
        } else {
            "Pasul 1, Conturul. Un covor are ${state.length} cm lungime și ${state.width} cm " +
                "lățime. Cât e de jur împrejur?"
        },
        answer = state.border,
        hint = if (en(language)) {
            "A rectangle has two long sides and two short ones. Add one of each first, " +
                "then double it."
        } else {
            "Un dreptunghi are două laturi lungi și două scurte. Adună întâi una din " +
                "fiecare, apoi dublează."
        },
        notes = borderNotes(language),
        language = language,
    )

    private fun sideStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 2. A square mat has exactly that same distance round. " +
                "How long is one of its sides? That length is Y."
        } else {
            "Pasul 2. Un preș pătrat are exact același contur. " +
                "Cât are una dintre laturile lui? Acea lungime e Y."
        },
        answer = state.side,
        hint = if (en(language)) {
            "The Border was ${state.border} cm. A square's four sides are all the same, " +
                "so they share it out equally."
        } else {
            "Conturul a fost de ${state.border} cm. Cele patru laturi ale unui pătrat sunt " +
                "egale, deci își împart conturul în mod egal."
        },
        notes = squareNotes(language),
        language = language,
    )

    private fun areaStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 3. How much floor does that square mat cover, " +
                "in square centimetres? Call it Z."
        } else {
            "Pasul 3. Ce suprafață acoperă preșul acela pătrat, " +
                "în centimetri pătrați? Să-i zicem Z."
        },
        answer = state.area,
        hint = if (en(language)) {
            "Y was ${state.side} cm. Area is one side along multiplied by the other side " +
                "down, and on a square those are the same number."
        } else {
            "Y a fost de ${state.side} cm. Aria e o latură înmulțită cu cealaltă, " +
                "iar la un pătrat cele două sunt același număr."
        },
        notes = areaNotes(language),
        language = language,
    )

    private fun hundredsStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 4. How many whole hundreds fit inside Z? Call that count W."
        } else {
            "Pasul 4. Câte sute întregi încap în Z? Numărul lor e W."
        },
        answer = state.hundreds,
        hint = if (en(language)) {
            "Z was ${state.area}. Whole hundreds only, so whatever is left below a " +
                "hundred is left behind rather than rounded up."
        } else {
            "Z a fost ${state.area}. Doar sute întregi, deci ce rămâne sub o sută " +
                "rămâne pe dinafară, nu se rotunjește în sus."
        },
        notes = groupingNotes(language),
        language = language,
    )

    private fun finaleStep(state: State, language: AppLanguage): Problem = stage(
        text = if (en(language)) {
            "Step 5, the finish. Multiply W by the Border from step 1, " +
                "then take away ${state.tail}. What are you left with?"
        } else {
            "Pasul 5, finalul. Înmulțește W cu Conturul de la pasul 1, " +
                "apoi scade ${state.tail}. Cu cât rămâi?"
        },
        answer = state.finale,
        hint = if (en(language)) {
            "W was ${state.hundreds} and the Border, all the way back at step 1, " +
                "was ${state.border} cm."
        } else {
            "W a fost ${state.hundreds}, iar Conturul, tocmai de la pasul 1, " +
                "a fost de ${state.border} cm."
        },
        notes = chainNotes(language),
        language = language,
    )

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
                "On a square both sides are the same, so the area is that side times itself. " +
                    "A side of 7 covers 49.",
                "Area grows fast. Doubling a side does not double the area, it quadruples it.",
            )
        } else {
            listOf(
                "Aria e cât din suprafață e acoperit, numărat în pătrățele: " +
                    "un dreptunghi de 6 pe 4 cuprinde 24 de pătrățele.",
                "La un pătrat ambele laturi sunt egale, deci aria e latura înmulțită cu ea " +
                    "însăși. O latură de 7 acoperă 49.",
                "Aria crește repede. Dacă dublezi latura, aria nu se dublează, ci se face de patru ori.",
            )
        }

    private data class State(
        val length: Int,
        val width: Int,
        val tail: Int,
    ) {
        val border: Int get() = 2 * (length + width)

        /** Exact, because the two sides share a parity; see the draw. */
        val side: Int get() = border / 4
        val area: Int get() = side * side

        /**
         * At least 2, because the smallest possible side is 17 and 17
         * squared already clears two hundred.
         *
         * Hundreds rather than tens on purpose: tens would leave the last
         * step multiplying a three-figure number by another one, which is
         * a different and much heavier exercise than the other themes ask
         * for. This keeps every finale in the same league.
         */
        val hundreds: Int get() = area / 100
        val finale: Int get() = hundreds * border - tail
    }

    /**
     * Sides big enough that the square's area always clears 100, so step 4
     * always has whole tens to find, and small enough that the finale stays
     * a number she can hold.
     */
    private const val SIDE_MIN = 16
    private const val SIDE_MAX = 46

    /** Far under the smallest possible product, which is 2 hundreds times a 68 border. */
    private val TAILS = listOf(10, 15, 20, 25)
}
