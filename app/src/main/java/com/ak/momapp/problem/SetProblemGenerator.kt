package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * Mulțimi: two small sets written out in full; she counts the elements
 * of A ∩ B, A ∪ B or (at HARD) A \ B and types the count. EASY sticks
 * to intersections — spotting the shared numbers is the friendly entry
 * point; the union and difference arrive with the levels above.
 */
class SetProblemGenerator(private val random: Random) {

    private enum class Op { INTERSECT, UNION, DIFFERENCE }

    fun generate(difficulty: Difficulty, language: AppLanguage): Problem {
        val op = when (difficulty) {
            Difficulty.EASY -> Op.INTERSECT
            Difficulty.MEDIUM -> if (random.nextBoolean()) Op.INTERSECT else Op.UNION
            Difficulty.HARD -> Op.entries.random(random)
        }

        val sizeA: Int
        val sizeB: Int
        val overlap: Int
        val range: IntRange
        when (difficulty) {
            Difficulty.EASY -> {
                sizeA = random.nextInt(4, 6); sizeB = random.nextInt(4, 6)
                overlap = random.nextInt(1, 4); range = 1..20
            }
            Difficulty.MEDIUM -> {
                sizeA = random.nextInt(5, 7); sizeB = random.nextInt(5, 7)
                overlap = random.nextInt(1, 4); range = 1..40
            }
            Difficulty.HARD -> {
                sizeA = random.nextInt(6, 8); sizeB = random.nextInt(6, 8)
                overlap = random.nextInt(2, 5); range = 1..60
            }
        }

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
            difficulty = difficulty,
            kind = ProblemKind.SETS,
            hints = listOf(opHint(op, language), HintText.digits(answer, language)),
            notes = notesFor(op, language),
        )
    }

    private fun opHint(op: Op, language: AppLanguage): String = when (op) {
        Op.INTERSECT -> when (language) {
            AppLanguage.ENGLISH ->
                "A ∩ B keeps only the numbers that sit in both lists — walk through A and look for each number in B."
            AppLanguage.ROMANIAN ->
                "A ∩ B păstrează doar numerele aflate în ambele liste — ia lista A și caută fiecare număr în B."
        }
        Op.UNION -> when (language) {
            AppLanguage.ENGLISH ->
                "A ∪ B gathers everything from both lists, but a number that appears in both is counted once."
            AppLanguage.ROMANIAN ->
                "A ∪ B strânge tot din ambele liste, dar un număr care apare în amândouă se numără o singură dată."
        }
        Op.DIFFERENCE -> when (language) {
            AppLanguage.ENGLISH ->
                "A \\ B keeps what is only in A — cross out every number that also shows up in B."
            AppLanguage.ROMANIAN ->
                "A \\ B păstrează doar ce e numai în A — taie numerele care apar și în B."
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
                "Counting tip: |A ∪ B| = |A| + |B| − |A ∩ B| — subtract the shared ones so they aren't counted twice."
            AppLanguage.ROMANIAN ->
                "Truc de numărat: |A ∪ B| = |A| + |B| − |A ∩ B| — scazi elementele comune ca să nu le numeri de două ori."
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
