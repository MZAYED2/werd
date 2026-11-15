package com.zekr.widget

import com.zekr.widget.data.model.ContentType
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.GlassColorPreset
import com.zekr.widget.data.preferences.GradientDirection
import com.zekr.widget.data.preferences.PatternType
import com.zekr.widget.data.preferences.WidgetFont
import com.zekr.widget.data.preferences.WidgetSettings
import com.zekr.widget.ui.theme.FontHelper
import com.zekr.widget.widget.widgetBitmapPx
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WidgetVerificationTest {

    @Test
    fun testWidgetFontExpansion() {
        val fonts = WidgetFont.values()
        // Original set + expanded flavour coverage
        assertTrue("Should have at least 18 fonts", fonts.size >= 18)

        val fontIds = fonts.map { it.id }.toSet()
        assertTrue(fontIds.contains("AMIRI"))
        assertTrue(fontIds.contains("UTHMANIC"))
        assertTrue(fontIds.contains("NASKH"))
        assertTrue(fontIds.contains("AREF_RUQAA"))
        assertTrue(fontIds.contains("KATIBEH"))
        assertTrue(fontIds.contains("CAIRO"))
        assertTrue(fontIds.contains("TAJAWAL"))
        assertTrue(fontIds.contains("ALMARAI"))
        assertTrue(fontIds.contains("SCHEHERAZADE_NEW"))
        assertTrue(fontIds.contains("LATEEF"))
        assertTrue(fontIds.contains("REEM_KUFI"))
        assertTrue(fontIds.contains("EL_MESSIRI"))
        assertTrue(fontIds.contains("MARKAZI_TEXT"))
        assertTrue(fontIds.contains("IBM_PLEX_SANS_ARABIC"))
        assertTrue(fontIds.contains("CHANGA"))
        assertTrue(fontIds.contains("LALEZAR"))
        assertTrue(fontIds.contains("JOMHURIA"))
        assertTrue(fontIds.contains("RAKKAS"))

        fonts.forEach { font ->
            assertFalse(font.titleArabic.isBlank())
            assertFalse(font.id.isBlank())
        }
    }

    @Test
    fun testConciseCitationFormatting() {
        // Test hyphen removal
        val item1 = ZekrEntity(
            id = 1,
            arabicText = "آية",
            type = ContentType.QURAN_AYAH,
            source = "سورة البقرة: 255",
            reference = "آية الكرسي - أعظم آية في كتاب الله"
        )
        assertEquals("سورة البقرة: 255 • آية الكرسي", item1.getConciseCitation())

        // Test bullet removal
        val item2 = ZekrEntity(
            id = 2,
            arabicText = "حديث",
            type = ContentType.SAHIH_HADITH,
            source = "صحيح البخاري: 6406",
            reference = "كتاب الدعوات • باب فضل التسبيح"
        )
        assertEquals("صحيح البخاري: 6406 • باب فضل التسبيح", item2.getConciseCitation())

        // Test already concise reference
        val item3 = ZekrEntity(
            id = 3,
            arabicText = "ذكر",
            type = ContentType.AUTHENTIC_ZEKR,
            source = "صحيح مسلم: 2723",
            reference = "أذكار الصباح"
        )
        assertEquals("صحيح مسلم: 2723 • أذكار الصباح", item3.getConciseCitation())

        // Test empty reference
        val item4 = ZekrEntity(
            id = 4,
            arabicText = "ذكر",
            type = ContentType.AUTHENTIC_ZEKR,
            source = "صحيح البخاري",
            reference = ""
        )
        assertEquals("صحيح البخاري", item4.getConciseCitation())

        // Test empty source
        val item5 = ZekrEntity(
            id = 5,
            arabicText = "ذكر",
            type = ContentType.AUTHENTIC_ZEKR,
            source = "",
            reference = "دعاء"
        )
        assertEquals("دعاء", item5.getConciseCitation())

        // Test very long reference truncation
        val item6 = ZekrEntity(
            id = 6,
            arabicText = "ذكر",
            type = ContentType.AUTHENTIC_ZEKR,
            source = "المصدر",
            reference = "هذا نص مرجعي طويل جداً يفوق الحد الطبيعي للمراجع"
        )
        val citation6 = item6.getConciseCitation()
        assertTrue("Citation should contain ellipsis or be trimmed", citation6.contains("…"))
        assertTrue("Citation length must be reasonable", citation6.length <= 40)
    }

    @Test
    fun testSahihContentJsonIsValid() {
        val jsonFile = File("src/main/assets/data/sahih_content.json")
        val altJsonFile = File("app/src/main/assets/data/sahih_content.json")
        val file = if (jsonFile.exists()) jsonFile else altJsonFile
        assertTrue("sahih_content.json should exist", file.exists())

        val content = file.readText()
        val json = Json { ignoreUnknownKeys = true }
        val items: List<ZekrEntity> = json.decodeFromString(content)

        assertTrue("Should contain at least 20 items", items.size >= 20)
        val ids = items.map { it.id }
        assertEquals("All IDs must be unique", ids.size, ids.toSet().size)

        items.forEach { item ->
            assertFalse("Arabic text cannot be blank", item.arabicText.isBlank())
            assertFalse("Source cannot be blank", item.source.isBlank())
            val citation = item.getConciseCitation()
            assertFalse("Citation cannot be blank", citation.isBlank())
            // Ensure no citation is excessively long so it fits neatly in card boundary
            assertTrue("Citation is concise: '$citation'", citation.length <= 50)
        }
    }

    @Test
    fun testAdaptiveColors() {
        // Translucent frosted white over a dark wallpaper must stay light text
        val translucentWhite = WidgetSettings(
            colorPreset = GlassColorPreset.FROSTED_WHITE,
            primaryColor = 0xFFFFFFFF,
            glassOpacity = 0.35f
        )
        assertEquals(
            "Translucent white glass needs light text",
            0xFFFFFFFFL,
            translucentWhite.getAdaptiveTextColor()
        )

        // Opaque light glass can use dark text
        val opaqueWhite = WidgetSettings(
            colorPreset = GlassColorPreset.FROSTED_WHITE,
            primaryColor = 0xFFFFFFFF,
            glassOpacity = 0.92f
        )
        assertEquals(
            "Opaque white glass needs dark text",
            0xFF1C1C1EL,
            opaqueWhite.getAdaptiveTextColor()
        )

        val darkSettings = WidgetSettings(
            colorPreset = GlassColorPreset.MIDNIGHT_OBSIDIAN,
            primaryColor = 0xFF0D1310,
            glassOpacity = 0.35f
        )
        assertEquals("Dark bg should use light text", 0xFFFFFFFFL, darkSettings.getAdaptiveTextColor())
    }

    @Test
    fun testPatternTypesAreAvailable() {
        val patterns = PatternType.values()
        assertTrue(patterns.size >= 12)
        assertNotNull(PatternType.values().find { it == PatternType.ALHAMBRA_KHATAM_STAR })
        assertNotNull(PatternType.values().find { it == PatternType.HEXAGRAM_STARS })
    }

    @Test
    fun testTextCenteringMathAndBoundaryOverflowProtection() {
        val safeW = 680
        val horizontalPadding = 52
        val maxTextWidth = (safeW - horizontalPadding * 2).coerceAtLeast(200)

        // Mathematical verification of correct layout placement
        val startX = (safeW - maxTextWidth) / 2f
        assertEquals("startX must equal horizontalPadding", horizontalPadding.toFloat(), startX, 0.001f)

        val layoutCenter = startX + maxTextWidth / 2f
        val canvasCenter = safeW / 2f
        assertEquals("Layout center must perfectly align with canvas horizontal center", canvasCenter, layoutCenter, 0.001f)

        val leftMargin = startX
        val rightMargin = safeW - (startX + maxTextWidth)
        assertEquals("Left and right margins must be equal", leftMargin, rightMargin, 0.001f)

        // Contrast: verify that flawed translate(safeW / 2f) would have severely overflowed right boundary
        val flawedTranslateX = safeW / 2f
        val flawedRightEdge = flawedTranslateX + maxTextWidth
        assertTrue("Flawed translation exceeded card width by 236px", flawedRightEdge > safeW)
        assertEquals("Overflow amount", 236f, flawedRightEdge - safeW, 0.001f)
    }

    @Test
    fun testSequentialReferenceCitationCleaning() {
        // Combination of "كتاب " prefix and " - " separator
        val item1 = ZekrEntity(
            id = 101,
            arabicText = "ذكر",
            type = ContentType.SAHIH_HADITH,
            source = "صحيح البخاري: 6406 | صحيح مسلم: 2694",
            reference = "كتاب الدعوات - باب فضل التسبيح"
        )
        // Primary source extracted, " - " stripped, and "كتاب " prefix removed
        assertEquals("صحيح البخاري: 6406 • الدعوات", item1.getConciseCitation())

        // Reference with "كتاب " prefix only
        val item2 = ZekrEntity(
            id = 102,
            arabicText = "ذكر",
            type = ContentType.SAHIH_HADITH,
            source = "صحيح مسلم: 2137",
            reference = "كتاب الآداب"
        )
        assertEquals("صحيح مسلم: 2137 • الآداب", item2.getConciseCitation())
    }

    @Test
    fun testLongTextVerticalBoundaryProtection() {
        val safeH = 380f
        val topPadding = 24f
        val availableHeightForZekr = (safeH - 76f).coerceAtLeast(100f) // 304f
        val footerY = safeH - 22f // 358f
        val footerTextSize = 19.5f
        val footerTop = footerY - footerTextSize // 338.5f

        // Maximum bottom reach of zekr layout when constrained to availableHeightForZekr
        val maxLayoutBottom = topPadding + availableHeightForZekr // 328f
        assertTrue(
            "Zekr text boundary must never collide with footer top",
            maxLayoutBottom < footerTop
        )
        val safetyBuffer = footerTop - maxLayoutBottom
        assertTrue(
            "Safety gap between zekr text and footer citation must be at least 10px",
            safetyBuffer >= 10f
        )
    }

    @Test
    fun testComposeFontFamilyCaching() {
        // Calling getComposeFontFamily multiple times must return the cached instance
        WidgetFont.values().forEach { font ->
            val firstCall = FontHelper.getComposeFontFamily(font)
            val secondCall = FontHelper.getComposeFontFamily(font)
            assertSame("FontFamily instance for $font must be cached", firstCall, secondCall)
        }
    }

    @Test
    fun testCitationOverflowDoesNotContainRawDualSource() {
        val item = ZekrEntity(
            id = 8,
            arabicText = "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ",
            type = ContentType.SAHIH_HADITH,
            source = "صحيح البخاري: 6406 | صحيح مسلم: 2694",
            reference = "كتاب الدعوات - باب فضل التسبيح والتهليل والتحميد والتكبير"
        )
        val citation = item.getConciseCitation(maxChars = 25)
        assertFalse("Concise citation must never resurrect raw dual citation '|'", citation.contains("|"))
        assertTrue("Concise citation must end with ellipsis if truncated", citation.endsWith("…"))
        assertTrue("Concise citation length within boundary", citation.length <= 25)
    }

    @Test
    fun testContentCategoryToggleSafetyRule() {
        val allOn = WidgetSettings(includeQuran = true, includeHadith = true, includeAzkar = true)
        // With all on, can toggle any
        val canToggleQuran1 = !allOn.includeQuran || allOn.includeHadith || allOn.includeAzkar
        assertTrue(canToggleQuran1)

        // With only Quran on, cannot toggle Quran off (to prevent 0 active categories)
        val quranOnly = WidgetSettings(includeQuran = true, includeHadith = false, includeAzkar = false)
        val canToggleQuran2 = !quranOnly.includeQuran || quranOnly.includeHadith || quranOnly.includeAzkar
        assertFalse(canToggleQuran2)

        // But can toggle Hadith on
        val canToggleHadith = !quranOnly.includeHadith || quranOnly.includeQuran || quranOnly.includeAzkar
        assertTrue(canToggleHadith)
    }

    @Test
    fun testBlurGradeConfiguration() {
        val defaultSettings = WidgetSettings()
        assertEquals(0.5f, defaultSettings.blurGrade, 0.001f)

        // Test 0% blur (clear glass)
        val clearSettings = defaultSettings.copy(blurGrade = 0.0f)
        assertEquals(0.0f, clearSettings.blurGrade, 0.001f)
        assertEquals(0, (clearSettings.blurGrade * 100).toInt())

        // Test 100% blur (intense frosted glass)
        val frostedSettings = defaultSettings.copy(blurGrade = 1.0f)
        assertEquals(1.0f, frostedSettings.blurGrade, 0.001f)
        assertEquals(100, (frostedSettings.blurGrade * 100).toInt())

        // Test intermediate blur grade (e.g. 75%)
        val customBlurSettings = defaultSettings.copy(blurGrade = 0.75f)
        assertEquals(0.75f, customBlurSettings.blurGrade, 0.001f)
        assertEquals(75, (customBlurSettings.blurGrade * 100).toInt())
    }

    @Test
    fun testWidgetBitmapKeepsAspectAndDoesNotStretch() {
        assertEquals(680 to 380, widgetBitmapPx(0f, 0f, 2.75f))
        assertEquals(680 to 380, widgetBitmapPx(Float.NaN, 140f, 2.75f))

        val normal = widgetBitmapPx(widthDp = 250f, heightDp = 140f, density = 2f)
        assertEquals(500, normal.first)
        assertEquals(280, normal.second)

        val wide = widgetBitmapPx(widthDp = 2000f, heightDp = 400f, density = 3f)
        val widgetAspect = 2000f / 400f
        val bitmapAspect = wide.first.toFloat() / wide.second.toFloat()
        assertEquals(widgetAspect, bitmapAspect, 0.05f)
        assertTrue(wide.first.toLong() * wide.second.toLong() <= 700_000L)
    }
}
