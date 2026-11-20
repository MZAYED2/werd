package com.zekr.widget.data.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class DailyWirdRecord(
    val date: String, // yyyy-MM-dd
    val completedCount: Int = 0,
    val totalTasbihTaps: Int = 0,
    val quranCompleted: Int = 0,
    val hadithCompleted: Int = 0,
    val azkarCompleted: Int = 0
)

@Serializable
data class WirdStats(
    val dailyGoal: Int = 10,
    val bestStreak: Int = 0,
    val records: Map<String, DailyWirdRecord> = emptyMap()
) {
    fun todayRecord(today: LocalDate = LocalDate.now()): DailyWirdRecord {
        val key = formatDate(today)
        return records[key] ?: DailyWirdRecord(date = key)
    }

    fun currentStreak(today: LocalDate = LocalDate.now()): Int {
        var streak = 0
        var cursor = today
        val todayRec = records[formatDate(today)]
        // If user hasn't completed any yet today, check streak ending yesterday
        if (todayRec == null || todayRec.completedCount <= 0) {
            cursor = today.minusDays(1)
        }
        while (true) {
            val rec = records[formatDate(cursor)]
            if (rec != null && rec.completedCount > 0) {
                streak++
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    fun weeklySummary(today: LocalDate = LocalDate.now()): List<WeekDaySummary> {
        // Last 7 days ending with today (chronological)
        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val key = formatDate(date)
            val rec = records[key]
            val completed = rec?.completedCount ?: 0
            val progress = if (dailyGoal > 0) (completed.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f
            WeekDaySummary(
                dateKey = key,
                arabicDayShort = arabicDayName(date.dayOfWeek),
                completedCount = completed,
                totalTasbihTaps = rec?.totalTasbihTaps ?: 0,
                progress = progress,
                isToday = offset == 0
            )
        }
    }

    fun withRecordedTap(
        itemType: ContentType,
        completedItem: Boolean,
        today: LocalDate = LocalDate.now()
    ): WirdStats {
        val todayKey = formatDate(today)
        val existing = records[todayKey] ?: DailyWirdRecord(date = todayKey)
        val updatedToday = existing.copy(
            completedCount = existing.completedCount + if (completedItem) 1 else 0,
            totalTasbihTaps = existing.totalTasbihTaps + 1,
            quranCompleted = existing.quranCompleted + if (completedItem && itemType == ContentType.QURAN_AYAH) 1 else 0,
            hadithCompleted = existing.hadithCompleted + if (completedItem && itemType == ContentType.SAHIH_HADITH) 1 else 0,
            azkarCompleted = existing.azkarCompleted + if (completedItem && itemType == ContentType.AUTHENTIC_ZEKR) 1 else 0
        )

        // Retain last 60 days to keep DataStore compact
        val cutoff = today.minusDays(60)
        val pruned = records
            .filterKeys { key ->
                runCatching { LocalDate.parse(key, DATE_FORMATTER) }
                    .getOrNull()
                    ?.let { !it.isBefore(cutoff) } ?: false
            }
            .toMutableMap()
        pruned[todayKey] = updatedToday

        val updated = copy(records = pruned)
        val streakNow = updated.currentStreak(today)
        return updated.copy(bestStreak = maxOf(bestStreak, streakNow))
    }

    fun withRecordedCompletion(
        itemType: ContentType,
        count: Int = 1,
        today: LocalDate = LocalDate.now()
    ): WirdStats {
        val todayKey = formatDate(today)
        val existing = records[todayKey] ?: DailyWirdRecord(date = todayKey)
        val updatedToday = existing.copy(
            completedCount = existing.completedCount + count,
            quranCompleted = existing.quranCompleted + if (itemType == ContentType.QURAN_AYAH) count else 0,
            hadithCompleted = existing.hadithCompleted + if (itemType == ContentType.SAHIH_HADITH) count else 0,
            azkarCompleted = existing.azkarCompleted + if (itemType == ContentType.AUTHENTIC_ZEKR) count else 0
        )

        val cutoff = today.minusDays(60)
        val pruned = records
            .filterKeys { key ->
                runCatching { LocalDate.parse(key, DATE_FORMATTER) }
                    .getOrNull()
                    ?.let { !it.isBefore(cutoff) } ?: false
            }
            .toMutableMap()
        pruned[todayKey] = updatedToday

        val updated = copy(records = pruned)
        val streakNow = updated.currentStreak(today)
        return updated.copy(bestStreak = maxOf(bestStreak, streakNow))
    }

    fun withUndoneTap(
        itemType: ContentType,
        uncompletedItem: Boolean = false,
        today: LocalDate = LocalDate.now()
    ): WirdStats {
        val todayKey = formatDate(today)
        val existing = records[todayKey] ?: return this
        val updatedToday = existing.copy(
            completedCount = (existing.completedCount - if (uncompletedItem) 1 else 0).coerceAtLeast(0),
            totalTasbihTaps = (existing.totalTasbihTaps - 1).coerceAtLeast(0),
            quranCompleted = (existing.quranCompleted - if (uncompletedItem && itemType == ContentType.QURAN_AYAH) 1 else 0).coerceAtLeast(0),
            hadithCompleted = (existing.hadithCompleted - if (uncompletedItem && itemType == ContentType.SAHIH_HADITH) 1 else 0).coerceAtLeast(0),
            azkarCompleted = (existing.azkarCompleted - if (uncompletedItem && itemType == ContentType.AUTHENTIC_ZEKR) 1 else 0).coerceAtLeast(0)
        )
        val updatedRecords = records.toMutableMap().apply { put(todayKey, updatedToday) }
        return copy(records = updatedRecords)
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun formatDate(date: LocalDate): String = date.format(DATE_FORMATTER)

        fun arabicDayName(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
            DayOfWeek.SATURDAY -> "سبت"
            DayOfWeek.SUNDAY -> "أحد"
            DayOfWeek.MONDAY -> "إثنين"
            DayOfWeek.TUESDAY -> "ثلاثاء"
            DayOfWeek.WEDNESDAY -> "أربعاء"
            DayOfWeek.THURSDAY -> "خميس"
            DayOfWeek.FRIDAY -> "جمعة"
        }
    }
}

data class WeekDaySummary(
    val dateKey: String,
    val arabicDayShort: String,
    val completedCount: Int,
    val totalTasbihTaps: Int,
    val progress: Float,
    val isToday: Boolean
)
