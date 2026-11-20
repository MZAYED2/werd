package com.zekr.widget

import com.zekr.widget.ui.theme.WerdAppearance
import com.zekr.widget.ui.theme.WerdThemeMode
import com.zekr.widget.ui.theme.WerdSolarCity
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WerdAppearanceTest {
    @Test
    fun systemAppearanceFollowsSystemState() {
        val appearance = WerdAppearance(mode = WerdThemeMode.System)

        assertTrue(appearance.isDark(systemDark = true, minute = 12 * 60))
        assertFalse(appearance.isDark(systemDark = false, minute = 12 * 60))
    }

    @Test
    fun explicitModesOverrideSystem() {
        assertTrue(WerdAppearance(mode = WerdThemeMode.Dark).isDark(false, 12 * 60))
        assertFalse(WerdAppearance(mode = WerdThemeMode.Light).isDark(true, 0))
    }

    @Test
    fun overnightScheduleIncludesStartAndExcludesEnd() {
        val appearance = WerdAppearance(mode = WerdThemeMode.Schedule, nightStart = 1140, nightEnd = 420)

        assertFalse(appearance.isDark(false, 1139))
        assertTrue(appearance.isDark(false, 1140))
        assertTrue(appearance.isDark(false, 0))
        assertTrue(appearance.isDark(false, 419))
        assertFalse(appearance.isDark(false, 420))
    }

    @Test
    fun citySolarAppearanceUsesCoordinatesAtTheSameInstant() {
        val appearance = WerdAppearance(mode = WerdThemeMode.Solar, solarCity = WerdSolarCity.Cairo)
        val utc = Instant.parse("2026-11-19T05:30:00Z")

        assertFalse(appearance.isDark(systemDark = true, minute = 0, instant = utc))
        assertTrue(appearance.copy(solarCity = WerdSolarCity.NewYork).isDark(false, 0, utc))
    }

    @Test
    fun sameDayAndEmptySchedulesAreHandled() {
        val appearance = WerdAppearance(mode = WerdThemeMode.Schedule, nightStart = 300, nightEnd = 480)

        assertFalse(appearance.isDark(false, 299))
        assertTrue(appearance.isDark(false, 300))
        assertFalse(appearance.isDark(false, 480))
        assertFalse(appearance.copy(nightEnd = 300).isDark(false, 300))
    }
}
