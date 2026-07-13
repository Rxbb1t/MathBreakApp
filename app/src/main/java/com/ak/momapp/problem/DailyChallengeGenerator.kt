package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import java.time.LocalDate
import kotlin.random.Random

/**
 * Deals the daily challenge: one five-stage story per calendar day,
 * seeded from the date, so the same day always deals the same story;
 * closing the app, restarting the phone, or switching language changes
 * nothing but the words.
 *
 * Five story arcs rotate day by day: market day, garden day, a family
 * visit, baking day, and a walk in the park. Every stage is a normal
 * [Problem] (hints, units, diagrams all work), told in the second
 * person, and later stages carry earlier answers in their text.
 *
 * Same rules as everywhere else: whole non-negative answers only, and
 * hints restate meaning. Never the solving steps.
 */
class DailyChallengeGenerator {

    fun generate(date: LocalDate, language: AppLanguage): DailyChallenge {
        val day = date.toEpochDay()
        // One random stream per day; both languages draw the same numbers.
        val random = Random(day)
        return when ((day % ARC_COUNT).toInt()) {
            0 -> marketDay(random, language)
            1 -> gardenDay(random, language)
            2 -> visitDay(random, language)
            3 -> bakingDay(random, language)
            else -> parkDay(random, language)
        }
    }

    // ── Arc 1: market day. Money, a clock, and the morning purse ───────

    private fun marketDay(random: Random, language: AppLanguage): DailyChallenge {
        val q1 = random.nextInt(2, 7)
        val p1 = random.nextInt(3, 10)
        val q2 = random.nextInt(2, 7)
        val p2 = random.nextInt(3, 10)
        val spent = q1 * p1 + q2 * p2
        val change = 200 - spent
        val start = random.nextInt(8 * 60, 10 * 60).let { it - it % 5 }
        val span = random.nextInt(35, 86)
        val perKid = random.nextInt(2, 5)
        val kids = random.nextInt(2, 6)
        val pharmacy = random.nextInt(15, 61)
        val leftOver = random.nextInt(20, 91)
        val purse = spent + pharmacy + leftOver
        val en = language == AppLanguage.ENGLISH

        val intro =
            if (en) "Market day! Keep count of your money as you go."
            else "Zi de piață! Ține socoteala banilor pe drum."
        return DailyChallenge(
            intro = intro,
            stages = listOf(
                stage(
                    text = if (en) {
                        "At the market you buy $q1 kg of potatoes at $p1 lei per kilo " +
                            "and $q2 kg of apples at $p2 lei per kilo. How much do you spend?"
                    } else {
                        "La piață cumperi $q1 kg de cartofi cu $p1 lei kilogramul " +
                            "și $q2 kg de mere cu $p2 lei kilogramul. Cât cheltuiești?"
                    },
                    answer = spent,
                    kind = ProblemKind.MONEY,
                    hint = if (en) "Two buys, one purse. The answer is everything you paid together."
                    else "Două cumpărături, un singur portofel. Răspunsul e tot ce ai plătit laolaltă.",
                    language = language,
                    unit = "lei",
                ),
                stage(
                    text = if (en) {
                        "You hand the seller a 200-lei banknote for the $spent lei of shopping. " +
                            "How much change do you get back?"
                    } else {
                        "Îi dai vânzătoarei o bancnotă de 200 de lei pentru cumpărăturile " +
                            "de ${de(spent)} lei. Cât rest primești?"
                    },
                    answer = change,
                    kind = ProblemKind.MONEY,
                    hint = if (en) "The change is what the banknote covers beyond the shopping."
                    else "Restul e cât acoperă bancnota peste cumpărături.",
                    language = language,
                    unit = "lei",
                ),
                stage(
                    text = if (en) {
                        "You left home at ${clock(start)} and get back at ${clock(start + span)}. " +
                            "How many minutes were you out?"
                    } else {
                        "Ai plecat de acasă la ${clock(start)} și te întorci la ${clock(start + span)}. " +
                            "Câte minute ai lipsit?"
                    },
                    answer = span,
                    kind = ProblemKind.TIME,
                    hint = if (en) "From leaving to returning. The minutes between the two clocks."
                    else "De la plecare la întoarcere. Minutele dintre cele două ceasuri.",
                    language = language,
                    unit = "min",
                ),
                stage(
                    text = if (en) {
                        "Guests are coming over: you set aside $perKid covrigi for each " +
                            "of the $kids kids. How many covrigi is that?"
                    } else {
                        "Vin musafiri: pui deoparte câte $perKid covrigi pentru fiecare " +
                            "dintre cei $kids copii. Câți covrigi în total?"
                    },
                    answer = perKid * kids,
                    kind = ProblemKind.WORD,
                    hint = if (en) "The same little bundle for every child."
                    else "Același pachețel pentru fiecare copil.",
                    language = language,
                ),
                stage(
                    text = if (en) {
                        "This morning you had x lei in your purse. You spent $spent lei at the " +
                            "market and $pharmacy lei at the pharmacy, and now $leftOver lei " +
                            "are left. How much was x?"
                    } else {
                        "Azi-dimineață aveai x lei în portofel. Ai cheltuit ${de(spent)} lei la " +
                            "piață și ${de(pharmacy)} lei la farmacie, iar acum ți-au rămas " +
                            "${de(leftOver)} lei. Cât era x?"
                    },
                    answer = purse,
                    kind = ProblemKind.EQUATION,
                    hint = if (en) "x was there before any spending. Bigger than everything that followed."
                    else "x era acolo înainte de orice cheltuială. Mai mare decât tot ce a urmat.",
                    language = language,
                    unit = "lei",
                ),
            ),
        )
    }

