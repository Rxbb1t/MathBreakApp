package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every equation shape is matched by a regex and its math re-derived from
 * the text, so a wrong answer or a malformed problem fails loudly.
 */
class EquationGeneratorTest {

    private val generator = EquationGenerator(Random(seed = 11))

    private fun many(difficulty: Difficulty): List<Problem> =
        List(SAMPLE_SIZE) { generator.generate(difficulty, AppLanguage.ENGLISH) }

    @Test
    fun `medium equations compute correctly and every shape appears`() {
        val seen = mutableSetOf<String>()
        for (problem in many(Difficulty.MEDIUM)) {
            assertEquals(ProblemKind.EQUATION, problem.kind)
            val text = problem.text
            val x = problem.answer
            when {
                FIND_X.matches(text) -> {
                    seen += "findX"
                    val (a, op, b, rhs) = FIND_X.find(text)!!.destructured
                    val lhs = if (op == "+") a.toInt() * x + b.toInt() else a.toInt() * x - b.toInt()
                    assertEquals("'$text'", rhs.toInt(), lhs)
                    assertTrue("non-positive rhs in '$text'", rhs.toInt() > 0)
                }

                SYSTEM.matches(text) -> {
                    seen += "system"
                    val (s, d, asked) = SYSTEM.find(text)!!.destructured
                    val bigger = (s.toInt() + d.toInt()) / 2
                    val smaller = s.toInt() - bigger
                    assertEquals("odd system in '$text'", 0, (s.toInt() + d.toInt()) % 2)
                    assertTrue("y not positive in '$text'", smaller >= 1)
                    assertEquals("'$text'", if (asked == "x") bigger else smaller, x)
                }

                SUBSTITUTION.matches(text) -> {
                    seen += "substitution"
                    val (a, c, b) = SUBSTITUTION.find(text)!!.destructured
                    assertEquals("'$text'", c.toInt(), a.toInt() * x + b.toInt())
                }

                ROOT_PLUS.matches(text) -> {
                    seen += "root"
                    val (n, c) = ROOT_PLUS.find(text)!!.destructured
                    val root = exactRoot(n.toInt(), text)
                    assertEquals("'$text'", c.toInt(), x + root)
                }

                SQUARE_PLUS.matches(text) -> {
                    seen += "squarePlus"
                    val (a, c) = SQUARE_PLUS.find(text)!!.destructured
                    assertEquals("'$text'", c.toInt(), a.toInt() * a.toInt() + x)
                }

                X_SQUARED.matches(text) -> {
                    seen += "xSquared"
                    assertEquals("'$text'", X_SQUARED.find(text)!!.groupValues[1].toInt(), x * x)
                }

                X_CUBED.matches(text) -> {
                    seen += "xCubed"
                    assertEquals("'$text'", X_CUBED.find(text)!!.groupValues[1].toInt(), x * x * x)
                }

                else -> throw AssertionError("unrecognised medium equation: '$text'")
            }
        }
        assertEquals(
            setOf("findX", "system", "substitution", "root", "squarePlus", "xSquared", "xCubed"),
            seen,
        )
    }

