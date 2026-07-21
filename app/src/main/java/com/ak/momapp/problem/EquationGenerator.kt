package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Unknown-value problems for MEDIUM and HARD: solve for x (sometimes with
 * a second unknown y), with simple square roots and small powers mixed in.
 * HARD also deals simple derivatives, evaluated at a small whole point.
 *
 * Guarantees: every answer and every number shown is a non-negative whole
 * number; at MEDIUM nothing on screen or in the answer passes 3500.
 *
 * Hints clarify what the symbols mean or what is being asked. They never
 * walk through the solving steps. The rules themselves (balance, roots,
 * powers) ride along in [Problem.notes] for the helper sheet.
 */
class EquationGenerator(private val random: Random) {

    /**
     * The harder shapes (two coefficients, x², derivatives) do not switch
     * on the moment she is called Hard. They fade in across the lower half
     * of the band, so the first Hard sittings are mostly familiar work with
     * the occasional new shape, rather than a wall.
     */
    fun generate(level: Level, language: AppLanguage): Problem =
        if (random.nextDouble() < level.ramp(Level.MEDIUM_TOP, Level.HARD_ANCHOR)) {
            rollHard(level, language)
        } else {
            rollMedium(level, language)
        }

    // ── The everyday shapes: one or two unknowns, numbers led by the level ──

    private fun rollMedium(level: Level, language: AppLanguage): Problem = when (random.nextInt(5)) {
        0, 1 -> findX(level, language)
        2 -> sumDifferenceSystem(level, language, askProduct = false)
        3 -> substitutionSystem(level, language)
        else -> rootOrPower(level, language)
    }

    /** "3x + 7 = 25". The bread and butter. */
    private fun findX(level: Level, language: AppLanguage): Problem {
        val a = random.nextInt(level.span(2..5, 2..9, 3..9))
        val x = random.nextInt(level.span(2..8, 2..12, 3..15))
        val plus = random.nextBoolean()
        // The minus variant keeps the right-hand side positive.
        val b = if (plus) random.nextInt(level.span(1..40, 1..3000, 1..3000)) else random.nextInt(1, a * x)
        val rhs = if (plus) a * x + b else a * x - b
        val text = if (plus) {
            "${findXLabel(language)}\n${a}x + $b = $rhs"
        } else {
            "${findXLabel(language)}\n${a}x − $b = $rhs"
        }
        val clear = if (plus) {
            step(
                "Take $b off both sides: ${a}x = $rhs − $b = ${a * x}",
                "Scade $b din ambele părți: ${a}x = $rhs − $b = ${a * x}",
                language,
            )
        } else {
            step(
                "Add $b to both sides: ${a}x = $rhs + $b = ${a * x}",
                "Adună $b la ambele părți: ${a}x = $rhs + $b = ${a * x}",
                language,
            )
        }
        return Problem(
            text = text,
            answer = x,
            level = level,
            kind = ProblemKind.EQUATION,
            // One unknown in one line: the gentlest equation there is, so
            // it should not carry her up like a two-unknown system does.
            effort = SINGLE_UNKNOWN_EFFORT,
            hints = listOf(shorthandHint(a, language), HintText.digits(x, language)),
            notes = listOf(balanceNote(language), shorthandNote(language)),
            solution = listOf(clear, divideBoth(a, a * x, x, language)),
        )
    }