    // ── Arc 2: garden day. A fence, beds in a grid, and the clock ──────

    private fun gardenDay(random: Random, language: AppLanguage): DailyChallenge {
        val a = random.nextInt(7, 15)
        val b = random.nextInt(4, a)
        val fence = 2 * (a + b)
        val wire = random.nextInt(3, 9)
        val squares = a * b
        val flowers = random.nextInt(5, 21)
        val start = random.nextInt(9 * 60, 17 * 60).let { it - it % 5 }
        val span = random.nextInt(45, 121)
        val en = language == AppLanguage.ENGLISH

        val intro =
            if (en) "A day in the garden. It starts with the fence."
            else "O zi în grădină. Începe cu gardul."
        return DailyChallenge(
            intro = intro,
            stages = listOf(
                stage(
                    text = if (en) {
                        "Your vegetable patch is $a m long and $b m wide. You want a fence all " +
                            "the way around it. How many meters of fence do you need?"
                    } else {
                        "Grădina ta de legume are $a m lungime și $b m lățime. Vrei să pui gard " +
                            "de jur împrejurul ei. De câți metri de gard ai nevoie?"
                    },
                    answer = fence,
                    kind = ProblemKind.GEOMETRY,
                    hint = if (en) "The fence follows every side. The short ones and the long ones alike."
                    else "Gardul urmează fiecare latură. Și pe cele scurte, și pe cele lungi.",
                    language = language,
                    unit = "m",
                    diagram = Diagram.Rectangle(
                        widthLabel = "$a m",
                        heightLabel = "$b m",
                        aspect = a.toFloat() / b,
                    ),
                ),
                stage(
                    text = if (en) {
                        "The fence wire costs $wire lei per meter. " +
                            "How much will all $fence meters cost?"
                    } else {
                        "Plasa de gard costă $wire lei metrul. " +
                            "Cât vor costa toți cei ${de(fence)} metri?"
                    },
                    answer = wire * fence,
                    kind = ProblemKind.MONEY,
                    hint = if (en) "Every meter of fence carries the same price."
                    else "Fiecare metru de gard costă la fel.",
                    language = language,
                    unit = "lei",
                ),
                stage(
                    text = if (en) {
                        "Inside, you divide the whole $a by $b meter patch into one-meter " +
                            "squares, one seedling in each. How many squares do you get?"
                    } else {
                        "Împarți apoi grădina de $a pe $b metri în pătrate de câte un metru, " +
                            "cu un răsad în fiecare. Câte pătrate ies?"
                    },
                    answer = squares,
                    kind = ProblemKind.GEOMETRY,
                    hint = if (en) "The squares fill the patch like a tray of ice cubes. Count them all."
                    else "Pătratele umplu grădina ca o tavă de cuburi. Numără-le pe toate.",
                    language = language,
                    diagram = Diagram.Rectangle(
                        widthLabel = "$a m",
                        heightLabel = "$b m",
                        aspect = a.toFloat() / b,
                        grid = true,
                    ),
                ),
                stage(
                    text = if (en) {
                        "Of the $squares squares, $flowers are saved for flowers. " +
                            "How many squares are left for vegetables?"
                    } else {
                        "Din cele ${de(squares)} pătrate, $flowers le păstrezi pentru flori. " +
                            "Câte pătrate rămân pentru legume?"
                    },
                    answer = squares - flowers,
                    kind = ProblemKind.WORD,
                    hint = if (en) "A few squares leave the vegetable count."
                    else "Câteva pătrate ies din socoteala legumelor.",
                    language = language,
                ),
                stage(
                    text = if (en) {
                        "You started planting at ${clock(start)} and finished at " +
                            "${clock(start + span)}. How many minutes did you work?"
                    } else {
                        "Ai început plantatul la ${clock(start)} și ai terminat la " +
                            "${clock(start + span)}. Câte minute ai muncit?"
                    },
                    answer = span,
                    kind = ProblemKind.TIME,
                    hint = if (en) "From the first seedling to the last. The minutes between the clocks."
                    else "De la primul răsad la ultimul. Minutele dintre cele două ceasuri.",
                    language = language,
                    unit = "min",
                ),
            ),
        )
    }

