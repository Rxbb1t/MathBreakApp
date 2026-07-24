package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage

/**
 * The app's warm little phrases and the name pool for story problems,
 * kept in one place. Each list exists in English and Romanian; the app
 * picks by the language chosen in settings.
 */
object PersonalContent {

    /** shown occasionally after a correct answer */
    val ENCOURAGEMENTS: List<String> = listOf(
        "Nice one!",
        "Sharp as ever!",
        "Good thinking!",
        "Look at you go!",
        "You've earned that coffee.",
    )

    val ENCOURAGEMENTS_RO: List<String> = listOf(
        "Bravo!",
        "Isteț ca întotdeauna!",
        "Bine gândit!",
        "Așa se face!",
        "Cafeaua e binemeritată.",
    )

    fun encouragements(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> ENCOURAGEMENTS
        AppLanguage.ROMANIAN -> ENCOURAGEMENTS_RO
    }

    /**
     * The names word problems and riddles are told with ("Ana baked 24
     * cookies…"). Deliberately short and readable in both languages.
     *
     * The pool is spread across the world rather than drawn from one place,
     * so the stories feel like they belong to everybody. Two rules it has
     * to keep: plain ASCII, because [com.ak.momapp.i18n.LanguagePurityTest]
     * reads a Romanian letter in an English problem as a translation leak;
     * and short, because a long name pushes an already tight problem card
     * toward its shrink-to-fit floor.
     */
    val NAMES: List<String> = listOf(
        "Ana", "Maria", "Elena", "Dan", "Mihai", "Ivan", "Alex", "Tony",
        "Sofia", "Diego", "Emma", "Noah", "Omar", "Layla", "Mei", "Kenji",
        "Priya", "Arjun", "Kofi", "Amara", "Lena", "Marco", "Nina", "Hugo",
        "Sara", "Leo", "Yuki", "Ravi",
    )

    /** Roughly how often a correct answer shows an encouragement (percent). */
    const val ENCOURAGEMENT_CHANCE_PERCENT = 100

    /**
     * The notification text for a break. One is picked at random each
     * time, so a little variety keeps it fresh. Notifications get one
     * line, so these stay short.
     */
    val BREAK_NOTIFICATION_LINES: List<String> = listOf(
        "Time for a little brain break.",
        "One quick problem, 30 seconds tops.",
        "Got a minute for one problem?",
    )

    val BREAK_NOTIFICATION_LINES_RO: List<String> = listOf(
        "E timpul pentru o mică pauză de creier.",
        "O problemă scurtă, max 30 de secunde.",
        "Ai un minut pentru o problemă?",
    )

    fun notificationLines(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> BREAK_NOTIFICATION_LINES
        AppLanguage.ROMANIAN -> BREAK_NOTIFICATION_LINES_RO
    }

    /** shown by the quiet reminder ~20 min after an ignored break */
    const val RENUDGE_LINE = "No rush, your break is still waiting."
    const val RENUDGE_LINE_RO = "Fără grabă, pauza ta încă te așteaptă."

    fun renudgeLine(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> RENUDGE_LINE
        AppLanguage.ROMANIAN -> RENUDGE_LINE_RO
    }
}