    /**
     * "x + y = 24, x − y = 6". Ask for x, y, or (at HARD) their product.
     */
    private fun sumDifferenceSystem(
        level: Level,
        language: AppLanguage,
        askProduct: Boolean,
    ): Problem {
        // Asking for the product needs both numbers small enough to
        // multiply in her head; asking for one of them does not.
        val y = if (askProduct) {
            random.nextInt(1, 61)
        } else {
            random.nextInt(level.span(1..80, 1..1200, 1..1200))
        }
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
        // Adding the two lines cancels y, which is the whole trick here.
        val solution = buildList {
            add(
                step(
                    "Add the two lines: the y cancels, so 2x = $s + $d = ${s + d}",
                    "Adună cele două rânduri: y se anulează, deci 2x = $s + $d = ${s + d}",
                    language,
                ),
            )
            add(
                step(
                    "x = ${s + d} ÷ 2 = $x",
                    "x = ${s + d} ÷ 2 = $x",
                    language,
                ),
            )
            if (askProduct || answer != x) {
                add(
                    step(
                        "y = $s − $x = $y",
                        "y = $s − $x = $y",
                        language,
                    ),
                )
            }
            if (askProduct) {
                add(
                    step(
                        "x × y = $x × $y = ${x * y}",
                        "x × y = $x × $y = ${x * y}",
                        language,
                    ),
                )
            }
        }
        return Problem(
            text = "x + y = $s\nx − y = $d\n$question",
            answer = answer,
            level = level,
            kind = ProblemKind.EQUATION,
            hints = listOf(restateHint, HintText.digits(answer, language)),
            notes = listOf(sumDifferenceNote(language), balanceNote(language)),
            solution = solution,
        )
    }

    /** "4x + y = 31, y = 3". Substitute, then solve. */
    private fun substitutionSystem(level: Level, language: AppLanguage): Problem {
        val a = random.nextInt(level.span(2..4, 2..6, 2..6))
        val x = random.nextInt(level.span(2..8, 2..12, 2..12))
        val b = random.nextInt(level.span(1..30, 1..2000, 1..2000))
        val c = a * x + b
        val givenHint = when (language) {
            AppLanguage.ENGLISH -> "The second line already tells you exactly what y is."
            AppLanguage.ROMANIAN -> "Al doilea rând îți spune deja exact cât e y."
        }
        return Problem(
            text = "${a}x + y = $c\ny = $b\nx = ?",
            answer = x,
            level = level,
            kind = ProblemKind.EQUATION,
            hints = listOf(givenHint, HintText.digits(x, language)),
            notes = listOf(systemNote(language), shorthandNote(language)),
            solution = listOf(
                step(
                    "Put y = $b into the first line: ${a}x + $b = $c",
                    "Pune y = $b în primul rând: ${a}x + $b = $c",
                    language,
                ),
                step(
                    "Take $b off both sides: ${a}x = $c − $b = ${a * x}",
                    "Scade $b din ambele părți: ${a}x = $c − $b = ${a * x}",
                    language,
                ),
                divideBoth(a, a * x, x, language),
            ),
        )
    }

    /** Square roots and small powers, kept whole and small. */
    private fun rootOrPower(level: Level, language: AppLanguage): Problem = when (random.nextInt(4)) {
        0 -> {
            val r = random.nextInt(level.span(2..8, 2..15, 2..15))
            val x = random.nextInt(level.span(1..50, 1..3400, 1..3400))
            val c = x + r
            Problem(
                text = "x + √${r * r} = $c\nx = ?",
                answer = x,
                level = level,
                kind = ProblemKind.EQUATION,
                hints = listOf(rootMeaningHint(r * r, language), HintText.digits(x, language)),
                notes = listOf(rootNote(language), balanceNote(language)),
                solution = listOf(
                    rootIs(r, language),
                    step(
                        "So x + $r = $c, which means x = $c − $r = $x",
                        "Deci x + $r = $c, adică x = $c − $r = $x",
                        language,
                    ),
                ),
            )
        }

        1 -> {
            val a = random.nextInt(level.span(3..7, 3..12, 3..12))
            val x = random.nextInt(level.span(5..60, 5..3300, 5..3300))
            val c = a * a + x
            Problem(
                text = "$a² + x = $c\nx = ?",
                answer = x,
                level = level,
                kind = ProblemKind.EQUATION,
                hints = listOf(squareMeaningHint(a, language), HintText.digits(x, language)),
                notes = listOf(powerNote(language), balanceNote(language)),
                solution = listOf(
                    step(
                        "$a² = $a × $a = ${a * a}",
                        "$a² = $a × $a = ${a * a}",
                        language,
                    ),
                    step(
                        "So ${a * a} + x = $c, which means x = $c − ${a * a} = $x",
                        "Deci ${a * a} + x = $c, adică x = $c − ${a * a} = $x",
                        language,
                    ),
                ),
            )
        }

        2 -> {
            val x = random.nextInt(level.span(3..9, 3..15, 3..15))
            Problem(
                text = "x² = ${x * x}\nx = ?",
                answer = x,
                level = level,
                kind = ProblemKind.EQUATION,
                hints = listOf(
                    rootMeaningHint(x * x, language),
                    HintText.digits(x, language),
                ),
                notes = listOf(powerNote(language), rootNote(language)),
                solution = listOf(
                    step(
                        "x² means x × x, so look for the number that times itself gives ${x * x}",
                        "x² înseamnă x × x, deci caută numărul care înmulțit cu el însuși dă ${x * x}",
                        language,
                    ),
                    step(
                        "$x × $x = ${x * x}, so x = $x",
                        "$x × $x = ${x * x}, deci x = $x",
                        language,
                    ),
                ),
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
                level = level,
                kind = ProblemKind.EQUATION,
                hints = listOf(cubeHint, HintText.digits(x, language)),
                notes = listOf(powerNote(language)),
                solution = listOf(
                    step(
                        "x³ means x × x × x, so look for the number that gives ${x * x * x}",
                        "x³ înseamnă x × x × x, deci caută numărul care dă ${x * x * x}",
                        language,
                    ),
                    step(
                        "$x × $x × $x = ${x * x * x}, so x = $x",
                        "$x × $x × $x = ${x * x * x}, deci x = $x",
                        language,
                    ),
                ),
            )
        }
    }

