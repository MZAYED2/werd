package com.zekr.widget.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ZekrEntity(
    val id: Int,
    val arabicText: String,
    val type: ContentType,
    val source: String,             // e.g. "صحيح البخاري: 6407" or "سورة الإسراء: 82"
    val reference: String,          // Book/Chapter name
    val category: String = "عام",   // "أذكار الصباح", "أذكار المساء", "الاستغفار", "الرقية", etc.
    val repetition: Int = 1,
    val englishMeaning: String = "",
    val isCustom: Boolean = false,
    val tasbeehText: String? = null,
    val virtueText: String? = null,
    val documentedCount: Int? = null
) {
    /**
     * Authentic documented repetition count from the Hadith/Sunnah, if explicitly specified.
     * Returns null when no specific count was documented (so we don't present 1 as a fake Sunnah count).
     */
    fun getEffectiveDocumentedCount(): Int? {
        return documentedCount ?: if (repetition > 1) repetition else null
    }

    /**
     * Repeatable formula for Tasbeeh (excludes hadith prelude/virtue explanation).
     */
    fun getEffectiveTasbeehText(): String {
        return tasbeehText?.takeIf { it.isNotBlank() } ?: arabicText
    }

    /**
     * Authentic text formatting: wraps Quranic Ayahs with traditional Quran brackets ﴿ ﴾.
     * Preserves Hadiths and general duas as-is without brackets.
     */
    fun getFormattedArabicText(): String {
        return if (type == ContentType.QURAN_AYAH) {
            val trimmed = arabicText.trim()
            if (trimmed.startsWith("﴿") && trimmed.endsWith("﴾")) {
                trimmed
            } else {
                "﴿ $trimmed ﴾"
            }
        } else {
            arabicText
        }
    }
    fun getConciseCitation(maxChars: Int = 42): String {
        // If source has dual citations like "البخاري | مسلم", take the primary citation
        val conciseSource = if (source.contains(" | ")) {
            source.substringBefore(" | ").trim()
        } else {
            source.trim()
        }

        var cleanRef = reference.trim()
        if (cleanRef.contains(" - ")) {
            cleanRef = cleanRef.substringBefore(" - ").trim()
        }
        if (cleanRef.contains(" • ")) {
            cleanRef = cleanRef.substringAfterLast(" • ").trim()
        }
        if (cleanRef.startsWith("كتاب ")) {
            cleanRef = cleanRef.removePrefix("كتاب ").trim()
        }
        if (cleanRef.length > 22) {
            cleanRef = cleanRef.take(20).trimEnd() + "…"
        }

        val combined = when {
            conciseSource.isNotEmpty() && cleanRef.isNotEmpty() -> "$conciseSource • $cleanRef"
            conciseSource.isNotEmpty() -> conciseSource
            cleanRef.isNotEmpty() -> cleanRef
            else -> ""
        }

        return if (combined.length > maxChars) {
            combined.take(maxChars - 1).trimEnd() + "…"
        } else {
            combined
        }
    }
}
