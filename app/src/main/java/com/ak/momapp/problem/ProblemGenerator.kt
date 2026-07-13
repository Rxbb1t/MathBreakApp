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
 *  - money stories in lei ([MoneyProblemGenerator]). Every difficulty
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

    fun generate(
        difficulty: Difficulty,
        language: AppLanguage = AppLanguage.ENGLISH,
        topics: Set<ProblemTopic> = ProblemTopic.ALL,
    ): Problem {
        // Geometry rides only MEDIUM/HARD, about as common as the riddles.
        // Compare and target-builder taps lean EASY/MEDIUM and thin out
        // at HARD.
        val mix = when (difficulty) {
            Difficulty.EASY -> Mix(11, 14, 0, 11, 10, 11, 9, 9, 10)
            Difficulty.HARD -> Mix(10, 16, 15, 10, 9, 11, 5, 5, 9)
            else -> Mix(9, 15, 14, 9, 8, 11, 7, 7, 9)
        }
        // A switched-off topic loses its slice.
        fun weightOf(topic: ProblemTopic, weight: Int) = if (topic in topics) weight else 0
        val slices = listOf(
            Slice(weightOf(ProblemTopic.PUZZLE, mix.puzzle)) { puzzles.generate(difficulty, language) },
            Slice(weightOf(ProblemTopic.LOGIC, mix.logic)) {
                logic.generate(difficulty, PersonalContent.NAMES, language)
            },
            Slice(weightOf(ProblemTopic.GEOMETRY, mix.geometry)) { geometry.generate(difficulty, language) },
            Slice(weightOf(ProblemTopic.MONEY, mix.money)) { money.generate(difficulty, language) },
            Slice(weightOf(ProblemTopic.TIME, mix.time)) { time.generate(difficulty, language) },
            Slice(weightOf(ProblemTopic.WORD, mix.word)) { wordProblem(difficulty, language) },
            // One topic, two flavors: < = > taps and ✓/✗ claims.
            Slice(weightOf(ProblemTopic.COMPARE, mix.compare)) {
                if (random.nextBoolean()) {
                    compare.generate(difficulty, language)
                } else {
                    trueFalse.generate(difficulty, language)
                }
            },
            Slice(weightOf(ProblemTopic.TARGET, mix.target)) { target.generate(difficulty, language) },
            // One topic, two flavors: tap hunts and typed set counting.
            Slice(weightOf(ProblemTopic.NUMBERS, mix.numbers)) {
                if (random.nextBoolean()) {
                    hunt.generate(difficulty, language)
                } else {
                    sets.generate(difficulty, language)
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
        return when {
            random.nextInt(4) == 0 -> missingOp.generate(difficulty, language)
            difficulty == Difficulty.EASY -> easyChain(language)
            else -> equations.generate(difficulty, language)
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
        }

        // No step-by-step walkthroughs. A calm nudge, then the answer's shape.
        val calmHint = when (language) {
            AppLanguage.ENGLISH -> "No rush. Every step lands on a whole number."
            AppLanguage.ROMANIAN -> "Fără grabă. Fiecare pas dă un număr întreg."
        }
        return Problem(
            text = "$text = ?",
            answer = running,
            difficulty = Difficulty.EASY,
            kind = ProblemKind.ARITHMETIC,
            hints = listOf(calmHint, HintText.digits(running, language)),
        )
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

    private fun wordProblem(
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
                AppLanguage.ENGLISH -> "The two amounts come together. The answer is bigger than either one."
                AppLanguage.ROMANIAN -> "Cele două cantități se strâng laolaltă. Răspunsul e mai mare decât fiecare."
            }
            Op.SUB -> when (language) {
                AppLanguage.ENGLISH -> "Part of it goes away. The answer is smaller than the start."
                AppLanguage.ROMANIAN -> "O parte se duce. Răspunsul e mai mic decât la început."
            }
            Op.MUL -> when (language) {
                AppLanguage.ENGLISH -> "The same little group repeats. Count group by group."
                AppLanguage.ROMANIAN -> "Același grup mic se repetă. Numără grup cu grup."
            }
        }
        val notes = if (difficulty == Difficulty.EASY) emptyList() else wordNotes(language)
        return Problem(
            text = text,
            answer = answer,
            difficulty = difficulty,
            kind = ProblemKind.WORD,
            hints = listOf(storyHint, HintText.digits(answer, language)),
            notes = notes,
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
        // ════════════════════════════════════════════════════════════════
        // WORD PROBLEM TEMPLATES, in both languages. {name} → a name from
        // PersonalContent.NAMES, {a} and {b} → the two numbers. In
        // Romanian, write {a_de}/{b_de} when a counted noun follows. It
        // becomes "12" or "45 de" as the grammar needs. Keep the math
        // implied by each list: ADD = a + b, SUB = a − b, MUL = a × b.
        // ════════════════════════════════════════════════════════════════
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
            "{name} avea {a_de} lei și a cheltuit {b} la piață. Cât a rămas?",
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