    // ── HARD: 4–7 numbers, up to two unknowns, bigger values ────────────

    private fun rollHard(level: Level, language: AppLanguage): Problem {
        val roll = random.nextInt(6)
        // The derivative carries its own, heavier weight. Every other hard
        // shape is a multi-number equation and gets the same solid bump, so
        // wrestling through "5x + 12 − 7 = 65" or a two-unknown system is
        // worth more than an ordinary problem.
        if (roll == 3) return derivative(level, language)
        return when (roll) {
        0 -> {
            // "5x + 12 − 7 = 65"
            val a = random.nextInt(level.span(3..7, 3..9, 3..9))
            val x = random.nextInt(level.span(3..10, 3..15, 3..15))
            val b = random.nextInt(2, 41)
            val c = random.nextInt(2, minOf(41, a * x + b))
            val rhs = a * x + b - c
            Problem(
                text = "${findXLabel(language)}\n${a}x + $b − $c = $rhs",
                answer = x,
                level = level,
                kind = ProblemKind.EQUATION,
                hints = listOf(shorthandHint(a, language), HintText.digits(x, language)),
                notes = listOf(balanceNote(language), shorthandNote(language)),
                solution = listOf(
                    step(
                        "Move both loose numbers over: ${a}x = $rhs − $b + $c = ${a * x}",
                        "Mută ambele numere libere: ${a}x = $rhs − $b + $c = ${a * x}",
                        language,
                    ),
                    divideBoth(a, a * x, x, language),
                ),
            )
        }

        1 -> {
            // "3x + 4y = 53, y = 5"
            val a = random.nextInt(level.span(2..6, 2..9, 2..9))
            val b = random.nextInt(level.span(2..6, 2..9, 2..9))
            val x = random.nextInt(level.span(2..10, 2..15, 2..15))
            val y = random.nextInt(level.span(2..8, 2..12, 2..12))
            val e = a * x + b * y
            val givenHint = when (language) {
                AppLanguage.ENGLISH -> "The second line already tells you exactly what y is."
                AppLanguage.ROMANIAN -> "Al doilea rând îți spune deja exact cât e y."
            }
            Problem(
                text = "${a}x + ${b}y = $e\ny = $y\nx = ?",
                answer = x,
                level = level,
                kind = ProblemKind.EQUATION,
                hints = listOf(givenHint, HintText.digits(x, language)),
                notes = listOf(systemNote(language), shorthandNote(language)),
                solution = listOf(
                    step(
                        "y is $y, so ${b}y = $b × $y = ${b * y}",
                        "y este $y, deci ${b}y = $b × $y = ${b * y}",
                        language,
                    ),
                    step(
                        "Take ${b * y} off both sides: ${a}x = $e − ${b * y} = ${a * x}",
                        "Scade ${b * y} din ambele părți: ${a}x = $e − ${b * y} = ${a * x}",
                        language,
                    ),
                    divideBoth(a, a * x, x, language),
                ),
            )
        }

        2 -> sumDifferenceSystem(level, language, askProduct = true)

        4 -> {
            // "x² − 9 = 40"
            val x = random.nextInt(level.span(4..12, 4..20, 4..20))
            val b = random.nextInt(1, x * x)
            Problem(
                text = "x² − $b = ${x * x - b}\nx = ?",
                answer = x,
                level = level,
                kind = ProblemKind.EQUATION,
                hints = listOf(squareOfXHint(language), HintText.digits(x, language)),
                notes = listOf(balanceNote(language), powerNote(language)),
                solution = listOf(
                    step(
                        "Add $b to both sides: x² = ${x * x - b} + $b = ${x * x}",
                        "Adună $b la ambele părți: x² = ${x * x - b} + $b = ${x * x}",
                        language,
                    ),
                    step(
                        "$x × $x = ${x * x}, so x = $x",
                        "$x × $x = ${x * x}, deci x = $x",
                        language,
                    ),
                ),
            )
        }

        else -> {
            // "√81 × 7 − 13 = ?". No unknown, the root is the twist.
            val r = random.nextInt(level.span(3..8, 3..12, 3..12))
            val a = random.nextInt(level.span(2..6, 2..9, 2..9))
            val b = random.nextInt(1, r * a + 1)
            val answer = r * a - b
            Problem(
                text = "√${r * r} × $a − $b = ?",
                answer = answer,
                level = level,
                kind = ProblemKind.EQUATION,
                hints = listOf(
                    rootMeaningHint(r * r, language),
                    HintText.digits(answer, language),
                ),
                notes = listOf(rootNote(language)),
                solution = listOf(
                    rootIs(r, language),
                    step(
                        "$r × $a = ${r * a}",
                        "$r × $a = ${r * a}",
                        language,
                    ),
                    step(
                        "${r * a} − $b = $answer",
                        "${r * a} − $b = $answer",
                        language,
                    ),
                ),
            )
        }
        }.copy(effort = HARD_EQUATION_EFFORT)
    }

