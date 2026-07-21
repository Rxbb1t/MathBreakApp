package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * Thirty classic puzzle shapes, the kind that have an "oh!" in them
 * rather than just arithmetic: the snail in the well, the log that needs
 * one cut fewer than it has pieces, the lily that was half the pond
 * yesterday.
 *
 * The mathematics here is old and belongs to nobody -- handshake
 * counting, Gauss's pairing, work rates, inclusion and exclusion. What is
 * written here is written for this app: the wording, the settings and the
 * explanations are the app's own, in its own voice.
 *
 * Every one of them is PARAMETERISED rather than fixed. A showpiece that
 * arrived with the same numbers each time would be memorised in a week
 * and would stop being a puzzle; this way the shape is familiar and the
 * work is real. The numbers are chosen so the answer is always a
 * non-negative whole number, by construction and not by rounding.
 *
 * These are dealt at HARD only, as a fraction of the logic riddles, so
 * they stay a treat rather than the norm. [ProblemGenerator]'s repeat
 * rings space them out: the shape ring blanks digits, so the same
 * showpiece won't come round twice in five problems.
 */
class ShowpieceGenerator(private val random: Random) {

    fun generate(language: AppLanguage): Problem = when (random.nextInt(30)) {
        0 -> handshakes(language)
        1 -> gaussSum(language)
        2 -> logCuts(language)
        3 -> fencePosts(language)
        4 -> clockStrikes(language)
        5 -> snailWell(language)
        6 -> twoTaps(language)
        7 -> walkersMeet(language)
        8 -> twiceAsOldIn(language)
        9 -> balanceScale(language)
        10 -> sharingRemainder(language)
        11 -> averageJoins(language)
        12 -> missingBasket(language)
        13 -> coinMix(language)
        14 -> bulkSaving(language)
        15 -> twoDiscounts(language)
        16 -> waterRises(language)
        17 -> clockAngle(language)
        18 -> candleBurn(language)
        19 -> doublingLily(language)
        20 -> borderSlabs(language)
        21 -> pageDigits(language)
        22 -> teaAndCoffee(language)
        23 -> ropeFolds(language)
        24 -> billSplit(language)
        25 -> backwardsShopping(language)
        26 -> savingUp(language)
        27 -> leakyBarrel(language)
        28 -> postcards(language)
        29 -> treePlanting(language)
        else -> handshakes(language)
    }

    // ── Plumbing ─────────────────────────────────────────────────────────

    /** Every phrase is authored as `english to romanian`. */
    private fun Pair<String, String>.pick(language: AppLanguage): String =
        if (language == AppLanguage.ROMANIAN) second else first

    /**
     * Romanian counts "12 mere" but "45 de mere": from twenty up, the
     * noun needs "de" in front of it. Any Romanian phrase here that puts
     * a noun straight after a number has to go through this, and
     * `RomanianCountingTest` checks that they all do.
     *
     * Written-out units are the exception ("30 m", "12 cm"), which is why
     * that test carries an allowlist of abbreviations.
     */
    private fun de(n: Int): String = if (n < 20) "$n" else "$n de"

    private fun piece(
        language: AppLanguage,
        text: Pair<String, String>,
        answer: Int,
        hint: Pair<String, String>,
        note: Pair<String, String>,
        solution: List<Pair<String, String>>,
        unit: String = "",
    ): Problem = Problem(
        text = text.pick(language),
        answer = answer,
        difficulty = Difficulty.HARD,
        kind = ProblemKind.LOGIC,
        hints = listOf(hint.pick(language), HintText.digits(answer, language)),
        notes = listOf(note.pick(language)),
        solution = solution.map { it.pick(language) },
        answerUnit = unit,
    )

    // ── The thirty ───────────────────────────────────────────────────────

    /** Everyone shakes everyone's hand once: n(n−1)/2, halved for sharing. */
    private fun handshakes(language: AppLanguage): Problem {
        val n = random.nextInt(5, 11)
        val pairs = n * (n - 1) / 2
        return piece(
            language,
            text = "There are $n people at a small gathering, and everyone shakes hands with everyone else exactly once.\nHow many handshakes is that?" to
                "La o adunare mică sunt $n persoane și fiecare dă mâna cu fiecare exact o dată.\nCâte strângeri de mână sunt?",
            answer = pairs,
            hint = "Each person shakes ${n - 1} hands. But a handshake needs two people." to
                "Fiecare dă mâna de ${n - 1} ori. Dar o strângere de mână cere doi oameni.",
            note = "Each of n people meets the other n − 1: that is n × (n − 1). Every handshake got counted from both sides, so halve it." to
                "Fiecare dintre cei n întâlnește ceilalți n − 1: adică n × (n − 1). Fiecare strângere a fost numărată din ambele părți, deci se împarte la 2.",
            solution = listOf(
                "Each of the $n shakes ${n - 1} hands: $n × ${n - 1} = ${n * (n - 1)}" to
                    "Fiecare dintre cei $n dă mâna de ${n - 1} ori: $n × ${n - 1} = ${n * (n - 1)}",
                "That counts every handshake twice, so halve it: ${n * (n - 1)} ÷ 2 = $pairs" to
                    "Asta numără fiecare strângere de două ori, deci împarte la 2: ${n * (n - 1)} ÷ 2 = $pairs",
            ),
        )
    }

