package com.zekr.widget.ui.theme

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class WerdThemeMode(val title: String) {
    System("اتباع النظام"), Light("نهاري"), Dark("ليلي"),
    Solar("حسب الشروق والغروب"), Schedule("جدول مخصص")
}

enum class WerdSolarCity(val title: String, val latitude: Double, val longitude: Double) {
    Cairo("القاهرة", 30.0444, 31.2357),
    Mecca("مكة المكرمة", 21.3891, 39.8579),
    Dubai("دبي", 25.2048, 55.2708),
    Amman("عمّان", 31.9539, 35.9106),
    Casablanca("الدار البيضاء", 33.5731, -7.5898),
    London("لندن", 51.5074, -0.1278),
    NewYork("نيويورك", 40.7128, -74.0060)
}

enum class WerdAccent(val title: String) {
    Jade("يشم"), StrongSelection("اختيار أوضح")
}

data class WerdAppearance(
    val mode: WerdThemeMode = WerdThemeMode.Dark,
    val accent: WerdAccent = WerdAccent.Jade,
    val nightStart: Int = 19 * 60,
    val nightEnd: Int = 7 * 60,
    val solarCity: WerdSolarCity = WerdSolarCity.Cairo
) {
    fun isDark(systemDark: Boolean, minute: Int, instant: Instant = Instant.now()): Boolean = when (mode) {
        WerdThemeMode.System -> systemDark
        WerdThemeMode.Dark -> true
        WerdThemeMode.Light -> false
        WerdThemeMode.Solar -> isAfterSunset(instant, solarCity)
        WerdThemeMode.Schedule -> when {
            nightStart == nightEnd -> false
            nightStart < nightEnd -> minute in nightStart until nightEnd
            else -> minute >= nightStart || minute < nightEnd
        }
    }
}

// NOAA solar zenith approximation. UTC and the selected city coordinates keep the cycle
// independent of the device time zone. 90.833 degrees includes standard refraction.
private fun isAfterSunset(instant: Instant, city: WerdSolarCity): Boolean {
    val utc = instant.atOffset(ZoneOffset.UTC)
    val yearLength = if (utc.toLocalDate().isLeapYear) 366 else 365
    val hours = utc.hour + utc.minute / 60.0 + utc.second / 3600.0
    val yearAngle = 2 * PI / yearLength * (utc.dayOfYear - 1 + (hours - 12) / 24)
    val equationOfTime = 229.18 * (
        0.000075 + 0.001868 * cos(yearAngle) - 0.032077 * sin(yearAngle) -
            0.014615 * cos(2 * yearAngle) - 0.040849 * sin(2 * yearAngle)
        )
    val declination = 0.006918 - 0.399912 * cos(yearAngle) + 0.070257 * sin(yearAngle) -
        0.006758 * cos(2 * yearAngle) + 0.000907 * sin(2 * yearAngle) -
        0.002697 * cos(3 * yearAngle) + 0.00148 * sin(3 * yearAngle)
    val solarMinutes = hours * 60 + equationOfTime + 4 * city.longitude
    val hourAngle = Math.toRadians(solarMinutes / 4 - 180)
    val latitude = Math.toRadians(city.latitude)
    val cosineZenith = sin(latitude) * sin(declination) + cos(latitude) * cos(declination) * cos(hourAngle)
    return cosineZenith < cos(Math.toRadians(90.833))
}

@Composable
fun rememberWerdDarkTheme(appearance: WerdAppearance): Boolean {
    var instant by remember { mutableStateOf(Instant.now()) }
    val context = LocalContext.current
    if (appearance.mode == WerdThemeMode.Schedule || appearance.mode == WerdThemeMode.Solar) {
        DisposableEffect(context) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    instant = Instant.now()
                }
            }
            val filter = IntentFilter(Intent.ACTION_TIME_TICK).apply {
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            instant = Instant.now()
            onDispose { context.unregisterReceiver(receiver) }
        }
    }
    val local = instant.atZone(ZoneId.systemDefault())
    return appearance.isDark(isSystemInDarkTheme(), local.hour * 60 + local.minute, instant)
}

fun loadWerdAppearance(context: Context): WerdAppearance =
    readAppearance(context.getSharedPreferences("werd_appearance", Context.MODE_PRIVATE))

private fun readAppearance(prefs: android.content.SharedPreferences) = WerdAppearance(
    mode = WerdThemeMode.entries.firstOrNull { it.name == prefs.getString("mode", null) }
        ?: WerdThemeMode.Dark,
    accent = WerdAccent.entries.firstOrNull { it.name == prefs.getString("accent", null) }
        ?: WerdAccent.Jade,
    nightStart = prefs.getInt("night_start", 19 * 60).coerceIn(0, 1439),
    nightEnd = prefs.getInt("night_end", 7 * 60).coerceIn(0, 1439),
    solarCity = WerdSolarCity.entries.firstOrNull { it.name == prefs.getString("solar_city", null) }
        ?: WerdSolarCity.Cairo
)

@Composable
fun rememberWerdAppearance(): MutableState<WerdAppearance> {
    val context = LocalContext.current.applicationContext
    val prefs = remember(context) { context.getSharedPreferences("werd_appearance", Context.MODE_PRIVATE) }
    fun loadAppearance() = readAppearance(prefs)
    val state = remember(prefs) { mutableStateOf(loadAppearance()) }
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            state.value = loadAppearance()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

fun saveWerdAppearance(context: Context, appearance: WerdAppearance) {
    context.getSharedPreferences("werd_appearance", Context.MODE_PRIVATE).edit()
        .putString("mode", appearance.mode.name)
        .putString("accent", appearance.accent.name)
        .putInt("night_start", appearance.nightStart)
        .putInt("night_end", appearance.nightEnd)
        .putString("solar_city", appearance.solarCity.name)
        .apply()
}
