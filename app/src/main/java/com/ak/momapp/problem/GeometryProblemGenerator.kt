package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * Story geometry with a single whole-number answer. Engaging framings
 * (fences, ladders, tiles, jogging laps), never bare "radius and
 * diameter" drills.
 *
 * MEDIUM: rectangle perimeter, tiling areas, the third angle of a
 * triangle, a square's side from its fence, ladder-against-wall
 * Pythagoras (whole triples only). Everything shown or asked stays ≤ 500.
 * HARD: right-angle shortcuts, borders from an area, box volumes,
 * isosceles roof angles, laps around a park.
 *
 * Templates are EN/RO pairs. Same authoring rules as the riddles: keep
 * placeholder order, keep each type's English marker phrase, no digits
 * in template literals. The tests re-derive every answer from the text.
 * Each problem also carries the matching theorem or formula in [Problem.notes]
 * for the helper sheet.
 */
class GeometryProblemGenerator(private val random: Random) {

    fun generate(difficulty: Difficulty, language: AppLanguage): Problem =
        if (difficulty == Difficulty.HARD) {
            when (random.nextInt(5)) {
                0 -> shortcut(language)
                1 -> borderFromArea(language)
                2 -> boxVolume(language)
                3 -> isoscelesAngles(language)
                else -> laps(language)
            }
        } else {
            when (random.nextInt(5)) {
                0 -> perimeterFence(language)
                1 -> areaTiles(language)
                2 -> thirdAngle(language)
                3 -> squareSide(language)
                else -> ladder(language)
            }
        }

    /**
     * An EN/RO pair telling the same story; [unit] is its measuring unit
     * (same in both languages), which rides into the diagram labels.
     */
    private data class Template(val en: String, val ro: String, val unit: String = "")

    /** The chosen template's text in [language], plus its unit. */
    private fun pick(templates: List<Template>, language: AppLanguage): Pair<String, String> =
        templates.random(random).let {
            (if (language == AppLanguage.ROMANIAN) it.ro else it.en) to it.unit
        }

    private fun fill(text: String, values: Map<String, String>): String {
        var result = text
        for ((key, value) in values) result = result.replace("{$key}", value)
        return result
    }

    // ── MEDIUM ───────────────────────────────────────────────────────────

