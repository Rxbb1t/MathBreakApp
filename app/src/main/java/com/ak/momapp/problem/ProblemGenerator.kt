package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * Hands out problems by mixing ten sources:
 *
 *  - shape puzzles ([ShapePuzzleGenerator]). Every difficulty
 *  - logic riddles ([LogicProblemGenerator]). Every difficulty
 *  - story geometry ([GeometryProblemGenerator]). MEDIUM and HARD,
 *    as common as the riddles
 *  - money stories in euros ([MoneyProblemGenerator]). Every difficulty
 *  - clock stories in minutes ([TimeProblemGenerator]). Every difficulty
 *  - word problems told with the [PersonalContent.NAMES] pool. Every
 *    difficulty
 *  - tap-to-compare expressions and ✓/✗ claims, one topic switch
 *    ([ComparisonProblemGenerator], [TrueFalseProblemGenerator]). Every
 *    difficulty, leaning EASY/MEDIUM
 *  - target builder ([TargetProblemGenerator]). Every difficulty,
 *    leaning EASY/MEDIUM
 *  - number hunts and set exercises ([NumberHuntGenerator],
 *    [SetProblemGenerator]). Every difficulty, one topic switch
 *  - the difficulty's core: EASY gets 3–4 number chains using all four
 *    operations; MEDIUM and HARD get unknown-value equations, HARD adds
 *    simple derivatives ([EquationGenerator]); every level mixes in
 *    tap-the-missing-sign lines ([MissingOperatorGenerator])
 *
 * Settings can switch whole topics off ([ProblemTopic]); the roll then
 * spans only what is left, and the core steps in when nothing is.
 *
 * Guarantees: every answer. And every left-to-right running total in a
 * chain. Is a non-negative whole number, and divisions are exact.
 */
class ProblemGenerator(private val random: Random = Random.Default) {

    private val equations = EquationGenerator(random)
    private val puzzles = ShapePuzzleGenerator(random)
    private val logic = LogicProblemGenerator(random)
    private val geometry = GeometryProblemGenerator(random)
    private val money = MoneyProblemGenerator(random)
    private val time = TimeProblemGenerator(random)
    private val compare = ComparisonProblemGenerator(random)
    private val trueFalse = TrueFalseProblemGenerator(random)
    private val target = TargetProblemGenerator(random)
    private val missingOp = MissingOperatorGenerator(random)
    private val hunt = NumberHuntGenerator(random)
    private val sets = SetProblemGenerator(random)
    private val showpieces = ShowpieceGenerator(random)

    /** Percent slices for one roll of the dice; whatever is left is core. */
    private data class Mix(
        val puzzle: Int,
        val logic: Int,
        val geometry: Int,
        val money: Int,
        val time: Int,
        val word: Int,
        val compare: Int,
        val target: Int,
        val numbers: Int,
    )

    /** One story source in the roll: its share of the mix and its maker. */
    private class Slice(val weight: Int, val make: () -> Problem)

    /**
     * The last few problems served, so the same one doesn't come round
     * twice in a sitting. Two rings, because two different things read as
     * "I've seen this": [recentTexts] catches the identical question,
     * [recentShapes] catches the same template with the numbers swapped,
     * which is the one she actually notices ("another ladder problem?").
     * The shape ring is short on purpose — with only one topic switched
     * on, blocking a template for long would starve the roll.
     */
    private val recentTexts = ArrayDeque<String>()
    private val recentShapes = ArrayDeque<String>()

    /**
     * Hands out a problem, rerolling past anything too recently seen.
     * Bounded, because with a single topic enabled there may be nothing
     * else to serve: after [REROLL_LIMIT] tries it takes what it gets
     * rather than looping.
     */
    fun generate(
        difficulty: Difficulty,
        language: AppLanguage = AppLanguage.ENGLISH,
        topics: Set<ProblemTopic> = ProblemTopic.ALL,
        /**
         * The level to deal a given topic at. Each topic carries its own
         * adaptive level, and which topic this roll lands on isn't known
         * until [roll] has thrown the dice, so the level has to be looked
         * up in there rather than handed in. Defaults to one level for
         * everything, which is what practice mode and the tests want.
         */
        levelFor: (ProblemTopic) -> Difficulty = { difficulty },
    ): Problem {
        var fallback: Problem? = null
        repeat(REROLL_LIMIT) {
            val problem = roll(difficulty, language, topics, levelFor)
            // The first roll is worth keeping in case every later one is
            // also a repeat.
            if (fallback == null) fallback = problem
            if (keyOf(problem) !in recentTexts && shapeOf(problem) !in recentShapes) {
                return remember(problem)
            }
        }
        return remember(fallback ?: roll(difficulty, language, topics, levelFor))
    }

