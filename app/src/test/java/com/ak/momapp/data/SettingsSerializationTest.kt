package com.ak.momapp.data

import com.ak.momapp.i18n.AppLanguage
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.ui.theme.AppPalette
import com.ak.momapp.ui.theme.UiSkin
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSerializationTest {

    @Test
    fun `days round-trip through encoding`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY)
        assertEquals(days, SettingsSerialization.decodeDays(SettingsSerialization.encodeDays(days)))
    }

    @Test
    fun `null days means never set and yields the Mon-Fri default`() {
        assertEquals(BrainBreakSettings.DEFAULT_ACTIVE_DAYS, SettingsSerialization.decodeDays(null))
    }

    @Test
    fun `empty days string is a deliberate empty selection`() {
        assertTrue(SettingsSerialization.decodeDays("").isEmpty())
    }

    @Test
    fun `garbage day values are ignored`() {
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            SettingsSerialization.decodeDays("1,notaday,99,0,5"),
        )
    }

    @Test
    fun `difficulty decodes by name with fallback`() {
        assertEquals(Difficulty.HARD, SettingsSerialization.decodeDifficulty("HARD"))
        assertEquals(Difficulty.EASY, SettingsSerialization.decodeDifficulty(null))
        assertEquals(Difficulty.MEDIUM, SettingsSerialization.decodeDifficulty("bogus", Difficulty.MEDIUM))
    }

    @Test
    fun `stored expert difficulty from before the rewrite folds into hard`() {
        assertEquals(Difficulty.HARD, SettingsSerialization.decodeDifficulty("EXPERT"))
    }

    @Test
    fun `language follows the phone before the user picks, then sticks`() {
        // Never set: the system locale decides.
        assertEquals(AppLanguage.ROMANIAN, SettingsSerialization.decodeLanguage(null, "ro"))
        assertEquals(AppLanguage.ENGLISH, SettingsSerialization.decodeLanguage(null, "en"))
        assertEquals(AppLanguage.ENGLISH, SettingsSerialization.decodeLanguage(null, "de"))
        // A saved choice always wins over the locale.
        assertEquals(AppLanguage.ENGLISH, SettingsSerialization.decodeLanguage("ENGLISH", "ro"))
        assertEquals(AppLanguage.ROMANIAN, SettingsSerialization.decodeLanguage("ROMANIAN", "en"))
    }

    @Test
    fun `palette decodes by name with warm clay as the fallback`() {
        assertEquals(AppPalette.MIDNIGHT, SettingsSerialization.decodePalette("MIDNIGHT"))
        assertEquals(AppPalette.CLAY, SettingsSerialization.decodePalette(null))
        assertEquals(AppPalette.CLAY, SettingsSerialization.decodePalette("bogus"))
    }

    @Test
    fun `topics round-trip through encoding`() {
        val topics = setOf(ProblemTopic.CORE, ProblemTopic.MONEY, ProblemTopic.TIME)
        assertEquals(topics, SettingsSerialization.decodeTopics(SettingsSerialization.encodeTopics(topics)))
    }

    @Test
    fun `topics are stored as the switched-off set so future topics default on`() {
        // Everything on encodes to an empty string: a topic added in a
        // later version is absent from it and therefore lands enabled.
        assertEquals("", SettingsSerialization.encodeTopics(ProblemTopic.ALL))
        assertEquals(
            ProblemTopic.ALL - ProblemTopic.LOGIC,
            SettingsSerialization.decodeTopics("LOGIC"),
        )
    }

    @Test
    fun `null topics means never set and yields all on`() {
        assertEquals(ProblemTopic.ALL, SettingsSerialization.decodeTopics(null))
    }

    @Test
    fun `garbage disabled values are ignored and everything stays on`() {
        assertEquals(ProblemTopic.ALL, SettingsSerialization.decodeTopics(""))
        assertEquals(ProblemTopic.ALL, SettingsSerialization.decodeTopics("bogus,also-bogus"))
        assertEquals(
            ProblemTopic.ALL - ProblemTopic.LOGIC,
            SettingsSerialization.decodeTopics("LOGIC,bogus"),
        )
    }

    @Test
    fun `one topic may stand alone but zero enabled falls back to all on`() {
        val oneOn = SettingsSerialization.encodeTopics(setOf(ProblemTopic.LOGIC))
        assertEquals(setOf(ProblemTopic.LOGIC), SettingsSerialization.decodeTopics(oneOn))
        val allOff = SettingsSerialization.encodeTopics(emptySet())
        assertEquals(ProblemTopic.ALL, SettingsSerialization.decodeTopics(allOff))
    }

    @Test
    fun `a v2 legacy set carries over with numbers switched on`() {
        assertEquals(
            setOf(ProblemTopic.LOGIC, ProblemTopic.PUZZLE, ProblemTopic.NUMBERS),
            SettingsSerialization.decodeTopics(null, "LOGIC,PUZZLE"),
        )
    }

    @Test
    fun `a v1 legacy set carries over with all later topics on`() {
        assertEquals(
            setOf(
                ProblemTopic.LOGIC, ProblemTopic.PUZZLE,
                ProblemTopic.COMPARE, ProblemTopic.TARGET, ProblemTopic.NUMBERS,
            ),
            SettingsSerialization.decodeTopics(null, null, "LOGIC,PUZZLE"),
        )
    }

    @Test
    fun `garbage legacy topics still fall back to all on`() {
        assertEquals(ProblemTopic.ALL, SettingsSerialization.decodeTopics(null, "bogus"))
        assertEquals(ProblemTopic.ALL, SettingsSerialization.decodeTopics(null, null, "bogus"))
    }

    @Test
    fun `the current encoding wins over both legacy ones`() {
        assertEquals(
            ProblemTopic.ALL - ProblemTopic.GEOMETRY,
            SettingsSerialization.decodeTopics("GEOMETRY", "LOGIC,PUZZLE", "MONEY,TIME"),
        )
    }

    @Test
    fun `the v2 legacy encoding wins over v1`() {
        assertEquals(
            setOf(ProblemTopic.MONEY, ProblemTopic.TIME, ProblemTopic.NUMBERS),
            SettingsSerialization.decodeTopics(null, "MONEY,TIME", "LOGIC,PUZZLE"),
        )
    }

    @Test
    fun `an unset or unknown skin means modern`() {
        assertEquals(UiSkin.MODERN, SettingsSerialization.decodeSkin(null))
        assertEquals(UiSkin.MODERN, SettingsSerialization.decodeSkin(""))
        assertEquals(UiSkin.MODERN, SettingsSerialization.decodeSkin("SPARKLY"))
    }

    @Test
    fun `a stored skin round trips`() {
        assertEquals(UiSkin.LEGACY, SettingsSerialization.decodeSkin("LEGACY"))
        assertEquals(UiSkin.MODERN, SettingsSerialization.decodeSkin("MODERN"))
    }
}
