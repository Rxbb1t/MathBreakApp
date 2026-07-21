package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * Short logic riddles with a single numeric answer, in both languages.
 *
 * EASY: what-comes-next sequences, one-step "how many at first", leftovers.
 * MEDIUM: doubling sequences, two-step backwards, age puzzles.
 * HARD: heads-and-legs, three-step backwards, twice-as-old ages.
 *
 * Each riddle type dresses the same math in one of eight settings (zoo,
 * school, library, liters of water or soda, …). Templates are authored in
 * EN/RO pairs; number order inside the text is fixed per type, and every
 * English template carries its type's marker phrase. The tests rely on
 * both to re-derive answers.
 *
 * People puzzles borrow the family names from settings when there are
 * enough, topping up from a small neutral pool otherwise.
 */
class LogicProblemGenerator(private val random: Random) {

    fun generate(
        difficulty: Difficulty,
        familyNames: List<String>,
        language: AppLanguage,
    ): Problem = when (difficulty) {
        Difficulty.EASY -> when (random.nextInt(3)) {
            0 -> sequence(language, growByStep = true)
            1 -> backwardsOneStep(language)
            else -> leftover(language)
        }

        Difficulty.MEDIUM -> when (random.nextInt(3)) {
            0 -> sequence(language, growByStep = false)
            1 -> backwardsTwoSteps(language)
            else -> ages(language, names(familyNames))
        }

        Difficulty.HARD -> when (random.nextInt(3)) {
            0 -> headsAndLegs(language)
            1 -> backwardsThreeSteps(language)
            else -> agesTwiceAsOld(language, names(familyNames))
        }
    }

    /** An EN/RO pair; corresponding entries tell the same story. */
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

    /** "3, 7, 11, 15, …" (+step) or "3, 6, 12, 24, …" (×ratio). */
    private fun sequence(language: AppLanguage, growByStep: Boolean): Problem {
        val terms: List<Int>
        val ruleHint: String
        if (growByStep) {
            val start = random.nextInt(1, 13)
            val step = random.nextInt(2, 10)
            terms = List(5) { start + it * step }
            ruleHint = when (language) {
                AppLanguage.ENGLISH -> "It grows by $step every time"
                AppLanguage.ROMANIAN -> "Crește cu $step de fiecare dată"
            }
        } else {
            val ratio = if (random.nextBoolean()) 2 else 3
            val start = if (ratio == 2) random.nextInt(2, 16) else random.nextInt(2, 7)
            terms = List(5) { i -> start * pow(ratio, i) }
            ruleHint = when (language) {
                AppLanguage.ENGLISH -> "Each number is the one before it × $ratio"
                AppLanguage.ROMANIAN -> "Fiecare număr e cel dinainte × $ratio"
            }
        }
        val growHint = when (language) {
            AppLanguage.ENGLISH -> "How do you get from one number to the next?"
            AppLanguage.ROMANIAN -> "Cum ajungi de la un număr la următorul?"
        }
        val patternNote = when (language) {
            AppLanguage.ENGLISH ->
                "In a number pattern, test both ideas: is each number the one before it plus something, or times something?"
            AppLanguage.ROMANIAN ->
                "Într-un șir de numere, încearcă ambele idei: fiecare număr e cel dinainte plus ceva, sau ori ceva?"
        }
        // The numbers live in the drawn row of boxes, not in the text.
        return logicProblem(
            text = pick(SEQUENCE_TEMPLATES, language),
            answer = terms.last(),
            difficulty = if (growByStep) Difficulty.EASY else Difficulty.MEDIUM,
            hints = listOf(growHint, ruleHint),
            notes = if (growByStep) emptyList() else listOf(patternNote),
            diagram = Diagram.SequenceRow(terms.dropLast(1).map(Int::toString) + "?"),
            solution = listOf(
                step(
                    "The rule: $ruleHint",
                    "Regula: $ruleHint",
                    language,
                ),
                step(
                    "Check it: ${terms.dropLast(1).joinToString(", ")}",
                    "Verifică: ${terms.dropLast(1).joinToString(", ")}",
                    language,
                ),
                step(
                    "Apply it once more after ${terms[terms.size - 2]}: ${terms.last()}",
                    "Mai aplic-o o dată după ${terms[terms.size - 2]}: ${terms.last()}",
                    language,
                ),
            ),
        )
    }

