package com.ak.momapp.problem

/**
 * A problem's template with every run of digits blanked out, so that two
 * fillings of the same story collapse to one signature: "Ana had # apples
 * and gave away #…".
 *
 * Two very different features want exactly this. The generator uses it to
 * avoid serving the same shape twice in a sitting, and the review queue
 * uses it to bring a shape she got wrong back later with fresh numbers.
 * They have to agree on what "the same kind of problem" means, so the
 * definition lives in one place.
 */
object ProblemShape {

    private val DIGITS = Regex("\\d+")

    /**
     * Null for card exercises. There are only a handful of hunt prompts by
     * design, so their text is nearly constant and says nothing about which
     * problem it was; all their variety lives in the cards.
     */
    fun of(problem: Problem): String? =
        if (problem.cards.isEmpty()) problem.text.replace(DIGITS, "#") else null
}

/**
 * A shape she got wrong once and is due to meet again, with the topic it
 * belongs to so the roll knows where to look for it.
 *
 * Nothing about this is ever shown to her. It must not read as a re-test:
 * no "you got this wrong before", no counter, no marker on the problem. It
 * simply comes round again, with different numbers, the way a good teacher
 * would quietly circle back rather than announcing a retake.
 */
data class ReviewPick(val topic: ProblemTopic, val shape: String)
