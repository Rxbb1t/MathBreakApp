package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * Clock stories answered in whole minutes, so the plain number keyboard
 * still fits.
 *
 * EASY: short durations between two clock times, whole hours in minutes.
 * MEDIUM: durations spanning a few hours, "h hours and m minutes" as
 * minutes.
 * HARD: long durations, and journeys with in-between clock readings that
 * try to distract from the start and the end.
 *
 * Templates are EN/RO pairs with the usual authoring rules: keep the
 * placeholder order, keep each type's English marker phrase, write no
 * digits into the template text. Clock times are always printed h:mm,
 * so the tests can re-derive every answer from the text.
 */
class TimeProblemGenerator(private val random: Random) {

    fun generate(difficulty: Difficulty, language: AppLanguage): Problem = when (difficulty) {
        Difficulty.EASY ->
            if (random.nextInt(3) < 2) {
                duration(language, DURATION_TEMPLATES, Difficulty.EASY, startHours = 7..18, span = 15..55)
            } else {
                wholeHours(language)
            }

        Difficulty.MEDIUM ->
            if (random.nextBoolean()) {
                duration(language, DURATION_TEMPLATES, Difficulty.MEDIUM, startHours = 6..15, span = 70..240)
            } else {
                conversion(language)
            }

        Difficulty.HARD ->
            if (random.nextBoolean()) {
                duration(language, LONG_DURATION_TEMPLATES, Difficulty.HARD, startHours = 6..12, span = 150..420)
            } else {
                journey(language)
            }
    }

    /** An EN/RO pair telling the same story. */
    private data class Template(val en: String, val ro: String)

    private fun pick(templates: List<Template>, language: AppLanguage): String {
        val template = templates.random(random)
        return if (language == AppLanguage.ROMANIAN) template.ro else template.en
    }

    private fun fill(text: String, values: Map<String, String>): String {
        var result = text
        for ((key, value) in values) result = result.replace("{$key}", value)
        return result
    }

    /** Minutes since midnight printed as a clock reading, "9:05". */
    private fun clock(minutes: Int): String = "%d:%02d".format(minutes / 60, minutes % 60)