    // ── Arc 3: a family visit. Trains, gifts, pocket money ─────────────

    private fun visitDay(random: Random, language: AppLanguage): DailyChallenge {
        val start = random.nextInt(7 * 60, 11 * 60).let { it - it % 5 }
        val ride = random.nextInt(55, 176)
        val gifts = random.nextInt(2, 6)
        val price = random.nextInt(8, 26)
        val giftCost = gifts * price
        val ticketThere = random.nextInt(25, 61)
        val ticketBack = random.nextInt(25, 61)
        val tickets = ticketThere + ticketBack
        val bus = random.nextInt(20, 46)
        val leftOver = random.nextInt(30, 121)
        val wallet = giftCost + tickets + leftOver
        val en = language == AppLanguage.ENGLISH

        val intro =
            if (en) "You're going visiting today. First, catch the train."
            else "Azi mergi în vizită. Mai întâi, trenul."
        return DailyChallenge(
            intro = intro,
            stages = listOf(
                stage(
                    text = if (en) {
                        "Your train leaves at ${clock(start)} and arrives at " +
                            "${clock(start + ride)}. How many minutes does the ride take?"
                    } else {
                        "Trenul tău pleacă la ${clock(start)} și ajunge la " +
                            "${clock(start + ride)}. Câte minute durează drumul?"
                    },
                    answer = ride,
                    kind = ProblemKind.TIME,
                    hint = if (en) "Departure to arrival. The minutes between the two clocks."
                    else "De la plecare la sosire. Minutele dintre cele două ceasuri.",
                    language = language,
                    unit = "min",
                ),
                stage(
                    text = if (en) {
                        "At the station shop you pick up $gifts little gifts at $price lei " +
                            "each. How much do the gifts cost?"
                    } else {
                        "De la magazinul din gară iei $gifts cadouri mici de câte $price lei. " +
                            "Cât costă cadourile?"
                    },
                    answer = giftCost,
                    kind = ProblemKind.MONEY,
                    hint = if (en) "The same price, once for every gift."
                    else "Același preț, o dată pentru fiecare cadou.",
                    language = language,
                    unit = "lei",
                ),
                stage(
                    text = if (en) {
                        "The return ticket costs x lei. The two tickets together came to " +
                            "$tickets lei, and the ticket there was $ticketThere lei. What is x?"
                    } else {
                        "Biletul de întors costă x lei. Amândouă biletele au costat " +
                            "${de(tickets)} lei, iar cel de dus a fost ${de(ticketThere)} lei. " +
                            "Cât e x?"
                    },
                    answer = ticketBack,
                    kind = ProblemKind.EQUATION,
                    hint = if (en) "Both tickets together hold x inside them."
                    else "Amândouă biletele îl ascund pe x înăuntru.",
                    language = language,
                    unit = "lei",
                ),
                stage(
                    text = if (en) {
                        "The bus at the other end adds $bus more minutes to the $ride-minute " +
                            "train ride. How many minutes is the whole way?"
                    } else {
                        "Autobuzul de la capăt mai adaugă ${de(bus)} minute la cele " +
                            "${de(ride)} minute de tren. Câte minute ține tot drumul?"
                    },
                    answer = bus + ride,
                    kind = ProblemKind.WORD,
                    hint = if (en) "Train first, then the bus. The whole way is both together."
                    else "Întâi trenul, apoi autobuzul. Tot drumul le cuprinde pe amândouă.",
                    language = language,
                    unit = "min",
                ),
                stage(
                    text = if (en) {
                        "You left home with $wallet lei. After $giftCost lei for gifts and " +
                            "$tickets lei for the two tickets, how much do you bring home?"
                    } else {
                        "Ai plecat de acasă cu ${de(wallet)} lei. După ${de(giftCost)} lei pe " +
                            "cadouri și ${de(tickets)} lei pe bilete, cu câți lei te întorci acasă?"
                    },
                    answer = leftOver,
                    kind = ProblemKind.MONEY,
                    hint = if (en) "What comes home is what the gifts and tickets didn't take."
                    else "Acasă ajunge ce nu au luat cadourile și biletele.",
                    language = language,
                    unit = "lei",
                ),
            ),
        )
    }

