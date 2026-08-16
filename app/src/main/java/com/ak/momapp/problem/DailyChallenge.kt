package com.ak.momapp.problem

/**
 * The one-a-day multi-stage puzzle: a short scene-setting line and five
 * [Problem]s worked in order, each one feeding the next.
 *
 * Which story the five steps are told through is [ChallengeTheme]'s job.
 * Each theme keeps its own drawn values in its own private state class,
 * because the values a market stall needs and the values a clock needs
 * have nothing to say to each other.
 */
data class DailyChallenge(
    val intro: String,
    val stages: List<Problem>,
)