    /** 1 + 2 + … + n, by pairing the ends. */
    private fun gaussSum(language: AppLanguage): Problem {
        val n = listOf(10, 12, 16, 20, 24, 30, 40, 50).random(random)
        val total = n * (n + 1) / 2
        return piece(
            language,
            text = "A staircase has $n steps. You count 1 on the first, 2 on the second, and so on up to $n on the last.\nWhat do all the counts add up to?" to
                "O scară are ${de(n)} trepte. Numeri 1 pe prima, 2 pe a doua, și tot așa până la $n pe ultima.\nCât fac toate la un loc?",
            answer = total,
            hint = "Pair the first with the last, the second with the second-last. Every pair makes the same total." to
                "Împerechează prima cu ultima, a doua cu penultima. Fiecare pereche dă același total.",
            note = "1 + 2 + … + n = n × (n + 1) ÷ 2. The pairs all make n + 1, and there are half as many pairs as numbers." to
                "1 + 2 + … + n = n × (n + 1) ÷ 2. Perechile fac toate n + 1, iar perechile sunt pe jumătate cât numerele.",
            solution = listOf(
                "Pair them off: 1 + $n = ${n + 1}, 2 + ${n - 1} = ${n + 1}, and so on." to
                    "Fă perechi: 1 + $n = ${n + 1}, 2 + ${n - 1} = ${n + 1}, și tot așa.",
                "There are $n ÷ 2 = ${n / 2} pairs, each worth ${n + 1}: ${n / 2} × ${n + 1} = $total" to
                    "Sunt $n ÷ 2 = ${de(n / 2)} perechi, fiecare de ${n + 1}: ${n / 2} × ${n + 1} = $total",
            ),
        )
    }

    /** Pieces need one cut fewer than there are pieces. */
    private fun logCuts(language: AppLanguage): Problem {
        val pieces = random.nextInt(4, 10)
        val minutes = random.nextInt(2, 7)
        return piece(
            language,
            text = "A log is sawn into $pieces pieces. Every cut takes $minutes minutes.\nHow many minutes of sawing is that?" to
                "Un buștean e tăiat în $pieces bucăți. Fiecare tăietură durează $minutes minute.\nCâte minute de tăiat sunt în total?",
            answer = (pieces - 1) * minutes,
            hint = "Try it with a short log: two pieces need only one cut." to
                "Încearcă pe un buștean scurt: două bucăți cer o singură tăietură.",
            note = "Cutting into p pieces takes p − 1 cuts. The last piece falls off with the cut before it, and needs none of its own." to
                "Ca să tai în p bucăți, faci p − 1 tăieturi. Ultima bucată cade la tăietura dinainte și nu mai cere una a ei.",
            solution = listOf(
                "To get $pieces pieces you make ${pieces - 1} cuts, not $pieces." to
                    "Ca să obții $pieces bucăți faci ${pieces - 1} tăieturi, nu $pieces.",
                "${pieces - 1} × $minutes = ${(pieces - 1) * minutes}" to
                    "${pieces - 1} × $minutes = ${(pieces - 1) * minutes}",
            ),
            unit = "min",
        )
    }

    /** Posts along a straight run: gaps plus one. */
    private fun fencePosts(language: AppLanguage): Problem {
        val spacing = listOf(2, 3, 4, 5).random(random)
        val gaps = random.nextInt(6, 16)
        val length = spacing * gaps
        return piece(
            language,
            text = "A straight fence is $length m long, with a post every $spacing m and one at each end.\nHow many posts are there?" to
                "Un gard drept are $length m, cu câte un stâlp la fiecare $spacing m și câte unul la fiecare capăt.\nCâți stâlpi sunt?",
            answer = gaps + 1,
            hint = "Count the gaps first, then remember the post at the far end." to
                "Numără întâi intervalele, apoi nu uita stâlpul de la capăt.",
            note = "Along a straight line, posts = gaps + 1. Around a closed loop they would be equal." to
                "Pe o linie dreaptă, stâlpi = intervale + 1. Pe un contur închis ar fi la fel de mulți.",
            solution = listOf(
                "$length ÷ $spacing = $gaps gaps between posts." to
                    "$length ÷ $spacing = $gaps intervale între stâlpi.",
                "A straight run has one post more than it has gaps: $gaps + 1 = ${gaps + 1}" to
                    "Un gard drept are cu un stâlp mai mult decât intervale: $gaps + 1 = ${gaps + 1}",
            ),
        )
    }

    /** The time is in the gaps between strikes, not in the strikes. */
    private fun clockStrikes(language: AppLanguage): Problem {
        val perGap = random.nextInt(1, 4)
        val first = random.nextInt(4, 8)
        val second = random.nextInt(9, 13)
        return piece(
            language,
            text = "A clock takes ${perGap * (first - 1)} seconds to strike $first o'clock.\nHow many seconds does it take to strike $second o'clock?" to
                "Un ceas bate ora $first în ${perGap * (first - 1)} secunde.\nÎn câte secunde bate ora $second?",
            answer = perGap * (second - 1),
            hint = "The seconds pass between the strikes, not during them." to
                "Secundele trec între bătăi, nu în timpul lor.",
            note = "n strikes have n − 1 gaps between them. Divide the time by the gaps, never by the strikes." to
                "n bătăi au n − 1 intervale între ele. Împarte timpul la intervale, niciodată la bătăi.",
            solution = listOf(
                "$first strikes have ${first - 1} gaps: ${perGap * (first - 1)} ÷ ${first - 1} = $perGap seconds each." to
                    "$first bătăi au ${first - 1} intervale: ${perGap * (first - 1)} ÷ ${first - 1} = $perGap secunde fiecare.",
                "$second strikes have ${second - 1} gaps: ${second - 1} × $perGap = ${perGap * (second - 1)}" to
                    "$second bătăi au ${second - 1} intervale: ${second - 1} × $perGap = ${perGap * (second - 1)}",
            ),
            unit = "s",
        )
    }