    /** Marker: "all the way around". Order: {a}, {b}. Answer 2(a+b). */
    private fun perimeterFence(language: AppLanguage): Problem {
        val a = random.nextInt(12, 61)
        val b = random.nextInt(8, a)
        val (template, unit) = pick(PERIMETER_TEMPLATES, language)
        return geometry(
            text = fill(template, mapOf("a" to "$a", "b" to "$b")),
            answer = 2 * (a + b),
            difficulty = Difficulty.MEDIUM,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "The fence follows every side. A rectangle has two lengths and two widths."
                AppLanguage.ROMANIAN -> "Gardul urmează fiecare latură. Un dreptunghi are două lungimi și două lățimi."
            },
            notes = perimeterNotes(language),
            diagram = Diagram.Rectangle(
                widthLabel = "$a $unit",
                heightLabel = "$b $unit",
                aspect = a.toFloat() / b,
            ),
            answerUnit = unit,
        )
    }

    /** Marker: "one square meter". Order: {a}, {b}. Answer a×b. */
    private fun areaTiles(language: AppLanguage): Problem {
        val a = random.nextInt(4, 21)
        val b = random.nextInt(3, 16)
        val (template, unit) = pick(AREA_TEMPLATES, language)
        return geometry(
            text = fill(template, mapOf("a" to "$a", "b" to "$b")),
            answer = a * b,
            difficulty = Difficulty.MEDIUM,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "Picture the floor as rows of equal squares."
                AppLanguage.ROMANIAN -> "Imaginează-ți podeaua ca rânduri de pătrate egale."
            },
            notes = areaNotes(language),
            diagram = Diagram.Rectangle(
                widthLabel = "$a $unit",
                heightLabel = "$b $unit",
                aspect = a.toFloat() / b,
                grid = true,
            ),
        )
    }

    /** Marker: "third angle". Order: {x}, {y}. Answer 180−x−y. */
    private fun thirdAngle(language: AppLanguage): Problem {
        val x = random.nextInt(30, 101)
        val y = random.nextInt(25, 161 - x)
        return geometry(
            text = fill(pick(THIRD_ANGLE_TEMPLATES, language).first, mapOf("x" to "$x", "y" to "$y")),
            answer = 180 - x - y,
            difficulty = Difficulty.MEDIUM,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "All three angles of a triangle make 180° together."
                AppLanguage.ROMANIAN -> "Toate cele trei unghiuri ale unui triunghi fac împreună 180°."
            },
            notes = angleNotes(language),
            diagram = Diagram.AngleTriangle(
                leftDegrees = x,
                rightDegrees = y,
                leftLabel = "$x°",
                rightLabel = "$y°",
                topLabel = "?",
            ),
            answerUnit = "°",
        )
    }

    /** Marker: "one side". Order: {p}. Answer p ÷ 4. */
    private fun squareSide(language: AppLanguage): Problem {
        val side = random.nextInt(6, 61)
        val (template, unit) = pick(SQUARE_SIDE_TEMPLATES, language)
        return geometry(
            text = fill(template, mapOf("p" to "${4 * side}")),
            answer = side,
            difficulty = Difficulty.MEDIUM,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "All four sides of a square are the same length."
                AppLanguage.ROMANIAN -> "Toate cele patru laturi ale unui pătrat sunt la fel de lungi."
            },
            notes = perimeterNotes(language),
            // "P" matches the helper sheet's formula in both languages.
            diagram = Diagram.Square(
                sideLabel = "?",
                edgeLabel = "P = ${4 * side} $unit",
            ),
            answerUnit = unit,
        )
    }

    /** Marker: "ladder". Order: {c}, {a}. Answer √(c²−a²), whole by construction. */
    private fun ladder(language: AppLanguage): Problem {
        val (c, a, b) = LADDER_TRIPLES.random(random)
        val (template, unit) = pick(LADDER_TEMPLATES, language)
        return geometry(
            text = fill(template, mapOf("c" to "$c", "a" to "$a")),
            answer = b,
            difficulty = Difficulty.MEDIUM,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "Wall, ground and ladder form a right triangle. The ladder is its longest side."
                AppLanguage.ROMANIAN -> "Peretele, pământul și scara formează un triunghi dreptunghic. Scara e latura cea mai lungă."
            },
            notes = pythagorasNotes(language),
            diagram = Diagram.RightTriangle(
                bottomLabel = "$a $unit",
                rightLabel = "?",
                slantLabel = "$c $unit",
                aspect = a.toFloat() / b,
            ),
            answerUnit = unit,
        )
    }

    // ── HARD ─────────────────────────────────────────────────────────────

    /** Marker: "right angle". Order: {a}, {b}. Answer √(a²+b²), whole. */
    private fun shortcut(language: AppLanguage): Problem {
        val (a, b, c) = SHORTCUT_TRIPLES.random(random)
        val (template, unit) = pick(SHORTCUT_TEMPLATES, language)
        return geometry(
            text = fill(template, mapOf("a" to "$a", "b" to "$b")),
            answer = c,
            difficulty = Difficulty.HARD,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "The straight line is the longest side of a right triangle."
                AppLanguage.ROMANIAN -> "Linia dreaptă e latura cea mai lungă a unui triunghi dreptunghic."
            },
            notes = pythagorasNotes(language),
            diagram = Diagram.RightTriangle(
                bottomLabel = "$a $unit",
                rightLabel = "$b $unit",
                slantLabel = "?",
                aspect = a.toFloat() / b,
            ),
            answerUnit = unit,
        )
    }

    /** Marker: "border". Order: {A}, {a}. Answer 2(a + A÷a). */
    private fun borderFromArea(language: AppLanguage): Problem {
        val a = random.nextInt(8, 26)
        val b = random.nextInt(5, 21)
        val (template, unit) = pick(BORDER_TEMPLATES, language)
        return geometry(
            text = fill(template, mapOf("A" to "${a * b}", "a" to "$a")),
            answer = 2 * (a + b),
            difficulty = Difficulty.HARD,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "The area and the length together reveal the width."
                AppLanguage.ROMANIAN -> "Aria și lungimea, împreună, îți dezvăluie lățimea."
            },
            notes = areaNotes(language) + perimeterNotes(language).first(),
            // "A" matches the helper sheet's area formula in both languages.
            diagram = Diagram.Rectangle(
                widthLabel = "$a $unit",
                heightLabel = "?",
                innerLabel = "A = ${a * b} $unit²",
            ),
            answerUnit = unit,
        )
    }

    /** Marker: "cubes". Order: {a}, {b}, {c}. Answer a×b×c. */
    private fun boxVolume(language: AppLanguage): Problem {
        val a = random.nextInt(4, 13)
        val b = random.nextInt(3, 13)
        val c = random.nextInt(3, 13)
        val (template, unit) = pick(VOLUME_TEMPLATES, language)
        return geometry(
            text = fill(template, mapOf("a" to "$a", "b" to "$b", "c" to "$c")),
            answer = a * b * c,
            difficulty = Difficulty.HARD,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "The box fills up with identical layers of little cubes."
                AppLanguage.ROMANIAN -> "Cutia se umple cu straturi identice de cuburi mici."
            },
            notes = volumeNotes(language),
            diagram = Diagram.Box(
                lengthLabel = "$a $unit",
                depthLabel = "$b $unit",
                heightLabel = "$c $unit",
            ),
        )
    }

    /** Marker: "equal". Order: {t}. Answer (180−t)÷2, whole via even t. */
    private fun isoscelesAngles(language: AppLanguage): Problem {
        val top = 2 * random.nextInt(10, 61)
        return geometry(
            text = fill(pick(ISOSCELES_TEMPLATES, language).first, mapOf("t" to "$top")),
            answer = (180 - top) / 2,
            difficulty = Difficulty.HARD,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "The three angles still make 180°. And two of them match."
                AppLanguage.ROMANIAN -> "Cele trei unghiuri tot 180° fac. Iar două dintre ele sunt la fel."
            },
            notes = angleNotes(language),
            diagram = Diagram.AngleTriangle(
                leftDegrees = (180 - top) / 2,
                rightDegrees = (180 - top) / 2,
                leftLabel = "?",
                rightLabel = "?",
                topLabel = "$top°",
            ),
            answerUnit = "°",
        )
    }

    /** Marker: "laps". Order: {a}, {b}, {n}. Answer n×2(a+b). */
    private fun laps(language: AppLanguage): Problem {
        val a = random.nextInt(25, 61)
        val b = random.nextInt(15, 46)
        val n = random.nextInt(2, 7)
        val (template, unit) = pick(LAPS_TEMPLATES, language)
        return geometry(
            text = fill(template, mapOf("a" to "$a", "b" to "$b", "n" to "$n")),
            answer = n * 2 * (a + b),
            difficulty = Difficulty.HARD,
            language = language,
            hint = when (language) {
                AppLanguage.ENGLISH -> "One lap is the whole way around the rectangle."
                AppLanguage.ROMANIAN -> "O tură înseamnă tot ocolul dreptunghiului."
            },
            notes = perimeterNotes(language),
            diagram = Diagram.Rectangle(
                widthLabel = "$a $unit",
                heightLabel = "$b $unit",
                aspect = a.toFloat() / b,
                lapsLabel = "× $n",
            ),
            answerUnit = unit,
        )
    }

    private fun geometry(
        text: String,
        answer: Int,
        difficulty: Difficulty,
        language: AppLanguage,
        hint: String,
        notes: List<String>,
        diagram: Diagram,
        answerUnit: String = "",
    ): Problem = Problem(
        text = text,
        answer = answer,
        difficulty = difficulty,
        kind = ProblemKind.GEOMETRY,
        hints = listOf(hint, HintText.digits(answer, language)),
        notes = notes,
        diagram = diagram,
        answerUnit = answerUnit,
    )

    // ── Helper-sheet notes ───────────────────────────────────────────────

    private fun perimeterNotes(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "Perimeter = the distance all the way around a shape.",
            "Rectangle: P = 2 × (length + width). Square: P = 4 × side.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Perimetrul = distanța de jur împrejurul unei figuri.",
            "Dreptunghi: P = 2 × (lungime + lățime). Pătrat: P = 4 × latură.",
        )
    }

    private fun areaNotes(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "Area of a rectangle = length × width. A square: side × side.",
            "Area counts how many unit squares cover the surface.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Aria dreptunghiului = lungime × lățime. Pătrat: latură × latură.",
            "Aria numără câte pătrate de o unitate acoperă suprafața.",
        )
    }

    private fun angleNotes(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "The three angles of any triangle add up to 180°.",
            "An isosceles triangle has two equal sides. And two equal angles.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Cele trei unghiuri ale oricărui triunghi adunate dau 180°.",
            "Un triunghi isoscel are două laturi egale. Și două unghiuri egale.",
        )
    }

    private fun pythagorasNotes(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "Pythagoras' theorem: in a right triangle, a² + b² = c², where c is the longest side (opposite the right angle).",
            "Example: 3² + 4² = 9 + 16 = 25, and 25 = 5². So the sides 3, 4, 5 make a right triangle.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Teorema lui Pitagora: într-un triunghi dreptunghic, a² + b² = c², unde c e latura cea mai lungă (opusă unghiului drept).",
            "Exemplu: 3² + 4² = 9 + 16 = 25, iar 25 = 5². Deci laturile 3, 4, 5 formează un triunghi dreptunghic.",
        )
    }

    private fun volumeNotes(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "Volume of a box = length × width × height.",
            "It counts the one-centimeter cubes that fit inside.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Volumul unei cutii = lungime × lățime × înălțime.",
            "El numără cuburile de un centimetru care încap înăuntru.",
        )
    }

    companion object {
        /** (ladder, foot distance, height). Whole Pythagorean triples. */
        private val LADDER_TRIPLES = listOf(
            Triple(5, 3, 4), Triple(10, 6, 8), Triple(13, 5, 12),
            Triple(15, 9, 12), Triple(17, 8, 15), Triple(20, 12, 16),
            Triple(25, 7, 24), Triple(26, 10, 24),
        )

        /** (leg, leg, hypotenuse) for the HARD shortcut problems. */
        private val SHORTCUT_TRIPLES = listOf(
            Triple(30, 40, 50), Triple(60, 80, 100), Triple(50, 120, 130),
            Triple(80, 150, 170), Triple(70, 240, 250), Triple(90, 120, 150),
            Triple(20, 21, 29), Triple(9, 40, 41),
        )

        // ════════════════════════════════════════════════════════════════
        // ✏️ GEOMETRY TEMPLATES. EN/RO pairs, same three rules as the
        // riddles: keep placeholder order, keep the English marker phrase
        // of each list, write no digits into the template text.
        // ════════════════════════════════════════════════════════════════

        // Marker: "all the way around". {a} length, {b} width.
        private val PERIMETER_TEMPLATES = listOf(
            Template(
                "A vegetable garden is {a} m long and {b} m wide.\nHow many meters of fence go all the way around it?",
                "O grădină de legume are {a} m lungime și {b} m lățime.\nCâți metri de gard îi trebuie de jur împrejur?",
                unit = "m",
            ),
            Template(
                "A painting is {a} cm long and {b} cm wide.\nHow many centimeters of wooden frame go all the way around it?",
                "Un tablou are {a} cm lungime și {b} cm lățime.\nCâți centimetri de ramă de lemn îi trebuie de jur împrejur?",
                unit = "cm",
            ),
            Template(
                "A tablecloth is {a} dm long and {b} dm wide.\nHow many decimeters of lace trim go all the way around it?",
                "O față de masă are {a} dm lungime și {b} dm lățime.\nCâți decimetri de dantelă îi trebuie de jur împrejur?",
                unit = "dm",
            ),
        )

        // Marker: "one square meter". {a} length, {b} width.
        private val AREA_TEMPLATES = listOf(
            Template(
                "A kitchen floor is {a} m long and {b} m wide. Each tile covers one square meter.\nHow many tiles are needed?",
                "Podeaua bucătăriei are {a} m lungime și {b} m lățime. Fiecare placă acoperă un metru pătrat.\nCâte plăci sunt necesare?",
                unit = "m",
            ),
            Template(
                "A lawn is {a} m long and {b} m wide. One bag of seed covers one square meter.\nHow many bags are needed?",
                "Un gazon are {a} m lungime și {b} m lățime. Un săculeț de semințe acoperă un metru pătrat.\nCâți săculeți sunt necesari?",
                unit = "m",
            ),
            Template(
                "A wall is {a} m long and {b} m tall. A can of paint covers one square meter.\nHow many cans are needed?",
                "Un perete are {a} m lungime și {b} m înălțime. O cutie de vopsea acoperă un metru pătrat.\nCâte cutii sunt necesare?",
                unit = "m",
            ),
        )

        // Marker: "third angle". {x}, {y} known angles.
        private val THIRD_ANGLE_TEMPLATES = listOf(
            Template(
                "Two angles of a triangle measure {x}° and {y}°.\nHow many degrees is the third angle?",
                "Două unghiuri ale unui triunghi au {x}° și {y}°.\nCâte grade are al treilea unghi?",
            ),
            Template(
                "A triangular garden bed has corners of {x}° and {y}°.\nHow many degrees is the third angle?",
                "Un strat de flori triunghiular are colțuri de {x}° și {y}°.\nCâte grade are al treilea unghi?",
            ),
            Template(
                "A slice of pie has angles of {x}° and {y}° at two corners.\nHow many degrees is the third angle?",
                "O felie de plăcintă are unghiuri de {x}° și {y}° la două colțuri.\nCâte grade are al treilea unghi?",
            ),
        )

        // Marker: "one side". {p} full perimeter of a square.
        private val SQUARE_SIDE_TEMPLATES = listOf(
            Template(
                "A square playground needs {p} m of fence in total.\nHow many meters long is one side?",
                "Unui loc de joacă pătrat îi trebuie {p} m de gard în total.\nCâți metri are o latură?",
                unit = "m",
            ),
            Template(
                "A square napkin has {p} cm of stitched edge in total.\nHow many centimeters long is one side?",
                "Un șervețel pătrat are {p} cm de tiv în total.\nCâți centimetri are o latură?",
                unit = "cm",
            ),
            Template(
                "A square sheep pen uses {p} m of fencing in total.\nHow many meters long is one side?",
                "Un țarc pătrat pentru oi folosește {p} m de gard în total.\nCâți metri are o latură?",
                unit = "m",
            ),
        )

        // Marker: "ladder". {c} ladder length, {a} distance from the wall.
        private val LADDER_TEMPLATES = listOf(
            Template(
                "A ladder {c} m long leans against a wall, its foot {a} m from the wall.\nHow many meters up the wall does it reach?",
                "O scară de {c} m e sprijinită de un perete, cu piciorul la {a} m de perete.\nLa câți metri înălțime ajunge pe perete?",
                unit = "m",
            ),
            Template(
                "A ladder {c} m long rests on the side of a barn, standing {a} m away from it.\nHow many meters high does it touch the barn?",
                "O scară de {c} m stă rezemată de un hambar, la {a} m distanță de el.\nLa ce înălțime, în metri, atinge hambarul?",
                unit = "m",
            ),
            Template(
                "To pick cherries, a ladder {c} m long is set with its base {a} m from the tree trunk.\nHow many meters up the trunk does it lean?",
                "Pentru cireșe, o scară de {c} m e așezată cu baza la {a} m de trunchi.\nLa câți metri pe trunchi se sprijină?",
                unit = "m",
            ),
        )

        // Marker: "right angle". {a}, {b} the two legs.
        private val SHORTCUT_TEMPLATES = listOf(
            Template(
                "A path goes {a} m straight ahead, turns at a right angle, and goes {b} m more.\nHow many meters is the straight shortcut back to the start?",
                "O potecă merge {a} m drept înainte, cotește în unghi drept și mai merge {b} m.\nCâți metri are scurtătura dreaptă înapoi la start?",
                unit = "m",
            ),
            Template(
                "Two streets meet at a right angle. One walk is {a} m, the other {b} m.\nHow many meters is the diagonal path across the corner lot?",
                "Două străzi se întâlnesc în unghi drept. Un drum are {a} m, celălalt {b} m.\nCâți metri are poteca în diagonală peste teren?",
                unit = "m",
            ),
            Template(
                "A kite string is staked where two field edges meet at a right angle, {a} m along one edge and {b} m along the other.\nHow many meters is the straight line between the two ends?",
                "Marginile unui câmp se întâlnesc în unghi drept: {a} m pe o margine și {b} m pe cealaltă.\nCâți metri are linia dreaptă dintre cele două capete?",
                unit = "m",
            ),
        )

        // Marker: "border". {A} area, {a} known length.
        private val BORDER_TEMPLATES = listOf(
            Template(
                "A rug covers {A} m² and is {a} m long.\nHow many meters of border go around its edge?",
                "Un covor acoperă {A} m² și are {a} m lungime.\nCâți metri de bordură îi trebuie pe margine?",
                unit = "m",
            ),
            Template(
                "A flower bed covers {A} m² and is {a} m long.\nHow many meters of brick border wrap around it?",
                "Un strat de flori acoperă {A} m² și are {a} m lungime.\nCâți metri de bordură de cărămidă îl înconjoară?",
                unit = "m",
            ),
            Template(
                "A quilt covers {A} dm² and is {a} dm long.\nHow many decimeters of ribbon border does its edge need?",
                "O cuvertură acoperă {A} dm² și are {a} dm lungime.\nCâți decimetri de panglică de bordură îi trebuie pe margine?",
                unit = "dm",
            ),
        )

        // Marker: "cubes". {a}, {b}, {c} the box sides.
        private val VOLUME_TEMPLATES = listOf(
            Template(
                "A box is {a} cm long, {b} cm wide and {c} cm tall.\nHow many one-centimeter sugar cubes fill it completely?",
                "O cutie are {a} cm lungime, {b} cm lățime și {c} cm înălțime.\nCâte cuburi de zahăr de un centimetru o umplu complet?",
                unit = "cm",
            ),
            Template(
                "A toy chest is {a} dm long, {b} dm wide and {c} dm tall.\nHow many one-decimeter building cubes fit inside?",
                "O ladă de jucării are {a} dm lungime, {b} dm lățime și {c} dm înălțime.\nCâte cuburi de construcție de un decimetru încap înăuntru?",
                unit = "dm",
            ),
            Template(
                "An aquarium is {a} dm long, {b} dm wide and {c} dm deep. Each liter of water fills a cube of one decimeter.\nHow many liters (cubes of water) fill it to the brim?",
                "Un acvariu are {a} dm lungime, {b} dm lățime și {c} dm adâncime. Fiecare litru de apă umple un cub de un decimetru.\nCâți litri (cuburi de apă) îl umplu până sus?",
                unit = "dm",
            ),
        )

        // Marker: "equal". {t} the top angle; the two equal ones are asked.
        private val ISOSCELES_TEMPLATES = listOf(
            Template(
                "A roof truss is a triangle with a top angle of {t}°. Its other two angles are equal.\nHow many degrees is each of them?",
                "Un acoperiș e un triunghi cu unghiul de sus de {t}°. Celelalte două unghiuri sunt egale.\nCâte grade are fiecare dintre ele?",
            ),
            Template(
                "A slice of cake is a triangle: the tip measures {t}°, and the two angles at the crust are equal.\nHow many degrees is each of them?",
                "O felie de tort e un triunghi: vârful are {t}°, iar cele două unghiuri de la margine sunt egale.\nCâte grade are fiecare dintre ele?",
            ),
            Template(
                "A sailing boat's sail is a triangle with {t}° at the mast top; the two lower angles are equal.\nHow many degrees is each of them?",
                "Vela unei bărci e un triunghi cu {t}° în vârful catargului; cele două unghiuri de jos sunt egale.\nCâte grade are fiecare dintre ele?",
            ),
        )

        // Marker: "laps". {a} length, {b} width, {n} laps.
        private val LAPS_TEMPLATES = listOf(
            Template(
                "A rectangular park is {a} m long and {b} m wide. A jogger runs {n} laps around it.\nHow many meters does she run in all?",
                "Un parc dreptunghiular are {a} m lungime și {b} m lățime. O alergătoare face {n} ture în jurul lui.\nCâți metri aleargă în total?",
                unit = "m",
            ),
            Template(
                "A rectangular schoolyard is {a} m long and {b} m wide. The gym class walks {n} laps around it.\nHow many meters do they walk in all?",
                "O curte de școală dreptunghiulară are {a} m lungime și {b} m lățime. Clasa de sport face {n} ture în jurul ei.\nCâți metri merg în total?",
                unit = "m",
            ),
            Template(
                "A rectangular pond is {a} m long and {b} m wide. A duck paddles {n} laps around its edge.\nHow many meters does it paddle in all?",
                "Un iaz dreptunghiular are {a} m lungime și {b} m lățime. O rață înoată {n} ture pe lângă mal.\nCâți metri înoată în total?",
                unit = "m",
            ),
        )
    }
}