    /** Something grew by {c} to reach {r}; how much was there at first? */
    private fun backwardsOneStep(language: AppLanguage): Problem {
        val x = random.nextInt(2, 61)
        val c = random.nextInt(2, 31)
        val r = x + c
        val beforeHint = when (language) {
            AppLanguage.ENGLISH -> "Work backwards: subtract what was added later."
            AppLanguage.ROMANIAN -> "Lucrează invers: scade ce s-a adăugat mai târziu."
        }
        return logicProblem(
            text = fill(
                pick(ADD_BACKWARDS_TEMPLATES, language),
                mapOf("c" to "$c", "r" to "$r"),
            ),
            answer = x,
            difficulty = Difficulty.EASY,
            hints = listOf(beforeHint, HintText.digits(x, language)),
            solution = listOf(
                step(
                    "The story ends at $r after $c were added.",
                    "Povestea se termină la $r după ce s-au adăugat $c.",
                    language,
                ),
                step(
                    "Run it backwards: $r − $c = $x",
                    "Ia-o invers: $r − $c = $x",
                    language,
                ),
            ),
        )
    }

    /** {n} things, {p} takers × {k} each. How many are left? */
    private fun leftover(language: AppLanguage): Problem {
        val people = random.nextInt(2, 5)
        val each = random.nextInt(2, 4)
        val left = random.nextInt(1, 7)
        val total = people * each + left
        val takenHint = when (language) {
            AppLanguage.ENGLISH -> "Same amount each, so multiply to see how much is taken."
            AppLanguage.ROMANIAN -> "Aceeași cantitate fiecare, deci înmulțește ca să afli cât se ia."
        }
        return logicProblem(
            text = fill(
                pick(LEFTOVER_TEMPLATES, language),
                mapOf("n" to "$total", "p" to "$people", "k" to "$each"),
            ),
            answer = left,
            difficulty = Difficulty.EASY,
            hints = listOf(takenHint, HintText.digits(left, language)),
            solution = listOf(
                step(
                    "Everyone takes the same amount, so multiply: $people × $each = ${people * each}",
                    "Fiecare ia la fel, deci înmulțește: $people × $each = ${people * each}",
                    language,
                ),
                step(
                    "Take that off the pile: $total − ${people * each} = $left",
                    "Scade din grămadă: $total − ${people * each} = $left",
                    language,
                ),
            ),
        )
    }

    /** Something doubled, then {c} more, reaching {r}. */
    private fun backwardsTwoSteps(language: AppLanguage): Problem {
        val x = random.nextInt(3, 41)
        val c = random.nextInt(2, 31)
        val r = 2 * x + c
        val beforeBothHint = when (language) {
            AppLanguage.ENGLISH -> "Work backwards through both steps to reach the start."
            AppLanguage.ROMANIAN -> "Lucrează invers prin ambii pași ca să ajungi la început."
        }
        val undoNote = when (language) {
            AppLanguage.ENGLISH ->
                "To find a starting number, undo the story in reverse order: what was added, take away; what was doubled, halve."
            AppLanguage.ROMANIAN ->
                "Ca să afli numărul de pornire, desfă povestea în ordine inversă: ce s-a adunat, scade; ce s-a dublat, înjumătățește."
        }
        return logicProblem(
            text = fill(
                pick(DOUBLE_BACKWARDS_TEMPLATES, language),
                mapOf("c" to "$c", "r" to "$r"),
            ),
            answer = x,
            difficulty = Difficulty.MEDIUM,
            hints = listOf(beforeBothHint, HintText.digits(x, language)),
            notes = listOf(undoNote),
            solution = listOf(
                step(
                    "Undo the last step first: $c were added, so $r − $c = ${2 * x}",
                    "Anulează întâi ultimul pas: s-au adăugat $c, deci $r − $c = ${2 * x}",
                    language,
                ),
                step(
                    "Before that it had doubled, so halve it: ${2 * x} ÷ 2 = $x",
                    "Înainte se dublase, deci înjumătățește: ${2 * x} ÷ 2 = $x",
                    language,
                ),
            ),
        )
    }