    // ── Derivatives: f′ at a small point, whole numbers throughout ──────

    /**
     * "f(x) = 3x² + 5x + 12, f′(2) = ?". Quadratic or cubic, always with
     * a constant so the "constants vanish" tip has something to bite on.
     * f′ of ax² + bx + c at k is 2ak + b; of ax³ + bx + c it is 3ak² + b;
     * whole and positive by construction.
     */
    /**
     * A one-line, one-unknown equation. Still an equation, but the easiest
     * shape of one, so it sits below the kind's usual worth.
     */
    private val SINGLE_UNKNOWN_EFFORT = 1.15

    /**
     * The multi-number hard equations: 4-to-7-number find-x, two-coefficient
     * systems, x²−b, √-chains. Worth well above an ordinary problem (about
     * two points more at a steady pace), because that is real wrestling.
     */
    private val HARD_EQUATION_EFFORT = 2.1

    /**
     * Derivatives are the hardest thing the app asks for, and priced like
     * it: about two points above the other hard equations at a steady pace.
     */
    private val DERIVATIVE_EFFORT = 2.5

    private fun derivative(level: Level, language: AppLanguage): Problem {
        val b = random.nextInt(2, 14)
        val c = random.nextInt(2, 31)
        // The cubic is the harder of the two, so it arrives later.
        val cubic = random.nextDouble() < level.ramp(Level.HARD_ANCHOR - 8, Level.MAX)
        // Three steps every time: differentiate, substitute, arrive. The
        // constant c is named as vanishing, because that is the step she
        // is most likely to doubt.
        val (text, answer, solution) = if (cubic) {
            val a = random.nextInt(2, 5)
            val k = random.nextInt(2, 5)
            val answer = 3 * a * k * k + b
            Triple(
                "f(x) = ${a}x³ + ${b}x + $c\nf′($k) = ?",
                answer,
                listOf(
                    step(
                        "Derive each piece: ${a}x³ becomes ${3 * a}x², ${b}x becomes $b, and the lone $c vanishes.",
                        "Derivează fiecare bucată: ${a}x³ devine ${3 * a}x², ${b}x devine $b, iar $c singur dispare.",
                        language,
                    ),
                    step(
                        "So f′(x) = ${3 * a}x² + $b",
                        "Deci f′(x) = ${3 * a}x² + $b",
                        language,
                    ),
                    step(
                        "Put $k in place of x: f′($k) = ${3 * a} × $k × $k + $b = $answer",
                        "Pune $k în locul lui x: f′($k) = ${3 * a} × $k × $k + $b = $answer",
                        language,
                    ),
                ),
            )
        } else {
            val a = random.nextInt(2, 8)
            val k = random.nextInt(2, 7)
            val answer = 2 * a * k + b
            Triple(
                "f(x) = ${a}x² + ${b}x + $c\nf′($k) = ?",
                answer,
                listOf(
                    step(
                        "Derive each piece: ${a}x² becomes ${2 * a}x, ${b}x becomes $b, and the lone $c vanishes.",
                        "Derivează fiecare bucată: ${a}x² devine ${2 * a}x, ${b}x devine $b, iar $c singur dispare.",
                        language,
                    ),
                    step(
                        "So f′(x) = ${2 * a}x + $b",
                        "Deci f′(x) = ${2 * a}x + $b",
                        language,
                    ),
                    step(
                        "Put $k in place of x: f′($k) = ${2 * a} × $k + $b = $answer",
                        "Pune $k în locul lui x: f′($k) = ${2 * a} × $k + $b = $answer",
                        language,
                    ),
                ),
            )
        }
        return Problem(
            text = text,
            answer = answer,
            level = level,
            kind = ProblemKind.EQUATION,
            effort = DERIVATIVE_EFFORT,
            solution = solution,
            hints = listOf(derivativeMeaningHint(language), HintText.digits(answer, language)),
            notes = listOf(
                derivativeNote(language),
                derivativeRecipeNote(cubic, language),
                powerRuleNote(language),
                derivativeConstantNote(language),
            ),
        )
    }

