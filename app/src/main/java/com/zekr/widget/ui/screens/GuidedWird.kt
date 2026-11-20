package com.zekr.widget.ui.screens

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zekr.widget.data.model.ContentType
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.ui.theme.sourceTextColor
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.repository.ZekrRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private enum class WirdGroup(val title: String) {
    Morning("الصباح"), Evening("المساء"), Sleep("النوم"), Waking("الاستيقاظ"),
    Quran("آيات وتدبر"), Selected("أذكار مختارة");

    fun includes(item: ZekrEntity): Boolean {
        if (item.isCustom) return false
        return when (this) {
            Morning -> item.category in setOf("أذكار الصباح", "أذكار الصباح والمساء")
            Evening -> item.category in setOf("أذكار المساء", "أذكار الصباح والمساء")
            Sleep -> item.category == "أذكار النوم"
            Waking -> item.category == "أذكار الاستيقاظ" || item.reference == "ذكر الاستيقاظ من النوم"
            Quran -> item.type == ContentType.QURAN_AYAH
            Selected -> item.category == "عام" && item.type == ContentType.AUTHENTIC_ZEKR
        }
    }
}

@Serializable
private data class GuidedProgress(
    val currentItem: Int? = null,
    val repetitions: Map<Int, Int> = emptyMap(),
    val completedSessions: Int = 0,
    val finished: Boolean = false
)

private fun ZekrEntity.guidedTarget() = getEffectiveDocumentedCount()?.coerceAtLeast(1) ?: 1

@Composable
internal fun GuidedWirdCard(allItems: List<ZekrEntity>, openQuran: Boolean, onQuranOpened: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember(context) { WidgetPreferencesRepository(context) }
    val prefs = remember(context) {
        context.applicationContext.getSharedPreferences("werd_guided_sessions", Context.MODE_PRIVATE)
    }
    val progress = remember(prefs) {
        mutableStateMapOf<WirdGroup, GuidedProgress>().apply {
            WirdGroup.entries.forEach { group ->
                val saved = prefs.getString(group.name, null)
                put(group, saved?.let { runCatching { Json.decodeFromString<GuidedProgress>(it) }.getOrNull() }
                    ?: GuidedProgress())
            }
        }
    }
    var selected by rememberSaveable { mutableStateOf<WirdGroup?>(null) }
    val groups = remember(allItems) {
        WirdGroup.entries.associateWith { group -> allItems.filter(group::includes) }
    }

    fun save(group: WirdGroup, value: GuidedProgress) {
        progress[group] = value
        prefs.edit().putString(group.name, Json.encodeToString(value)).apply()
    }

    fun open(group: WirdGroup) {
        val first = groups.getValue(group).firstOrNull() ?: return
        val previous = progress.getValue(group)
        if (previous.currentItem == null || previous.finished) {
            save(group, GuidedProgress(currentItem = first.id, completedSessions = previous.completedSessions))
        }
        selected = group
    }

    LaunchedEffect(openQuran, groups) {
        if (openQuran && groups.getValue(WirdGroup.Quran).isNotEmpty()) {
            open(WirdGroup.Quran)
            onQuranOpened()
        }
    }

    ModernSectionCard(title = "الورد الموجّه") {
        val total = progress.values.sumOf { it.completedSessions }
        Text("جلسات مكتملة: ${ZekrRepository.toArabicDigits(total)}", style = MaterialTheme.typography.bodyMedium)
        val group = selected
        if (group == null) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WirdGroup.entries.forEach { option ->
                    val available = groups.getValue(option).isNotEmpty()
                    val resume = progress.getValue(option).let { it.currentItem != null && !it.finished }
                    OutlinedButton(
                        onClick = { open(option) },
                        enabled = available,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(if (resume) "استكمال ${option.title}" else option.title) }
                }
            }
        } else {
            val items = groups.getValue(group)
            val state = progress.getValue(group)
            val index = items.indexOfFirst { it.id == state.currentItem }.coerceAtLeast(0)
            val item = items.getOrNull(index)
            if (item == null || state.finished) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (state.finished) "تقبل الله · اكتملت جلسة ${group.title} بنجاح"
                        else "لا توجد أذكار متاحة لهذه الجلسة",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.finished && items.isNotEmpty()) {
                            Button(onClick = {
                                save(group, GuidedProgress(currentItem = items.first().id, completedSessions = state.completedSessions, finished = false))
                            }) { Text("بدء الجلسة من جديد") }
                        }
                        OutlinedButton(onClick = { selected = null }) { Text("العودة إلى الجلسات") }
                    }
                }
            } else {
                val completed = items.count { (state.repetitions[it.id] ?: 0) >= it.guidedTarget() }
                val target = item.guidedTarget()
                val read = (state.repetitions[item.id] ?: 0).coerceIn(0, target)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${group.title} · ${ZekrRepository.toArabicDigits(completed)} من ${ZekrRepository.toArabicDigits(items.size)}")
                    LinearProgressIndicator(
                        progress = { completed.toFloat() / items.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(item.arabicText, fontSize = 22.sp, lineHeight = 36.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                    if (item.source.isNotBlank()) {
                        Text(item.source, color = sourceTextColor)
                    }
                    if (target > 1) Text("التكرار · ${ZekrRepository.toArabicDigits(read)} من ${ZekrRepository.toArabicDigits(target)}")
                    Button(
                        onClick = {
                            val updated = state.repetitions + (item.id to (read + 1).coerceAtMost(target))
                            val ordered = items.drop(index + 1) + items.take(index)
                            val next = if (read + 1 < target) item else {
                                ordered.firstOrNull { (updated[it.id] ?: 0) < it.guidedTarget() }
                            }
                            val done = next == null
                            if (read + 1 >= target || done) {
                                scope.launch {
                                    repo.recordCompletedReadItem(item.type)
                                }
                            }
                            save(group, state.copy(
                                currentItem = next?.id ?: item.id,
                                repetitions = updated,
                                completedSessions = state.completedSessions + if (done) 1 else 0,
                                finished = done
                            ))
                        },
                        enabled = read < target,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (target > 1 && read + 1 < target) "قرأت مرة" else "إكمال الذكر") }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = {
                            save(group, state.copy(currentItem = items[index - 1].id))
                        }, enabled = index > 0) { Text("السابق") }
                        TextButton(onClick = { selected = null }) { Text("حفظ وخروج") }
                        TextButton(onClick = {
                            save(group, state.copy(currentItem = items[index + 1].id))
                        }, enabled = index < items.lastIndex) { Text("التالي") }
                    }
                }
            }
        }
    }
}