    /** The last climb doesn't slip back. */
    private fun snailWell(language: AppLanguage): Problem {
        val climb = random.nextInt(3, 6)
        val slip = random.nextInt(1, climb)
        val days = random.nextInt(5, 10)
        val depth = (climb - slip) * (days - 1) + climb
        return piece(
            language,
            text = "A snail sits at the bottom of a $depth m well. Each day it climbs $climb m, and each night it slips back $slip m.\nOn which day does it get out?" to
                "Un melc e pe fundul unei fântâni de $depth m. Ziua urcă $climb m, iar noaptea alunecă înapoi $slip m.\nÎn a câta zi iese?",
            answer = days,
            hint = "Once it reaches the top it is out, and there is no night to slip back down." to
                "Când ajunge sus a ieșit, și nu mai vine nicio noapte în care să alunece.",
            note = "A full day and night gains climb − slip. The last day is different: the snail leaves before the slipping, so take one climb off the depth first." to
                "O zi cu noaptea ei aduce urcare − alunecare. Ultima zi e altfel: melcul pleacă înainte de alunecare, deci scade întâi o urcare din adâncime.",
            solution = listOf(
                "Day and night together gain $climb − $slip = ${climb - slip} m." to
                    "Ziua cu noaptea aduc $climb − $slip = ${climb - slip} m.",
                "It only needs to reach $depth − $climb = ${depth - climb} m, because the last climb takes it out: ${depth - climb} ÷ ${climb - slip} = ${days - 1} days." to
                    "Îi ajunge să ajungă la $depth − $climb = ${depth - climb} m, fiindcă ultima urcare îl scoate: ${depth - climb} ÷ ${climb - slip} = ${days - 1} zile.",
                "Then one more day to climb clear: ${days - 1} + 1 = $days" to
                    "Apoi încă o zi ca să iasă: ${days - 1} + 1 = $days",
            ),
        )
    }

    /** Working together, rates add. */
    private fun twoTaps(language: AppLanguage): Problem {
        val first = random.nextInt(2, 6)
        val second = random.nextInt(2, 6)
        val minutes = random.nextInt(4, 10)
        val buckets = (first + second) * minutes
        return piece(
            language,
            text = "One tap fills $first buckets a minute, another fills $second. Together they fill a tub that holds $buckets buckets.\nHow many minutes does it take?" to
                "Un robinet umple $first găleți pe minut, altul umple $second. Împreună umplu o cadă de ${de(buckets)} găleți.\nÎn câte minute?",
            answer = minutes,
            hint = "Work out what the two of them do in a single minute." to
                "Află ce fac cei doi împreună într-un singur minut.",
            note = "Two things working at once add their rates. Never average them." to
                "Două lucruri care merg deodată își adună ritmurile. Nu se face media.",
            solution = listOf(
                "In one minute together: $first + $second = ${first + second} buckets." to
                    "Într-un minut împreună: $first + $second = ${first + second} găleți.",
                "$buckets ÷ ${first + second} = $minutes" to
                    "$buckets ÷ ${first + second} = $minutes",
            ),
            unit = "min",
        )
    }

    /** Two walkers close the gap at the sum of their speeds. */
    private fun walkersMeet(language: AppLanguage): Problem {
        val first = random.nextInt(40, 91)
        val second = random.nextInt(40, 91)
        val minutes = random.nextInt(3, 10)
        val apart = (first + second) * minutes
        return piece(
            language,
            text = "Two friends set off toward each other from $apart m apart. One walks $first m a minute, the other $second m a minute.\nAfter how many minutes do they meet?" to
                "Doi prieteni pornesc unul spre altul de la $apart m distanță. Unul merge $first m pe minut, celălalt $second m pe minut.\nDupă câte minute se întâlnesc?",
            answer = minutes,
            hint = "Ask how much shorter the gap gets each minute, not how far each one walks." to
                "Întreabă cu cât se scurtează distanța în fiecare minut, nu cât merge fiecare.",
            note = "Walking toward each other, the gap closes at both speeds added together. Time = distance ÷ closing speed." to
                "Când merg unul spre altul, distanța scade cu suma vitezelor. Timp = distanță ÷ viteza de apropiere.",
            solution = listOf(
                "Each minute the gap shrinks by $first + $second = ${first + second} m." to
                    "În fiecare minut distanța scade cu $first + $second = ${first + second} m.",
                "$apart ÷ ${first + second} = $minutes" to
                    "$apart ÷ ${first + second} = $minutes",
            ),
            unit = "min",
        )
    }

    /** The gap between two ages never changes. */
    private fun twiceAsOldIn(language: AppLanguage): Problem {
        val child = random.nextInt(8, 17)
        val years = random.nextInt(2, 13)
        val parent = 2 * child + years
        return piece(
            language,
            text = "A mother is $parent and her child is $child.\nIn how many years will she be exactly twice the child's age?" to
                "O mamă are ${de(parent)} ani, iar copilul ei are $child.\nPeste câți ani va fi exact de două ori mai în vârstă decât copilul?",
            answer = years,
            hint = "The difference between their ages is the same today as it will ever be." to
                "Diferența dintre vârstele lor e aceeași azi și oricând altcândva.",
            note = "When one age is twice another, the difference between them equals the younger age. The difference never moves, so it is the younger one who has to grow into it." to
                "Când o vârstă e dublul alteia, diferența dintre ele e chiar vârsta celui mic. Diferența nu se schimbă, deci cel mic e cel care trebuie să crească până la ea.",
            solution = listOf(
                "The gap is $parent − $child = ${parent - child} years, and it stays that way forever." to
                    "Diferența e $parent − $child = ${de(parent - child)} ani și rămâne așa mereu.",
                "She is twice as old exactly when the child turns ${parent - child}." to
                    "Va fi de două ori mai mare exact când copilul face ${de(parent - child)} ani.",
                "That happens in ${parent - child} − $child = $years years." to
                    "Asta se întâmplă peste ${parent - child} − $child = $years ani.",
            ),
        )
    }