    /** Files a problem in both rings and hands it back. */
    private fun remember(problem: Problem): Problem {
        recentTexts.addLast(keyOf(problem))
        if (recentTexts.size > RECENT_TEXTS) recentTexts.removeFirst()
        shapeOf(problem)?.let {
            recentShapes.addLast(it)
            if (recentShapes.size > RECENT_SHAPES) recentShapes.removeFirst()
        }
        return problem
    }

    /**
     * What makes this problem *this* problem. Usually the text, but a
     * tap exercise's question is only its instruction ("Tap all the even
     * numbers") and all its variety lives in the cards, so those have to
     * count. Keying on the text alone would make every number hunt look
     * like a repeat of the last one and quietly starve the topic.
     */
    private fun keyOf(problem: Problem): String =
        if (problem.cards.isEmpty()) {
            problem.text
        } else {
            problem.text + "|" + problem.cards.sorted().joinToString(",")
        }

    /**
     * A problem's template, with every run of digits blanked out, so two
     * fillings of the same story collapse to one signature.
     *
     * Null for card exercises: there are only a handful of hunt prompts
     * by design, and blocking a prompt for several problems would starve
     * a sitting that has just that topic switched on. Their repetition is
     * handled by [keyOf], which can see the cards.
     */
    private fun shapeOf(problem: Problem): String? =
        if (problem.cards.isEmpty()) problem.text.replace(DIGITS, "#") else null

    private fun roll(
        difficulty: Difficulty,
        language: AppLanguage = AppLanguage.ENGLISH,
        topics: Set<ProblemTopic> = ProblemTopic.ALL,
        levelFor: (ProblemTopic) -> Difficulty = { difficulty },
    ): Problem {
        // Geometry rides only MEDIUM/HARD, about as common as the riddles.
        // Compare and target-builder taps lean EASY/MEDIUM and thin out
        // at HARD.
        //
        // The MIX is chosen by the overall level, not the per-topic ones:
        // it decides which topics are worth offering and how often, which
        // is a question about the sitting rather than about any one
        // topic. Only the problem itself is dealt at the topic's level.
        val mix = when (difficulty) {
            Difficulty.EASY -> Mix(11, 14, 0, 11, 10, 11, 9, 9, 10)
            Difficulty.HARD -> Mix(10, 16, 15, 10, 9, 11, 5, 5, 9)
            else -> Mix(9, 15, 14, 9, 8, 11, 7, 7, 9)
        }
        // A switched-off topic loses its slice.
        fun weightOf(topic: ProblemTopic, weight: Int) = if (topic in topics) weight else 0
        val slices = listOf(
            Slice(weightOf(ProblemTopic.PUZZLE, mix.puzzle)) {
                puzzles.generate(levelFor(ProblemTopic.PUZZLE), language)
            },
            Slice(weightOf(ProblemTopic.LOGIC, mix.logic)) {
                val level = levelFor(ProblemTopic.LOGIC)
                // The showpieces are riddles in spirit, so they ride the
                // riddle switch, and only at HARD -- they are meant to be
                // the occasional treat, not the house style.
                if (level == Difficulty.HARD && random.nextInt(100) < SHOWPIECE_PERCENT) {
                    showpieces.generate(language)
                } else {
                    logic.generate(level, PersonalContent.NAMES, language)
                }
            },
            Slice(weightOf(ProblemTopic.GEOMETRY, mix.geometry)) {
                geometry.generate(levelFor(ProblemTopic.GEOMETRY), language)
            },
            Slice(weightOf(ProblemTopic.MONEY, mix.money)) {
                money.generate(levelFor(ProblemTopic.MONEY), language)
            },
            Slice(weightOf(ProblemTopic.TIME, mix.time)) {
                time.generate(levelFor(ProblemTopic.TIME), language)
            },
            Slice(weightOf(ProblemTopic.WORD, mix.word)) {
                wordProblem(levelFor(ProblemTopic.WORD), language)
            },
            // One topic, two flavors: < = > taps and ✓/✗ claims.
            Slice(weightOf(ProblemTopic.COMPARE, mix.compare)) {
                val level = levelFor(ProblemTopic.COMPARE)
                if (random.nextBoolean()) {
                    compare.generate(level, language)
                } else {
                    trueFalse.generate(level, language)
                }
            },
            Slice(weightOf(ProblemTopic.TARGET, mix.target)) {
                target.generate(levelFor(ProblemTopic.TARGET), language)
            },
            // One topic, two flavors: tap hunts and typed set counting.
            Slice(weightOf(ProblemTopic.NUMBERS, mix.numbers)) {
                val level = levelFor(ProblemTopic.NUMBERS)
                if (random.nextBoolean()) {
                    hunt.generate(level, language)
                } else {
                    sets.generate(level, language)
                }
            },
        )

        // With CORE on the rest of the usual 100 rolls falls through to it;
        // with CORE off the roll spans only the enabled stories. When no
        // story can serve (say, only geometry left at EASY) core steps in
        // anyway so there is always a problem.
        val storyWeight = slices.sumOf(Slice::weight)
        val total = if (ProblemTopic.CORE in topics) 100 else storyWeight
        if (total > 0) {
            var roll = random.nextInt(total)
            for (slice in slices) {
                if (roll < slice.weight) return slice.make()
                roll -= slice.weight
            }
        }
        // The core itself mixes: mostly chains/equations, with the odd
        // tap-the-missing-sign line for variety.
        val core = levelFor(ProblemTopic.CORE)
        return when {
            random.nextInt(4) == 0 -> missingOp.generate(core, language)
            core == Difficulty.EASY -> easyChain(language)
            else -> equations.generate(core, language)
        }
    }