    /** "{n1} is {d} years older than {n2}, together {t}." */
    private fun ages(language: AppLanguage, names: Pair<String, String>): Problem {
        val (older, younger) = names
        val youngerAge = random.nextInt(5, 61)
        val gap = random.nextInt(2, 16)
        val total = 2 * youngerAge + gap
        val headStartHint = when (language) {
            AppLanguage.ENGLISH -> "Take the $gap-year gap out of the total, split the rest in two, then add the gap back to the older one."
            AppLanguage.ROMANIAN -> "Scoate diferența de $gap ani din total, împarte restul în două, apoi adaugă diferența înapoi la cel mare."
        }
        val agesNote = when (language) {
            AppLanguage.ENGLISH ->
                "Two ages, a total, and a gap: take the gap out of the total, split what's left evenly, then give the gap back to the older one."
            AppLanguage.ROMANIAN ->
                "Două vârste, un total și o diferență: scoate diferența din total, împarte restul egal, apoi dă diferența înapoi celui mai mare."
        }
        return logicProblem(
            text = fill(
                pick(AGES_TEMPLATES, language),
                mapOf(
                    "n1" to older,
                    "n2" to younger,
                    "d" to "$gap",
                    "t" to yearsFor(total, language),
                ),
            ),
            answer = youngerAge + gap,
            difficulty = Difficulty.MEDIUM,
            hints = listOf(headStartHint, HintText.digits(youngerAge + gap, language)),
            notes = listOf(agesNote),
            solution = listOf(
                step(
                    "Take the $gap-year gap out of the total: $total − $gap = ${total - gap}",
                    "Scoate diferența de $gap ani din total: $total − $gap = ${total - gap}",
                    language,
                ),
                step(
                    "What is left splits evenly, so $younger is ${total - gap} ÷ 2 = $youngerAge",
                    "Ce rămâne se împarte egal, deci $younger are ${total - gap} ÷ 2 = $youngerAge",
                    language,
                ),
                step(
                    "Give the gap back to $older: $youngerAge + $gap = ${youngerAge + gap}",
                    "Dă diferența înapoi lui $older: $youngerAge + $gap = ${youngerAge + gap}",
                    language,
                ),
            ),
        )
    }

    /** Two-legged and four-legged animals: {h} heads, {l} legs. */
    private fun headsAndLegs(language: AppLanguage): Problem {
        val fourLegged = random.nextInt(2, 9)
        val twoLegged = random.nextInt(2, 11)
        val heads = fourLegged + twoLegged
        val legs = 4 * fourLegged + 2 * twoLegged
        val extraLegsHint = when (language) {
            AppLanguage.ENGLISH -> "Give every head two legs first, then share out the legs left over."
            AppLanguage.ROMANIAN -> "Dă întâi două picioare fiecărui cap, apoi împarte picioarele rămase."
        }
        val headsLegsNote = when (language) {
            AppLanguage.ENGLISH ->
                "Heads and legs: imagine every animal with just two legs first. Each four-legged animal adds exactly two extra legs, so the extra legs, shared out two by two, count the four-legged ones."
            AppLanguage.ROMANIAN ->
                "Capete și picioare: imaginează-ți mai întâi că toate animalele au doar două picioare. Fiecare animal cu patru picioare mai aduce exact două, deci picioarele în plus, împărțite câte două, numără animalele cu patru picioare."
        }
        // The Romanian noun rides along with the number ("52 de picioare").
        val legsValue = when (language) {
            AppLanguage.ENGLISH -> "$legs"
            AppLanguage.ROMANIAN -> if (legs < 20) "$legs picioare" else "$legs de picioare"
        }
        return logicProblem(
            text = fill(
                pick(HEADS_LEGS_TEMPLATES, language),
                mapOf("h" to "$heads", "l" to legsValue),
            ),
            answer = fourLegged,
            difficulty = Difficulty.HARD,
            hints = listOf(extraLegsHint, HintText.digits(fourLegged, language)),
            notes = listOf(headsLegsNote),
            solution = listOf(
                step(
                    "Give every one of the $heads animals two legs first: $heads × 2 = ${2 * heads}",
                    "Dă întâi câte două picioare fiecăruia dintre cele $heads animale: $heads × 2 = ${2 * heads}",
                    language,
                ),
                step(
                    "That leaves $legs − ${2 * heads} = ${legs - 2 * heads} legs unaccounted for",
                    "Rămân $legs − ${2 * heads} = ${legs - 2 * heads} picioare nefolosite",
                    language,
                ),
                step(
                    "Each four-legged animal needs exactly two extra, so ${legs - 2 * heads} ÷ 2 = $fourLegged",
                    "Fiecare animal cu patru picioare mai cere exact două, deci ${legs - 2 * heads} ÷ 2 = $fourLegged",
                    language,
                ),
            ),
        )
    }

