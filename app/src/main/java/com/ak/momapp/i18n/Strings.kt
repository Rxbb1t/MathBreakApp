package com.ak.momapp.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.ak.momapp.data.SetupPreset
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.ProblemKind
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.problem.TopicGroup
import com.ak.momapp.ui.theme.AppPalette
import java.util.Locale

enum class AppLanguage { ENGLISH, ROMANIAN }

fun AppLanguage.strings(): Strings = when (this) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.ROMANIAN -> RomanianStrings
}

/** Provided at the app root; screens read `LocalStrings.current`. */
val LocalStrings = staticCompositionLocalOf { EnglishStrings }

/**
 * Every user-visible phrase, one field per phrase. Phrases with values
 * baked in are lambdas so each language controls its own word order.
 */
class Strings(
    val language: AppLanguage,
    val locale: Locale,
    // Problem screen
    val solvedToday: (Int) -> String,
    val statsIconDescription: String,
    val settingsIconDescription: String,
    val correctFeedback: String,
    /**
     * Praise for an estimate that landed near the answer without hitting
     * it. Takes the exact value, because being close is only worth
     * anything once she knows what it was close to.
     */
    val closeEnoughFeedback: (String) -> String,
    val tryAgainFeedback: String,
    /** Hint button label; the number is how many presses are left. */
    val hintButton: (Int) -> String,
    val skipButton: String,
    /** Takes the display form of the answer: "42", ">", "6 + 4 + 9". */
    val revealedFeedback: (String) -> String,
    val check: String,
    val startButton: String,
    val readyLine: String,
    val oneMore: String,
    // Practice mode: drill one exercise type at a level she picks
    val practiceTitle: String,
    val practiceIntro: String,
    val practiceButton: String,
    val practiceLevel: String,
    val showSolution: String,
    val hideSolution: String,
    // Home-screen widget. The button lives in Settings; the other two are
    // drawn on the widget itself and are kept to a few words, since it
    // has to stay legible at one cell tall and the system, not the app,
    // decides how much room it gets.
    val addWidget: String,
    val widgetTapPrompt: String,
    val widgetRemindersOff: String,
    // Notebook (scratch paper drawer, MEDIUM/HARD only)
    val notebookTitle: String,
    val notebookSubtitle: String,
    // One-time setup guide (first open): pick a preset, tweak later
    val guideTitle: String,
    val guideIntro: String,
    val presetTitle: (SetupPreset) -> String,
    val presetBody: (SetupPreset) -> String,
    // The same presets, offered again from Settings
    val quickSetupAction: String,
    val quickSetupTitle: String,
    val quickSetupIntro: String,
    val snooze15: String,
    val breakDone: String,
    /** Offered on the problem screen once the per-break cap is reached. */
    val anotherRound: String,
    val difficultyLabel: (Difficulty) -> String,
    // Settings
    val settingsTitle: String,
    /** Headers grouping the settings cards below them. */
    val settingsGroupBreaks: String,
    val settingsGroupExercises: String,
    val settingsGroupApp: String,
    val back: String,
    val errorReportAction: String,
    val errorReportTitle: String,
    val errorReportBody: String,
    val errorReportNoneBody: String,
    val errorReportCopy: String,
    val errorReportCopied: String,
    val errorReportClear: String,
    val supportAction: String,
    val languageSection: String,
    val remindersSection: String,
    val breakReminders: String,
    val notificationsOffHint: String,
    val turnOnNotifications: String,
    val exactAlarmHint: String,
    val allowExactTiming: String,
    val nextReminder: (String) -> String,
    val todayAt: (String) -> String,
    val tomorrowAt: (String) -> String,
    val remindMeEvery: String,
    val hourOption: (Int) -> String,
    val activeHours: String,
    val activeHoursSubtitle: String,
    val fromTime: (String) -> String,
    val untilTime: (String) -> String,
    val activeDays: String,
    val problemsPerBreak: String,
    val problemsPerBreakSubtitle: String,
    val resetSittingButton: String,
    val noLimit: String,
    val customEllipsis: String,
    val customValue: (Int) -> String,
    val customPlaceholder: String,
    val startingLevel: String,
    val startingLevelSubtitle: String,
    /** One-line description of what a difficulty actually serves. */
    val difficultyExplanation: (Difficulty) -> String,
    // Exercise types screen (reached from a summary row in Settings)
    val exerciseTypesTitle: String,
    /** The Settings summary row: (switched on, total) → "8 of 10 on". */
    val exerciseTypesSummary: (Int, Int) -> String,
    val problemTypesSubtitle: String,
    /** One line under the Exercises heading explaining the level markers. */
    val topicLevelsHint: String,
    val topicGroupLabel: (TopicGroup) -> String,
    val topicLabel: (ProblemTopic) -> String,
    /** The little instruction over a one-tap exercise; empty for typed kinds. */
    val tapPrompt: (ProblemKind) -> String,
    /** One calm line under each switch saying what the topic deals. */
    val topicDescription: (ProblemTopic) -> String,
    // Daily challenge
    val challengeTitle: String,
    val challengeStage: (Int, Int) -> String,
    val challengeContinue: String,
    val challengeDoneHeadline: String,
    val challengeDoneBody: (Int) -> String,
    val challengeTomorrow: String,
    val activeFromTitle: String,
    val activeUntilTitle: String,
    val personalize: String,
    val personalizeTitle: String,
    val paletteName: (AppPalette) -> String,
    val ok: String,
    val cancel: String,
    // Stats screen
    val statsTitle: String,
    val problemsThisWeek: (Int) -> String,
    val todayCard: String,
    val accuracyCard: String,
    val fastestCard: String,
    val allTimeCard: String,
    val statsLineToday: String,
    val statsLineWeek: String,
    val statsLineFreshWeek: String,
    /** Header of the per-exercise-type accuracy card. */
    val topicBreakdownTitle: String,
    /** Header of the 14-day activity chart card. */
    val activityChartTitle: String,
    // Success chime
    val soundSection: String,
    val successSoundLabel: String,
    val successSoundSubtitle: String,
)