    // ── EASY chains: 3–4 numbers, all four operations, small values ──────

    /** A self-contained piece of a chain: "48 ÷ 6" or just "27". */
    private data class Segment(val text: String, val value: Int, val terms: Int)

    /**
     * Builds "a op b op c…" left to right; a subtracted segment is always
     * capped by the running total, so no step goes negative.
     */
    private fun easyChain(language: AppLanguage): Problem {
        val targetTerms = random.nextInt(3, 5)
        var segment = firstSegment()
        val text = StringBuilder(segment.text)
        var running = segment.value
        var terms = segment.terms
        // Kept so the worked solution can replay the chain rather than
        // try to re-parse the printed text.
        val first = segment
        val rest = mutableListOf<Pair<Boolean, Segment>>()

        while (terms < targetTerms) {
            val remaining = targetTerms - terms
            val plus = running <= 1 || random.nextBoolean()
            segment = nextSegment(
                allowMulDiv = remaining >= 2,
                maxValue = if (plus) null else running,
            )
            text.append(if (plus) " + " else " − ").append(segment.text)
            running = if (plus) running + segment.value else running - segment.value
            terms += segment.terms
            rest += plus to segment
        }

        // No step-by-step walkthroughs. A calm nudge, then the answer's shape.
        val calmHint = when (language) {
            AppLanguage.ENGLISH -> "Work left to right. Every step lands on a whole number."
            AppLanguage.ROMANIAN -> "Mergi de la stânga la dreapta. Fiecare pas dă un număr întreg."
        }
        return Problem(
            text = "$text = ?",
            answer = running,
            difficulty = Difficulty.EASY,
            kind = ProblemKind.ARITHMETIC,
            hints = listOf(calmHint, HintText.digits(running, language)),
            solution = chainSolution(first, rest, language),
        )
    }

    /**
     * Replays the chain: the × and ÷ pieces first (that is the rule she
     * has to remember), then one running total per step, so every number
     * on screen is accounted for.
     */
    private fun chainSolution(
        first: Segment,
        rest: List<Pair<Boolean, Segment>>,
        language: AppLanguage,
    ): List<String> {
        val ro = language == AppLanguage.ROMANIAN
        val steps = mutableListOf<String>()
        val compound = (listOf(first) + rest.map { it.second }).filter { it.terms > 1 }
        if (compound.isNotEmpty()) {
            steps += if (ro) "Întâi × și ÷: " + compound.joinToString(",  ") { "${it.text} = ${it.value}" }
            else "The × and ÷ come first: " + compound.joinToString(",  ") { "${it.text} = ${it.value}" }
        }
        var running = first.value
        for ((plus, segment) in rest) {
            val next = if (plus) running + segment.value else running - segment.value
            steps += "$running ${if (plus) "+" else "−"} ${segment.value} = $next"
            running = next
        }
        return steps
    }

