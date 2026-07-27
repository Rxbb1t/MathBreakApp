package com.ak.momapp.problem

import kotlin.math.abs

enum class ProblemKind {
    /** Plain number chains: "24 + 7 × 3 − 5". */
    ARITHMETIC,

    /** Story problems built around a family name. */
    WORD,

    /** Solve for x (and sometimes y), incl. roots and small powers. */
    EQUATION,

    /** A grid of equations where shapes stand in for numbers. */
    PUZZLE,

    /** Short logic riddles with a numeric answer. */
    LOGIC,

    /** Story geometry: perimeter, area, angles, Pythagoras, volume. */
    GEOMETRY,

    /** Shopping stories in euros: totals, change, discounts, budgets. */
    MONEY,

    /** Clock stories: durations and conversions, answered in minutes. */
    TIME,

    /** Two expressions; she taps <, = or > instead of typing. */
    COMPARE,

    /** Pick the cards that add up to a target. Tapped, not typed. */
    TARGET,

    /** Tap every card that fits a rule: evens, multiples, the largest prime. */
    SELECT,

    /** Mulțimi: count the elements of A ∩ B, A ∪ B or A \ B. */
    SETS,

    /** A claim like "7 × 8 = 54"; she taps ✓ or ✗. */
    TRUE_FALSE,

    /** "12 ? 3 = 4": tap the operation sign that makes it true. */
    MISSING_OP,

    /**
     * "Roughly how much is 297 × 4?" Typed, and a near-enough answer
     * counts: see [Problem.tolerance].
     */
    ESTIMATE,
    ;

    /**
     * How much answering one of these says about her level, as a multiple
     * of an ordinary problem.
     *
     * Not every right answer is the same size of achievement, and treating
     * them alike meant a sitting of quick taps carried her along at the
     * same rate as a sitting of riddles. Tapping ✓ or ✗ on a claim is a
     * coin flip away from free; solving a two-unknown equation is not.
     *
     * The weight moves the CLIMB, and it moves a miss too, but never past
     * the ordinary amount (see [LevelLadder]). So the cheap exercises drift
     * gently in both directions, which is what weak evidence deserves,
     * while the demanding ones carry her up faster without also punishing
     * her harder for getting one wrong.
     */
    val effort: Double
        get() = when (this) {
            // Two choices: half of getting it right is knowing nothing.
            TRUE_FALSE -> 0.5
            // Three and four choices respectively.
            COMPARE -> 0.6
            MISSING_OP -> 0.7
            // A search rather than a calculation, and a shallow one.
            TARGET -> 0.7
            SELECT -> 0.8
            // The everyday middle: a story with a step or two in it.
            // Estimating sits here too: choosing what to round is real
            // thinking, but the arithmetic left afterwards is deliberately
            // easy, and being close is enough.
            ARITHMETIC, ESTIMATE -> 0.9
            WORD, MONEY, TIME, SETS -> 1.0
            // These are the ones worth being pleased about.
            GEOMETRY, PUZZLE -> 1.2
            LOGIC -> 1.3
            EQUATION -> 1.4
        }
}

/**
 * A single math problem. Answers are always non-negative whole numbers
 * so the input field only ever needs a plain number keyboard.
 */