    /** Trade one side of a balance for the other. */
    private fun balanceScale(language: AppLanguage): Problem {
        val apple = random.nextInt(20, 61)
        val pear = apple * 3 / 2
        val sixPears = 6 * pear
        return piece(
            language,
            text = "Three apples balance two pears exactly. Six pears weigh $sixPears grams.\nHow many grams does one apple weigh?" to
                "Trei mere cântăresc exact cât două pere. Șase pere cântăresc ${de(sixPears)} grame.\nCâte grame are un măr?",
            answer = apple,
            hint = "Find what one pear weighs before you touch the apples." to
                "Află cât cântărește o pară înainte să te atingi de mere.",
            note = "A balance is an equals sign. Work out one item, then trade it for the other through whatever the balance says." to
                "O balanță e un semn de egal. Află cât face un obiect, apoi schimbă-l pe celălalt prin ce spune balanța.",
            solution = listOf(
                "One pear: $sixPears ÷ 6 = $pear grams." to
                    "O pară: $sixPears ÷ 6 = ${de(pear)} grame.",
                "Two pears weigh ${2 * pear} grams, and that is what three apples weigh." to
                    "Două pere fac ${de(2 * pear)} grame, adică exact cât trei mere.",
                "One apple: ${2 * pear} ÷ 3 = $apple" to
                    "Un măr: ${2 * pear} ÷ 3 = $apple",
            ),
            unit = "g",
        )
    }

    /** Divide, and let the remainder be a remainder. */
    private fun sharingRemainder(language: AppLanguage): Problem {
        val children = random.nextInt(3, 8)
        val each = random.nextInt(4, 13)
        val left = random.nextInt(1, children)
        val total = children * each + left
        return piece(
            language,
            text = "You share $total biscuits among $children grandchildren, as evenly as they will go.\nHow many does each one get?" to
                "Împarți ${de(total)} biscuiți la $children nepoți, cât se poate de egal.\nCâți primește fiecare?",
            answer = each,
            hint = "Share out as many whole rounds as you can; a few will be left in the tin." to
                "Împarte câte runde întregi poți; câțiva rămân în cutie.",
            note = "Sharing n among k gives each the whole part of n ÷ k. What is left over cannot be shared without breaking one." to
                "Împărțind n la k, fiecare primește partea întreagă din n ÷ k. Ce rămâne nu se poate împărți fără să rupi ceva.",
            solution = listOf(
                "$children × $each = ${children * each}, which is as close to $total as whole rounds get." to
                    "$children × $each = ${children * each}, cât mai aproape de $total cu runde întregi.",
                "$left are left in the tin, and each grandchild gets $each" to
                    "$left rămân în cutie, iar fiecare nepot primește $each",
            ),
        )
    }

    /** Rebuild the total, then divide by the new count. */
    private fun averageJoins(language: AppLanguage): Problem {
        val count = random.nextInt(3, 7)
        val average = random.nextInt(8, 21)
        val joiner = average + count + 1
        val total = count * average
        return piece(
            language,
            text = "The average of $count numbers is $average. One more number joins them: $joiner.\nWhat is the average now?" to
                "Media a $count numere este $average. Li se alătură încă un număr: $joiner.\nCare e media acum?",
            answer = average + 1,
            hint = "An average hides a total. Get the total back first." to
                "O medie ascunde un total. Scoate întâi totalul la iveală.",
            note = "Average = total ÷ count. To add a number, rebuild the total, add it, then divide by the new count." to
                "Media = total ÷ câte sunt. Ca să adaugi un număr, reconstruiești totalul, îl aduni, apoi împarți la noul număr.",
            solution = listOf(
                "The $count numbers total $count × $average = $total." to
                    "Cele $count numere fac în total $count × $average = $total.",
                "With the new one: $total + $joiner = ${total + joiner}." to
                    "Cu cel nou: $total + $joiner = ${total + joiner}.",
                "There are ${count + 1} numbers now: ${total + joiner} ÷ ${count + 1} = ${average + 1}" to
                    "Acum sunt ${count + 1} numere: ${total + joiner} ÷ ${count + 1} = ${average + 1}",
            ),
        )
    }

    /** The total the average demands, minus what is already there. */
    private fun missingBasket(language: AppLanguage): Problem {
        val average = random.nextInt(10, 26)
        val first = random.nextInt(5, average + 6)
        val second = random.nextInt(5, average + 6)
        val third = random.nextInt(5, average + 6)
        val needed = 4 * average - (first + second + third)
        return piece(
            language,
            text = "Four baskets should hold $average plums each on average. The first three hold $first, $second and $third.\nHow many must the fourth hold?" to
                "Patru coșuri ar trebui să aibă în medie ${de(average)} prune fiecare. Primele trei au $first, $second și $third.\nCâte trebuie să aibă al patrulea?",
            answer = needed,
            hint = "Work out how many plums four baskets need altogether." to
                "Află câte prune le trebuie celor patru coșuri la un loc.",
            note = "If the average of n numbers is a, their total must be n × a. Averages are easiest to work with as totals." to
                "Dacă media a n numere e a, totalul lor e n × a. Cu medii e mai ușor de lucrat ca totaluri.",
            solution = listOf(
                "Four baskets averaging $average need 4 × $average = ${4 * average} plums in all." to
                    "Patru coșuri cu media $average au nevoie de 4 × $average = ${de(4 * average)} prune în total.",
                "The first three hold $first + $second + $third = ${first + second + third}." to
                    "Primele trei au $first + $second + $third = ${first + second + third}.",
                "${4 * average} − ${first + second + third} = $needed" to
                    "${4 * average} − ${first + second + third} = $needed",
            ),
        )
    }