    private fun firstSegment(): Segment =
        if (random.nextBoolean()) {
            mulOrDivSegment(maxValue = null) ?: plainSegment(maxValue = null)
        } else {
            plainSegment(maxValue = null)
        }

    private fun nextSegment(allowMulDiv: Boolean, maxValue: Int?): Segment {
        if (allowMulDiv && random.nextBoolean()) {
            mulOrDivSegment(maxValue)?.let { return it }
        }
        return plainSegment(maxValue)
    }

    private fun plainSegment(maxValue: Int?): Segment {
        val value = if (maxValue == null) {
            random.nextInt(5, 31)
        } else {
            random.nextInt(1, minOf(30, maxValue) + 1)
        }
        return Segment("$value", value, 1)
    }

    /** Returns null when [maxValue] leaves no room; caller falls back. */
    private fun mulOrDivSegment(maxValue: Int?): Segment? {
        return if (random.nextBoolean()) {
            val a = random.nextInt(2, 10)
            val bMax = if (maxValue == null) 9 else minOf(9, maxValue / a)
            if (bMax < 2) return null
            val b = random.nextInt(2, bMax + 1)
            Segment("$a × $b", a * b, 2)
        } else {
            val quotientMax = if (maxValue == null) 10 else minOf(10, maxValue)
            if (quotientMax < 2) return null
            val quotient = random.nextInt(2, quotientMax + 1)
            val divisor = random.nextInt(2, 10)
            Segment("${quotient * divisor} ÷ $divisor", quotient, 2)
        }
    }

    // ── Word problems: two numbers wrapped in a little story ─────────────

    private enum class Op { ADD, SUB, MUL }

    /**
     * A little story with numbers in it, in one of three shapes.
     *
     * Most run forwards in one step. At HARD half of them take two
     * operations instead: the level's other stories already grow by
     * making the arithmetic fiddlier, and a story is better made harder
     * by asking for more thinking than by using bigger numbers.
     * Everywhere, one story in five runs backwards -- the end is given
     * and the beginning is the question -- which is the same arithmetic
     * approached from the other side.
     */
    private fun wordProblem(
        difficulty: Difficulty,
        language: AppLanguage,
    ): Problem = when {
        difficulty == Difficulty.HARD && random.nextBoolean() ->
            twoStepWord(difficulty, language)
        random.nextInt(5) == 0 -> reverseWord(difficulty, language)
        else -> directWord(difficulty, language)
    }

    /** Fills {name}, and each number's plain and Romanian-"de" slots. */
    private fun fillStory(template: String, values: List<Pair<String, Int>>): String {
        var text = template.replace("{name}", PersonalContent.NAMES.random(random))
        for ((key, n) in values) {
            // Romanian slot: the number plus "de" when it needs one
            // ("12 pagini" but "45 de pagini").
            text = text.replace("{${key}_de}", if (n < 20) "$n" else "$n de")
            text = text.replace("{$key}", n.toString())
        }
        return text
    }

    /**
     * Two operations in one story: so many groups of so many, and then a
     * few more (or a few short). The numbers stay modest on purpose --
     * the work is in holding two steps, not in the size of them.
     */
    private fun twoStepWord(difficulty: Difficulty, language: AppLanguage): Problem {
        val groups = random.nextInt(3, 10)
        val each = random.nextInt(4, 13)
        val extra = random.nextInt(6, 40)
        val adding = random.nextBoolean()
        val product = groups * each
        // Never ask for a number below zero, and never make the second
        // step a no-op by taking away everything.
        val takeAway = if (adding) 0 else minOf(extra, product - 1)
        val answer = if (adding) product + extra else product - takeAway

        val templates = if (adding) {
            if (language == AppLanguage.ROMANIAN) TWO_STEP_ADD_RO else TWO_STEP_ADD
        } else {
            if (language == AppLanguage.ROMANIAN) TWO_STEP_SUB_RO else TWO_STEP_SUB
        }
        val text = fillStory(
            templates.random(random),
            listOf("a" to groups, "b" to each, "c" to if (adding) extra else takeAway),
        )
        val hint = when (language) {
            AppLanguage.ENGLISH -> "Two steps: work out the equal groups first, then the rest."
            AppLanguage.ROMANIAN -> "Doi pași: află întâi grupurile egale, apoi restul."
        }
        val ro = language == AppLanguage.ROMANIAN
        return Problem(
            text = text,
            answer = answer,
            difficulty = difficulty,
            kind = ProblemKind.WORD,
            hints = listOf(hint, HintText.digits(answer, language)),
            notes = wordNotes(language),
            solution = listOf(
                if (ro) {
                    "Întâi grupurile egale: $groups × $each = $product"
                } else {
                    "The equal groups first: $groups × $each = $product"
                },
                if (adding) {
                    if (ro) {
                        "Apoi se adaugă restul: $product + $extra = $answer"
                    } else {
                        "Then the rest joins in: $product + $extra = $answer"
                    }
                } else {
                    if (ro) {
                        "Apoi se scade ce se pierde: $product − $takeAway = $answer"
                    } else {
                        "Then what's lost comes off: $product − $takeAway = $answer"
                    }
                },
            ),
        )
    }