    /** Times {m}, plus {c}, then halved makes {r}. */
    private fun backwardsThreeSteps(language: AppLanguage): Problem {
        val m = random.nextInt(2, 6)
        val x = random.nextInt(2, 21)
        // Keep the halving whole: nudge the added constant's parity.
        var c = random.nextInt(2, 21)
        if ((m * x + c) % 2 != 0) c++
        val r = (m * x + c) / 2
        val threeChangesHint = when (language) {
            AppLanguage.ENGLISH -> "Undo all three changes in reverse to get back to the start."
            AppLanguage.ROMANIAN -> "Anulează toate cele trei schimbări în ordine inversă ca să ajungi la început."
        }
        val reverseNote = when (language) {
            AppLanguage.ENGLISH ->
                "To find a starting number, undo the story in reverse order, last step first: what was halved, double; what was added, take away; what was multiplied, divide."
            AppLanguage.ROMANIAN ->
                "Ca să afli numărul de pornire, desfă povestea în ordine inversă, ultimul pas primul: ce s-a înjumătățit, dublează; ce s-a adunat, scade; ce s-a înmulțit, împarte."
        }
        return logicProblem(
            text = fill(
                pick(THREE_STEP_TEMPLATES, language),
                // {c_de} for the Romanian template that counts a noun with
                // it ("21 de linguri"); {c} for the ones that don't.
                mapOf("m" to "$m", "c" to "$c", "c_de" to de(c), "r" to "$r"),
            ),
            answer = x,
            difficulty = Difficulty.HARD,
            hints = listOf(threeChangesHint, HintText.digits(x, language)),
            notes = listOf(reverseNote),
            solution = listOf(
                step(
                    "Last step first: it was halved to reach $r, so double back: $r × 2 = ${m * x + c}",
                    "Ultimul pas primul: s-a înjumătățit până la $r, deci dublează înapoi: $r × 2 = ${m * x + c}",
                    language,
                ),
                step(
                    "Before that $c had been added: ${m * x + c} − $c = ${m * x}",
                    "Înainte se adăugaseră $c: ${m * x + c} − $c = ${m * x}",
                    language,
                ),
                step(
                    "And before that it was multiplied by $m: ${m * x} ÷ $m = $x",
                    "Iar înainte fusese înmulțit cu $m: ${m * x} ÷ $m = $x",
                    language,
                ),
            ),
        )
    }

    /** "{n1} is twice as old as {n2}, together {t}." */
    private fun agesTwiceAsOld(language: AppLanguage, names: Pair<String, String>): Problem {
        val (older, younger) = names
        val youngerAge = random.nextInt(5, 41)
        val total = 3 * youngerAge
        val partsHint = when (language) {
            AppLanguage.ENGLISH -> "Twice as old means 3 equal parts in total. Divide to find one part."
            AppLanguage.ROMANIAN -> "De două ori mai mare înseamnă 3 părți egale în total. Împarte ca să afli o parte."
        }
        val partsNote = when (language) {
            AppLanguage.ENGLISH ->
                "\"Twice as old\" splits the total into 3 equal shares: one share for the younger, two shares for the older."
            AppLanguage.ROMANIAN ->
                "„De două ori mai mare” împarte totalul în 3 părți egale: o parte pentru cel mic, două părți pentru cel mare."
        }
        return logicProblem(
            text = fill(
                pick(TWICE_AS_OLD_TEMPLATES, language),
                mapOf("n1" to older, "n2" to younger, "t" to yearsFor(total, language)),
            ),
            answer = 2 * youngerAge,
            difficulty = Difficulty.HARD,
            hints = listOf(partsHint, HintText.digits(2 * youngerAge, language)),
            notes = listOf(partsNote),
            solution = listOf(
                step(
                    "Twice as old means $younger is 1 share and $older is 2, so the total is 3 shares.",
                    "De două ori mai mare înseamnă că $younger e 1 parte și $older e 2, deci totalul e 3 părți.",
                    language,
                ),
                step(
                    "One share: $total ÷ 3 = $youngerAge",
                    "O parte: $total ÷ 3 = $youngerAge",
                    language,
                ),
                step(
                    "$older has two shares: $youngerAge × 2 = ${2 * youngerAge}",
                    "$older are două părți: $youngerAge × 2 = ${2 * youngerAge}",
                    language,
                ),
            ),
        )
    }

