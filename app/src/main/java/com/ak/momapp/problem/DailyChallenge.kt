package com.ak.momapp.problem

/**
 * The one-a-day multi-stage story: a short scene-setting line and five
 * [Problem]s that tell it in order. Later stages quote earlier answers in
 * their text, so the story only moves forward as she solves it.
 */
data class DailyChallenge(
    val intro: String,
    val stages: List<Problem>,
)
