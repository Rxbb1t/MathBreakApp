package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import kotlin.random.nextInt

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

    /**
     * The clock stories arrive in the order they get harder: reading a gap
     * between two times all the way down, converting hours-and-minutes from
     * the middle, and the multi-leg journey with its distractor readings
     * only at the top. The gap itself widens with the level, so even the
     * plain duration keeps growing after the shapes have stopped changing.
     */
    fun generate(level: Level, language: AppLanguage): Problem {
        // Reading the gap between two clock times is the backbone and never
        // goes away; the other three are the ones that come and go. Written
        // as weights so that a shape arriving cannot silently drive the
        // backbone out at the top of its window.
        val shape = random.pickWeighted(
            listOf(
                Shape.DURATION to 1.0,
                // Whole hours in minutes is the gentlest, and it is done
                // with by the time the clock stories get interesting.
                Shape.WHOLE_HOURS to level.fade(Level.EASY_TOP, Level.MEDIUM_ANCHOR) * 0.5,
                // Converting h-and-m belongs to the middle of the scale.
                Shape.CONVERSION to level.ramp(Level.EASY_TOP - 8, Level.MEDIUM_ANCHOR) *
                    level.fade(Level.MEDIUM_TOP, Level.HARD_ANCHOR),
                // The multi-leg journey with its distractor readings is the
                // hardest, so it only turns up near the top.
                Shape.JOURNEY to level.ramp(Level.MEDIUM_ANCHOR, Level.HARD_ANCHOR),
            ),
        )
        return when (shape) {
            Shape.WHOLE_HOURS -> wholeHours(level, language)
            Shape.CONVERSION -> conversion(level, language)
            Shape.JOURNEY -> journey(level, language)
            Shape.DURATION -> duration(
                language = language,
                templates = if (level.points > Level.MEDIUM_TOP) {
                    LONG_DURATION_TEMPLATES
                } else {
                    DURATION_TEMPLATES
                },
                level = level,
                startHours = level.span(7..18, 6..15, 6..12),
                span = level.span(15..55, 70..240, 150..420),
            )
        }
    }

    private enum class Shape { DURATION, WHOLE_HOURS, CONVERSION, JOURNEY }

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
        level: Level,
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
            level = level,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "Count the minutes between the two clock times."
                AppLanguage.ROMANIAN -> "Numără minutele dintre cele două ore de pe ceas."
            },
            notes = if (level.band == Difficulty.EASY) emptyList() else clockNotes(language),
            solution = buildList {
                add(
                    step(
                        "From ${clock(start)} to ${clock(start + length)}.",
                        "De la ${clock(start)} la ${clock(start + length)}.",
                        language,
                    ),
                )
                // Under an hour there's nothing to break down, and a
                // "0 h" line would only get in the way.
                if (length >= 60) {
                    add(
                        step(
                            "That's ${length / 60} h and ${length % 60} min: " +
                                "${length / 60} × 60 + ${length % 60} = $length",
                            "Adică ${length / 60} h și ${length % 60} min: " +
                                "${length / 60} × 60 + ${length % 60} = $length",
                            language,
                        ),
                    )
                } else {
                    add(
                        step(
                            "Counting the minutes between them gives $length.",
                            "Numărând minutele dintre ele iese $length.",
                            language,
                        ),
                    )
                }
            },
        )
    }

    /** Marker: "hours" with one number. Order: {h}. Answer 60 × h. */
    private fun wholeHours(level: Level, language: AppLanguage): Problem {
        val h = random.nextInt(2, 6)
        return time(
            text = fill(pick(WHOLE_HOURS_TEMPLATES, language), mapOf("h" to "$h")),
            answer = 60 * h,
            level = level,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "Each hour is 60 minutes, so multiply."
                AppLanguage.ROMANIAN -> "Fiecare oră are 60 de minute, deci înmulțește."
            },
            solution = listOf(
                step(
                    "One hour is 60 minutes, and there are $h of them.",
                    "O oră are 60 de minute, iar aici sunt $h ore.",
                    language,
                ),
                step("$h × 60 = ${60 * h}", "$h × 60 = ${60 * h}", language),
            ),
        )
    }

    /** Marker: "hours" with two numbers. Order: {h}, {m}. Answer 60h + m. */
    private fun conversion(level: Level, language: AppLanguage): Problem {
        val h = random.nextInt(2, 5)
        val m = random.nextInt(5, 56)
        return time(
            text = fill(
                pick(CONVERSION_TEMPLATES, language),
                mapOf("h" to "$h", "m" to "$m", "m_de" to if (m < 20) "$m" else "$m de"),
            ),
            answer = 60 * h + m,
            level = level,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "Turn the hours into minutes, then add the loose minutes."
                AppLanguage.ROMANIAN -> "Transformă orele în minute, apoi adună minutele rămase."
            },
            notes = clockNotes(language),
            solution = listOf(
                step(
                    "The hours first: $h × 60 = ${60 * h}",
                    "Întâi orele: $h × 60 = ${60 * h}",
                    language,
                ),
                step(
                    "Then the loose minutes: ${60 * h} + $m = ${60 * h + m}",
                    "Apoi minutele rămase: ${60 * h} + $m = ${60 * h + m}",
                    language,
                ),
            ),
        )
    }

    /**
     * Marker: "away". Order: {t1}, {t2}, {t3}, {t4} clock times; only the
     * first and last matter. The middle two try to distract.
     * Answer t4 − t1.
     */
    private fun journey(level: Level, language: AppLanguage): Problem {
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
            level = level,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "Count from when you left to when you got back."
                AppLanguage.ROMANIAN -> "Numără de când ai plecat până când te-ai întors."
            },
            notes = clockNotes(language) + journeyNote(language),
            solution = listOf(
                step(
                    "Only the leaving time and the coming-home time matter: " +
                        "${clock(start)} and ${clock(start + leg1 + stay + leg2)}.",
                    "Contează doar ora plecării și ora întoarcerii: " +
                        "${clock(start)} și ${clock(start + leg1 + stay + leg2)}.",
                    language,
                ),
                step(
                    "The way there took $leg1 min, the stay $stay min, the way back $leg2 min.",
                    "Dusul a durat $leg1 min, șederea $stay min, întorsul $leg2 min.",
                    language,
                ),
                step(
                    "$leg1 + $stay + $leg2 = ${leg1 + stay + leg2}",
                    "$leg1 + $stay + $leg2 = ${leg1 + stay + leg2}",
                    language,
                ),
            ),
        )
    }

    private fun time(
        text: String,
        answer: Int,
        level: Level,
        language: AppLanguage,
        hint: String,
        notes: List<String> = emptyList(),
        solution: List<String> = emptyList(),
    ): Problem = Problem(
        text = text,
        answer = answer,
        level = level,
        kind = ProblemKind.TIME,
        hints = listOf(hint, HintText.digits(answer, language)),
        notes = notes,
        answerUnit = "min",
        solution = solution,
    )

    /** Picks the language's wording for one worked step. */
    private fun step(en: String, ro: String, language: AppLanguage): String =
        if (language == AppLanguage.ROMANIAN) ro else en

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
            "The total time away runs from the first clock reading to the last, with everything in between already counted.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Timpul total de plecare curge de la prima oră de pe ceas până la ultima, cu tot ce e între ele deja cuprins.",
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