    private fun logicProblem(
        text: String,
        answer: Int,
        difficulty: Difficulty,
        hints: List<String>,
        solution: List<String>,
        notes: List<String> = emptyList(),
        diagram: Diagram? = null,
    ): Problem = Problem(
        text = text,
        answer = answer,
        difficulty = difficulty,
        kind = ProblemKind.LOGIC,
        hints = hints,
        notes = notes,
        diagram = diagram,
        solution = solution,
    )

    /**
     * Worked-solution steps. A riddle's solution has to say WHY the move
     * works ("undo the story backwards", "two legs each first"), not just
     * show the arithmetic -- the arithmetic was never the hard part.
     */
    private fun step(en: String, ro: String, language: AppLanguage): String =
        if (language == AppLanguage.ROMANIAN) ro else en

    /** Two distinct people from the given pool. */
    private fun names(familyNames: List<String>): Pair<String, String> {
        val pool = if (familyNames.size >= 2) {
            familyNames
        } else {
            (familyNames + FALLBACK_NAMES).distinct()
        }
        val picked = pool.shuffled(random).take(2)
        return picked[0] to picked[1]
    }

    /** Romanian counts years as "17 ani" but "26 de ani". */
    private fun yearsFor(total: Int, language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "$total"
        AppLanguage.ROMANIAN -> "${de(total)} ani"
    }

    /** The same rule on its own, for templates that count something else. */
    private fun de(n: Int): String = if (n < 20) "$n" else "$n de"

    private fun pow(base: Int, exponent: Int): Int {
        var result = 1
        repeat(exponent) { result *= base }
        return result
    }

