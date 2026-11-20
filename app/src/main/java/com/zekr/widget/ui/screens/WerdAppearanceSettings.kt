package com.zekr.widget.ui.screens

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.zekr.widget.ui.theme.WerdSolarCity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.zekr.widget.ui.theme.WerdAccent
import com.zekr.widget.ui.theme.WerdAppearance
import com.zekr.widget.ui.theme.WerdThemeMode
import com.zekr.widget.ui.theme.rememberWerdAppearance
import com.zekr.widget.ui.theme.saveWerdAppearance
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun WerdAppearanceSettings() {
    val context = LocalContext.current
    val appearance = rememberWerdAppearance().value
    val light = MaterialTheme.colorScheme.surface.luminance() > .5f
    val use24HourClock = DateFormat.is24HourFormat(context)
    val formatter = DateTimeFormatter.ofPattern(if (use24HourClock) "HH:mm" else "h:mm a", Locale.getDefault())

    fun pickTime(minute: Int, onPick: (Int) -> Unit) {
        TimePickerDialog(
            context,
            if (light) android.R.style.Theme_Material_Light_Dialog_Alert else android.R.style.Theme_Material_Dialog_Alert,
            { _, hour, minutes -> onPick(hour * 60 + minutes) },
            minute / 60,
            minute % 60,
            use24HourClock
        ).show()
    }

    ModernSectionCard(title = "المظهر والتخصيص") {
        Column(Modifier.selectableGroup()) {
            WerdThemeMode.entries.forEach { mode ->
                val selected = appearance.mode == mode
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .selectable(selected, role = Role.RadioButton) {
                            saveWerdAppearance(context, appearance.copy(mode = mode))
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(mode.title)
                }
            }
        }
        if (appearance.mode == WerdThemeMode.Solar) {
            var showCities by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { showCities = true }) { Text("المدينة · ${appearance.solarCity.title}") }
                DropdownMenu(expanded = showCities, onDismissRequest = { showCities = false }) {
                    WerdSolarCity.entries.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city.title) },
                            onClick = {
                                showCities = false
                                saveWerdAppearance(context, appearance.copy(solarCity = city))
                            }
                        )
                    }
                }
            }
        }
        if (appearance.mode == WerdThemeMode.Schedule) {
            val start = LocalTime.ofSecondOfDay(appearance.nightStart * 60L).format(formatter)
            val end = LocalTime.ofSecondOfDay(appearance.nightEnd * 60L).format(formatter)
            TextButton(onClick = {
                pickTime(appearance.nightStart) { saveWerdAppearance(context, appearance.copy(nightStart = it)) }
            }) { Text("بداية الليل · $start") }
            TextButton(onClick = {
                pickTime(appearance.nightEnd) { saveWerdAppearance(context, appearance.copy(nightEnd = it)) }
            }) { Text("نهاية الليل · $end") }
            if (appearance.nightStart == appearance.nightEnd) {
                Text("اختر وقتين مختلفين لتفعيل الفترة الليلية", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("لون التمييز", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WerdAccent.entries.forEach { accent ->
                FilterChip(
                    selected = appearance.accent == accent,
                    onClick = { saveWerdAppearance(context, appearance.copy(accent = accent)) },
                    label = { Text(accent.title) }
                )
            }
        }
        TextButton(onClick = { saveWerdAppearance(context, WerdAppearance()) }) {
            Text("استعادة المظهر الافتراضي")
        }
    }
}
