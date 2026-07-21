package com.ak.momapp.problem

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
}

/**
 * A single math problem. Answers are always non-negative whole numbers
 * so the input field only ever needs a plain number keyboard.
 */
data class Problem(
    val text: String,
    val answer: Int,
    val difficulty: Difficulty,
    val kind: ProblemKind = ProblemKind.ARITHMETIC,
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
) {
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
     * Extra thinking time when the countdown is on. Only the single
     * highest factor applies. They never stack.
     */
    val timerMultiplier: Double
        get() = when {
            difficulty == Difficulty.HARD -> 2.0
            kind == ProblemKind.LOGIC -> 1.5
            kind == ProblemKind.WORD || kind == ProblemKind.GEOMETRY ||
                kind == ProblemKind.MONEY || kind == ProblemKind.TIME -> 1.25
            else -> 1.0
        }
}
