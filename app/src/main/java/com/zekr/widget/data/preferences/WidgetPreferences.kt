package com.zekr.widget.data.preferences

enum class WidgetFont(val id: String, val titleArabic: String) {
    AMIRI("AMIRI", "أميري قرآني"),
    UTHMANIC("UTHMANIC", "عثماني حفص"),
    CAIRO("CAIRO", "كايرو عصري"),
    NASKH("NASKH", "نسخ كلاسيكي")
}

enum class GlassColorPreset(
    val id: String,
    val titleArabic: String,
    val hexColor: Long,
    val textColor: Long,
    val accentColor: Long
) {
    FROSTED_WHITE("FROSTED_WHITE", "أبيض ضبابي", 0xFFFFFFFF, 0xFF1C1C1E, 0xFF2E7D32),
    MIDNIGHT_OBSIDIAN("MIDNIGHT_OBSIDIAN", "ليلي داكن", 0xFF0D1310, 0xFFF5F7F6, 0xFF81C784),
    EMERALD_SAGE("EMERALD_SAGE", "زمردي ملكي", 0xFF0A3D28, 0xFFFFFFFF, 0xFFA5D6A7),
    AMBER_WARMTH("AMBER_WARMTH", "عنبري دافئ", 0xFF3D270A, 0xFFFFFFFF, 0xFFFFD54F),
    ROYAL_SAPPHIRE("ROYAL_SAPPHIRE", "ياقوتي أزرق", 0xFF0D253D, 0xFFFFFFFF, 0xFF81D4FA)
}

enum class PatternType(val id: String, val titleArabic: String) {
    NONE("NONE", "بدون نقوش"),
    ARABESQUE("ARABESQUE", "أرابيسك إسلامي"),
    STAR_OCTAGRAM("STAR_OCTAGRAM", "نجمة ثمانية"),
    FLORAL_DAMASK("FLORAL_DAMASK", "زخرفة نباتية"),
    GEOMETRIC_HEX("GEOMETRIC_HEX", "هندسي مسدس"),
    MINIMAL_DOTS("MINIMAL_DOTS", "نقاط هادئة")
}

data class WidgetSettings(
    val font: WidgetFont = WidgetFont.AMIRI,
    val fontSizeSp: Int = 18,
    val colorPreset: GlassColorPreset = GlassColorPreset.EMERALD_SAGE,
    val glassOpacity: Float = 0.35f,
    val pattern: PatternType = PatternType.ARABESQUE,
    val includeQuran: Boolean = true,
    val includeHadith: Boolean = true,
    val includeAzkar: Boolean = true,
    val serviceEnabled: Boolean = true
)