    /**
     * The story told backwards: what's left is given, the starting
     * amount is the question. Same arithmetic as a subtraction story,
     * but she has to notice it runs the other way, which is the part
     * that's actually worth practising.
     */
    private fun reverseWord(difficulty: Difficulty, language: AppLanguage): Problem {
        val (gone, left) = when (difficulty) {
            Difficulty.EASY -> random.nextInt(4, 30) to random.nextInt(5, 40)
            Difficulty.MEDIUM -> random.nextInt(25, 180) to random.nextInt(30, 250)
            Difficulty.HARD -> random.nextInt(120, 480) to random.nextInt(150, 600)
        }
        val templates = if (language == AppLanguage.ROMANIAN) REVERSE_TEMPLATES_RO else REVERSE_TEMPLATES
        val text = fillStory(templates.random(random), listOf("b" to gone, "r" to left))
        val hint = when (language) {
            AppLanguage.ENGLISH ->
                "The start is bigger than what's left: put back what went away."
            AppLanguage.ROMANIAN ->
                "Începutul e mai mare decât ce a rămas: pune la loc ce s-a dus."
        }
        val ro = language == AppLanguage.ROMANIAN
        return Problem(
            text = text,
            answer = gone + left,
            difficulty = difficulty,
            kind = ProblemKind.WORD,
            hints = listOf(hint, HintText.digits(gone + left, language)),
            notes = wordNotes(language),
            solution = listOf(
                if (ro) {
                    "Povestea merge invers: se știe cât a rămas, se caută începutul."
                } else {
                    "The story runs backwards: the end is known, the start is the question."
                },
                if (ro) {
                    "Pui la loc ce s-a dus: $left + $gone = ${gone + left}"
                } else {
                    "Put back what went away: $left + $gone = ${gone + left}"
                },
            ),
        )
    }

    private fun directWord(
        difficulty: Difficulty,
        language: AppLanguage,
    ): Problem {
        val op = Op.entries.random(random)
        val (a, b) = operandsFor(op, difficulty)
        val answer = when (op) {
            Op.ADD -> a + b
            Op.SUB -> a - b
            Op.MUL -> a * b
        }
        val text = templatesFor(op, language).random(random)
            .replace("{name}", PersonalContent.NAMES.random(random))
            // Romanian slot: the number plus "de" when it needs one
            // ("12 pagini" but "45 de pagini").
            .replace("{a_de}", if (a < 20) "$a" else "$a de")
            .replace("{b_de}", if (b < 20) "$b" else "$b de")
            .replace("{a}", a.toString())
            .replace("{b}", b.toString())
        // What the story means, not which buttons to press.
        val storyHint = when (op) {
            Op.ADD -> when (language) {
                AppLanguage.ENGLISH -> "Add the two amounts together."
                AppLanguage.ROMANIAN -> "Adună cele două cantități."
            }
            Op.SUB -> when (language) {
                AppLanguage.ENGLISH -> "Subtract the part that goes away."
                AppLanguage.ROMANIAN -> "Scade partea care se duce."
            }
            Op.MUL -> when (language) {
                AppLanguage.ENGLISH -> "Equal groups repeat, so multiply."
                AppLanguage.ROMANIAN -> "Grupuri egale se repetă, deci înmulțește."
            }
        }
        val notes = if (difficulty == Difficulty.EASY) emptyList() else wordNotes(language)
        val ro = language == AppLanguage.ROMANIAN
        // Name the operation the story asks for, then do it. The first
        // line is the part worth learning; the second is just arithmetic.
        val whichOperation = when (op) {
            Op.ADD -> if (ro) "Cele două cantități se adună." else "The two amounts join together."
            Op.SUB -> if (ro) "O parte se duce, deci se scade." else "A part goes away, so subtract."
            Op.MUL ->
                if (ro) "Același grup se repetă, deci se înmulțește." else "The same group repeats, so multiply."
        }
        val sum = when (op) {
            Op.ADD -> "$a + $b = $answer"
            Op.SUB -> "$a − $b = $answer"
            Op.MUL -> "$a × $b = $answer"
        }
        return Problem(
            text = text,
            answer = answer,
            difficulty = difficulty,
            kind = ProblemKind.WORD,
            hints = listOf(storyHint, HintText.digits(answer, language)),
            notes = notes,
            solution = listOf(whichOperation, sum),
        )
    }

