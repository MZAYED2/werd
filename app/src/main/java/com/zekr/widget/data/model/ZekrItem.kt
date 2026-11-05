package com.zekr.widget.data.model

data class ZekrItem(
    val id: Int,
    val arabicText: String,
    val targetCount: Int = 1,
    val reference: String = "",
    val category: ZekrCategory = ZekrCategory.DAILY,
    val transliteration: String = "",
    val englishMeaning: String = ""
)