    /** Assume the cheaper coin everywhere, then explain the shortfall. */
    private fun coinMix(language: AppLanguage): Problem {
        val fives = random.nextInt(2, 7)
        val twos = random.nextInt(2, 9)
        val coins = fives + twos
        val total = 5 * fives + 2 * twos
        return piece(
            language,
            text = "You have $coins coins in your purse, some worth 2 euros and some worth 5, $total euros in all.\nHow many of them are 5-euro coins?" to
                "Ai $coins monede în portofel, unele de 2 euro și altele de 5, în total ${de(total)} euro.\nCâte sunt de 5 euro?",
            answer = fives,
            hint = "Pretend for a moment that every coin is a 2-euro one." to
                "Prefă-te o clipă că toate monedele sunt de 2 euro.",
            note = "The same trick as heads and legs: assume the smaller value everywhere, see how much is missing, then see how much each swap adds back." to
                "Același truc ca la capete și picioare: presupui peste tot valoarea mică, vezi cât lipsește, apoi cât adaugă fiecare schimb.",
            solution = listOf(
                "If all $coins were 2-euro coins: $coins × 2 = ${2 * coins} euros." to
                    "Dacă toate cele $coins ar fi de 2 euro: $coins × 2 = ${de(2 * coins)} euro.",
                "That is $total − ${2 * coins} = ${total - 2 * coins} euros short." to
                    "Asta e cu $total − ${2 * coins} = ${de(total - 2 * coins)} euro mai puțin.",
                "Swapping a 2 for a 5 adds 3 euros, so ${total - 2 * coins} ÷ 3 = $fives" to
                    "Schimbând un 2 cu un 5 adaugi 3 euro, deci ${total - 2 * coins} ÷ 3 = $fives",
            ),
        )
    }

    /** Bring two prices to the same number of items before comparing. */
    private fun bulkSaving(language: AppLanguage): Problem {
        val perPack = random.nextInt(4, 7)
        val loose = random.nextInt(3, 7)
        val savedPerPack = random.nextInt(2, 6)
        val packPrice = perPack * loose - savedPerPack
        val packs = random.nextInt(2, 5)
        val bars = perPack * packs
        return piece(
            language,
            text = "A pack of $perPack bars costs $packPrice euros. Loose bars cost $loose euros each.\nHow many euros do you save buying $bars bars in packs?" to
                "Un pachet de $perPack batoane costă ${de(packPrice)} euro. Batoanele la bucată costă $loose euro fiecare.\nCâți euro economisești cumpărând ${de(bars)} batoane la pachet?",
            answer = packs * savedPerPack,
            hint = "Price up both ways of buying the same $bars bars, then compare." to
                "Calculează ambele feluri de a cumpăra aceleași ${de(bars)} batoane, apoi compară.",
            note = "Two prices can only be compared once they buy the same amount. Work out both totals, then take one from the other." to
                "Două prețuri se pot compara doar când cumpără aceeași cantitate. Calculează ambele totaluri, apoi scade-l pe unul din celălalt.",
            solution = listOf(
                "Loose: $bars × $loose = ${bars * loose} euros." to
                    "La bucată: $bars × $loose = ${de(bars * loose)} euro.",
                "In packs: $packs × $packPrice = ${packs * packPrice} euros." to
                    "La pachet: $packs × $packPrice = ${de(packs * packPrice)} euro.",
                "${bars * loose} − ${packs * packPrice} = ${packs * savedPerPack}" to
                    "${bars * loose} − ${packs * packPrice} = ${packs * savedPerPack}",
            ),
            unit = "€",
        )
    }

    /** The second cut comes off the new price, not the old one. */
    private fun twoDiscounts(language: AppLanguage): Problem {
        val price = 100 * random.nextInt(2, 8)
        val afterFirst = price - price / 10
        val afterSecond = afterFirst - afterFirst / 10
        return piece(
            language,
            text = "A coat costs $price euros. In the sale it drops by a tenth. The week after, the new price drops by another tenth.\nHow many euros is it now?" to
                "Un palton costă ${de(price)} euro. La reduceri scade cu o zecime. Săptămâna următoare, noul preț scade cu încă o zecime.\nCât costă acum?",
            answer = afterSecond,
            hint = "The second tenth is a tenth of the reduced price, not of the first one." to
                "A doua zecime e o zecime din prețul redus, nu din cel dintâi.",
            note = "Two cuts of a tenth do not make a fifth. The second one is taken from a smaller price, so it takes off less." to
                "Două reduceri de o zecime nu fac o cincime. A doua se ia dintr-un preț mai mic, deci scade mai puțin.",
            solution = listOf(
                "A tenth of $price is ${price / 10}, so the price becomes $price − ${price / 10} = $afterFirst." to
                    "O zecime din $price e ${price / 10}, deci prețul devine $price − ${price / 10} = $afterFirst.",
                "A tenth of $afterFirst is ${afterFirst / 10}, so it becomes $afterFirst − ${afterFirst / 10} = $afterSecond" to
                    "O zecime din $afterFirst e ${afterFirst / 10}, deci devine $afterFirst − ${afterFirst / 10} = $afterSecond",
            ),
            unit = "€",
        )
    }

    /** What you sink pushes up its own volume. */
    private fun waterRises(language: AppLanguage): Problem {
        val base = listOf(20, 40).random(random)
        val stones = listOf(2, 4, 5).random(random)
        val rise = random.nextInt(2, 6)
        val each = base * rise / stones
        return piece(
            language,
            text = "A tank's base is $base square centimeters. You lower $stones stones into the water, each taking up $each cubic centimeters.\nBy how many centimeters does the water rise?" to
                "Baza unui vas are ${de(base)} centimetri pătrați. Cobori în apă $stones pietre, fiecare ocupând ${de(each)} centimetri cubi.\nCu câți centimetri urcă apa?",
            answer = rise,
            hint = "The stones don't add water. They add the room the water has to go somewhere else." to
                "Pietrele nu adaugă apă. Adaugă locul din care apa trebuie să plece în altă parte.",
            note = "Volume = base area × height. Sinking something displaces exactly its own volume, so the rise is that volume spread over the base." to
                "Volum = aria bazei × înălțime. Ce scufunzi împinge exact volumul lui, deci creșterea e acel volum întins peste bază.",
            solution = listOf(
                "The stones take up $stones × $each = ${stones * each} cubic centimeters." to
                    "Pietrele ocupă $stones × $each = ${de(stones * each)} centimetri cubi.",
                "Spread over the base: ${stones * each} ÷ $base = $rise" to
                    "Întins peste bază: ${stones * each} ÷ $base = $rise",
            ),
            unit = "cm",
        )
    }

