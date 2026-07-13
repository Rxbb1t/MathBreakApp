package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * Shopping stories in lei. Everyday market math with whole-lei answers.
 *
 * EASY: two prices together, change from a banknote, a price per piece.
 * MEDIUM: a small basket (kilograms plus an extra item), change after
 * two purchases, how many fit an exact budget.
 * HARD: percent discounts, a two-kind market haul paid with a banknote,
 * saving up week by week.
 *
 * Templates are EN/RO pairs with the usual authoring rules: keep the
 * placeholder order, keep each type's English marker phrase, write no
 * digits into the template text. The tests re-derive every answer.
 * Romanian templates use {x_de} where a counted "lei" follows, so
 * "12 lei" and "45 de lei" both come out right.
 */
class MoneyProblemGenerator(private val random: Random) {

    fun generate(difficulty: Difficulty, language: AppLanguage): Problem = when (difficulty) {
        Difficulty.EASY -> when (random.nextInt(3)) {
            0 -> twoItems(language)
            1 -> payNote(language)
            else -> perPiece(language)
        }

        Difficulty.MEDIUM -> when (random.nextInt(3)) {
            0 -> basket(language)
            1 -> payTwo(language)
            else -> exactBudget(language)
        }

        Difficulty.HARD -> when (random.nextInt(3)) {
            0 -> discount(language)
            1 -> marketHaul(language)
            else -> saveUp(language)
        }
    }

    /** An EN/RO pair telling the same story. */
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

    /** Romanian counting: "12 lei" but "45 de lei". */
    private fun de(n: Int): String = if (n < 20) "$n" else "$n de"

    /** The fill entries for one number: its plain and Romanian-"de" forms. */
    private fun slot(key: String, n: Int): List<Pair<String, String>> =
        listOf(key to "$n", "${key}_de" to de(n))

    // ── EASY ─────────────────────────────────────────────────────────────

