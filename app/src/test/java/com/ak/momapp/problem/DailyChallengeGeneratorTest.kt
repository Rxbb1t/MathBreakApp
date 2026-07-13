package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every arc's math is re-derived from the printed numbers, stage by
 * stage, so a broken chain (a stage quoting the wrong earlier answer)
 * fails loudly. Days are walked in order so all arcs get exercised.
 */
class DailyChallengeGeneratorTest {

    private val generator = DailyChallengeGenerator()
    private val numbers = Regex("""\d+""")

    private fun nums(text: String): List<Int> =
        numbers.findAll(text).map { it.value.toInt() }.toList()

    /** Minutes between the clock readings "h:mm … h:mm" in the text. */
    private fun clockSpan(text: String): Int {
        val n = nums(text)
        return (n[2] * 60 + n[3]) - (n[0] * 60 + n[1])
    }

    @Test
    fun `the same date always deals the same challenge`() {
        val date = LocalDate.of(2026, 7, 10)
        val first = generator.generate(date, AppLanguage.ENGLISH)
        val second = generator.generate(date, AppLanguage.ENGLISH)
        assertEquals(first, second)
    }

    @Test
    fun `both languages share the same numbers and answers`() {
        for (day in 0 until 60L) {
            val date = LocalDate.ofEpochDay(day)
            val english = generator.generate(date, AppLanguage.ENGLISH)
            val romanian = generator.generate(date, AppLanguage.ROMANIAN)
            assertEquals(
                "answers differ on $date",
                english.stages.map(Problem::answer),
                romanian.stages.map(Problem::answer),
            )
        }
    }

    @Test
    fun `every day of every arc is well-formed`() {
        for (day in 0 until 120L) {
            for (language in AppLanguage.entries) {
                val challenge = generator.generate(LocalDate.ofEpochDay(day), language)
                assertTrue("blank intro on day $day", challenge.intro.isNotBlank())
                assertEquals("stage count on day $day", 5, challenge.stages.size)
                for (stage in challenge.stages) {
                    assertTrue("negative answer in '${stage.text}'", stage.answer >= 0)
                    assertEquals("hints in '${stage.text}'", 2, stage.hints.size)
                    assertTrue("blank hint in '${stage.text}'", stage.hints.all(String::isNotBlank))
                    assertTrue("placeholder in '${stage.text}'", !stage.text.contains("{"))
                }
            }
        }
    }

    @Test
    fun `market day chains its answers correctly`() {
        forEachArcDay(arc = 0) { stages ->
            val (s1, s2, s3, s4, s5) = stages
            val n1 = nums(s1.text)
            assertEquals("'${s1.text}'", n1[0] * n1[1] + n1[2] * n1[3], s1.answer)
            assertEquals("lei", s1.answerUnit)

            val n2 = nums(s2.text)
            assertEquals("stage 2 must quote stage 1's answer", s1.answer, n2[1])
            assertEquals("'${s2.text}'", 200 - s1.answer, s2.answer)

            assertEquals("'${s3.text}'", clockSpan(s3.text), s3.answer)
            assertEquals("min", s3.answerUnit)

            val n4 = nums(s4.text)
            assertEquals("'${s4.text}'", n4[0] * n4[1], s4.answer)

            val n5 = nums(s5.text)
            assertEquals("stage 5 must quote stage 1's answer", s1.answer, n5[0])
            assertEquals("'${s5.text}'", n5[0] + n5[1] + n5[2], s5.answer)
        }
    }

    @Test
    fun `garden day chains its answers correctly`() {
        forEachArcDay(arc = 1) { stages ->
            val (s1, s2, s3, s4, s5) = stages
            val (a, b) = nums(s1.text)
            assertEquals("'${s1.text}'", 2 * (a + b), s1.answer)
            assertTrue("no diagram in '${s1.text}'", s1.diagram is Diagram.Rectangle)

            val n2 = nums(s2.text)
            assertEquals("stage 2 must quote the fence length", s1.answer, n2[1])
            assertEquals("'${s2.text}'", n2[0] * s1.answer, s2.answer)

            val n3 = nums(s3.text)
            assertEquals("stage 3 reuses the same garden", listOf(a, b), n3.take(2))
            assertEquals("'${s3.text}'", a * b, s3.answer)
            assertTrue(
                "stage 3 should show the grid",
                (s3.diagram as Diagram.Rectangle).grid,
            )

            val n4 = nums(s4.text)
            assertEquals("stage 4 must quote the square count", s3.answer, n4[0])
            assertEquals("'${s4.text}'", s3.answer - n4[1], s4.answer)

            assertEquals("'${s5.text}'", clockSpan(s5.text), s5.answer)
        }
    }

