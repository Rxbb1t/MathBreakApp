package com.ak.momapp.problem

/** The headers the Exercise-types screen groups the switches under. */
enum class TopicGroup { NUMBERS, STORIES, THINKING }

/**
 * The problem families that can be switched on and off on the Exercise
 * types screen, one switch per topic, grouped by [TopicGroup].
 */
enum class ProblemTopic(val group: TopicGroup) {
    /** The difficulty's core: chains at EASY, equations above, plus missing-sign taps. */
    CORE(TopicGroup.NUMBERS),

    /** Emoji shape puzzles. */
    PUZZLE(TopicGroup.THINKING),

    /** Logic riddles. */
    LOGIC(TopicGroup.THINKING),

    /** Story geometry (appears at MEDIUM and HARD). */
    GEOMETRY(TopicGroup.THINKING),

    /** Shopping stories in euros. */
    MONEY(TopicGroup.STORIES),

    /** Clock stories in minutes. */
    TIME(TopicGroup.STORIES),

    /** Word problems: little stories around everyday names. */
    WORD(TopicGroup.STORIES),

    /** Two expressions compared by tapping < = >, plus ✓/✗ claims. */
    COMPARE(TopicGroup.NUMBERS),

    /** Number cards picked to hit a target sum. */
    TARGET(TopicGroup.NUMBERS),

    /** Number hunts (evens, primes, …) and set exercises (A ∩ B). */
    NUMBERS(TopicGroup.NUMBERS),

    /** Round and approximate: "roughly how much is 297 × 4?" */
    ESTIMATION(TopicGroup.NUMBERS),
    ;

    companion object {
        val ALL: Set<ProblemTopic> = entries.toSet()

        /** The switches never let the mix shrink below this many topics. */
        const val MIN_ENABLED = 1
    }
}

/** The topic switch a dealt problem belongs to. Used by the per-topic stats. */
val ProblemKind.topic: ProblemTopic
    get() = when (this) {
        ProblemKind.ARITHMETIC, ProblemKind.EQUATION, ProblemKind.MISSING_OP -> ProblemTopic.CORE
        ProblemKind.WORD -> ProblemTopic.WORD
        ProblemKind.PUZZLE -> ProblemTopic.PUZZLE
        ProblemKind.LOGIC -> ProblemTopic.LOGIC
        ProblemKind.GEOMETRY -> ProblemTopic.GEOMETRY
        ProblemKind.MONEY -> ProblemTopic.MONEY
        ProblemKind.TIME -> ProblemTopic.TIME
        ProblemKind.COMPARE, ProblemKind.TRUE_FALSE -> ProblemTopic.COMPARE
        ProblemKind.TARGET -> ProblemTopic.TARGET
        ProblemKind.SELECT, ProblemKind.SETS -> ProblemTopic.NUMBERS
        ProblemKind.ESTIMATE -> ProblemTopic.ESTIMATION
    }