    /** Twelve hour marks divide the full turn. */
    private fun clockAngle(language: AppLanguage): Problem {
        val hour = random.nextInt(2, 6)
        return piece(
            language,
            text = "How many degrees is the angle between the hands of a clock at exactly $hour o'clock?" to
                "Câte grade are unghiul dintre acele ceasului la fix ora $hour?",
            answer = 30 * hour,
            hint = "A full turn shared between twelve hour marks." to
                "O rotație completă împărțită la douăsprezece repere.",
            note = "A full turn is 360°, and the clock face divides it into 12, so each hour mark is 30° from the next." to
                "O rotație completă are 360°, iar cadranul o împarte în 12, deci fiecare reper e la 30° de următorul.",
            solution = listOf(
                "Each hour mark is 360 ÷ 12 = 30 degrees from the next." to
                    "Fiecare reper e la 360 ÷ 12 = 30 de grade de următorul.",
                "At $hour o'clock the hands stand $hour marks apart: $hour × 30 = ${30 * hour}" to
                    "La ora $hour acele sunt la $hour repere distanță: $hour × 30 = ${30 * hour}",
            ),
            unit = "°",
        )
    }

    /** Find the amount that changes, then divide by the rate. */
    private fun candleBurn(language: AppLanguage): Problem {
        val rate = random.nextInt(2, 5)
        val hours = random.nextInt(3, 9)
        val leftOver = random.nextInt(1, 6)
        val height = leftOver + rate * hours
        return piece(
            language,
            text = "A candle is $height cm tall and burns $rate cm every hour.\nAfter how many hours is $leftOver cm of it left?" to
                "O lumânare are $height cm și arde $rate cm pe oră.\nDupă câte ore mai rămân $leftOver cm din ea?",
            answer = hours,
            hint = "Ask how much wax has to disappear, not how tall the candle is." to
                "Întreabă câtă ceară trebuie să dispară, nu cât e de înaltă lumânarea.",
            note = "Work out the amount that actually changes, then divide it by the rate of change." to
                "Află cât se schimbă de fapt, apoi împarte la ritmul schimbării.",
            solution = listOf(
                "It has to burn away $height − $leftOver = ${height - leftOver} cm." to
                    "Trebuie să ardă $height − $leftOver = ${height - leftOver} cm.",
                "${height - leftOver} ÷ $rate = $hours" to
                    "${height - leftOver} ÷ $rate = $hours",
            ),
            unit = "h",
        )
    }

    /** Doubling forwards is halving backwards. */
    private fun doublingLily(language: AppLanguage): Problem {
        val day = random.nextInt(12, 41)
        return piece(
            language,
            text = "A water lily doubles in size every day, and covers the whole pond on day $day.\nOn which day did it cover half the pond?" to
                "Un nufăr își dublează mărimea în fiecare zi și acoperă tot iazul în ziua $day.\nÎn ce zi acoperea jumătate din iaz?",
            answer = day - 1,
            hint = "Not halfway through. Step back a single day." to
                "Nu la jumătatea timpului. Dă înapoi o singură zi.",
            note = "Doubling forwards means halving backwards. Whatever is full today was half of it yesterday, however long it took to get there." to
                "Dublarea înainte înseamnă înjumătățire înapoi. Ce e plin azi era pe jumătate ieri, oricât ar fi durat până acolo.",
            solution = listOf(
                "The day before it was full, it was half as big." to
                    "Cu o zi înainte să fie plin, era pe jumătate.",
                "$day − 1 = ${day - 1}" to
                    "$day − 1 = ${day - 1}",
            ),
        )
    }

    /** Four sides count each corner twice. */
    private fun borderSlabs(language: AppLanguage): Problem {
        val side = random.nextInt(5, 16)
        return piece(
            language,
            text = "A square patio is $side slabs by $side slabs.\nHow many slabs run along its edge?" to
                "O terasă pătrată are $side dale pe $side dale.\nCâte dale sunt pe margine?",
            answer = 4 * side - 4,
            hint = "A corner slab sits on two sides at once." to
                "O dală din colț stă pe două laturi deodată.",
            note = "Four sides of n would be 4 × n, but each of the four corners belongs to two sides and gets counted twice: 4 × n − 4." to
                "Patru laturi de n ar face 4 × n, dar fiecare dintre cele patru colțuri ține de două laturi și e numărat de două ori: 4 × n − 4.",
            solution = listOf(
                "Four sides of $side slabs: 4 × $side = ${4 * side}." to
                    "Patru laturi de $side dale: 4 × $side = ${4 * side}.",
                "The 4 corners were each counted twice, so take them off once: ${4 * side} − 4 = ${4 * side - 4}" to
                    "Cele 4 colțuri au fost numărate de două ori, deci scade-le o dată: ${4 * side} − 4 = ${4 * side - 4}",
            ),
        )
    }

    /** Count by how long the numbers are, not how many there are. */
    private fun pageDigits(language: AppLanguage): Problem {
        val pages = random.nextInt(20, 100)
        val digits = 9 + 2 * (pages - 9)
        return piece(
            language,
            text = "A booklet is numbered from page 1 to page $pages.\nHow many digits are printed in all?" to
                "O broșură e numerotată de la pagina 1 la pagina $pages.\nCâte cifre sunt tipărite în total?",
            answer = digits,
            hint = "Pages 1 to 9 take one digit each. Everything after takes two." to
                "Paginile de la 1 la 9 iau câte o cifră. Toate celelalte iau câte două.",
            note = "Count the digits, not the pages. Split the run where the numbers get longer." to
                "Numeri cifrele, nu paginile. Împarte șirul acolo unde numerele devin mai lungi.",
            solution = listOf(
                "Pages 1 to 9: 9 pages with one digit each, so 9 digits." to
                    "Paginile 1–9: 9 pagini cu câte o cifră, deci 9 cifre.",
                "Pages 10 to $pages: ${pages - 9} pages with two digits each, so ${2 * (pages - 9)} digits." to
                    "Paginile 10–$pages: ${de(pages - 9)} pagini cu câte două cifre, deci ${de(2 * (pages - 9))} cifre.",
                "9 + ${2 * (pages - 9)} = $digits" to
                    "9 + ${2 * (pages - 9)} = $digits",
            ),
        )
    }