val EnglishStrings = Strings(
    language = AppLanguage.ENGLISH,
    locale = Locale.ENGLISH,
    solvedToday = { "Solved today: $it" },
    statsIconDescription = "Your numbers",
    settingsIconDescription = "Settings",
    correctFeedback = "That's right!",
    closeEnoughFeedback = { "Close enough! It was exactly $it." },
    tryAgainFeedback = "Not quite. Give it another go!",
    hintButton = { "Hint ($it)" },
    skipButton = "Skip",
    revealedFeedback = { "The answer was $it.\nOn to the next one!" },
    check = "Check",
    startButton = "Start",
    readyLine = "Ready when you are.",
    oneMore = "One more?",
    practiceTitle = "Practice",
    practiceIntro = "Pick one type and a level. Nothing is capped here, so take as long as you like.",
    practiceButton = "Practice a type",
    practiceLevel = "Level",
    showSolution = "Show me why",
    hideSolution = "Hide",
    addWidget = "Add to home screen",
    widgetTapPrompt = "Tap for a problem",
    widgetRemindersOff = "Reminders are off",
    notebookTitle = "Helper sheet",
    notebookSubtitle = "The rules, formulas and definitions that fit this problem.",
    guideTitle = "Welcome!",
    guideIntro = "Pick how you want your breaks to work. You can change everything later in Settings.",
    presetTitle = {
        when (it) {
            SetupPreset.DEFAULT -> "Balanced"
            SetupPreset.RELAXED -> "Relaxed"
            SetupPreset.CHALLENGE -> "Challenge"
        }
    },
    presetBody = {
        when (it) {
            SetupPreset.DEFAULT ->
                "Breaks of 10 problems. Starts easy and finds your level as you go."
            SetupPreset.RELAXED ->
                "Short breaks of 5 problems, and it stays gentle however well you do."
            SetupPreset.CHALLENGE ->
                "Long breaks of 20 problems, starting at Normal and climbing as high as you can take it."
        }
    },
    quickSetupAction = "Change how breaks work",
    quickSetupTitle = "How should breaks work?",
    quickSetupIntro = "Pick a style. This starts your level fresh, so give it a few problems to settle in again.",
    snooze15 = "Snooze 15 min",
    breakDone = "That's your break! See you at the next one.",
    anotherRound = "Another round",
    difficultyLabel = {
        when (it) {
            Difficulty.EASY -> "Easy"
            Difficulty.MEDIUM -> "Normal"
            Difficulty.HARD -> "Hard"
        }
    },
    settingsTitle = "Settings",
    settingsGroupBreaks = "When breaks arrive",
    settingsGroupExercises = "What a break is like",
    settingsGroupApp = "The app itself",
    back = "Back",
    errorReportAction = "Report a problem",
    errorReportTitle = "Report a problem",
    errorReportBody = "Something went wrong last time. Copying this and sending it along is the only way anyone will know it happened, since the app never sends anything on its own.",
    errorReportNoneBody = "Nothing has gone wrong so far. If something does, the details will show up here so you can pass them on.",
    errorReportCopy = "Copy the details",
    errorReportCopied = "Copied",
    errorReportClear = "Clear this",
    supportAction = "Support this app",
    languageSection = "Language",
    remindersSection = "Reminders",
    breakReminders = "Break reminders",
    notificationsOffHint = "Notifications are turned off, so reminders can't appear.",
    turnOnNotifications = "Turn on notifications",
    exactAlarmHint = "Reminders may drift a few minutes without \"Alarms & reminders\" access.",
    allowExactTiming = "Allow exact timing",
    nextReminder = { "Next reminder: $it" },
    todayAt = { "today at $it" },
    tomorrowAt = { "tomorrow at $it" },
    remindMeEvery = "Remind me every",
    hourOption = { if (it == 1) "1 hour" else "$it h" },
    activeHours = "Active hours",
    activeHoursSubtitle = "Reminders only arrive inside this window.",
    fromTime = { "From $it" },
    untilTime = { "Until $it" },
    activeDays = "Active days",
    problemsPerBreak = "Problems per break",
    problemsPerBreakSubtitle = "After this many, the app sends you back to work.",
    resetSittingButton = "Start a fresh round",
    noLimit = "No limit",
    customEllipsis = "Custom…",
    customValue = { "Custom: $it" },
    customPlaceholder = "e.g. 7",
    startingLevel = "Starting level",
    startingLevelSubtitle = "Adapts as you go: it climbs while you're on a roll and eases off after a rough patch.",
    difficultyExplanation = {
        when (it) {
            Difficulty.EASY ->
                "Easy: chains of 3–4 small numbers with all four operations, friendly shape puzzles, short riddles, market prices and clock questions."
            Difficulty.MEDIUM ->
                "Normal: equations with x and y, geometry, money and clock stories, and trickier puzzles."
            Difficulty.HARD ->
                "Hard: longer equations with 4–7 numbers, simple derivatives, tougher geometry, discounts and journeys, the hardest puzzles and riddles."
        }
    },
    exerciseTypesTitle = "Exercise types",
    exerciseTypesSummary = { on, total -> "$on of $total switched on" },
    problemTypesSubtitle = "Pick which problem types show up. At least one always stays on.",
    topicLevelsHint = "Each type finds its own level: it climbs where you are quick and eases off where you are not.",
    topicGroupLabel = {
        when (it) {
            TopicGroup.NUMBERS -> "Numbers"
            TopicGroup.STORIES -> "Everyday stories"
            TopicGroup.THINKING -> "Thinking"
        }
    },
    topicLabel = {
        when (it) {
            ProblemTopic.CORE -> "Numbers & equations"
            ProblemTopic.PUZZLE -> "Shape puzzles"
            ProblemTopic.LOGIC -> "Logic riddles"
            ProblemTopic.GEOMETRY -> "Geometry"
            ProblemTopic.MONEY -> "Money & shopping"
            ProblemTopic.TIME -> "Clock & time"
            ProblemTopic.WORD -> "Word problems"
            ProblemTopic.COMPARE -> "Comparisons (tap < = >)"
            ProblemTopic.TARGET -> "Number cards"
            ProblemTopic.NUMBERS -> "Numbers & sets"
            ProblemTopic.ESTIMATION -> "Estimating"
        }
    },
    tapPrompt = {
        when (it) {
            ProblemKind.COMPARE -> "Which sign fits between the two sides?"
            ProblemKind.TRUE_FALSE -> "True or false?"
            ProblemKind.MISSING_OP -> "Which sign is hiding behind the ?"
            ProblemKind.ESTIMATE -> "Close is good enough here. No need to work it out exactly."
            else -> ""
        }
    },
    topicDescription = {
        when (it) {
            ProblemTopic.CORE -> "Number chains on Easy, equations above, and tap the missing sign."
            ProblemTopic.PUZZLE -> "Grids where little shapes stand for numbers."
            ProblemTopic.LOGIC -> "Short riddles with a numeric answer."
            ProblemTopic.GEOMETRY -> "Perimeter, area and angles, from Normal up."
            ProblemTopic.MONEY -> "Shopping in euros: totals, change, discounts."
            ProblemTopic.TIME -> "Clocks, durations and minutes."
            ProblemTopic.WORD -> "Little number stories about everyday things."
            ProblemTopic.COMPARE -> "Tap <, = or > between two expressions, plus quick true-or-false checks."
            ProblemTopic.TARGET -> "Tap the cards that add up to the target."
            ProblemTopic.NUMBERS -> "Hunt evens, odds and primes; count A ∩ B."
            ProblemTopic.ESTIMATION -> "Round the awkward numbers and get close without working it out."
        }
    },
    challengeTitle = "Today's challenge",
    challengeStage = { step, total -> "Step $step of $total" },
    challengeContinue = "Continue",
    challengeDoneHeadline = "Challenge complete!",
    challengeDoneBody = {
        if (it == 1) "Your first daily challenge. Bravo!"
        else "That's $it daily challenges solved."
    },
    challengeTomorrow = "A fresh one arrives tomorrow.",
    activeFromTitle = "Active from",
    activeUntilTitle = "Active until",
    personalize = "Personalize",
    personalizeTitle = "Pick your colors",
    paletteName = {
        when (it) {
            AppPalette.CLAY -> "Warm clay"
            AppPalette.OCEAN -> "Ocean"
            AppPalette.FOREST -> "Deep forest"
            AppPalette.HONEY -> "Honey"
            AppPalette.MIDNIGHT -> "Midnight"
        }
    },
    ok = "OK",
    cancel = "Cancel",
    statsTitle = "Your numbers",
    problemsThisWeek = { if (it == 1) "problem this week" else "problems this week" },
    todayCard = "today",
    accuracyCard = "right first try",
    fastestCard = "fastest solve",
    allTimeCard = "all time",
    statsLineToday = "Every little break counts.",
    statsLineWeek = "Fresh day, fresh brain.",
    statsLineFreshWeek = "Fresh week, fresh brain.",
    topicBreakdownTitle = "Right first try, by exercise",
    activityChartTitle = "Your last two weeks",
    soundSection = "Sound",
    successSoundLabel = "Success chime",
    successSoundSubtitle = "A soft ding for right answers. Silent mode always wins.",
)

