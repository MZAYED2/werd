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
    val englishMeaning: String = ""
) {
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
