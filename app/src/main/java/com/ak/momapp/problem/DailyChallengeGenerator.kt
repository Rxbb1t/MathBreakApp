package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import java.time.LocalDate
import kotlin.random.Random

/**
 * Deals the daily challenge: one five-step chain per calendar day, seeded
 * from the date, so the same day always deals the same chain. Closing the
 * app, restarting the phone, or switching language changes nothing but
 * the words.
 *
 * The five steps are ONE problem, not five. Each answer is the next
 * step's input, so nothing can be skipped and the numbers have to be
 * carried forward in her head. The wording never repeats an earlier
 * answer, which is the point of a chain, and it is why the first hint of
 * every step hands the value back: forgetting a number is not a maths
 * failure and should not end the day.
 *
 * What the day picks now is not only the numbers but the STORY. Four
 * themes take turns: hunting an anchor, timing a journey, working a
 * market stall, measuring a rug. They exist because the roles used to be
 * fixed forever, so a week of it taught the shape rather than the
 * arithmetic. See [ChallengeTheme] for the rules every one of them still
 * has to obey.
 *
 * Same rules as everywhere else. Whole non-negative answers only, and
 * hints restate what a step means. Never the steps to take.
 */
class DailyChallengeGenerator {

    fun generate(date: LocalDate, language: AppLanguage): DailyChallenge {
        // One random stream per day; both languages draw the same numbers,
        // and the theme comes out of the same stream as everything else.
        val random = Random(date.toEpochDay())
        return themeFor(date).chain.build(random, language)
    }

    /**
     * Which story today is told through.
     *
     * Drawn from its own stream rather than the chain's, so that adding a
     * theme later reshuffles which day gets which story without also
     * changing the numbers inside the stories that were already there.
     */
    fun themeFor(date: LocalDate): ChallengeTheme =
        ChallengeTheme.of(Random(date.toEpochDay() * 31 + 7))

    companion object {
        /**
         * The daily chain sits at one fixed level for everyone, on purpose.
         * It is the same challenge on the same day whoever opens it, so it
         * cannot follow a personal ladder without stopping being that. The
         * adaptive scale belongs to the breaks.
         */
        val STAGE_LEVEL: Level = Difficulty.MEDIUM.toLevel()
    }
}
