package com.ak.momapp.problem

/** The headers the Exercise-types screen groups the switches under. */
enum class TopicGroup { NUMBERS, STORIES, THINKING }

/**
 * The problem families that can be switched on and off on the Exercise
 * types screen, one switch per topic, grouped by [TopicGroup].
 */
enum class ProblemTopic(val group: TopicGroup) {
    /** The difficulty's core: number chains at EASY, equations above. */
    CORE(TopicGroup.NUMBERS),

    /** Emoji shape puzzles. */
    PUZZLE(TopicGroup.THINKING),

    /** Logic riddles. */
    LOGIC(TopicGroup.THINKING),

    /** Story geometry (appears at MEDIUM and HARD). */
    GEOMETRY(TopicGroup.THINKING),

    /** Shopping stories in lei. */
    MONEY(TopicGroup.STORIES),

    /** Clock stories in minutes. */
    TIME(TopicGroup.STORIES),

    /** Word problems: little stories around everyday names. */
    WORD(TopicGroup.STORIES),

    /** Two expressions compared by tapping < = >. */
    COMPARE(TopicGroup.NUMBERS),

    /** Number cards picked to hit a target sum. */
    TARGET(TopicGroup.NUMBERS),

    /** Number hunts (evens, primes, …) and set exercises (A ∩ B). */
    NUMBERS(TopicGroup.NUMBERS),
    ;

    companion object {
        val ALL: Set<ProblemTopic> = entries.toSet()

        /** The switches never let the mix shrink below this many topics. */
        const val MIN_ENABLED = 2
    }
}

/** The topic switch a dealt problem belongs to — used by the per-topic stats. */
val ProblemKind.topic: ProblemTopic
    get() = when (this) {
        ProblemKind.ARITHMETIC, ProblemKind.EQUATION -> ProblemTopic.CORE
        ProblemKind.WORD -> ProblemTopic.WORD
        ProblemKind.PUZZLE -> ProblemTopic.PUZZLE
        ProblemKind.LOGIC -> ProblemTopic.LOGIC
        ProblemKind.GEOMETRY -> ProblemTopic.GEOMETRY
        ProblemKind.MONEY -> ProblemTopic.MONEY
        ProblemKind.TIME -> ProblemTopic.TIME
        ProblemKind.COMPARE -> ProblemTopic.COMPARE
        ProblemKind.TARGET -> ProblemTopic.TARGET
        ProblemKind.SELECT, ProblemKind.SETS -> ProblemTopic.NUMBERS
    }