    // ── Arc 4: baking day. Supplies, change, trays, and the oven ───────

    private fun bakingDay(random: Random, language: AppLanguage): DailyChallenge {
        val flour = random.nextInt(12, 40)
        val butter = random.nextInt(8, 30)
        val sugar = random.nextInt(5, 25)
        val total = flour + butter + sugar
        val trays = random.nextInt(3, 7)
        val pieces = random.nextInt(6, 13)
        val batch = trays * pieces
        val given = random.nextInt(5, minOf(batch, 21))
        val start = random.nextInt(14 * 60, 17 * 60).let { it - it % 5 }
        val span = random.nextInt(40, 101)
        val en = language == AppLanguage.ENGLISH

        val intro =
            if (en) "Baking day! It starts at the shop."
            else "Zi de copt! Totul începe la magazin."
        return DailyChallenge(
            intro = intro,
            stages = listOf(
                stage(
                    text = if (en) {
                        "At the shop, the flour costs $flour lei, the butter $butter lei and " +
                            "the sugar $sugar lei. How much do the baking supplies cost?"
                    } else {
                        "La magazin, făina costă ${de(flour)} lei, untul ${de(butter)} lei și " +
                            "zahărul ${de(sugar)} lei. Cât costă toate cumpărăturile?"
                    },
                    answer = total,
                    kind = ProblemKind.MONEY,
                    hint = if (en) "Three prices, one basket. The answer holds them all."
                    else "Trei prețuri, un singur coș. Răspunsul le cuprinde pe toate.",
                    language = language,
                    unit = "lei",
                ),
                stage(
                    text = if (en) {
                        "You pay with a 100-lei banknote for the $total lei of supplies. " +
                            "How much change do you get?"
                    } else {
                        "Plătești cu o bancnotă de 100 de lei pentru cumpărăturile de " +
                            "${de(total)} lei. Cât rest primești?"
                    },
                    answer = 100 - total,
                    kind = ProblemKind.MONEY,
                    hint = if (en) "The change is what the banknote covers beyond the shopping."
                    else "Restul e cât acoperă bancnota peste cumpărături.",
                    language = language,
                    unit = "lei",
                ),
                stage(
                    text = if (en) {
                        "You bake $trays trays with $pieces covrigi on each. " +
                            "How many covrigi in the batch?"
                    } else {
                        "Coci $trays tăvi cu câte $pieces covrigi. Câți covrigi în total?"
                    },
                    answer = batch,
                    kind = ProblemKind.WORD,
                    hint = if (en) "Every tray carries the same little crowd."
                    else "Fiecare tavă duce același grup mic.",
                    language = language,
                ),
                stage(
                    text = if (en) {
                        "Of the $batch covrigi, you take $given to the neighbours. " +
                            "How many stay home?"
                    } else {
                        "Din cei ${de(batch)} covrigi, duci $given vecinilor. " +
                            "Câți rămân acasă?"
                    },
                    answer = batch - given,
                    kind = ProblemKind.WORD,
                    hint = if (en) "A few covrigi leave the batch."
                    else "Câțiva covrigi pleacă din socoteală.",
                    language = language,
                ),
                stage(
                    text = if (en) {
                        "The first tray went in at ${clock(start)} and the last one came out " +
                            "at ${clock(start + span)}. How many minutes did the baking take?"
                    } else {
                        "Prima tavă a intrat la ${clock(start)} și ultima a ieșit la " +
                            "${clock(start + span)}. Câte minute a durat coptul?"
                    },
                    answer = span,
                    kind = ProblemKind.TIME,
                    hint = if (en) "From the first tray to the last. The minutes between the clocks."
                    else "De la prima tavă la ultima. Minutele dintre cele două ceasuri.",
                    language = language,
                    unit = "min",
                ),
            ),
        )
    }

    // ── Arc 5: a walk in the park. A lap to measure, laps to count ─────

