package com.ak.momapp.data

import com.ak.momapp.problem.ProblemShape
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.problem.ReviewPick
import kotlin.random.Random

/**
 * One shape she got wrong, and the day it should quietly come round again.
 */
data class ReviewItem(
    val topic: ProblemTopic,
    /** The digit-blanked signature; see [ProblemShape]. */
    val shape: String,
    /** Epoch day this becomes due. */
    val dueDay: Long,
    /** How many times it has come back, which sets the next gap. */
    val stage: Int = 0,
)

/**
 * Spaced repetition for the problems she gets wrong.
 *
 * The app has always recorded that a topic went badly, but never that a
 * particular KIND of problem did: one wrong answer moved a number and the
 * problem itself was gone forever. So the thing she actually struggled with
 * was no likelier to come back than anything else, and practice stayed
 * random instead of aimed.
 *
 * This stores the missed SHAPE, not the problem. When it comes round the
 * numbers are new, so it is the same kind of thinking rather than the same
 * question, and it cannot be answered from memory.
 *
 * IT MUST NEVER READ AS A RE-TEST. Nothing here surfaces in the UI: no
 * "you got this wrong before", no counter, no marker on the problem, no
 * screen listing what she is bad at. It just comes back, the way a good
 * teacher circles round to something rather than announcing a retake.
 */
object ReviewQueue {

    /**
     * Days until a missed shape returns, by how many times it has already
     * come back. Tomorrow, then a few days, then a week, then a fortnight.
     * Getting it right at the last stage retires it.
     */
    val INTERVALS = listOf(1L, 3L, 7L, 14L)

    /**
     * How often a roll goes looking for something due, when anything is.
     *
     * Deliberately not higher. Review has to stay a seasoning rather than
     * the meal: a break that is mostly problems she once got wrong would be
     * a bad afternoon, whatever it is called, and it would also crowd out
     * the topics she enjoys.
     */
    const val REVIEW_CHANCE_PERCENT = 22

    /** Beyond this the oldest entries are dropped, newest kept. */
    const val MAX_ITEMS = 40

    fun encode(items: List<ReviewItem>): String =
        items.joinToString("|") { "${it.topic.name}:${it.dueDay}:${it.stage}:${it.shape}" }

    /**
     * Entries are separated by "|" and the shape is LAST, because a shape
     * is arbitrary problem text that may itself contain colons or commas.
     * Splitting it off by position rather than by delimiter is what keeps a
     * set problem ("A = {2, 5}") from corrupting the whole queue.
     */
    fun decode(raw: String?): List<ReviewItem> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            val parts = entry.split(":", limit = 4)
            if (parts.size != 4) return@mapNotNull null
            val topic = ProblemTopic.entries.firstOrNull { it.name == parts[0] }
                ?: return@mapNotNull null
            val due = parts[1].toLongOrNull() ?: return@mapNotNull null
            val stage = parts[2].toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            val shape = parts[3].takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            ReviewItem(topic, shape, due, stage)
        }
    }

    /**
     * Files a missed shape, due tomorrow. A shape already in the queue is
     * left where it is rather than reset: missing it twice does not make it
     * twice as urgent, and resetting would let one stubborn shape crowd
     * everything else out.
     */
    fun recordMiss(raw: String?, topic: ProblemTopic, shape: String?, today: Long): String {
        if (shape == null) return raw.orEmpty()
        val items = decode(raw)
        if (items.any { it.shape == shape }) return raw.orEmpty()
        return encode((items + ReviewItem(topic, shape, today + INTERVALS.first())).takeLast(MAX_ITEMS))
    }

    /**
     * Files a correct answer. A shape that was due and has now been solved
     * moves to the next, longer gap; one solved at the last stage is
     * retired, because it has been right across two weeks and is simply
     * known.
     */
    fun recordCorrect(raw: String?, shape: String?, today: Long): String {
        if (shape == null) return raw.orEmpty()
        val items = decode(raw)
        val item = items.firstOrNull { it.shape == shape } ?: return raw.orEmpty()
        val nextStage = item.stage + 1
        val rest = items.filterNot { it.shape == shape }
        if (nextStage >= INTERVALS.size) return encode(rest)
        return encode(rest + item.copy(dueDay = today + INTERVALS[nextStage], stage = nextStage))
    }

    /**
     * Picks something due to bring back, or null to leave the roll alone.
     *
     * Only topics that are switched on are eligible: bringing back a shape
     * from a topic she has turned off would be putting it back on for her.
     */
    fun due(
        raw: String?,
        today: Long,
        enabledTopics: Set<ProblemTopic>,
        random: Random = Random.Default,
    ): ReviewPick? {
        if (random.nextInt(100) >= REVIEW_CHANCE_PERCENT) return null
        val ready = decode(raw).filter { it.dueDay <= today && it.topic in enabledTopics }
        if (ready.isEmpty()) return null
        // The one that has been waiting longest goes first.
        val oldest = ready.minOf { it.dueDay }
        val pick = ready.filter { it.dueDay == oldest }.random(random)
        return ReviewPick(pick.topic, pick.shape)
    }
}