val RomanianStrings = Strings(
    language = AppLanguage.ROMANIAN,
    locale = Locale.forLanguageTag("ro"),
    solvedToday = { "Rezolvate azi: $it" },
    statsIconDescription = "Cifrele tale",
    settingsIconDescription = "Setări",
    correctFeedback = "Corect!",
    closeEnoughFeedback = { "Destul de aproape! Exact era $it." },
    tryAgainFeedback = "Nu chiar. Mai încearcă o dată!",
    hintButton = { "Indiciu ($it)" },
    skipButton = "Sari peste",
    revealedFeedback = { "Răspunsul era $it.\nMergem la următoarea!" },
    check = "Verifică",
    startButton = "Începe",
    readyLine = "Gata când ești tu.",
    oneMore = "Încă una?",
    practiceTitle = "Exersează",
    practiceIntro = "Alege un tip și un nivel. Aici nu e nicio limită, așa că poți sta cât vrei.",
    practiceButton = "Exersează un tip",
    practiceLevel = "Nivel",
    showSolution = "Arată-mi de ce",
    hideSolution = "Ascunde",
    addWidget = "Adaugă pe ecranul principal",
    widgetTapPrompt = "Apasă pentru o problemă",
    widgetRemindersOff = "Notificările sunt oprite",
    notebookTitle = "Foaie de ajutor",
    notebookSubtitle = "Regulile, formulele și definițiile potrivite problemei.",
    guideTitle = "Bun venit!",
    guideIntro = "Alege cum vrei să arate pauzele tale. Poți schimba orice mai târziu din Setări.",
    presetTitle = {
        when (it) {
            SetupPreset.DEFAULT -> "Echilibrat"
            SetupPreset.RELAXED -> "Relaxat"
            SetupPreset.CHALLENGE -> "Provocare"
        }
    },
    presetBody = {
        when (it) {
            SetupPreset.DEFAULT ->
                "Pauze de 10 probleme. Începe ușor și se adaptează după tine."
            SetupPreset.RELAXED ->
                "Pauze scurte, de 5 probleme, care rămân blânde oricât de bine ți-ar merge."
            SetupPreset.CHALLENGE ->
                "Pauze lungi, de 20 de probleme, care încep la Normal și urcă oricât de sus poți duce."
        }
    },
    quickSetupAction = "Schimbă cum decurg pauzele",
    quickSetupTitle = "Cum să decurgă pauzele?",
    quickSetupIntro = "Alege un stil. Nivelul o ia de la capăt, așa că lasă-i câteva probleme ca să se așeze din nou.",
    snooze15 = "Amână 15 min",
    breakDone = "Gata pauza! Ne vedem la următoarea.",
    anotherRound = "Încă o rundă",
    difficultyLabel = {
        when (it) {
            Difficulty.EASY -> "Ușor"
            Difficulty.MEDIUM -> "Normal"
            Difficulty.HARD -> "Greu"
        }
    },
    settingsTitle = "Setări",
    settingsGroupBreaks = "Când vin pauzele",
    settingsGroupExercises = "Cum arată o pauză",
    settingsGroupApp = "Aplicația în sine",
    back = "Înapoi",
    errorReportAction = "Raportează o problemă",
    errorReportTitle = "Raportează o problemă",
    errorReportBody = "Ceva n-a mers data trecută. Dacă le copiezi și le trimiți mai departe, e singurul fel în care va afla cineva, fiindcă aplicația nu trimite nimic de la sine.",
    errorReportNoneBody = "Până acum n-a mers nimic prost. Dacă se întâmplă, detaliile apar aici ca să le poți trimite mai departe.",
    errorReportCopy = "Copiază detaliile",
    errorReportCopied = "Copiat",
    errorReportClear = "Șterge",
    supportAction = "Sprijină aplicația",
    languageSection = "Limbă",
    remindersSection = "Notificări",
    breakReminders = "Notificări de pauză",
    notificationsOffHint = "Notificările sunt oprite, așa că aplicația nu te poate anunța.",
    turnOnNotifications = "Pornește notificările",
    exactAlarmHint = "Fără accesul „Alarme și memento-uri”, notificările pot întârzia câteva minute.",
    allowExactTiming = "Permite ora exactă",
    nextReminder = { "Următoarea notificare: $it" },
    todayAt = { "azi la $it" },
    tomorrowAt = { "mâine la $it" },
    remindMeEvery = "Amintește-mi la fiecare",
    hourOption = { if (it == 1) "1 oră" else "$it ore" },
    activeHours = "Ore active",
    activeHoursSubtitle = "Notificările sosesc doar în acest interval.",
    fromTime = { "De la $it" },
    untilTime = { "Până la $it" },
    activeDays = "Zile active",
    problemsPerBreak = "Probleme pe pauză",
    problemsPerBreakSubtitle = "După atâtea, aplicația te trimite frumos înapoi la treabă.",
    resetSittingButton = "Începe o rundă nouă",
    noLimit = "Fără limită",
    customEllipsis = "Alt număr…",
    customValue = { "Alt număr: $it" },
    customPlaceholder = "ex: 7",
    startingLevel = "Nivel de pornire",
    startingLevelSubtitle = "Se adaptează din mers: urcă atunci când îți merge bine și coboară după o serie mai grea.",
    difficultyExplanation = {
        when (it) {
            Difficulty.EASY ->
                "Ușor: șiruri de 3–4 numere mici cu toate cele patru operații, puzzle-uri cu forme prietenoase, ghicitori scurte, prețuri de la piață și întrebări cu ceasul."
            Difficulty.MEDIUM ->
                "Normal: ecuații cu x și y, povești de geometrie, de bani și cu ceasul și puzzle-uri mai încuietoare."
            Difficulty.HARD ->
                "Greu: ecuații mai lungi cu 4–7 numere, derivate simple, geometrie mai grea, reduceri și călătorii, cele mai grele puzzle-uri și ghicitori. Cronometrul dă timp dublu."
        }
    },
    exerciseTypesTitle = "Tipuri de exerciții",
    exerciseTypesSummary = { on, total -> "$on din $total pornite" },
    problemTypesSubtitle = "Alege ce tipuri de probleme apar. Cel puțin unul rămâne mereu pornit.",
    topicLevelsHint = "Fiecare tip își găsește nivelul lui: urcă unde mergi repede și coboară unde nu.",
    topicGroupLabel = {
        when (it) {
            TopicGroup.NUMBERS -> "Numere"
            TopicGroup.STORIES -> "Povești de zi cu zi"
            TopicGroup.THINKING -> "Gândire"
        }
    },
    topicLabel = {
        when (it) {
            ProblemTopic.CORE -> "Numere și ecuații"
            ProblemTopic.PUZZLE -> "Puzzle-uri cu forme"
            ProblemTopic.LOGIC -> "Ghicitori de logică"
            ProblemTopic.GEOMETRY -> "Geometrie"
            ProblemTopic.MONEY -> "Bani și cumpărături"
            ProblemTopic.TIME -> "Ceas și timp"
            ProblemTopic.WORD -> "Probleme cu poveste"
            ProblemTopic.COMPARE -> "Comparații (atinge < = >)"
            ProblemTopic.TARGET -> "Cartonașe cu numere"
            ProblemTopic.NUMBERS -> "Numere și mulțimi"
            ProblemTopic.ESTIMATION -> "Estimare"
        }
    },
    tapPrompt = {
        when (it) {
            ProblemKind.COMPARE -> "Ce semn se potrivește între cele două părți?"
            ProblemKind.TRUE_FALSE -> "Adevărat sau fals?"
            ProblemKind.MISSING_OP -> "Ce semn se ascunde în spatele lui ?"
            ProblemKind.ESTIMATE -> "Aproape e destul de bine aici. Nu trebuie calculat exact."
            else -> ""
        }
    },
    topicDescription = {
        when (it) {
            ProblemTopic.CORE -> "Șiruri de numere la Ușor, ecuații mai sus și semnul care lipsește."
            ProblemTopic.PUZZLE -> "Grile în care formele țin locul numerelor."
            ProblemTopic.LOGIC -> "Ghicitori scurte cu răspuns numeric."
            ProblemTopic.GEOMETRY -> "Perimetru, arie și unghiuri, de la Normal în sus."
            ProblemTopic.MONEY -> "Cumpărături în euro: totaluri, rest, reduceri."
            ProblemTopic.TIME -> "Ceasuri, durate și minute."
            ProblemTopic.WORD -> "Povești scurte cu numere din viața de zi cu zi."
            ProblemTopic.COMPARE -> "Atinge <, = sau > între două expresii, plus verificări rapide adevărat-fals."
            ProblemTopic.TARGET -> "Atinge cartonașele care adunate dau ținta."
            ProblemTopic.NUMBERS -> "Vânează pare, impare și prime; numără A ∩ B."
            ProblemTopic.ESTIMATION -> "Rotunjește numerele incomode și ajungi aproape fără să calculezi."
        }
    },
    challengeTitle = "Provocarea zilei",
    challengeStage = { step, total -> "Pasul $step din $total" },
    challengeContinue = "Continuă",
    challengeDoneHeadline = "Provocare rezolvată!",
    challengeDoneBody = {
        when {
            it == 1 -> "Prima ta provocare zilnică. Bravo!"
            it < 20 -> "Ai rezolvat $it provocări zilnice."
            else -> "Ai rezolvat $it de provocări zilnice."
        }
    },
    challengeTomorrow = "Mâine te așteaptă una nouă.",
    activeFromTitle = "Activ de la",
    activeUntilTitle = "Activ până la",
    personalize = "Personalizează",
    personalizeTitle = "Alege-ți culorile",
    paletteName = {
        when (it) {
            AppPalette.CLAY -> "Lut cald"
            AppPalette.OCEAN -> "Ocean"
            AppPalette.FOREST -> "Pădure adâncă"
            AppPalette.HONEY -> "Miere"
            AppPalette.MIDNIGHT -> "Miez de noapte"
        }
    },
    ok = "OK",
    cancel = "Renunță",
    statsTitle = "Cifrele tale",
    problemsThisWeek = {
        when {
            it == 1 -> "problemă săptămâna asta"
            it in 2..19 -> "probleme săptămâna asta"
            else -> "de probleme săptămâna asta"
        }
    },
    todayCard = "azi",
    accuracyCard = "corecte din prima",
    fastestCard = "cea mai rapidă",
    allTimeCard = "în total",
    statsLineToday = "Fiecare pauză mică contează.",
    statsLineWeek = "Zi nouă, minte proaspătă.",
    statsLineFreshWeek = "Săptămână nouă, minte proaspătă.",
    topicBreakdownTitle = "Corecte din prima, pe exerciții",
    activityChartTitle = "Ultimele două săptămâni",
    soundSection = "Sunet",
    successSoundLabel = "Clinchet de reușită",
    successSoundSubtitle = "Un ding blând la răspunsurile corecte. Modul silențios are mereu prioritate.",
)