    private fun parkDay(random: Random, language: AppLanguage): DailyChallenge {
        val start = random.nextInt(8 * 60, 11 * 60).let { it - it % 5 }
        val walk = random.nextInt(20, 61)
        val a = random.nextInt(60, 121)
        val b = random.nextInt(30, a)
        val lap = 2 * (a + b)
        val laps = random.nextInt(2, 5)
        val distance = laps * lap
        val yesterday = random.nextInt(300, 901)
        val rest = random.nextInt(2, 15)
        val en = language == AppLanguage.ENGLISH

        val intro =
            if (en) "A morning walk in the park, with a lemonade at the end."
            else "O plimbare de dimineață în parc, cu o limonadă la final."
        return DailyChallenge(
            intro = intro,
            stages = listOf(
                stage(
                    text = if (en) {
                        "You set off at ${clock(start)} and reach the park at " +
                            "${clock(start + walk)}. How many minutes is the walk there?"
                    } else {
                        "Pornești la ${clock(start)} și ajungi în parc la " +
                            "${clock(start + walk)}. Câte minute ține drumul?"
                    },
                    answer = walk,
                    kind = ProblemKind.TIME,
                    hint = if (en) "From your door to the park gate. The minutes between the clocks."
                    else "De la ușa ta la poarta parcului. Minutele dintre cele două ceasuri.",
                    language = language,
                    unit = "min",
                ),
                stage(
                    text = if (en) {
                        "The path around the lawn is a rectangle $a m long and $b m wide. " +
                            "How many meters is one lap all the way around?"
                    } else {
                        "Aleea din jurul peluzei e un dreptunghi de $a m lungime și $b m " +
                            "lățime. Câți metri are o tură de jur împrejur?"
                    },
                    answer = lap,
                    kind = ProblemKind.GEOMETRY,
                    hint = if (en) "One lap follows every side. The short ones and the long ones alike."
                    else "O tură urmează fiecare latură. Și pe cele scurte, și pe cele lungi.",
                    language = language,
                    unit = "m",
                    diagram = Diagram.Rectangle(
                        widthLabel = "$a m",
                        heightLabel = "$b m",
                        aspect = a.toFloat() / b,
                    ),
                ),
                stage(
                    text = if (en) {
                        "You walk $laps laps of the $lap-meter path. How many meters is that?"
                    } else {
                        "Faci $laps ture pe aleea de ${de(lap)} metri. Câți metri înseamnă?"
                    },
                    answer = distance,
                    kind = ProblemKind.WORD,
                    hint = if (en) "The same lap, again and again."
                    else "Aceeași tură, iar și iar.",
                    language = language,
                    unit = "m",
                ),
                stage(
                    text = if (en) {
                        "Yesterday you walked $yesterday m. With today's $distance m, " +
                            "how many meters over the two days?"
                    } else {
                        "Ieri ai mers ${de(yesterday)} m. Cu cei ${de(distance)} m de azi, " +
                            "câți metri în cele două zile?"
                    },
                    answer = yesterday + distance,
                    kind = ProblemKind.WORD,
                    hint = if (en) "Both days go into one total. Bigger than either one."
                    else "Ambele zile intră într-un singur total. Mai mare decât fiecare.",
                    language = language,
                    unit = "m",
                ),
                stage(
                    text = if (en) {
                        "A lemonade at the kiosk costs x lei. You hand over 20 lei and get " +
                            "$rest lei back. What is x?"
                    } else {
                        "O limonadă la chioșc costă x lei. Dai 20 de lei și primești " +
                            "$rest lei rest. Cât e x?"
                    },
                    answer = 20 - rest,
                    kind = ProblemKind.EQUATION,
                    hint = if (en) "x and the change together make up the 20 lei."
                    else "x și restul împreună fac cei 20 de lei.",
                    language = language,
                    unit = "lei",
                ),
            ),
        )
    }

    // ── Shared helpers ───────────────────────────────────────────────────

    private fun stage(
        text: String,
        answer: Int,
        kind: ProblemKind,
        hint: String,
        language: AppLanguage,
        unit: String = "",
        diagram: Diagram? = null,
    ): Problem = Problem(
        text = text,
        answer = answer,
        difficulty = Difficulty.MEDIUM,
        kind = kind,
        hints = listOf(hint, HintText.digits(answer, language)),
        diagram = diagram,
        answerUnit = unit,
    )

    private fun clock(minutes: Int): String = "%d:%02d".format(minutes / 60, minutes % 60)

    /** Romanian counting: "12 lei" but "45 de lei". */
    private fun de(n: Int): String = if (n < 20) "$n" else "$n de"

    companion object {
        const val ARC_COUNT = 5
    }
}
