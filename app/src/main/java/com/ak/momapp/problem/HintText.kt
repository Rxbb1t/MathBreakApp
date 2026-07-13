package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage

/**
 * Shared phrasing for generated hints. Hints live on the [Problem] in the
 * language that was active when it was generated, same as the text.
 */
internal object HintText {

    /** "Starts with 4 and has 3 digits" — a shape-of-the-answer nudge. */
    fun digits(answer: Int, language: AppLanguage): String {
        val text = answer.toString()
        return if (text.length == 1) {
            when (language) {
                AppLanguage.ENGLISH -> "The answer is a single digit 😉"
                AppLanguage.ROMANIAN -> "Răspunsul e o singură cifră 😉"
            }
        } else {
            when (language) {
                AppLanguage.ENGLISH ->
                    "The answer starts with ${text.first()} and has ${text.length} digits"
                AppLanguage.ROMANIAN ->
                    "Răspunsul începe cu ${text.first()} și are ${text.length} cifre"
            }
        }
    }
}
