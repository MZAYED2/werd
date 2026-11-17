package com.zekr.widget.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ContentType(val labelArabic: String, val labelEnglish: String) {
    QURAN_AYAH("آية قرآنية", "Quranic Ayah"),
    SAHIH_HADITH("حديث صحيح", "Sahih Hadith"),
    AUTHENTIC_ZEKR("ذكر مأثور", "Authentic Zikr")
}
