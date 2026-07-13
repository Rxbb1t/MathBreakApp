package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * Unknown-value problems for MEDIUM and HARD: solve for x (sometimes with
 * a second unknown y), with simple square roots and small powers mixed in.
 * HARD also deals simple derivatives, evaluated at a small whole point.
 *
 * Guarantees: every answer and every number shown is a non-negative whole
 * number; at MEDIUM nothing on screen or in the answer passes 3500.
 *
 * Hints clarify what the symbols mean or what is being asked — they never
 * walk through the solving steps. The rules themselves (balance, roots,
 * powers) ride along in [Problem.notes] for the helper sheet.
 */
class EquationGenerator(private val random: Random) {

    fun generate(difficulty: Difficulty, language: AppLanguage): Problem = when (difficulty) {
        Difficulty.HARD -> rollHard(language)
        else -> rollMedium(language)
    }

    // ── MEDIUM: 3–5 numbers, one or two unknowns, everything ≤ 3500 ─────

    private fun rollMedium(language: AppLanguage): Problem = when (random.nextInt(5)) {
        0, 1 -> findX(language)
        2 -> sumDifferenceSystem(language, maxSmaller = 1200, askProduct = false)
        3 -> substitutionSystem(language)
        else -> rootOrPower(language)
    }

    /** "3x + 7 = 25" — the bread and butter. */
    private fun findX(language: AppLanguage): Problem {
        val a = random.nextInt(2, 10)
        val x = random.nextInt(2, 13)
        val plus = random.nextBoolean()
        // The minus variant keeps the right-hand side positive.
        val b = if (plus) random.nextInt(1, 3001) else random.nextInt(1, a * x)
        val rhs = if (plus) a * x + b else a * x - b
        val text = if (plus) {
            "${findXLabel(language)}\n${a}x + $b = $rhs"
        } else {
            "${findXLabel(language)}\n${a}x − $b = $rhs"
        }
        return Problem(
            text = text,
            answer = x,
            difficulty = Difficulty.MEDIUM,
            kind = ProblemKind.EQUATION,
            hints = listOf(shorthandHint(a, language), HintText.digits(x, language)),
            notes = listOf(balanceNote(language), shorthandNote(language)),
        )
    }

    /**
     * "x + y = 24, x − y = 6" — ask for x, y, or (at HARD) their product.
     */
    private fun sumDifferenceSystem(
        language: AppLanguage,
        maxSmaller: Int,
        askProduct: Boolean,
    ): Problem {
        val y = random.nextInt(1, maxSmaller + 1)
        val d = random.nextInt(2, 16)
        val x = y + d
        val s = x + y
        val restateHint = when (language) {
            AppLanguage.ENGLISH -> "Two mystery numbers: together they make $s, and one is $d more than the other."
            AppLanguage.ROMANIAN -> "Două numere-mister: împreună fac $s, iar unul e cu $d mai mare decât celălalt."
        }
        val (question, answer) = when {
            askProduct -> "x × y = ?" to x * y
            random.nextBoolean() -> "x = ?" to x
            else -> "y = ?" to y
        }
        return Problem(
            text = "x + y = $s\nx − y = $d\n$question",
            answer = answer,
            difficulty = if (askProduct) Difficulty.HARD else Difficulty.MEDIUM,
            kind = ProblemKind.EQUATION,
            hints = listOf(restateHint, HintText.digits(answer, language)),
            notes = listOf(sumDifferenceNote(language), balanceNote(language)),
        )
    }

    /** "4x + y = 31, y = 3" — substitute, then solve. */
    private fun substitutionSystem(language: AppLanguage): Problem {
        val a = random.nextInt(2, 7)
        val x = random.nextInt(2, 13)
        val b = random.nextInt(1, 2001)
        val c = a * x + b
        val givenHint = when (language) {
            AppLanguage.ENGLISH -> "The second line already tells you exactly what y is."
            AppLanguage.ROMANIAN -> "Al doilea rând îți spune deja exact cât e y."
        }
        return Problem(
            text = "${a}x + y = $c\ny = $b\nx = ?",
            answer = x,
            difficulty = Difficulty.MEDIUM,
            kind = ProblemKind.EQUATION,
            hints = listOf(givenHint, HintText.digits(x, language)),
            notes = listOf(systemNote(language), shorthandNote(language)),
        )
    }

