package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Mulțimi: two small sets written out in full; she counts the elements
 * of A ∩ B, A ∪ B or (at HARD) A \ B and types the count. EASY sticks
 * to intersections. Spotting the shared numbers is the friendly entry
 * point; the union and difference arrive with the levels above.
 */
class SetProblemGenerator(private val random: Random) {

    private enum class Op { INTERSECT, UNION, DIFFERENCE }

    fun generate(level: Level, language: AppLanguage): Problem {
        // The union joins the intersection first and the difference last,
        // each arriving over a stretch of the scale rather than at a band
        // boundary. At the bottom only the intersection is asked for.
        val op = random.pickWeighted(
            listOf(
                // Spotting the shared numbers is the friendly way in, and
                // it never stops being worth asking.
                Op.INTERSECT to 1.0,
                Op.UNION to level.ramp(Level.EASY_TOP - 8, Level.MEDIUM_ANCHOR),
                Op.DIFFERENCE to level.ramp(Level.MEDIUM_ANCHOR, Level.HARD_ANCHOR),
            ),
        )

        val sizeSpan = level.span(4..5, 5..6, 6..7)
        val sizeA = random.nextInt(sizeSpan)
        val sizeB = random.nextInt(sizeSpan)
        // Never so much overlap that one set swallows the other: A \ B has
        // to have something left in it to count.
        val overlap = minOf(
            random.nextInt(level.span(1..3, 1..3, 2..4)),
            minOf(sizeA, sizeB) - 1,
        )
        val range = level.span(1..20, 1..40, 1..60)

        // One pool of distinct values, split into shared / only-A / only-B.
        val total = sizeA + sizeB - overlap
        val pool = distinctValues(total, range)
        val shared = pool.take(overlap)
        val onlyA = pool.subList(overlap, sizeA)
        val onlyB = pool.subList(sizeA, total)
        val a = (shared + onlyA).sorted()
        val b = (shared + onlyB).sorted()

        val answer = when (op) {
            Op.INTERSECT -> overlap
            Op.UNION -> total
            Op.DIFFERENCE -> sizeA - overlap
        }
        val symbol = when (op) {
            Op.INTERSECT -> "∩"
            Op.UNION -> "∪"
            Op.DIFFERENCE -> "\\"
        }
        val question = when (language) {
            AppLanguage.ENGLISH -> "How many elements are in A $symbol B?"
            AppLanguage.ROMANIAN -> "Câte elemente are A $symbol B?"
        }
        val text = "A = {${a.joinToString(", ")}}\nB = {${b.joinToString(", ")}}\n$question"

        return Problem(
            text = text,
            answer = answer,
            level = level,
            kind = ProblemKind.SETS,
            hints = listOf(opHint(op, language), HintText.digits(answer, language)),
            notes = notesFor(op, language),
            solution = solutionFor(op, shared, onlyA, sizeA, sizeB, answer, language),
        )
    }

    /**
     * Names the actual elements before counting them. A bare "the answer
     * is 3" would prove nothing; seeing WHICH numbers were shared is the
     * part that carries over to the next one.
     */
    private fun solutionFor(
        op: Op,
        shared: List<Int>,
        onlyA: List<Int>,
        sizeA: Int,
        sizeB: Int,
        answer: Int,
        language: AppLanguage,
    ): List<String> {
        val ro = language == AppLanguage.ROMANIAN
        val sharedList = shared.sorted().joinToString(", ")
        return when (op) {
            Op.INTERSECT -> listOf(
                if (ro) "Numerele aflate în ambele liste: $sharedList"
                else "The numbers sitting in both lists: $sharedList",
                if (ro) "Le numeri: $answer" else "Counting them: $answer",
            )
            Op.UNION -> listOf(
                if (ro) "A are $sizeA elemente, B are $sizeB, iar ${shared.size} sunt comune ($sharedList)"
                else "A has $sizeA elements, B has $sizeB, and ${shared.size} are shared ($sharedList)",
                if (ro) "Comunele se numără o singură dată: $sizeA + $sizeB − ${shared.size} = $answer"
                else "The shared ones count once, not twice: $sizeA + $sizeB − ${shared.size} = $answer",
            )
            Op.DIFFERENCE -> listOf(
                if (ro) "Taie din A numerele care apar și în B: $sharedList"
                else "Cross out of A the numbers that also appear in B: $sharedList",
                if (ro) "Rămâne ${onlyA.sorted().joinToString(", ")}, adică $answer"
                else "What is left is ${onlyA.sorted().joinToString(", ")}, so $answer",
            )
        }
    }