    @Test
    fun `visit day chains its answers correctly`() {
        forEachArcDay(arc = 2) { stages ->
            val (s1, s2, s3, s4, s5) = stages
            assertEquals("'${s1.text}'", clockSpan(s1.text), s1.answer)

            val n2 = nums(s2.text)
            assertEquals("'${s2.text}'", n2[0] * n2[1], s2.answer)

            val n3 = nums(s3.text)
            assertEquals("'${s3.text}'", n3[0] - n3[1], s3.answer)

            val n4 = nums(s4.text)
            assertEquals("stage 4 must quote the train ride", s1.answer, n4[1])
            assertEquals("'${s4.text}'", n4[0] + s1.answer, s4.answer)

            val n5 = nums(s5.text)
            assertEquals("stage 5 must quote the gift cost", s2.answer, n5[1])
            assertEquals("'${s5.text}'", n5[0] - n5[1] - n5[2], s5.answer)
        }
    }

    @Test
    fun `baking day chains its answers correctly`() {
        forEachArcDay(arc = 3) { stages ->
            val (s1, s2, s3, s4, s5) = stages
            val n1 = nums(s1.text)
            assertEquals("'${s1.text}'", n1[0] + n1[1] + n1[2], s1.answer)
            assertEquals("lei", s1.answerUnit)

            val n2 = nums(s2.text)
            assertEquals("stage 2 must quote stage 1's answer", s1.answer, n2[1])
            assertEquals("'${s2.text}'", 100 - s1.answer, s2.answer)

            val n3 = nums(s3.text)
            assertEquals("'${s3.text}'", n3[0] * n3[1], s3.answer)

            val n4 = nums(s4.text)
            assertEquals("stage 4 must quote the batch size", s3.answer, n4[0])
            assertEquals("'${s4.text}'", s3.answer - n4[1], s4.answer)
            assertTrue("negative covrigi in '${s4.text}'", s4.answer > 0)

            assertEquals("'${s5.text}'", clockSpan(s5.text), s5.answer)
            assertEquals("min", s5.answerUnit)
        }
    }

    @Test
    fun `park day chains its answers correctly`() {
        forEachArcDay(arc = 4) { stages ->
            val (s1, s2, s3, s4, s5) = stages
            assertEquals("'${s1.text}'", clockSpan(s1.text), s1.answer)

            val (a, b) = nums(s2.text)
            assertEquals("'${s2.text}'", 2 * (a + b), s2.answer)
            assertTrue("no diagram in '${s2.text}'", s2.diagram is Diagram.Rectangle)

            val n3 = nums(s3.text)
            assertEquals("stage 3 must quote the lap length", s2.answer, n3[1])
            assertEquals("'${s3.text}'", n3[0] * s2.answer, s3.answer)

            val n4 = nums(s4.text)
            assertEquals("stage 4 must quote today's distance", s3.answer, n4[1])
            assertEquals("'${s4.text}'", n4[0] + n4[1], s4.answer)

            val n5 = nums(s5.text)
            assertEquals("'${s5.text}'", n5[0] - n5[1], s5.answer)
            assertEquals("lei", s5.answerUnit)
        }
    }

    /** Runs [check] for 40 days of one arc, in both languages. */
    private fun forEachArcDay(arc: Int, check: (List<Problem>) -> Unit) {
        var day = arc.toLong()
        repeat(40) {
            for (language in AppLanguage.entries) {
                check(generator.generate(LocalDate.ofEpochDay(day), language).stages)
            }
            day += DailyChallengeGenerator.ARC_COUNT
        }
    }
}