    /** Square roots and small powers, kept whole and small. */
    private fun rootOrPower(language: AppLanguage): Problem = when (random.nextInt(4)) {
        0 -> {
            val r = random.nextInt(2, 16)
            val x = random.nextInt(1, 3401)
            val c = x + r
            Problem(
                text = "x + √${r * r} = $c\nx = ?",
                answer = x,
                difficulty = Difficulty.MEDIUM,
                kind = ProblemKind.EQUATION,
                hints = listOf(rootMeaningHint(r * r, language), HintText.digits(x, language)),
                notes = listOf(rootNote(language), balanceNote(language)),
            )
        }

        1 -> {
            val a = random.nextInt(3, 13)
            val x = random.nextInt(5, 3301)
            val c = a * a + x
            Problem(
                text = "$a² + x = $c\nx = ?",
                answer = x,
                difficulty = Difficulty.MEDIUM,
                kind = ProblemKind.EQUATION,
                hints = listOf(squareMeaningHint(a, language), HintText.digits(x, language)),
                notes = listOf(powerNote(language), balanceNote(language)),
            )
        }

        2 -> {
            val x = random.nextInt(3, 16)
            Problem(
                text = "x² = ${x * x}\nx = ?",
                answer = x,
                difficulty = Difficulty.MEDIUM,
                kind = ProblemKind.EQUATION,
                hints = listOf(
                    rootMeaningHint(x * x, language),
                    HintText.digits(x, language),
                ),
                notes = listOf(powerNote(language), rootNote(language)),
            )
        }

        else -> {
            val x = random.nextInt(2, 8)
            val cubeHint = when (language) {
                AppLanguage.ENGLISH -> "x³ means x × x × x"
                AppLanguage.ROMANIAN -> "x³ înseamnă x × x × x"
            }
            Problem(
                text = "x³ = ${x * x * x}\nx = ?",
                answer = x,
                difficulty = Difficulty.MEDIUM,
                kind = ProblemKind.EQUATION,
                hints = listOf(cubeHint, HintText.digits(x, language)),
                notes = listOf(powerNote(language)),
            )
        }
    }

    // ── HARD: 4–7 numbers, up to two unknowns, bigger values ────────────

    private fun rollHard(language: AppLanguage): Problem = when (random.nextInt(6)) {
        0 -> {
            // "5x + 12 − 7 = 65"
            val a = random.nextInt(3, 10)
            val x = random.nextInt(3, 16)
            val b = random.nextInt(2, 41)
            val c = random.nextInt(2, minOf(41, a * x + b))
            val rhs = a * x + b - c
            Problem(
                text = "${findXLabel(language)}\n${a}x + $b − $c = $rhs",
                answer = x,
                difficulty = Difficulty.HARD,
                kind = ProblemKind.EQUATION,
                hints = listOf(shorthandHint(a, language), HintText.digits(x, language)),
                notes = listOf(balanceNote(language), shorthandNote(language)),
            )
        }

        1 -> {
            // "3x + 4y = 53, y = 5"
            val a = random.nextInt(2, 10)
            val b = random.nextInt(2, 10)
            val x = random.nextInt(2, 16)
            val y = random.nextInt(2, 13)
            val e = a * x + b * y
            val givenHint = when (language) {
                AppLanguage.ENGLISH -> "The second line already tells you exactly what y is."
                AppLanguage.ROMANIAN -> "Al doilea rând îți spune deja exact cât e y."
            }
            Problem(
                text = "${a}x + ${b}y = $e\ny = $y\nx = ?",
                answer = x,
                difficulty = Difficulty.HARD,
                kind = ProblemKind.EQUATION,
                hints = listOf(givenHint, HintText.digits(x, language)),
                notes = listOf(systemNote(language), shorthandNote(language)),
            )
        }

        2 -> sumDifferenceSystem(language, maxSmaller = 60, askProduct = true)

        3 -> derivative(language)

        4 -> {
            // "x² − 9 = 40"
            val x = random.nextInt(4, 21)
            val b = random.nextInt(1, x * x)
            Problem(
                text = "x² − $b = ${x * x - b}\nx = ?",
                answer = x,
                difficulty = Difficulty.HARD,
                kind = ProblemKind.EQUATION,
                hints = listOf(squareOfXHint(language), HintText.digits(x, language)),
                notes = listOf(balanceNote(language), powerNote(language)),
            )
        }

        else -> {
            // "√81 × 7 − 13 = ?" — no unknown, the root is the twist.
            val r = random.nextInt(3, 13)
            val a = random.nextInt(2, 10)
            val b = random.nextInt(1, r * a + 1)
            val answer = r * a - b
            Problem(
                text = "√${r * r} × $a − $b = ?",
                answer = answer,
                difficulty = Difficulty.HARD,
                kind = ProblemKind.EQUATION,
                hints = listOf(
                    rootMeaningHint(r * r, language),
                    HintText.digits(answer, language),
                ),
                notes = listOf(rootNote(language)),
            )
        }
    }