data class Problem(
    val text: String,
    val answer: Int,
    /**
     * Where on the 0–100 scale this problem was dealt. The fine value, not
     * the band: two Normal problems 20 points apart are genuinely
     * different, and the tests that check operand sizes need to know which
     * one they are holding.
     */
    val level: Level,
    val kind: ProblemKind = ProblemKind.ARITHMETIC,
    /**
     * What solving this is worth on the ladder, defaulting to what the
     * [kind] is usually worth. A generator overrides it when it knows this
     * particular problem is easier or harder than its kind suggests: a
     * chain of nothing but additions is not the same work as one with a
     * division in it, and neither is a two-unknown equation the same as
     * finding x in one line.
     */
    val effort: Double = kind.effort,
    /**
     * Two nudges revealed one at a time by the Hint button; the third
     * press shows the answer itself. Baked in at generation time, in the
     * language that was active, just like [text].
     */
    val hints: List<String> = emptyList(),
    /**
     * The helper sheet: definitions, rules, and theorems that fit this
     * problem (Pythagoras for a ladder problem, the balance rule for an
     * equation, …). Shown in the left-edge drawer; empty means the
     * drawer stays hidden. Frozen in the generation language.
     */
    val notes: List<String> = emptyList(),
    /**
     * How the answer is actually reached, one step per line, in the
     * generation language.
     *
     * This is the one place step-by-step working is allowed. [hints] stay
     * free of it on purpose: a nudge mid-problem should point at what the
     * story means, not walk her through the buttons. A solution is only
     * ever offered once the problem is over, right or revealed, where
     * showing the steps teaches instead of doing it for her.
     *
     * Empty for problems whose working would just restate the question
     * (a tap on ✓/✗, a "which sign fits").
     */
    val solution: List<String> = emptyList(),
    /**
     * A schematic figure drawn under the text. Geometry problems carry
     * one. Labels come pre-rendered ("12 m", "?"), never the answer.
     */
    val diagram: Diagram? = null,
    /**
     * The unit the answer is measured in ("m", "°", "€", "min"),
     * shown inside the input field so the expected unit is never a
     * guess. Empty when the answer is a plain count. Language-blind.
     */
    val answerUnit: String = "",
    /**
     * The tappable numbers of a [ProblemKind.TARGET] problem; she picks
     * [pickCount] of them that sum to [answer]. Empty for other kinds.
     */
    val cards: List<Int> = emptyList(),
    val pickCount: Int = 0,
    /**
     * For [ProblemKind.SELECT]: the card values that must all be tapped,
     * no more and no fewer. A hunt's cards are distinct, so values name
     * them. Empty for every other kind.
     */
    val correctCards: Set<Int> = emptySet(),
    /**
     * What a reveal shows when the raw [answer] number would be cryptic:
     * the symbol for COMPARE (">"), one valid pick for TARGET
     * ("6 + 4 + 9"). Empty means the answer itself is the display.
     */
    val revealText: String = "",
    /**
     * How far from [answer] still counts as solved, as an absolute amount.
     *
     * Zero for every ordinary problem, where only the exact number will do,
     * and that is what makes this safe to add: [accepts] collapses to an
     * equality check everywhere it isn't wanted. Only [ProblemKind.ESTIMATE]
     * sets it, and it is stored as a flat amount rather than a percentage so
     * that deciding whether an answer counts is plain integer arithmetic,
     * with no rounding to disagree about between here and the tests.
     */
    val tolerance: Int = 0,
) {
    /** The band this problem's level falls in: the name she would see. */
    val difficulty: Difficulty
        get() = level.band

    /** Whether a typed [value] counts as solving this problem. */
    fun accepts(value: Int): Boolean = abs(value - answer) <= tolerance

    /**
     * Right, but not the exact number. Worth knowing so the feedback can
     * hand her the true value: getting close is the skill being practised,
     * and never being told what it was close TO would waste the moment.
     */
    fun isApproximate(value: Int): Boolean = accepts(value) && value != answer

    /** The answer as it should be shown to her. */
    val answerText: String
        get() = revealText.ifEmpty { answer.toString() }

    /** The answer arrives by tapping, so no keyboard or number field. */
    val tapAnswered: Boolean
        get() = kind == ProblemKind.COMPARE || kind == ProblemKind.TARGET ||
            kind == ProblemKind.SELECT || kind == ProblemKind.TRUE_FALSE ||
            kind == ProblemKind.MISSING_OP

    /** One tap is the whole answer: no Check button, the tap submits. */
    val submitsOnTap: Boolean
        get() = kind == ProblemKind.COMPARE || kind == ProblemKind.TRUE_FALSE ||
            kind == ProblemKind.MISSING_OP

    /**
     * Tries before the answer is shown.
     *
     * A ✓/✗ claim gets ONE. It is a coin flip, so a second try is not
     * another go at the problem, it is the answer handed over: whatever she
     * tapped first, the other button is now known to be right. The rest of
     * the tap kinds offer three or four choices, so one wrong guess still
     * leaves something to think about, and typed answers keep all three.
     */
    val maxAttempts: Int
        get() = when {
            kind == ProblemKind.TRUE_FALSE -> 1
            submitsOnTap -> 2
            else -> 3
        }

}