    @Test
    fun `hard equations compute correctly and every shape appears`() {
        val seen = mutableSetOf<String>()
        for (problem in many(Difficulty.HARD)) {
            val text = problem.text
            val x = problem.answer
            when {
                FIND_X_LONG.matches(text) -> {
                    seen += "findXLong"
                    val (a, b, c, rhs) = FIND_X_LONG.find(text)!!.destructured
                    assertEquals("'$text'", rhs.toInt(), a.toInt() * x + b.toInt() - c.toInt())
                    assertTrue("negative rhs in '$text'", rhs.toInt() >= 0)
                }

                TWO_COEFFICIENTS.matches(text) -> {
                    seen += "twoCoefficients"
                    val (a, b, e, y) = TWO_COEFFICIENTS.find(text)!!.destructured
                    assertEquals("'$text'", e.toInt(), a.toInt() * x + b.toInt() * y.toInt())
                }

                PRODUCT_SYSTEM.matches(text) -> {
                    seen += "productSystem"
                    val (s, d) = PRODUCT_SYSTEM.find(text)!!.destructured
                    val bigger = (s.toInt() + d.toInt()) / 2
                    val smaller = s.toInt() - bigger
                    assertEquals("'$text'", bigger * smaller, x)
                }

                SQUARE_MINUS.matches(text) -> {
                    seen += "squareMinus"
                    val (b, c) = SQUARE_MINUS.find(text)!!.destructured
                    assertEquals("'$text'", c.toInt(), x * x - b.toInt())
                }

                ROOT_CHAIN.matches(text) -> {
                    seen += "rootChain"
                    val (n, a, b) = ROOT_CHAIN.find(text)!!.destructured
                    val root = exactRoot(n.toInt(), text)
                    assertEquals("'$text'", root * a.toInt() - b.toInt(), x)
                    assertTrue("negative answer in '$text'", x >= 0)
                }

                QUADRATIC_DERIVATIVE.matches(text) -> {
                    seen += "quadraticDerivative"
                    val (a, b, _, k) = QUADRATIC_DERIVATIVE.find(text)!!.destructured
                    assertEquals("'$text'", 2 * a.toInt() * k.toInt() + b.toInt(), x)
                }

                CUBIC_DERIVATIVE.matches(text) -> {
                    seen += "cubicDerivative"
                    val (a, b, _, k) = CUBIC_DERIVATIVE.find(text)!!.destructured
                    assertEquals("'$text'", 3 * a.toInt() * k.toInt() * k.toInt() + b.toInt(), x)
                }

                else -> throw AssertionError("unrecognised hard equation: '$text'")
            }
        }
        assertEquals(
            setOf(
                "findXLong", "twoCoefficients", "productSystem", "squareMinus", "rootChain",
                "quadraticDerivative", "cubicDerivative",
            ),
            seen,
        )
    }

    @Test
    fun `derivative problems carry the power rule on the notebook sheet`() {
        val derivatives = many(Difficulty.HARD).filter { it.text.contains("f′(") }
        assertTrue("no derivatives in $SAMPLE_SIZE hard samples", derivatives.isNotEmpty())
        for (problem in derivatives) {
            assertTrue(
                "no power rule note for '${problem.text}'",
                problem.notes.any { it.contains("xⁿ") },
            )
        }
    }

    @Test
    fun `romanian equations use the romanian label`() {
        val romanian = List(SAMPLE_SIZE) {
            generator.generate(Difficulty.MEDIUM, AppLanguage.ROMANIAN)
        }
        assertTrue(romanian.none { it.text.startsWith("Find x") })
        assertTrue(
            "no Romanian find-x sampled",
            romanian.any { it.text.startsWith("Află x") },
        )
    }

    private fun exactRoot(square: Int, text: String): Int {
        val root = sqrt(square.toDouble()).toInt()
        assertEquals("√$square is not exact in '$text'", square, root * root)
        return root
    }

    companion object {
        private const val SAMPLE_SIZE = 500

        private val FIND_X = Regex("""^Find x:\n(\d+)x ([+−]) (\d+) = (\d+)$""")
        private val SYSTEM = Regex("""^x \+ y = (\d+)\nx − y = (\d+)\n([xy]) = \?$""")
        private val SUBSTITUTION = Regex("""^(\d+)x \+ y = (\d+)\ny = (\d+)\nx = \?$""")
        private val ROOT_PLUS = Regex("""^x \+ √(\d+) = (\d+)\nx = \?$""")
        private val SQUARE_PLUS = Regex("""^(\d+)² \+ x = (\d+)\nx = \?$""")
        private val X_SQUARED = Regex("""^x² = (\d+)\nx = \?$""")
        private val X_CUBED = Regex("""^x³ = (\d+)\nx = \?$""")

        private val FIND_X_LONG = Regex("""^Find x:\n(\d+)x \+ (\d+) − (\d+) = (\d+)$""")
        private val TWO_COEFFICIENTS = Regex("""^(\d+)x \+ (\d+)y = (\d+)\ny = (\d+)\nx = \?$""")
        private val PRODUCT_SYSTEM = Regex("""^x \+ y = (\d+)\nx − y = (\d+)\nx × y = \?$""")
        private val SQUARE_MINUS = Regex("""^x² − (\d+) = (\d+)\nx = \?$""")
        private val ROOT_CHAIN = Regex("""^√(\d+) × (\d+) − (\d+) = \?$""")
        private val QUADRATIC_DERIVATIVE =
            Regex("""^f\(x\) = (\d+)x² \+ (\d+)x \+ (\d+)\nf′\((\d+)\) = \?$""")
        private val CUBIC_DERIVATIVE =
            Regex("""^f\(x\) = (\d+)x³ \+ (\d+)x \+ (\d+)\nf′\((\d+)\) = \?$""")
    }
}