    // ── Derivatives: f′ at a small point, whole numbers throughout ──────

    /**
     * "f(x) = 3x² + 5x + 12, f′(2) = ?" — quadratic or cubic, always with
     * a constant so the "constants vanish" tip has something to bite on.
     * f′ of ax² + bx + c at k is 2ak + b; of ax³ + bx + c it is 3ak² + b —
     * whole and positive by construction.
     */
    private fun derivative(language: AppLanguage): Problem {
        val b = random.nextInt(2, 14)
        val c = random.nextInt(2, 31)
        val cubic = random.nextBoolean()
        val (text, answer) = if (cubic) {
            val a = random.nextInt(2, 5)
            val k = random.nextInt(2, 5)
            "f(x) = ${a}x³ + ${b}x + $c\nf′($k) = ?" to 3 * a * k * k + b
        } else {
            val a = random.nextInt(2, 8)
            val k = random.nextInt(2, 7)
            "f(x) = ${a}x² + ${b}x + $c\nf′($k) = ?" to 2 * a * k + b
        }
        return Problem(
            text = text,
            answer = answer,
            difficulty = Difficulty.HARD,
            kind = ProblemKind.EQUATION,
            hints = listOf(derivativeMeaningHint(language), HintText.digits(answer, language)),
            notes = listOf(
                derivativeNote(language),
                derivativeRecipeNote(cubic, language),
                powerRuleNote(language),
                derivativeConstantNote(language),
            ),
        )
    }

    // ── Hint phrasing: meanings and restatements, never steps ───────────

    private fun findXLabel(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "Find x:"
        AppLanguage.ROMANIAN -> "Află x:"
    }

    private fun rootMeaningHint(square: Int, language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "√$square asks: which number times itself gives $square?"
        AppLanguage.ROMANIAN -> "√$square întreabă: ce număr înmulțit cu el însuși dă $square?"
    }

    private fun shorthandHint(a: Int, language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "${a}x is shorthand for $a × x"
        AppLanguage.ROMANIAN -> "${a}x e o prescurtare pentru $a × x"
    }

    private fun squareMeaningHint(a: Int, language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "$a² means $a × $a"
        AppLanguage.ROMANIAN -> "$a² înseamnă $a × $a"
    }

    private fun squareOfXHint(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "x² means x × x"
        AppLanguage.ROMANIAN -> "x² înseamnă x × x"
    }

    private fun derivativeMeaningHint(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH ->
            "f′(k) asks: find the derivative f′(x) first, then put k in the place of x. The notebook has the rules."
        AppLanguage.ROMANIAN ->
            "f′(k) cere: află întâi derivata f′(x), apoi pune k în locul lui x. Regulile sunt în caiet."
    }

    // ── Helper-sheet notes ───────────────────────────────────────────────

    private fun balanceNote(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH ->
            "An equation is a balance: both sides are worth the same. Do the same thing to both sides and it stays true."
        AppLanguage.ROMANIAN ->
            "O ecuație e o balanță: ambele părți valorează la fel. Fă același lucru pe ambele părți și rămâne adevărată."
    }