    companion object {
        /** Used when the caller passes fewer than two names. */
        private val FALLBACK_NAMES = listOf("Ana", "Maria", "Ion", "Elena")

        // ════════════════════════════════════════════════════════════════
        // ✏️ RIDDLE TEMPLATES. Eight settings per type, EN/RO pairs.
        // Add or reword freely, but keep three rules so nothing breaks:
        //  1. keep every {placeholder} and their left-to-right order;
        //  2. keep the English marker phrase of the type (noted above
        //     each list). Tests use it to recognise the riddle;
        //  3. write no digits into the template text itself.
        // ════════════════════════════════════════════════════════════════

        // Marker: "comes next". A question line; the numbers follow below.
        private val SEQUENCE_TEMPLATES = listOf(
            Template("What comes next?", "Ce urmează?"),
            Template("Which number comes next?", "Ce număr urmează?"),
            Template("The row continues. What comes next?", "Șirul continuă. Ce urmează?"),
            Template("Find the pattern. What comes next?", "Găsește regula. Ce urmează?"),
            Template("These numbers follow a rule. What comes next?", "Numerele urmează o regulă. Ce urmează?"),
            Template("Keep the row going. What comes next?", "Du șirul mai departe. Ce urmează?"),
            Template("The last one is missing. What comes next?", "Ultimul lipsește. Ce urmează?"),
            Template("Look closely. What comes next?", "Privește atent. Ce urmează?"),
        )

        // Marker: "now". Order: {c} then {r}; asks for the starting amount.
        private val ADD_BACKWARDS_TEMPLATES = listOf(
            Template(
                "I'm thinking of a number. I add {c} and now I have {r}.\nWhat's my number?",
                "Mă gândesc la un număr. Adun {c} și acum am {r}.\nCe număr e?",
            ),
            Template(
                "Some cars were parked. {c} more arrived, and now there are {r}.\nHow many were parked at first?",
                "În parcare erau niște mașini. Au mai venit {c} și acum sunt {r}.\nCâte erau la început?",
            ),
            Template(
                "Birds sat on a wire. {c} more landed, and now there are {r}.\nHow many sat there at first?",
                "Pe un fir stăteau niște păsări. Au mai venit {c} și acum sunt {r}.\nCâte erau la început?",
            ),
            Template(
                "A jar held some candies. I dropped in {c} more, and now it holds {r}.\nHow many were inside at first?",
                "Într-un borcan erau niște bomboane. Am mai pus {c} și acum sunt {r}.\nCâte erau la început?",
            ),
            Template(
                "A shelf held some books. The librarian added {c}, and now there are {r}.\nHow many stood there at first?",
                "Pe un raft erau niște cărți. Bibliotecara a mai adus {c} și acum sunt {r}.\nCâte erau la început?",
            ),
            Template(
                "A bus set off with some passengers. {c} got on at the stop, and now there are {r}.\nHow many rode at first?",
                "Într-un autobuz erau niște călători. Au urcat {c} în stație și acum sunt {r}.\nCâți erau la început?",
            ),
            Template(
                "A barrel held some liters of water. I poured in {c} more, and now it holds {r}.\nHow many liters at first?",
                "Într-un butoi erau niște litri de apă. Am mai turnat {c} și acum sunt {r}.\nCâți litri erau la început?",
            ),
            Template(
                "Some ducks swam on the zoo pond. {c} more paddled in, and now there are {r}.\nHow many swam there at first?",
                "Pe lacul de la zoo erau niște rațe. Au mai venit {c} și acum sunt {r}.\nCâte erau la început?",
            ),
        )

        // Marker: "left?". Order: {n} things, {p} takers, {k} each.
        private val LEFTOVER_TEMPLATES = listOf(
            Template(
                "A cake is cut into {n} slices. {p} people eat {k} slices each.\nHow many slices are left?",
                "Un tort e tăiat în {n} felii. {p} persoane mănâncă câte {k} felii fiecare.\nCâte felii rămân?",
            ),
            Template(
                "A pizza is cut into {n} pieces. {p} friends take {k} pieces each.\nHow many pieces are left?",
                "O pizza e tăiată în {n} bucăți. {p} prieteni iau câte {k} bucăți fiecare.\nCâte bucăți rămân?",
            ),
            Template(
                "A shelf holds {n} books. {p} readers borrow {k} books each.\nHow many books are left?",
                "Pe un raft sunt {n} cărți. {p} cititori împrumută câte {k} cărți fiecare.\nCâte cărți rămân?",
            ),
            Template(
                "A cooler holds {n} liters of water. {p} hikers drink {k} liters each.\nHow many liters are left?",
                "Un bidon are {n} litri de apă. {p} drumeți beau câte {k} litri fiecare.\nCâți litri rămân?",
            ),
            Template(
                "A zookeeper brings {n} apples. {p} elephants eat {k} apples each.\nHow many apples are left?",
                "Îngrijitorul de la zoo aduce {n} mere. {p} elefanți mănâncă câte {k} mere fiecare.\nCâte mere rămân?",
            ),
            Template(
                "A teacher has {n} pencils. {p} pupils take {k} pencils each.\nHow many pencils are left?",
                "O învățătoare are {n} creioane. {p} elevi iau câte {k} creioane fiecare.\nCâte creioane rămân?",
            ),
            Template(
                "A crate holds {n} bottles of soda. {p} guests take {k} bottles each.\nHow many bottles are left?",
                "Într-o ladă sunt {n} sticle de suc. {p} musafiri iau câte {k} sticle fiecare.\nCâte sticle rămân?",
            ),
            Template(
                "A basket holds {n} tomatoes. {p} neighbours take {k} tomatoes each.\nHow many tomatoes are left?",
                "Într-un coș sunt {n} roșii. {p} vecini iau câte {k} roșii fiecare.\nCâte roșii rămân?",
            ),
        )

        // Marker: "doubl". Order: {c} then {r}; asks for the start.
        private val DOUBLE_BACKWARDS_TEMPLATES = listOf(
            Template(
                "I double a number and add {c}. I get {r}.\nWhat was the number?",
                "Dublez un număr și adun {c}. Obțin {r}.\nCare era numărul?",
            ),
            Template(
                "If you double a number, then add {c}, you get {r}.\nWhat's the number?",
                "Dacă dublezi un număr și apoi adaugi {c}, obții {r}.\nCare e numărul?",
            ),
            Template(
                "My savings doubled, then I added {c} more, reaching {r}.\nHow much was saved at the start?",
                "Economiile mele s-au dublat, apoi am adăugat {c}, ajungând la {r}.\nCât aveam la început?",
            ),
            Template(
                "A plant doubled its flowers, then grew {c} more. {r} flowers in all.\nHow many did it have at the start?",
                "O plantă și-a dublat florile, apoi au apărut încă {c}. În total {r}.\nCâte avea la început?",
            ),
            Template(
                "The library doubled its puzzle books and bought {c} more, reaching {r}.\nHow many were there at the start?",
                "Biblioteca și-a dublat cărțile de jocuri și a mai cumpărat {c}, ajungând la {r}.\nCâte erau la început?",
            ),
            Template(
                "A tank's liters of water doubled in the rain, plus {c} from the hose. {r} liters in all.\nHow many liters at the start?",
                "Litrii de apă dintr-un bazin s-au dublat după ploaie, plus {c} de la furtun. În total {r}.\nCâți litri erau la început?",
            ),
            Template(
                "I doubled my stamp collection and got {c} as a gift. {r} stamps in all.\nHow many did I start with?",
                "Mi-am dublat colecția de timbre și am primit {c} cadou. În total {r}.\nCu câte am început?",
            ),
            Template(
                "The zoo doubled its parrots and welcomed {c} more. {r} parrots in all.\nHow many lived there at the start?",
                "Zoo-ul și-a dublat papagalii și a mai primit {c}. În total {r}.\nCâți erau la început?",
            ),
        )

        // Marker: "years older". Order: {d} then {t}; asks how old {n1} is.
        private val AGES_TEMPLATES = listOf(
            Template(
                "{n1} is {d} years older than {n2}. Together they are {t}.\nHow old is {n1}?",
                "{n1} e cu {d} ani mai mare decât {n2}. Împreună au {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "{n1} is {d} years older than {n2}. Their ages add up to {t}.\nHow old is {n1}?",
                "{n1} e cu {d} ani mai mare decât {n2}. Adunate, vârstele lor fac {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "Two cousins: {n1} is {d} years older than {n2}, and together they are {t}.\nHow old is {n1}?",
                "Doi verișori: {n1} e cu {d} ani mai mare decât {n2}, iar împreună au {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "Neighbours {n1} and {n2}: {n1} is {d} years older, and together they are {t}.\nHow old is {n1}?",
                "Vecinii {n1} și {n2}: {n1} e cu {d} ani mai mare, iar împreună au {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "At the family table, {n1} is {d} years older than {n2}. Their years together: {t}.\nHow old is {n1}?",
                "La masa de familie, {n1} e cu {d} ani mai mare decât {n2}. Anii lor la un loc: {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "{n1} is {d} years older than {n2}. Their ages together come to {t}.\nHow old is {n1}?",
                "{n1} e cu {d} ani mai mare decât {n2}. Vârstele lor împreună ajung la {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "Good friends {n1} and {n2}: {n1} is {d} years older, together they are {t}.\nHow old is {n1}?",
                "Bunii prieteni {n1} și {n2}: {n1} e cu {d} ani mai mare, împreună au {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "In the garden club, {n1} is {d} years older than {n2}, and together they are {t}.\nHow old is {n1}?",
                "La clubul de grădinărit, {n1} e cu {d} ani mai mare decât {n2}, iar împreună au {t}.\nCâți ani are {n1}?",
            ),
        )

        // Marker: "legs". Order: {h} heads then {l} legs; asks for the
        // four-legged kind (RO {l} already includes the word "picioare").
        private val HEADS_LEGS_TEMPLATES = listOf(
            Template(
                "In the yard there are chickens and rabbits:\n{h} heads and {l} legs.\nHow many rabbits?",
                "În curte sunt găini și iepuri:\n{h} capete și {l}.\nCâți iepuri?",
            ),
            Template(
                "On the farm there are ducks and dogs:\n{h} heads and {l} legs.\nHow many dogs?",
                "La fermă sunt rațe și câini:\n{h} capete și {l}.\nCâți câini?",
            ),
            Template(
                "In the barn there are hens and pigs:\n{h} heads and {l} legs.\nHow many pigs?",
                "În grajd sunt găini și porci:\n{h} capete și {l}.\nCâți porci?",
            ),
            Template(
                "In the garden there are birds and cats:\n{h} heads and {l} legs.\nHow many cats?",
                "În grădină sunt păsări și pisici:\n{h} capete și {l}.\nCâte pisici?",
            ),
            Template(
                "In the meadow there are geese and sheep:\n{h} heads and {l} legs.\nHow many sheep?",
                "Pe pajiște sunt gâște și oi:\n{h} capete și {l}.\nCâte oi?",
            ),
            Template(
                "At the petting zoo there are ducks and goats:\n{h} heads and {l} legs.\nHow many goats?",
                "La zoo sunt rațe și capre:\n{h} capete și {l}.\nCâte capre?",
            ),
            Template(
                "In the park there are pigeons and rabbits:\n{h} heads and {l} legs.\nHow many rabbits?",
                "În parc sunt porumbei și iepuri:\n{h} capete și {l}.\nCâți iepuri?",
            ),
            Template(
                "In the farmyard there are chickens and cows:\n{h} heads and {l} legs.\nHow many cows?",
                "În ogradă sunt găini și vaci:\n{h} capete și {l}.\nCâte vaci?",
            ),
        )

        // Marker: "halv". Order: {m}, {c}, {r}; asks for the start.
        private val THREE_STEP_TEMPLATES = listOf(
            Template(
                "I think of a number, multiply it by {m}, add {c},\nthen halve everything. I get {r}.\nWhat was my number?",
                "Mă gândesc la un număr, îl înmulțesc cu {m}, adun {c},\napoi înjumătățesc totul. Obțin {r}.\nCare era numărul?",
            ),
            Template(
                "A number machine multiplies by {m}, adds {c}, then halves the result. Out comes {r}.\nWhat went in?",
                "O mașinărie de numere înmulțește cu {m}, adună {c}, apoi înjumătățește. Iese {r}.\nCe număr a intrat?",
            ),
            Template(
                "Take a number, multiply by {m}, add {c}, halve it. You get {r}.\nWhat was the number?",
                "Ia un număr, înmulțește-l cu {m}, adună {c}, înjumătățește. Obții {r}.\nCare era numărul?",
            ),
            Template(
                "A magician multiplies a secret number by {m}, adds {c}, then halves it, revealing {r}.\nWhat's the secret number?",
                "Un magician înmulțește un număr secret cu {m}, adună {c}, apoi îl înjumătățește: {r}.\nCare e numărul secret?",
            ),
            Template(
                "My riddle: times {m}, plus {c}, then halved makes {r}.\nWhat number did I pick?",
                "Ghicitoarea mea: ori {m}, plus {c}, apoi înjumătățit dă {r}.\nCe număr am ales?",
            ),
            Template(
                "A recipe multiplies the sugar by {m}, adds {c} spoons, then halves the mix, leaving {r}.\nHow much sugar was there first?",
                "O rețetă înmulțește zahărul cu {m}, adaugă {c_de} linguri, apoi înjumătățește amestecul: rămân {r}.\nCât zahăr era la început?",
            ),
            Template(
                "The teacher's trick: multiply by {m}, add {c}, halve it. Result: {r}.\nWhat was the starting number?",
                "Trucul învățătoarei: înmulțește cu {m}, adună {c}, înjumătățește. Rezultat: {r}.\nCare era numărul de pornire?",
            ),
            Template(
                "A robot multiplies your number by {m}, adds {c}, then halves everything, showing {r}.\nWhat number did you give it?",
                "Un robot îți înmulțește numărul cu {m}, adună {c}, apoi înjumătățește totul, arătând {r}.\nCe număr i-ai dat?",
            ),
        )

        // Marker: "twice as old". Only number: {t}; asks how old {n1} is.
        private val TWICE_AS_OLD_TEMPLATES = listOf(
            Template(
                "{n1} is twice as old as {n2}. Together they are {t}.\nHow old is {n1}?",
                "{n1} e de două ori mai mare decât {n2}. Împreună au {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "{n1} is twice as old as {n2}. Their ages add up to {t}.\nHow old is {n1}?",
                "{n1} e de două ori mai mare decât {n2}. Adunate, vârstele lor fac {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "Two cousins: {n1} is twice as old as {n2}, and together they are {t}.\nHow old is {n1}?",
                "Doi verișori: {n1} e de două ori mai mare decât {n2}, iar împreună au {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "Neighbours {n1} and {n2}: {n1} is twice as old, and together they are {t}.\nHow old is {n1}?",
                "Vecinii {n1} și {n2}: {n1} e de două ori mai mare, iar împreună au {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "At the family table, {n1} is twice as old as {n2}. Their years together: {t}.\nHow old is {n1}?",
                "La masa de familie, {n1} e de două ori mai mare decât {n2}. Anii lor la un loc: {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "{n1} is twice as old as {n2}. Their ages together come to {t}.\nHow old is {n1}?",
                "{n1} e de două ori mai mare decât {n2}. Vârstele lor împreună ajung la {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "Good friends {n1} and {n2}: {n1} is twice as old, together they are {t}.\nHow old is {n1}?",
                "Bunii prieteni {n1} și {n2}: {n1} e de două ori mai mare, împreună au {t}.\nCâți ani are {n1}?",
            ),
            Template(
                "In the choir, {n1} is twice as old as {n2}, and together they are {t}.\nHow old is {n1}?",
                "La cor, {n1} e de două ori mai mare decât {n2}, iar împreună au {t}.\nCâți ani are {n1}?",
            ),
        )
    }
}