    // ── Worked-solution steps ────────────────────────────────────────────

    private fun step(en: String, ro: String, language: AppLanguage): String =
        if (language == AppLanguage.ROMANIAN) ro else en

    /** The last step of every "ax = total" shape: divide and land on x. */
    private fun divideBoth(a: Int, total: Int, x: Int, language: AppLanguage): String =
        step(
            "Both sides ÷ $a: x = $total ÷ $a = $x",
            "Ambele părți ÷ $a: x = $total ÷ $a = $x",
            language,
        )

    private fun rootIs(r: Int, language: AppLanguage): String =
        step(
            "√${r * r} = $r, because $r × $r = ${r * r}",
            "√${r * r} = $r, pentru că $r × $r = ${r * r}",
            language,
        )

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
        AppLanguage.ENGLISH -> "Writing 3x is shorthand for 3 × x. A number glued to a letter means multiply."
        AppLanguage.ROMANIAN -> "Scrierea 3x e o prescurtare pentru 3 × x. Un număr lipit de o literă înseamnă înmulțire."
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
            "The derivative f′ measures how fast f changes. The slope of its graph at that point."
        AppLanguage.ROMANIAN ->
            "Derivata f′ arată cât de repede se schimbă f. Panta graficului ei în acel punct."
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
            "A number in front stays: (5x²)′ = 5 · 2x = 10x. A lone number vanishes. Its derivative is 0."
        AppLanguage.ROMANIAN ->
            "Numărul din față rămâne: (5x²)′ = 5 · 2x = 10x. Un număr singur dispare. Derivata lui e 0."
    }

    private fun sumDifferenceNote(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH ->
            "For two numbers: the bigger one = (sum + difference) ÷ 2, the smaller one = (sum − difference) ÷ 2."
        AppLanguage.ROMANIAN ->
            "Pentru două numere: cel mare = (sumă + diferență) ÷ 2, cel mic = (sumă − diferență) ÷ 2."
    }
}