    private fun opHint(op: Op, language: AppLanguage): String = when (op) {
        Op.INTERSECT -> when (language) {
            AppLanguage.ENGLISH ->
                "A ∩ B keeps only the numbers that sit in both lists. Walk through A and look for each number in B."
            AppLanguage.ROMANIAN ->
                "A ∩ B păstrează doar numerele aflate în ambele liste. Ia lista A și caută fiecare număr în B."
        }
        Op.UNION -> when (language) {
            AppLanguage.ENGLISH ->
                "A ∪ B gathers everything from both lists, but a number that appears in both is counted once."
            AppLanguage.ROMANIAN ->
                "A ∪ B strânge tot din ambele liste, dar un număr care apare în amândouă se numără o singură dată."
        }
        Op.DIFFERENCE -> when (language) {
            AppLanguage.ENGLISH ->
                "A \\ B keeps what is only in A. Cross out every number that also shows up in B."
            AppLanguage.ROMANIAN ->
                "A \\ B păstrează doar ce e numai în A. Taie numerele care apar și în B."
        }
    }

    private fun notesFor(op: Op, language: AppLanguage): List<String> {
        val intersect = when (language) {
            AppLanguage.ENGLISH ->
                "∩ (intersection) = the shared elements: {2, 5, 9} ∩ {5, 9, 11} = {5, 9}."
            AppLanguage.ROMANIAN ->
                "∩ (intersecția) = elementele comune: {2, 5, 9} ∩ {5, 9, 11} = {5, 9}."
        }
        val union = when (language) {
            AppLanguage.ENGLISH ->
                "∪ (union) = everything from both sets, each element once: {2, 5} ∪ {5, 9} = {2, 5, 9}."
            AppLanguage.ROMANIAN ->
                "∪ (reuniunea) = tot ce e în ambele mulțimi, fiecare element o dată: {2, 5} ∪ {5, 9} = {2, 5, 9}."
        }
        val inclusionExclusion = when (language) {
            AppLanguage.ENGLISH ->
                "Counting tip: |A ∪ B| = |A| + |B| − |A ∩ B|. Subtract the shared ones so they aren't counted twice."
            AppLanguage.ROMANIAN ->
                "Truc de numărat: |A ∪ B| = |A| + |B| − |A ∩ B|. Scazi elementele comune ca să nu le numeri de două ori."
        }
        val difference = when (language) {
            AppLanguage.ENGLISH ->
                "\\ (difference) = what stays in the first set only: {2, 5, 9} \\ {5} = {2, 9}."
            AppLanguage.ROMANIAN ->
                "\\ (diferența) = ce rămâne doar în prima mulțime: {2, 5, 9} \\ {5} = {2, 9}."
        }
        return when (op) {
            Op.INTERSECT -> listOf(intersect, union)
            Op.UNION -> listOf(union, inclusionExclusion, intersect)
            Op.DIFFERENCE -> listOf(difference, intersect)
        }
    }

    private fun distinctValues(count: Int, range: IntRange): List<Int> {
        val values = mutableSetOf<Int>()
        while (values.size < count) {
            values.add(random.nextInt(range.first, range.last + 1))
        }
        return values.shuffled(random)
    }
}
