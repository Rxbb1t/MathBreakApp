package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemGeneratorTest {

    private val generator = ProblemGenerator(Random(seed = 42))

    private fun manyProblems(
        difficulty: Difficulty,
        language: AppLanguage = AppLanguage.ENGLISH,
    ): List<Problem> = List(SAMPLE_SIZE) {
        generator.generate(difficulty, language)
    }

    @Test
    fun `answers are always non-negative and fit the input field`() {
        for (difficulty in Difficulty.entries) {
            for (problem in manyProblems(difficulty)) {
                assertTrue("negative answer in '${problem.text}'", problem.answer >= 0)
                assertTrue("answer too big in '${problem.text}'", problem.answer <= 99_999)
            }
        }
    }

    @Test
    fun `every typed problem carries exactly two non-blank hints, tap exercises none`() {
        for (difficulty in Difficulty.entries) {
            for (language in AppLanguage.entries) {
                for (problem in manyProblems(difficulty, language = language)) {
                    if (problem.tapAnswered) {
                        assertTrue("hints in tap '${problem.text}'", problem.hints.isEmpty())
                    } else {
                        assertEquals("hints in '${problem.text}'", 2, problem.hints.size)
                        assertTrue(
                            "blank hint in '${problem.text}'",
                            problem.hints.all(String::isNotBlank),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `no unfilled placeholders remain in text or hints`() {
        // Word-style placeholders only — set problems legitimately print
        // braces ("A = {3, 7, 12}").
        val placeholder = Regex("""\{[a-zA-Z_]+\}""")
        for (difficulty in Difficulty.entries) {
            for (problem in manyProblems(difficulty)) {
                assertFalse(
                    "placeholder left in '${problem.text}'",
                    placeholder.containsMatchIn(problem.text),
                )
                assertTrue(
                    "placeholder left in hints of '${problem.text}'",
                    problem.hints.none { placeholder.containsMatchIn(it) },
                )
            }
        }
    }

    @Test
    fun `problems carry the requested difficulty`() {
        for (difficulty in Difficulty.entries) {
            assertTrue(manyProblems(difficulty).all { it.difficulty == difficulty })
        }
    }

    @Test
    fun `each difficulty mixes its expected kinds`() {
        for (difficulty in Difficulty.entries) {
            val kinds = manyProblems(difficulty).map { it.kind }.toSet()
            val core = if (difficulty == Difficulty.EASY) {
                ProblemKind.ARITHMETIC
            } else {
                ProblemKind.EQUATION
            }
            val everywhere = listOf(
                core, ProblemKind.PUZZLE, ProblemKind.LOGIC, ProblemKind.WORD,
                ProblemKind.MONEY, ProblemKind.TIME, ProblemKind.COMPARE, ProblemKind.TARGET,
                ProblemKind.SELECT, ProblemKind.SETS, ProblemKind.TRUE_FALSE,
                ProblemKind.MISSING_OP,
            )
            for (expected in everywhere) {
                assertTrue("no $expected at $difficulty in $SAMPLE_SIZE samples", expected in kinds)
            }
            if (difficulty == Difficulty.EASY) {
                assertFalse("geometry at EASY", ProblemKind.GEOMETRY in kinds)
            } else {
                assertTrue(
                    "no GEOMETRY at $difficulty in $SAMPLE_SIZE samples",
                    ProblemKind.GEOMETRY in kinds,
                )
            }
        }
    }

    @Test
    fun `plain chains only exist at easy and equations only above it`() {
        for (difficulty in Difficulty.entries) {
            val kinds = manyProblems(difficulty).map { it.kind }.toSet()
            if (difficulty == Difficulty.EASY) {
                assertFalse("equations at EASY", ProblemKind.EQUATION in kinds)
            } else {
                assertFalse("plain chains at $difficulty", ProblemKind.ARITHMETIC in kinds)
            }
        }
    }

    /** Evaluates "a op b op c… = ?" with ×÷ before ±; null if another shape. */
    private fun evalChain(text: String): Int? {
        if (!text.endsWith(" = ?")) return null
        val tokens = text.removeSuffix(" = ?").split(" ")
        if (tokens.size < 3 || tokens.size % 2 == 0) return null
        val numbers = tokens.filterIndexed { i, _ -> i % 2 == 0 }
            .map { it.toIntOrNull() ?: return null }
        val ops = tokens.filterIndexed { i, _ -> i % 2 == 1 }
        if (ops.any { it !in listOf("+", "−", "×", "÷") }) return null

        var result = 0
        var sign = 1
        var term = numbers[0]
        for (i in ops.indices) {
            val n = numbers[i + 1]
            when (ops[i]) {
                "×" -> term *= n
                "÷" -> {
                    assertEquals("non-exact division in '$text'", 0, term % n)
                    term /= n
                }
                else -> {
                    result += sign * term
                    sign = if (ops[i] == "+") 1 else -1
                    term = n
                }
            }
        }
        return result + sign * term
    }

    private fun numberCount(text: String): Int =
        (text.removeSuffix(" = ?").split(" ").size + 1) / 2

    @Test
    fun `easy chains have three or four numbers, compute correctly, and use all operations`() {
        val chains = manyProblems(Difficulty.EASY).filter { it.kind == ProblemKind.ARITHMETIC }
        assertTrue("no chains in $SAMPLE_SIZE easy samples", chains.isNotEmpty())
        for (problem in chains) {
            val value = evalChain(problem.text)
                ?: throw AssertionError("unparseable chain: '${problem.text}'")
            assertEquals("'${problem.text}'", value, problem.answer)
            val count = numberCount(problem.text)
            assertTrue("easy chain with $count numbers '${problem.text}'", count in 3..4)
        }
        for (op in listOf("+", "−", "×", "÷")) {
            assertTrue("no $op in easy chains", chains.any { op in it.text })
        }
    }

    @Test
    fun `medium numbers reach past 500 but never past 3500`() {
        val digitRuns = Regex("""\d+""")
        var reachedPast500 = false
        for (problem in manyProblems(Difficulty.MEDIUM)) {
            assertTrue("answer over 3500 in '${problem.text}'", problem.answer <= 3500)
            for (source in problem.hints + problem.text) {
                for (match in digitRuns.findAll(source)) {
                    assertTrue(
                        "number ${match.value} over 3500 in '$source' (problem '${problem.text}')",
                        match.value.toInt() <= 3500,
                    )
                    if (match.value.toInt() > 500) reachedPast500 = true
                }
            }
        }
        assertTrue("no medium number passed 500 in $SAMPLE_SIZE samples", reachedPast500)
    }

    @Test
    fun `word problems appear and are told with a pool name`() {
        for (difficulty in Difficulty.entries) {
            val wordProblems = manyProblems(difficulty)
                .filter { it.kind == ProblemKind.WORD }
            assertTrue("no word problems at $difficulty", wordProblems.isNotEmpty())
            for (problem in wordProblems) {
                assertTrue(
                    "no pool name in '${problem.text}'",
                    PersonalContent.NAMES.any { problem.text.contains(it) },
                )
            }
        }
    }

    /**
     * The mix weights geometry and logic at 14–16%, but the repeat
     * filter pulls the realised shares slightly below their weights: a
     * topic with fewer distinct problems gets rerolled more often, so it
     * is served a little less. That's the filter working as intended, so
     * the floor sits just under the nominal weight rather than on it.
     */
    @Test
    fun `geometry is about as common as riddles at normal and hard`() {
        for (difficulty in listOf(Difficulty.MEDIUM, Difficulty.HARD)) {
            val problems = List(2_000) { generator.generate(difficulty) }
            val geometryShare = problems.count { it.kind == ProblemKind.GEOMETRY } / 2_000.0
            val logicShare = problems.count { it.kind == ProblemKind.LOGIC } / 2_000.0
            assertTrue("geometry share at $difficulty was $geometryShare", geometryShare in 0.12..0.26)
            assertTrue("logic share at $difficulty was $logicShare", logicShare in 0.12..0.26)
        }
    }

    /** The kinds a topic switch controls at the given difficulty. */
    private fun kindsOf(topic: ProblemTopic, difficulty: Difficulty): Set<ProblemKind> = when (topic) {
        ProblemTopic.CORE -> setOf(
            if (difficulty == Difficulty.EASY) ProblemKind.ARITHMETIC else ProblemKind.EQUATION,
            ProblemKind.MISSING_OP,
        )
        ProblemTopic.PUZZLE -> setOf(ProblemKind.PUZZLE)
        ProblemTopic.LOGIC -> setOf(ProblemKind.LOGIC)
        ProblemTopic.GEOMETRY -> setOf(ProblemKind.GEOMETRY)
        ProblemTopic.MONEY -> setOf(ProblemKind.MONEY)
        ProblemTopic.TIME -> setOf(ProblemKind.TIME)
        ProblemTopic.WORD -> setOf(ProblemKind.WORD)
        ProblemTopic.COMPARE -> setOf(ProblemKind.COMPARE, ProblemKind.TRUE_FALSE)
        ProblemTopic.TARGET -> setOf(ProblemKind.TARGET)
        ProblemTopic.NUMBERS -> setOf(ProblemKind.SELECT, ProblemKind.SETS)
    }

    @Test
    fun `a switched-off story topic never deals its kind`() {
        for (difficulty in Difficulty.entries) {
            for (disabled in ProblemTopic.entries.filter { it != ProblemTopic.CORE }) {
                val kinds = List(SAMPLE_SIZE) {
                    generator.generate(
                        difficulty, AppLanguage.ENGLISH,
                        topics = ProblemTopic.ALL - disabled,
                    )
                }.map { it.kind }.toSet()
                for (banned in kindsOf(disabled, difficulty)) {
                    assertFalse("$banned appeared with $disabled off at $difficulty", banned in kinds)
                }
            }
        }
    }

    @Test
    fun `with core off the roll spans only the enabled stories`() {
        for (difficulty in Difficulty.entries) {
            val enabled = setOf(ProblemTopic.MONEY, ProblemTopic.TIME)
            val kinds = List(SAMPLE_SIZE) {
                generator.generate(difficulty, AppLanguage.ENGLISH, topics = enabled)
            }.map { it.kind }.toSet()
            assertEquals(
                "unexpected kinds at $difficulty: $kinds",
                setOf(ProblemKind.MONEY, ProblemKind.TIME),
                kinds,
            )
        }
    }

    @Test
    fun `two enabled topics still both appear when core stays on`() {
        for (difficulty in Difficulty.entries) {
            val kinds = List(SAMPLE_SIZE) {
                generator.generate(
                    difficulty, AppLanguage.ENGLISH,
                    topics = setOf(ProblemTopic.CORE, ProblemTopic.LOGIC),
                )
            }.map { it.kind }.toSet()
            val expected = kindsOf(ProblemTopic.CORE, difficulty) + ProblemKind.LOGIC
            assertEquals("unexpected kinds at $difficulty: $kinds", expected, kinds)
        }
    }

    @Test
    fun `core steps in when no enabled story can serve`() {
        // Geometry skips EASY, so with only it left on, EASY has nothing
        // to deal — core covers it.
        val kinds = List(SAMPLE_SIZE) {
            generator.generate(
                Difficulty.EASY, AppLanguage.ENGLISH,
                topics = setOf(ProblemTopic.GEOMETRY),
            )
        }.map { it.kind }.toSet()
        assertEquals(setOf(ProblemKind.ARITHMETIC, ProblemKind.MISSING_OP), kinds)
    }

    @Test
    fun `romanian problems avoid english wording`() {
        val englishMarkers = listOf(
            " of ", "Find x", "What", "How", "I add", "I double", "I think",
            "years", "slices", "legs", "heads", "left?", "next?",
        )
        for (difficulty in Difficulty.entries) {
            for (problem in manyProblems(difficulty, language = AppLanguage.ROMANIAN)) {
                for (marker in englishMarkers) {
                    assertFalse(
                        "english '$marker' in '${problem.text}'",
                        problem.text.contains(marker),
                    )
                }
            }
        }
    }

    companion object {
        private const val SAMPLE_SIZE = 500
    }
}