    private fun shorthandNote(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "Writing 3x is shorthand for 3 × x — a number glued to a letter means multiply."
        AppLanguage.ROMANIAN -> "Scrierea 3x e o prescurtare pentru 3 × x — un număr lipit de o literă înseamnă înmulțire."
    }

    private fun rootNote(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "√ (the square root) asks: which number times itself gives this? √25 = 5, because 5 × 5 = 25."
        AppLanguage.ROMANIAN -> "√ (radicalul) întreabă: ce număr înmulțit cu el însuși dă atât? √25 = 5, pentru că 5 × 5 = 25."
    }

    private fun powerNote(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "a² means a × a, and a³ means a × a × a. For example 5² = 25 and 2³ = 8."
        AppLanguage.ROMANIAN -> "a² înseamnă a × a, iar a³ înseamnă a × a × a. De exemplu 5² = 25 și 2³ = 8."
    }

    private fun systemNote(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "Two lines are two facts about the same x and y. What one line tells you can be used inside the other."
        AppLanguage.ROMANIAN -> "Două rânduri sunt două adevăruri despre aceiași x și y. Ce afli dintr-un rând poți folosi în celălalt."
    }

    private fun derivativeNote(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH ->
            "The derivative f′ measures how fast f changes — the slope of its graph at that point."
        AppLanguage.ROMANIAN ->
            "Derivata f′ arată cât de repede se schimbă f — panta graficului ei în acel punct."
    }

    /** The exact formula for the shape on screen, worked through once. */
    private fun derivativeRecipeNote(cubic: Boolean, language: AppLanguage): String = when {
        cubic -> when (language) {
            AppLanguage.ENGLISH ->
                "For f(x) = ax³ + bx + c the derivative is f′(x) = 3a·x² + b. " +
                    "Example: f(x) = 2x³ + 5x + 4 → f′(x) = 6x² + 5, so f′(2) = 6 · 2 · 2 + 5 = 29."
            AppLanguage.ROMANIAN ->
                "Pentru f(x) = ax³ + bx + c derivata este f′(x) = 3a·x² + b. " +
                    "Exemplu: f(x) = 2x³ + 5x + 4 → f′(x) = 6x² + 5, deci f′(2) = 6 · 2 · 2 + 5 = 29."
        }
        else -> when (language) {
            AppLanguage.ENGLISH ->
                "For f(x) = ax² + bx + c the derivative is f′(x) = 2a·x + b. " +
                    "Example: f(x) = 4x² + 3x + 7 → f′(x) = 8x + 3, so f′(2) = 8 · 2 + 3 = 19."
            AppLanguage.ROMANIAN ->
                "Pentru f(x) = ax² + bx + c derivata este f′(x) = 2a·x + b. " +
                    "Exemplu: f(x) = 4x² + 3x + 7 → f′(x) = 8x + 3, deci f′(2) = 8 · 2 + 3 = 19."
        }
    }

    private fun powerRuleNote(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH ->
            "Power rule: xⁿ becomes n·xⁿ⁻¹. So x² → 2x, x³ → 3x², and a plain x → 1. Derive piece by piece, then add up."
        AppLanguage.ROMANIAN ->
            "Regula puterii: xⁿ devine n·xⁿ⁻¹. Deci x² → 2x, x³ → 3x², iar un x simplu → 1. Derivează bucată cu bucată, apoi adună."
    }

    private fun derivativeConstantNote(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH ->
            "A number in front stays: (5x²)′ = 5 · 2x = 10x. A lone number vanishes — its derivative is 0."
        AppLanguage.ROMANIAN ->
            "Numărul din față rămâne: (5x²)′ = 5 · 2x = 10x. Un număr singur dispare — derivata lui e 0."
    }

    private fun sumDifferenceNote(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH ->
            "For two numbers: the bigger one = (sum + difference) ÷ 2, the smaller one = (sum − difference) ÷ 2."
        AppLanguage.ROMANIAN ->
            "Pentru două numere: cel mare = (sumă + diferență) ÷ 2, cel mic = (sumă − diferență) ÷ 2."
    }
}