    /** A tiny story-to-math glossary for the helper sheet. */
    private fun wordNotes(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "“In total” or “altogether” → the amounts add up.",
            "“Left” or “remain” → something is taken away.",
            "“Each” or “in every” → the same group repeats: multiply.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "„În total” sau „laolaltă” → cantitățile se adună.",
            "„Au rămas” → ceva se scade.",
            "„Câte … fiecare” → același grup se repetă: înmulțire.",
        )
    }

    /** At MEDIUM nothing on screen or in the answer passes 3500. */
    private fun operandsFor(op: Op, difficulty: Difficulty): Pair<Int, Int> = when (op) {
        Op.ADD -> when (difficulty) {
            Difficulty.EASY -> random.nextInt(11, 50) to random.nextInt(11, 50)
            Difficulty.MEDIUM -> random.nextInt(250, 1800) to random.nextInt(150, 1500)
            Difficulty.HARD -> random.nextInt(250, 900) to random.nextInt(250, 900)
        }

        Op.SUB -> {
            val a = when (difficulty) {
                Difficulty.EASY -> random.nextInt(25, 100)
                Difficulty.MEDIUM -> random.nextInt(600, 3500)
                Difficulty.HARD -> random.nextInt(400, 1000)
            }
            // Keep b close to a at higher levels for harder borrows.
            val bMin = if (difficulty == Difficulty.EASY) 3 else a / 3
            a to random.nextInt(bMin, a)
        }

        Op.MUL -> when (difficulty) {
            Difficulty.EASY -> random.nextInt(3, 10) to random.nextInt(3, 10)
            Difficulty.MEDIUM -> random.nextInt(25, 120) to random.nextInt(3, 10)
            Difficulty.HARD -> random.nextInt(12, 26) to random.nextInt(11, 20)
        }
    }

    private fun templatesFor(op: Op, language: AppLanguage): List<String> = when (op) {
        Op.ADD -> if (language == AppLanguage.ROMANIAN) ADD_TEMPLATES_RO else ADD_TEMPLATES
        Op.SUB -> if (language == AppLanguage.ROMANIAN) SUB_TEMPLATES_RO else SUB_TEMPLATES
        Op.MUL -> if (language == AppLanguage.ROMANIAN) MUL_TEMPLATES_RO else MUL_TEMPLATES
    }

    companion object {
        /** How many rerolls before taking whatever the dice gave. */
        private const val REROLL_LIMIT = 12

        /**
         * Share of HARD riddle rolls that become a [ShowpieceGenerator]
         * puzzle. Riddles are ~16% of a HARD sitting, so this lands a
         * showpiece roughly every fifteenth hard problem: often enough to
         * look forward to, rare enough that thirty of them last.
         */
        private const val SHOWPIECE_PERCENT = 40

        /** Identical questions blocked for this many problems. */
        private const val RECENT_TEXTS = 25

        /**
         * Same-template questions blocked for this many. Deliberately
         * short: a sitting with one topic on has only a handful of
         * templates, and starving the roll is worse than a repeat.
         */
        private const val RECENT_SHAPES = 5

        private val DIGITS = Regex("\\d+")

        // ════════════════════════════════════════════════════════════════
        // WORD PROBLEM TEMPLATES, in both languages. {name} → a name from
        // PersonalContent.NAMES, {a} and {b} → the two numbers. In
        // Romanian, write {a_de}/{b_de} when a counted noun follows. It
        // becomes "12" or "45 de" as the grammar needs. Keep the math
        // implied by each list: ADD = a + b, SUB = a − b, MUL = a × b.
        // ════════════════════════════════════════════════════════════════
        // ── Two-step stories: {a} groups of {b}, then {c} more or fewer.
        // The answer is a × b + c, or a × b − c. Keep both steps in the
        // story and keep the order of the placeholders.
        private val TWO_STEP_ADD = listOf(
            "{name} baked {a} trays of {b} muffins and then {c} more by hand. How many muffins in all?",
            "{name} planted {a} rows of {b} onions, then squeezed in {c} extra at the edge. How many onions?",
            "{name} packed {a} boxes with {b} jars each and carried {c} loose jars besides. How many jars?",
            "{name} knitted {a} squares a day for {b} days, then was given {c} finished ones. How many squares?",
        )
        private val TWO_STEP_ADD_RO = listOf(
            "{name} a copt {a} tăvi cu {b} brioșe și încă {c_de} făcute de mână. Câte brioșe în total?",
            "{name} a plantat {a} rânduri de {b} cepe, apoi a mai îndesat {c_de} la margine. Câte cepe?",
            "{name} a pus {a} cutii cu câte {b} borcane și a mai dus {c_de} borcane răzlețe. Câte borcane?",
            "{name} a tricotat {a} pătrate pe zi timp de {b} zile, apoi a primit {c_de} gata făcute. Câte pătrate?",
        )
        private val TWO_STEP_SUB = listOf(
            "{name} filled {a} baskets with {b} apples each, then {c} turned out to be bruised. How many good apples?",
            "{name} set out {a} tables with {b} chairs each, then took {c} chairs away. How many chairs are left?",
            "{name} bought {a} packets of {b} candles and burned {c} of them over winter. How many candles are left?",
            "{name} picked {a} bunches of {b} grapes, and {c} grapes fell on the way home. How many arrived?",
        )
        private val TWO_STEP_SUB_RO = listOf(
            "{name} a umplut {a} coșuri cu câte {b} mere, apoi {c_de} s-au dovedit lovite. Câte mere bune?",
            "{name} a pus {a} mese cu câte {b} scaune, apoi a luat {c_de} scaune. Câte scaune au rămas?",
            "{name} a cumpărat {a} pachete de {b} lumânări și a ars {c_de} peste iarnă. Câte lumânări au rămas?",
            "{name} a cules {a} ciorchini de {b} boabe, iar {c_de} boabe au căzut pe drum. Câte au ajuns acasă?",
        )

        // ── Backwards stories: {b} went away, {r} are left, the question
        // is what there was to begin with. The answer is b + r.
        private val REVERSE_TEMPLATES = listOf(
            "{name} gave {b} plums to the neighbours and has {r} left. How many were there to start with?",
            "{name} sold {b} eggs at the market and came home with {r}. How many eggs were there at first?",
            "{name} has a notebook that lost {b} pages and still has {r}. How many pages did it have?",
            "{name} used {b} beads on a necklace and {r} are still in the tin. How many beads were there before?",
            "{name} threw a party: {b} guests have already left and {r} are still there. How many came in all?",
        )

        // Romanian avoids the possessive here on purpose: "petrecerea lui
        // Maria" is wrong for a feminine name, and the pool has both.
        private val REVERSE_TEMPLATES_RO = listOf(
            "{name} a dat {b_de} prune vecinilor și i-au rămas {r_de}. Câte erau la început?",
            "{name} a vândut {b_de} ouă la piață și a venit acasă cu {r_de}. Câte ouă erau la început?",
            "{name} are un caiet din care au căzut {b_de} pagini; mai are {r_de}. Câte pagini avea?",
            "{name} a folosit {b_de} mărgele la un colier și {r_de} au rămas în cutie. Câte mărgele erau înainte?",
            "{name} a dat o petrecere: {b_de} invitați au plecat deja și {r_de} mai sunt acolo. Câți au venit în total?",
        )

        private val ADD_TEMPLATES = listOf(
            "{name} picked {a} strawberries in the morning and {b} more after lunch. How many in total?",
            "{name} read {a} pages yesterday and {b} pages today. How many pages altogether?",
            "{name} walked {a} minutes on Monday and {b} minutes on Tuesday. How many minutes in total?",
            "{name} counted {a} monkeys and {b} parrots at the zoo. How many animals in total?",
            "{name} borrowed {a} library books in spring and {b} more in autumn. How many books in total?",
            "{name} poured {a} liters of water and {b} liters of soda for the picnic. How many liters in total?",
            "{name} collected {a} stamps and was gifted {b} more. How many stamps in total?",
            "{name} solved {a} crossword clues before lunch and {b} after. How many clues in total?",
        )
        private val SUB_TEMPLATES = listOf(
            "{name} baked {a} cookies and gave {b} to the neighbours. How many are left?",
            "{name} had {a} euros and spent {b} at the market. How much is left?",
            "{name} planted {a} seeds but {b} didn't sprout. How many grew?",
            "{name} saw {a} visitors at the zoo on Saturday and {b} on Sunday. How many more came on Saturday?",
            "A book has {a} pages and {name} has read {b}. How many pages are left?",
            "{name} filled a barrel with {a} liters and used {b} liters in the garden. How many liters are left?",
            "{name} stacked {a} books at the library and lent out {b}. How many remain on the shelf?",
            "{name} baked {a} pretzels and sold {b} at the school fair. How many are left?",
        )
        private val MUL_TEMPLATES = listOf(
            "{name} bought {a} boxes with {b} chocolates in each. How many chocolates?",
            "{name} planted {a} rows of {b} tulips. How many tulips in the garden?",
            "{name} made {a} photo albums with {b} photos in each. How many photos?",
            "{name} watched {a} aquariums with {b} fish in each at the zoo. How many fish?",
            "{name} packed {a} crates with {b} bottles of soda in each. How many bottles?",
            "{name} tidied {a} library shelves with {b} books on each. How many books?",
            "{name} filled {a} baskets with {b} apples in each. How many apples?",
            "{name} prepared {a} school kits with {b} pencils in each. How many pencils?",
        )

        private val ADD_TEMPLATES_RO = listOf(
            "{name} a cules {a_de} căpșuni dimineața și încă {b} după prânz. Câte în total?",
            "{name} a citit {a_de} pagini ieri și {b_de} pagini azi. Câte pagini în total?",
            "{name} a mers {a_de} minute luni și {b_de} minute marți. Câte minute în total?",
            "{name} a numărat {a_de} maimuțe și {b_de} papagali la zoo. Câte animale în total?",
            "{name} a împrumutat {a_de} cărți de la bibliotecă primăvara și încă {b} toamna. Câte cărți în total?",
            "{name} a turnat {a_de} litri de apă și {b_de} litri de suc pentru picnic. Câți litri în total?",
            "{name} a strâns {a_de} timbre și a mai primit {b} cadou. Câte timbre în total?",
            "{name} a rezolvat {a_de} definiții de rebus înainte de prânz și {b} după. Câte în total?",
        )
        private val SUB_TEMPLATES_RO = listOf(
            "{name} a copt {a_de} biscuiți și a dat {b} vecinilor. Câți au rămas?",
            "{name} avea {a_de} euro și a cheltuit {b} la piață. Cât a rămas?",
            "{name} a plantat {a_de} semințe, dar {b} nu au răsărit. Câte au crescut?",
            "{name} a văzut {a_de} vizitatori la zoo sâmbătă și {b} duminică. Cu câți mai mulți sâmbătă?",
            "O carte are {a_de} pagini și {name} a citit {b}. Câte pagini au rămas?",
            "{name} a umplut un butoi cu {a_de} litri și a folosit {b} în grădină. Câți litri au rămas?",
            "{name} a așezat {a_de} cărți la bibliotecă și a împrumutat {b}. Câte au rămas pe raft?",
            "{name} a copt {a_de} covrigi și a vândut {b} la târgul școlii. Câți au rămas?",
        )
        private val MUL_TEMPLATES_RO = listOf(
            "{name} a cumpărat {a_de} cutii cu câte {b} bomboane. Câte bomboane în total?",
            "{name} a plantat {a_de} rânduri de câte {b} lalele. Câte lalele în grădină?",
            "{name} a făcut {a_de} albume cu câte {b} poze. Câte poze în total?",
            "{name} a privit {a_de} acvarii cu câte {b} pești la zoo. Câți pești în total?",
            "{name} a umplut {a_de} lăzi cu câte {b} sticle de suc. Câte sticle în total?",
            "{name} a aranjat {a_de} rafturi cu câte {b} cărți la bibliotecă. Câte cărți în total?",
            "{name} a umplut {a_de} coșuri cu câte {b} mere. Câte mere în total?",
            "{name} a pregătit {a_de} penare cu câte {b} creioane pentru școală. Câte creioane în total?",
        )
    }
}