    /** Marker: "together". Order: {a}, {b}. Answer a + b. */
    private fun twoItems(language: AppLanguage): Problem {
        val a = random.nextInt(4, 41)
        val b = random.nextInt(4, 41)
        return money(
            text = fill(pick(TWO_ITEMS_TEMPLATES, language), (slot("a", a) + slot("b", b)).toMap()),
            answer = a + b,
            difficulty = Difficulty.EASY,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "Both prices land in the same purse. The answer is more than either."
                AppLanguage.ROMANIAN -> "Ambele prețuri ajung în aceeași pungă. Răspunsul e mai mare decât fiecare."
            },
        )
    }

    /** Marker: "note" (2 numbers). Order: {c}, {n}. Answer n − c. */
    private fun payNote(language: AppLanguage): Problem {
        val note = listOf(10, 20, 50).random(random)
        val cost = random.nextInt(3, note - 1)
        return money(
            text = fill(pick(PAY_NOTE_TEMPLATES, language), (slot("c", cost) + slot("n", note)).toMap()),
            answer = note - cost,
            difficulty = Difficulty.EASY,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "The change is what's left of the banknote once the price is taken out."
                AppLanguage.ROMANIAN -> "Restul e ce rămâne din bancnotă după ce iese prețul."
            },
        )
    }

    /** Marker: "each". Order: {k}, {p}. Answer k × p. */
    private fun perPiece(language: AppLanguage): Problem {
        val k = random.nextInt(2, 7)
        val p = random.nextInt(2, 10)
        return money(
            text = fill(pick(PER_PIECE_TEMPLATES, language), (slot("k", k) + slot("p", p)).toMap()),
            answer = k * p,
            difficulty = Difficulty.EASY,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "The same price repeats for every piece."
                AppLanguage.ROMANIAN -> "Același preț se repetă pentru fiecare bucată."
            },
        )
    }

    // ── MEDIUM ───────────────────────────────────────────────────────────

    /** Marker: "altogether". Order: {q}, {p}, {c}. Answer q×p + c. */
    private fun basket(language: AppLanguage): Problem {
        val q = random.nextInt(2, 7)
        val p = random.nextInt(3, 10)
        val c = random.nextInt(5, 31)
        return money(
            text = fill(
                pick(BASKET_TEMPLATES, language),
                (slot("q", q) + slot("p", p) + slot("c", c)).toMap(),
            ),
            answer = q * p + c,
            difficulty = Difficulty.MEDIUM,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "It's all one basket: the kilograms have their cost, the extra item its own."
                AppLanguage.ROMANIAN -> "Totul e un singur coș: kilogramele au costul lor, produsul în plus pe al lui."
            },
            notes = changeNotes(language),
        )
    }

    /** Marker: "note" (3 numbers). Order: {a}, {b}, {n}. Answer n − a − b. */
    private fun payTwo(language: AppLanguage): Problem {
        val a = random.nextInt(25, 91)
        val b = random.nextInt(25, 91)
        val note = 200
        return money(
            text = fill(
                pick(PAY_TWO_TEMPLATES, language),
                (slot("a", a) + slot("b", b) + slot("n", note)).toMap(),
            ),
            answer = note - a - b,
            difficulty = Difficulty.MEDIUM,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "The change is what's left of the note after both prices are taken out."
                AppLanguage.ROMANIAN -> "Restul e ce rămâne din bancnotă după ce ies ambele prețuri."
            },
            notes = changeNotes(language),
        )
    }

    /** Marker: "exactly". Order: {p}, {t}. Answer t ÷ p, whole by construction. */
    private fun exactBudget(language: AppLanguage): Problem {
        val p = random.nextInt(6, 26)
        val k = random.nextInt(3, 10)
        return money(
            text = fill(
                pick(EXACT_BUDGET_TEMPLATES, language),
                (slot("p", p) + slot("t", k * p)).toMap(),
            ),
            answer = k,
            difficulty = Difficulty.MEDIUM,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "Every one costs the same, and the money is used up exactly."
                AppLanguage.ROMANIAN -> "Fiecare costă la fel, iar banii se termină exact."
            },
            notes = budgetNotes(language),
            answerUnit = "",
        )
    }

    // ── HARD ─────────────────────────────────────────────────────────────

    /** Marker: "off". Order: {p}, {d}. Answer p − p×d÷100, whole by construction. */
    private fun discount(language: AppLanguage): Problem {
        val p = 20 * random.nextInt(2, 21)
        val d = listOf(10, 20, 25, 50).random(random)
        return money(
            text = fill(pick(DISCOUNT_TEMPLATES, language), (slot("p", p) + slot("d", d)).toMap()),
            answer = p - p * d / 100,
            difficulty = Difficulty.HARD,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "The discount is the part of the price that's forgiven. You pay the rest."
                AppLanguage.ROMANIAN -> "Reducerea e partea din preț care se iartă. Plătești restul."
            },
            notes = percentNotes(language),
        )
    }

    /** Marker: "banknote". Order: {q1}, {p1}, {q2}, {p2}, {n}. Answer n − q1p1 − q2p2. */
    private fun marketHaul(language: AppLanguage): Problem {
        val q1 = random.nextInt(2, 6)
        val p1 = random.nextInt(3, 10)
        val q2 = random.nextInt(2, 6)
        val p2 = random.nextInt(3, 10)
        val note = if (random.nextBoolean()) 100 else 200
        return money(
            text = fill(
                pick(MARKET_HAUL_TEMPLATES, language),
                (slot("q1", q1) + slot("p1", p1) + slot("q2", q2) + slot("p2", p2) + slot("n", note)).toMap(),
            ),
            answer = note - q1 * p1 - q2 * p2,
            difficulty = Difficulty.HARD,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "The change is what's left of the banknote after the whole market haul."
                AppLanguage.ROMANIAN -> "Restul e ce rămâne din bancnotă după toate cumpărăturile."
            },
            notes = changeNotes(language),
        )
    }

    /** Marker: "week". Order: {p}, {s}, {m}. Answer (p − s) ÷ m, whole by construction. */
    private fun saveUp(language: AppLanguage): Problem {
        val m = random.nextInt(10, 51)
        val k = random.nextInt(3, 10)
        val s = random.nextInt(20, 201)
        val p = s + k * m
        return money(
            text = fill(
                pick(SAVE_UP_TEMPLATES, language),
                (slot("p", p) + slot("s", s) + slot("m", m)).toMap(),
            ),
            answer = k,
            difficulty = Difficulty.HARD,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "Each week the missing part shrinks by the same amount."
                AppLanguage.ROMANIAN -> "În fiecare săptămână, partea care lipsește scade cu aceeași sumă."
            },
            notes = saveUpNotes(language),
            answerUnit = "",
        )
    }

    private fun money(
        text: String,
        answer: Int,
        difficulty: Difficulty,
        language: AppLanguage,
        hint: String,
        notes: List<String> = emptyList(),
        answerUnit: String = "lei",
    ): Problem = Problem(
        text = text,
        answer = answer,
        difficulty = difficulty,
        kind = ProblemKind.MONEY,
        hints = listOf(hint, HintText.digits(answer, language)),
        notes = notes,
        answerUnit = answerUnit,
    )

    // ── Helper-sheet notes ───────────────────────────────────────────────

    private fun changeNotes(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "Change = what you pay with − what everything costs.",
            "Price per kilogram × the kilograms = the cost of that item.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Restul = cu cât plătești − cât costă totul.",
            "Prețul pe kilogram × kilogramele = costul acelui produs.",
        )
    }

    private fun budgetNotes(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "How many fit the budget: all the money ÷ the price of one.",
            "An exact budget means the division leaves nothing over.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Câte încap în buget: toți banii ÷ prețul unuia.",
            "Un buget exact înseamnă că împărțirea nu lasă rest.",
        )
    }

    private fun percentNotes(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "A percent is a part of a hundred: 10% is a tenth, 25% a quarter, 50% a half.",
            "A percent of a price: price × percent ÷ 100. Example: 25% of 200 lei is 50 lei.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Procentul e o parte dintr-o sută: 10% e o zecime, 25% un sfert, 50% o jumătate.",
            "Procent dintr-un preț: preț × procent ÷ 100. Exemplu: 25% din 200 de lei e 50 de lei.",
        )
    }

    private fun saveUpNotes(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "What's still missing = the price − what's saved already.",
            "The weeks needed = the missing part ÷ the amount saved each week.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Cât mai lipsește = prețul − cât e strâns deja.",
            "Săptămânile necesare = partea care lipsește ÷ suma pusă pe săptămână.",
        )
    }

    companion object {
        // ════════════════════════════════════════════════════════════════
        // ✏️ MONEY TEMPLATES. EN/RO pairs, same three rules as the
        // riddles: keep placeholder order, keep the English marker phrase
        // of each list, write no digits into the template text. Romanian
        // uses {x_de} before "lei" ("12 lei" / "45 de lei").
        // ════════════════════════════════════════════════════════════════

        // Marker: "together". {a}, {b} the two prices.
        private val TWO_ITEMS_TEMPLATES = listOf(
            Template(
                "At the market, a kilogram of apples costs {a} lei and a kilogram of pears {b} lei.\nHow much do they cost together?",
                "La piață, un kilogram de mere costă {a_de} lei, iar unul de pere {b_de} lei.\nCât costă împreună?",
            ),
            Template(
                "At the bakery, a cozonac costs {a} lei and a tray of pastries {b} lei.\nHow much do they cost together?",
                "La cofetărie, un cozonac costă {a_de} lei, iar o tavă de prăjituri {b_de} lei.\nCât costă împreună?",
            ),
            Template(
                "A bouquet of tulips costs {a} lei and a flowerpot {b} lei.\nHow much do they cost together?",
                "Un buchet de lalele costă {a_de} lei, iar un ghiveci {b_de} lei.\nCât costă împreună?",
            ),
        )

        // Marker: "note" with two numbers. {c} the cost, {n} the banknote.
        private val PAY_NOTE_TEMPLATES = listOf(
            Template(
                "A loaf of bread costs {c} lei. You pay with a {n}-lei note.\nHow much change do you get?",
                "O pâine costă {c_de} lei. Plătești cu o bancnotă de {n_de} lei.\nCât rest primești?",
            ),
            Template(
                "A bag of oranges costs {c} lei. You pay with a {n}-lei note.\nHow much change do you get?",
                "O plasă de portocale costă {c_de} lei. Plătești cu o bancnotă de {n_de} lei.\nCât rest primești?",
            ),
            Template(
                "A bottle of oil costs {c} lei. You pay with a {n}-lei note.\nHow much change do you get?",
                "O sticlă de ulei costă {c_de} lei. Plătești cu o bancnotă de {n_de} lei.\nCât rest primești?",
            ),
        )

        // Marker: "each". {k} pieces, {p} lei apiece.
        private val PER_PIECE_TEMPLATES = listOf(
            Template(
                "You buy {k} croissants at {p} lei each.\nHow much do they cost in all?",
                "Cumperi {k} cornuri, câte {p} lei fiecare.\nCât costă în total?",
            ),
            Template(
                "You buy {k} postcards at {p} lei each.\nHow much do they cost in all?",
                "Cumperi {k} vederi, câte {p} lei fiecare.\nCât costă în total?",
            ),
            Template(
                "You buy {k} bags of seeds at {p} lei each.\nHow much do they cost in all?",
                "Cumperi {k} plicuri de semințe, câte {p} lei fiecare.\nCât costă în total?",
            ),
        )

        // Marker: "altogether". {q} kg, {p} lei per kg, {c} the extra item.
        private val BASKET_TEMPLATES = listOf(
            Template(
                "{q} kg of potatoes at {p} lei per kilogram, and a pumpkin for {c} lei.\nHow much is it altogether?",
                "{q} kg de cartofi la {p} lei kilogramul și un dovleac de {c_de} lei.\nCât costă totul la un loc?",
            ),
            Template(
                "{q} kg of cherries at {p} lei per kilogram, and a jar of cream for {c} lei.\nHow much is it altogether?",
                "{q} kg de cireșe la {p} lei kilogramul și un borcan de smântână de {c_de} lei.\nCât costă totul la un loc?",
            ),
            Template(
                "{q} kg of flour at {p} lei per kilogram, and a packet of yeast for {c} lei.\nHow much is it altogether?",
                "{q} kg de făină la {p} lei kilogramul și un pachet de drojdie de {c_de} lei.\nCât costă totul la un loc?",
            ),
        )

        // Marker: "note" with three numbers. {a}, {b} prices, {n} the note.
        private val PAY_TWO_TEMPLATES = listOf(
            Template(
                "Cheese for {a} lei and a jar of honey for {b} lei, paid with a {n}-lei note.\nHow much change comes back?",
                "Brânză de {a_de} lei și un borcan de miere de {b_de} lei, plătite cu o bancnotă de {n_de} lei.\nCât rest primești înapoi?",
            ),
            Template(
                "A scarf for {a} lei and gloves for {b} lei, paid with a {n}-lei note.\nHow much change comes back?",
                "Un fular de {a_de} lei și mănuși de {b_de} lei, plătite cu o bancnotă de {n_de} lei.\nCât rest primești înapoi?",
            ),
            Template(
                "A cookbook for {a} lei and a teapot for {b} lei, paid with a {n}-lei note.\nHow much change comes back?",
                "O carte de bucate de {a_de} lei și un ceainic de {b_de} lei, plătite cu o bancnotă de {n_de} lei.\nCât rest primești înapoi?",
            ),
        )

        // Marker: "exactly". {p} the price of one, {t} the exact budget.
        private val EXACT_BUDGET_TEMPLATES = listOf(
            Template(
                "A jar of zacusca costs {p} lei. You have exactly {t} lei to spend.\nHow many jars can you buy?",
                "Un borcan de zacuscă costă {p_de} lei. Ai exact {t_de} lei de cheltuit.\nCâte borcane poți cumpăra?",
            ),
            Template(
                "A box of tea costs {p} lei. You have exactly {t} lei to spend.\nHow many boxes can you buy?",
                "O cutie de ceai costă {p_de} lei. Ai exact {t_de} lei de cheltuit.\nCâte cutii poți cumpăra?",
            ),
            Template(
                "A ball of wool costs {p} lei. You have exactly {t} lei to spend.\nHow many balls can you buy?",
                "Un ghem de lână costă {p_de} lei. Ai exact {t_de} lei de cheltuit.\nCâte gheme poți cumpăra?",
            ),
        )

        // Marker: "off". {p} the full price, {d} the percent.
        private val DISCOUNT_TEMPLATES = listOf(
            Template(
                "A coat costs {p} lei. Today it is {d}% off.\nHow much does it cost now?",
                "Un palton costă {p_de} lei. Azi are reducere de {d}%.\nCât costă acum?",
            ),
            Template(
                "A pair of shoes costs {p} lei. Today they are {d}% off.\nHow much do they cost now?",
                "O pereche de pantofi costă {p_de} lei. Azi are reducere de {d}%.\nCât costă acum?",
            ),
            Template(
                "A kitchen mixer costs {p} lei. Today it is {d}% off.\nHow much does it cost now?",
                "Un mixer de bucătărie costă {p_de} lei. Azi are reducere de {d}%.\nCât costă acum?",
            ),
        )

        // Marker: "banknote". {q1} kg at {p1} lei/kg, {q2} kg at {p2} lei/kg, {n} the banknote.
        private val MARKET_HAUL_TEMPLATES = listOf(
            Template(
                "{q1} kg of tomatoes at {p1} lei per kilogram and {q2} kg of peppers at {p2} lei per kilogram, paid with a {n}-lei banknote.\nHow much change comes back?",
                "{q1} kg de roșii la {p1} lei kilogramul și {q2} kg de ardei la {p2} lei kilogramul, plătite cu o bancnotă de {n_de} lei.\nCât rest primești înapoi?",
            ),
            Template(
                "{q1} kg of grapes at {p1} lei per kilogram and {q2} kg of plums at {p2} lei per kilogram, paid with a {n}-lei banknote.\nHow much change comes back?",
                "{q1} kg de struguri la {p1} lei kilogramul și {q2} kg de prune la {p2} lei kilogramul, plătite cu o bancnotă de {n_de} lei.\nCât rest primești înapoi?",
            ),
            Template(
                "{q1} kg of carrots at {p1} lei per kilogram and {q2} kg of onions at {p2} lei per kilogram, paid with a {n}-lei banknote.\nHow much change comes back?",
                "{q1} kg de morcovi la {p1} lei kilogramul și {q2} kg de ceapă la {p2} lei kilogramul, plătite cu o bancnotă de {n_de} lei.\nCât rest primești înapoi?",
            ),
        )

        // Marker: "week". {p} the price, {s} saved already, {m} saved weekly.
        private val SAVE_UP_TEMPLATES = listOf(
            Template(
                "A blender costs {p} lei. You've saved {s} lei and put aside {m} lei every week.\nAfter how many weeks can you buy it?",
                "Un blender costă {p_de} lei. Ai strâns {s_de} lei și pui deoparte câte {m_de} lei pe săptămână.\nDupă câte săptămâni îl poți cumpăra?",
            ),
            Template(
                "A sewing machine costs {p} lei. You've saved {s} lei and put aside {m} lei every week.\nAfter how many weeks can you buy it?",
                "O mașină de cusut costă {p_de} lei. Ai strâns {s_de} lei și pui deoparte câte {m_de} lei pe săptămână.\nDupă câte săptămâni o poți cumpăra?",
            ),
            Template(
                "A garden bench costs {p} lei. You've saved {s} lei and put aside {m} lei every week.\nAfter how many weeks can you buy it?",
                "O bancă de grădină costă {p_de} lei. Ai strâns {s_de} lei și pui deoparte câte {m_de} lei pe săptămână.\nDupă câte săptămâni o poți cumpăra?",
            ),
        )
    }
}