    /** Marker: "minutes". Order: {t1}, {t2} clock times. Answer t2 − t1. */
    private fun duration(
        language: AppLanguage,
        templates: List<Template>,
        difficulty: Difficulty,
        startHours: IntRange,
        span: IntRange,
    ): Problem {
        val start = random.nextInt(startHours.first, startHours.last + 1) * 60 + random.nextInt(0, 60)
        val length = random.nextInt(span.first, span.last + 1)
        return time(
            text = fill(
                pick(templates, language),
                mapOf("t1" to clock(start), "t2" to clock(start + length)),
            ),
            answer = length,
            difficulty = difficulty,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "It asks how much time passed between the two clock readings."
                AppLanguage.ROMANIAN -> "Întreabă cât timp a trecut între cele două ore de pe ceas."
            },
            notes = if (difficulty == Difficulty.EASY) emptyList() else clockNotes(language),
        )
    }

    /** Marker: "hours" with one number. Order: {h}. Answer 60 × h. */
    private fun wholeHours(language: AppLanguage): Problem {
        val h = random.nextInt(2, 6)
        return time(
            text = fill(pick(WHOLE_HOURS_TEMPLATES, language), mapOf("h" to "$h")),
            answer = 60 * h,
            difficulty = Difficulty.EASY,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "Every single hour brings the same number of minutes."
                AppLanguage.ROMANIAN -> "Fiecare oră aduce același număr de minute."
            },
        )
    }

    /** Marker: "hours" with two numbers. Order: {h}, {m}. Answer 60h + m. */
    private fun conversion(language: AppLanguage): Problem {
        val h = random.nextInt(2, 5)
        val m = random.nextInt(5, 56)
        return time(
            text = fill(
                pick(CONVERSION_TEMPLATES, language),
                mapOf("h" to "$h", "m" to "$m", "m_de" to if (m < 20) "$m" else "$m de"),
            ),
            answer = 60 * h + m,
            difficulty = Difficulty.MEDIUM,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "The hours hide minutes of their own. The loose minutes join them."
                AppLanguage.ROMANIAN -> "Orele ascund propriile lor minute. Minutele răzlețe li se alătură."
            },
            notes = clockNotes(language),
        )
    }

    /**
     * Marker: "away". Order: {t1}, {t2}, {t3}, {t4} clock times; only the
     * first and last matter. The middle two try to distract.
     * Answer t4 − t1.
     */
    private fun journey(language: AppLanguage): Problem {
        val start = random.nextInt(8, 15) * 60 + random.nextInt(0, 60)
        val leg1 = random.nextInt(15, 61)
        val stay = random.nextInt(20, 91)
        val leg2 = random.nextInt(15, 61)
        return time(
            text = fill(
                pick(JOURNEY_TEMPLATES, language),
                mapOf(
                    "t1" to clock(start),
                    "t2" to clock(start + leg1),
                    "t3" to clock(start + leg1 + stay),
                    "t4" to clock(start + leg1 + stay + leg2),
                ),
            ),
            answer = leg1 + stay + leg2,
            difficulty = Difficulty.HARD,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "Away in total means from stepping out to stepping back in. The stops in between are part of it."
                AppLanguage.ROMANIAN -> "Plecată în total înseamnă de la ieșire până la întoarcere. Opririle de pe drum se pun și ele."
            },
            notes = clockNotes(language) + journeyNote(language),
        )
    }

    private fun time(
        text: String,
        answer: Int,
        difficulty: Difficulty,
        language: AppLanguage,
        hint: String,
        notes: List<String> = emptyList(),
    ): Problem = Problem(
        text = text,
        answer = answer,
        difficulty = difficulty,
        kind = ProblemKind.TIME,
        hints = listOf(hint, HintText.digits(answer, language)),
        notes = notes,
        answerUnit = "min",
    )

    // ── Helper-sheet notes ───────────────────────────────────────────────

    private fun clockNotes(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "One hour has 60 minutes. From 15:40, twenty minutes reach 16:00.",
            "Minutes between two clock times: count up to the next full hour first, then hour by hour, then the minutes that are left.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "O oră are 60 de minute. De la 15:40, douăzeci de minute ajung la 16:00.",
            "Minutele dintre două ore de pe ceas: numără întâi până la ora fixă, apoi din oră în oră, apoi minutele rămase.",
        )
    }

    private fun journeyNote(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "The total time away runs from the first clock reading to the last. Everything in between already lives inside it.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Timpul total de plecare curge de la prima oră de pe ceas până la ultima. Tot ce e între ele e deja cuprins.",
        )
    }

    companion object {
        // ════════════════════════════════════════════════════════════════
        // ✏️ TIME TEMPLATES. EN/RO pairs, same three rules as the
        // riddles: keep placeholder order, keep the English marker phrase
        // of each list, write no digits into the template text.
        // ════════════════════════════════════════════════════════════════

        // Marker: "minutes" with two clock times. {t1} start, {t2} end.
        private val DURATION_TEMPLATES = listOf(
            Template(
                "The bread goes into the oven at {t1} and comes out at {t2}.\nHow many minutes does it bake?",
                "Pâinea intră în cuptor la {t1} și iese la {t2}.\nCâte minute se coace?",
            ),
            Template(
                "The bus leaves at {t1} and arrives at {t2}.\nHow many minutes does the ride take?",
                "Autobuzul pleacă la {t1} și ajunge la {t2}.\nCâte minute durează drumul?",
            ),
            Template(
                "The film starts at {t1} and ends at {t2}.\nHow many minutes does it last?",
                "Filmul începe la {t1} și se termină la {t2}.\nCâte minute durează?",
            ),
            Template(
                "It's {t1} now, and the guests arrive at {t2}.\nHow many minutes are left to get ready?",
                "Acum e {t1}, iar musafirii vin la {t2}.\nCâte minute mai sunt de pregătit?",
            ),
        )

        // Marker: "minutes", two clock times, longer spans for HARD.
        private val LONG_DURATION_TEMPLATES = listOf(
            Template(
                "The train leaves at {t1} and pulls in at {t2}.\nHow many minutes does the trip take?",
                "Trenul pleacă la {t1} și sosește la {t2}.\nCâte minute durează călătoria?",
            ),
            Template(
                "The slow roast goes in at {t1} and is done at {t2}.\nHow many minutes does it cook?",
                "Friptura înceată intră în cuptor la {t1} și e gata la {t2}.\nCâte minute se gătește?",
            ),
            Template(
                "The hike starts at {t1} and the group returns at {t2}.\nHow many minutes does it last?",
                "Drumeția începe la {t1}, iar grupul se întoarce la {t2}.\nCâte minute durează?",
            ),
        )

        // Marker: "hours" with one number. {h} whole hours.
        private val WHOLE_HOURS_TEMPLATES = listOf(
            Template(
                "How many minutes are in {h} hours?",
                "Câte minute sunt în {h} ore?",
            ),
            Template(
                "A pot simmers for {h} hours.\nHow many minutes is that?",
                "O oală fierbe încet {h} ore.\nCâte minute înseamnă asta?",
            ),
            Template(
                "The paint needs {h} hours to dry.\nHow many minutes is that?",
                "Vopseaua are nevoie de {h} ore ca să se usuce.\nCâte minute înseamnă asta?",
            ),
        )

        // Marker: "hours" with two numbers. {h} hours, {m} minutes.
        private val CONVERSION_TEMPLATES = listOf(
            Template(
                "The train ride takes {h} hours and {m} minutes.\nHow many minutes is that in all?",
                "Călătoria cu trenul durează {h} ore și {m_de} minute.\nCâte minute înseamnă în total?",
            ),
            Template(
                "The cake needs {h} hours and {m} minutes in the oven.\nHow many minutes is that in all?",
                "Tortul stă {h} ore și {m_de} minute în cuptor.\nCâte minute înseamnă în total?",
            ),
            Template(
                "The flight lasts {h} hours and {m} minutes.\nHow many minutes is that in all?",
                "Zborul durează {h} ore și {m_de} minute.\nCâte minute înseamnă în total?",
            ),
        )

        // Marker: "away". {t1} out, {t2} and {t3} distractors, {t4} back.
        private val JOURNEY_TEMPLATES = listOf(
            Template(
                "You leave home at {t1}, reach the market at {t2}, leave the market at {t3}, and are home again at {t4}.\nHow many minutes were you away in total?",
                "Pleci de acasă la {t1}, ajungi la piață la {t2}, pleci de la piață la {t3} și ești din nou acasă la {t4}.\nCâte minute ai fost plecată în total?",
            ),
            Template(
                "You step out at {t1}, get to the clinic at {t2}, leave the clinic at {t3}, and are back home at {t4}.\nHow many minutes were you away in total?",
                "Ieși din casă la {t1}, ajungi la clinică la {t2}, pleci de la clinică la {t3} și ești înapoi acasă la {t4}.\nCâte minute ai fost plecată în total?",
            ),
            Template(
                "You leave at {t1}, arrive at a friend's house at {t2}, say goodbye at {t3}, and are home again at {t4}.\nHow many minutes were you away in total?",
                "Pleci la {t1}, ajungi la o prietenă la {t2}, îți iei rămas-bun la {t3} și ești din nou acasă la {t4}.\nCâte minute ai fost plecată în total?",
            ),
        )
    }
}