    /** Two overlapping groups count the overlap twice. */
    private fun teaAndCoffee(language: AppLanguage): Problem {
        val both = random.nextInt(3, 9)
        val tea = both + random.nextInt(4, 12)
        val coffee = both + random.nextInt(4, 12)
        val neither = random.nextInt(2, 8)
        val people = tea + coffee - both + neither
        return piece(
            language,
            text = "At a gathering of $people people, $tea drink tea, $coffee drink coffee, and $both drink both.\nHow many drink neither?" to
                "La o adunare de ${de(people)} persoane, $tea beau ceai, $coffee beau cafea, iar $both beau și una și alta.\nCâte nu beau niciuna?",
            answer = neither,
            hint = "Adding the tea drinkers to the coffee drinkers counts some people twice." to
                "Adunând băutorii de ceai cu cei de cafea, unii sunt numărați de două ori.",
            note = "For two overlapping groups: in one or the other = first + second − both. Subtract the overlap once, because adding counted it twice." to
                "Pentru două grupuri care se suprapun: într-unul sau în celălalt = primul + al doilea − ambele. Scazi suprapunerea o dată, fiindcă adunarea a numărat-o de două ori.",
            solution = listOf(
                "Tea or coffee: $tea + $coffee − $both = ${tea + coffee - both} people." to
                    "Ceai sau cafea: $tea + $coffee − $both = ${de(tea + coffee - both)} persoane.",
                "$people − ${tea + coffee - both} = $neither" to
                    "$people − ${tea + coffee - both} = $neither",
            ),
        )
    }

    /** Each fold doubles the layers; one cut through L layers gives L + 1. */
    private fun ropeFolds(language: AppLanguage): Problem {
        val folds = random.nextInt(2, 6)
        var layers = 1
        repeat(folds) { layers *= 2 }
        return piece(
            language,
            text = "A rope is folded in half $folds times, then cut straight through, once.\nHow many pieces of rope are there?" to
                "O frânghie e împăturită în două de $folds ori, apoi tăiată drept, o singură dată.\nCâte bucăți de frânghie sunt?",
            answer = layers + 1,
            hint = "Count the layers the scissors pass through, then think about the two loose ends." to
                "Numără straturile prin care trec foarfecele, apoi gândește-te la cele două capete libere.",
            note = "Every fold doubles the layers. One cut through L layers makes L + 1 pieces, the same one-more rule as cutting a log." to
                "Fiecare împăturire dublează straturile. O tăietură prin L straturi face L + 1 bucăți, aceeași regulă ca la buștean.",
            solution = listOf(
                "$folds folds double the rope each time: $layers layers." to
                    "$folds împăturiri dublează frânghia de fiecare dată: ${de(layers)} straturi.",
                "One cut through $layers layers gives $layers + 1 = ${layers + 1}" to
                    "O tăietură prin ${de(layers)} straturi dă $layers + 1 = ${layers + 1}",
            ),
        )
    }

    /** Divide by the number who actually pay. */
    private fun billSplit(language: AppLanguage): Problem {
        val friends = random.nextInt(5, 10)
        val each = random.nextInt(6, 21)
        val total = (friends - 2) * each
        return piece(
            language,
            text = "A bill of $total euros was going to be split evenly between $friends friends, but 2 of them left their purses at home.\nHow many euros does each of the rest pay?" to
                "O notă de plată de ${de(total)} euro urma să fie împărțită egal între $friends prieteni, dar 2 dintre ei și-au uitat portofelele acasă.\nCâți euro plătește fiecare dintre ceilalți?",
            answer = each,
            hint = "Divide by the number who are actually paying." to
                "Împarte la câți plătesc de fapt.",
            note = "The number at the table and the number paying are two different numbers. Only one of them belongs under the line." to
                "Câți sunt la masă și câți plătesc sunt două numere diferite. Doar unul dintre ele stă sub linia de împărțire.",
            solution = listOf(
                "Only $friends − 2 = ${friends - 2} of them are paying." to
                    "Doar $friends − 2 = ${friends - 2} dintre ei plătesc.",
                "$total ÷ ${friends - 2} = $each" to
                    "$total ÷ ${friends - 2} = $each",
            ),
            unit = "€",
        )
    }

    /** Undo the story from the end. */
    private fun backwardsShopping(language: AppLanguage): Problem {
        val bakery = random.nextInt(3, 16)
        val left = random.nextInt(4, 26)
        val half = bakery + left
        return piece(
            language,
            text = "You spend half your money at the market, then $bakery euros at the bakery, and $left euros are left.\nHow many euros did you set out with?" to
                "Cheltui jumătate din bani la piață, apoi $bakery euro la brutărie, și îți rămân ${de(left)} euro.\nCu câți euro ai plecat de acasă?",
            answer = 2 * half,
            hint = "Start at the end and put the money back, one step at a time." to
                "Pornește de la final și pune banii înapoi, pas cu pas.",
            note = "To find a starting amount, undo the story in reverse order: what was spent, add back; what was halved, double." to
                "Ca să afli suma de pornire, desfă povestea în ordine inversă: ce s-a cheltuit, adaugi; ce s-a înjumătățit, dublezi.",
            solution = listOf(
                "Before the bakery you had $left + $bakery = $half euros." to
                    "Înainte de brutărie aveai $left + $bakery = ${de(half)} euro.",
                "That was the half you had left after the market, so you set out with $half × 2 = ${2 * half}" to
                    "Aceea era jumătatea rămasă după piață, deci ai plecat cu $half × 2 = ${2 * half}",
            ),
            unit = "€",
        )
    }

