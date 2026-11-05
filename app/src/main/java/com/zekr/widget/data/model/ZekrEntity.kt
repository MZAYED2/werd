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
)