    /** Take off what is already there, then count the weeks. */
    private fun savingUp(language: AppLanguage): Problem {
        val already = random.nextInt(10, 61)
        val weekly = random.nextInt(5, 16)
        val weeks = random.nextInt(4, 13)
        val target = already + weekly * weeks
        return piece(
            language,
            text = "You have $already euros put by and add $weekly euros every week.\nAfter how many weeks do you have $target euros?" to
                "Ai ${de(already)} euro puși deoparte și adaugi $weekly euro în fiecare săptămână.\nDupă câte săptămâni ai ${de(target)} euro?",
            answer = weeks,
            hint = "Only the missing part has to be saved." to
                "Doar partea care lipsește trebuie strânsă.",
            note = "Take off what is already saved before dividing. What you start with is not something the weeks have to earn." to
                "Scade ce e deja strâns înainte să împarți. Cu ce pornești nu trebuie câștigat de săptămâni.",
            solution = listOf(
                "Still missing: $target − $already = ${target - already} euros." to
                    "Mai lipsesc: $target − $already = ${de(target - already)} euro.",
                "${target - already} ÷ $weekly = $weeks" to
                    "${target - already} ÷ $weekly = $weeks",
            ),
        )
    }

    /** Filling and emptying at once: the net rate is the difference. */
    private fun leakyBarrel(language: AppLanguage): Problem {
        val inflow = random.nextInt(5, 13)
        val leak = random.nextInt(1, inflow)
        val minutes = random.nextInt(4, 13)
        val capacity = (inflow - leak) * minutes
        return piece(
            language,
            text = "A barrel holds $capacity liters. A hose pours in $inflow liters a minute, but a crack lets out $leak.\nAfter how many minutes is it full?" to
                "Un butoi ține ${de(capacity)} litri. Un furtun toarnă $inflow litri pe minut, dar o crăpătură lasă să iasă $leak.\nDupă câte minute e plin?",
            answer = minutes,
            hint = "Only the difference actually stays in the barrel." to
                "Doar diferența rămâne de fapt în butoi.",
            note = "When something fills and empties at the same time, work with the net rate: what goes in minus what goes out." to
                "Când ceva se umple și se golește deodată, lucrezi cu ritmul net: ce intră minus ce iese.",
            solution = listOf(
                "Each minute it really gains $inflow − $leak = ${inflow - leak} liters." to
                    "În fiecare minut câștigă de fapt $inflow − $leak = ${inflow - leak} litri.",
                "$capacity ÷ ${inflow - leak} = $minutes" to
                    "$capacity ÷ ${inflow - leak} = $minutes",
            ),
            unit = "min",
        )
    }

    /** A postcard goes one way, so nothing is halved. */
    private fun postcards(language: AppLanguage): Problem {
        val cousins = random.nextInt(4, 10)
        return piece(
            language,
            text = "Each of $cousins cousins sends a postcard to every other one of them.\nHow many postcards are sent?" to
                "Fiecare dintre cei $cousins veri trimite câte o vedere fiecăruia dintre ceilalți.\nCâte vederi se trimit?",
            answer = cousins * (cousins - 1),
            hint = "A postcard travels one way only. Nothing here is shared between two people." to
                "O vedere merge într-o singură direcție. Aici nimic nu e împărțit între doi oameni.",
            note = "Each of n people sends to the other n − 1: n × (n − 1). Unlike handshakes there is nothing to halve, because your card to me is not my card to you." to
                "Fiecare dintre cei n trimite celorlalți n − 1: n × (n − 1). Spre deosebire de strângerile de mână, nu se împarte la 2, fiindcă vederea ta către mine nu e vederea mea către tine.",
            solution = listOf(
                "Each of the $cousins sends ${cousins - 1} postcards, one to everybody else." to
                    "Fiecare dintre cei $cousins trimite ${cousins - 1} vederi, câte una fiecăruia.",
                "Nothing is counted twice here: $cousins × ${cousins - 1} = ${cousins * (cousins - 1)}" to
                    "Aici nimic nu e numărat de două ori: $cousins × ${cousins - 1} = ${cousins * (cousins - 1)}",
            ),
        )
    }

    /** Gaps plus one, then twice over for the second side. */
    private fun treePlanting(language: AppLanguage): Problem {
        val spacing = listOf(4, 5, 6, 10).random(random)
        val gaps = random.nextInt(5, 13)
        val length = spacing * gaps
        return piece(
            language,
            text = "Trees are planted every $spacing m along both sides of a $length m road, with one at each end.\nHow many trees are planted?" to
                "Se plantează câte un copac la fiecare $spacing m pe ambele părți ale unui drum de $length m, cu câte unul la fiecare capăt.\nCâți copaci se plantează?",
            answer = 2 * (gaps + 1),
            hint = "Do one side properly first, and don't forget the tree at the far end." to
                "Fă întâi bine o parte și nu uita copacul de la capăt.",
            note = "Along a straight line, trees = gaps + 1. Two sides simply double a side that was counted correctly." to
                "Pe o linie dreaptă, copaci = intervale + 1. Două părți doar dublează o parte numărată corect.",
            solution = listOf(
                "One side: $length ÷ $spacing = $gaps gaps, so $gaps + 1 = ${gaps + 1} trees." to
                    "O parte: $length ÷ $spacing = $gaps intervale, deci $gaps + 1 = ${gaps + 1} copaci.",
                "Both sides: ${gaps + 1} × 2 = ${2 * (gaps + 1)}" to
                    "Ambele părți: ${gaps + 1} × 2 = ${2 * (gaps + 1)}",
            ),
        )
    }
}
