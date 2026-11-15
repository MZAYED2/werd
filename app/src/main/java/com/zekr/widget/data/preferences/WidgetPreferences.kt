package com.zekr.widget.data.preferences

import com.zekr.widget.R

enum class WidgetFont(val id: String, val titleArabic: String) {
    AMIRI("AMIRI", "أميري قرآني"),
    UTHMANIC("UTHMANIC", "عثماني حفص"),
    NASKH("NASKH", "نسخ كلاسيكي"),
    SCHEHERAZADE_NEW("SCHEHERAZADE_NEW", "شهرزاد مخطوط"),
    LATEEF("LATEEF", "لطيف تقليدي"),
    AREF_RUQAA("AREF_RUQAA", "رقعة خط عربي"),
    KATIBEH("KATIBEH", "كتيبة تراثي"),
    REEM_KUFI("REEM_KUFI", "كوفي هندسي"),
    EL_MESSIRI("EL_MESSIRI", "المسيري أنيق"),
    MARKAZI_TEXT("MARKAZI_TEXT", "مركزي أدبي"),
    CAIRO("CAIRO", "كايرو عصري"),
    TAJAWAL("TAJAWAL", "تجوال عصري"),
    ALMARAI("ALMARAI", "المراعي شاشة"),
    IBM_PLEX_SANS_ARABIC("IBM_PLEX_SANS_ARABIC", "بليكس تقني"),
    CHANGA("CHANGA", "شانجا عريض"),
    LALEZAR("LALEZAR", "لالزار زخرفي"),
    JOMHURIA("JOMHURIA", "جمهورية عناوين"),
    RAKKAS("RAKKAS", "رقاص تزييني")
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
    ROYAL_SAPPHIRE("ROYAL_SAPPHIRE", "ياقوتي أزرق", 0xFF0D253D, 0xFFFFFFFF, 0xFF81D4FA),
    ROYAL_RUBY("ROYAL_RUBY", "ياقوتي قرمزي", 0xFF3A1118, 0xFFFFFFFF, 0xFFEF9A9A),
    DEEP_TEAL("DEEP_TEAL", "فيروزي داكن", 0xFF0C383B, 0xFFFFFFFF, 0xFF80CBC4),
    SLATE_CHARCOAL("SLATE_CHARCOAL", "رمادي فحمي", 0xFF1E232A, 0xFFFFFFFF, 0xFF90CAF9),
    PURE_BLACK("PURE_BLACK", "أسود نقي", 0xFF111111, 0xFFFFFFFF, 0xFFFFD54F),
    OLIVE_GARDEN("OLIVE_GARDEN", "أخضر زيتوني", 0xFF1D3318, 0xFFFFFFFF, 0xFFC5E1A5)
}

enum class GradientDirection(val id: String, val titleArabic: String, val symbol: String) {
    DIAGONAL("DIAGONAL", "مائل (قطري)", "⤡"),
    VERTICAL("VERTICAL", "رأسي (أعلى لأسفل)", "⤓"),
    HORIZONTAL("HORIZONTAL", "أفقي (يسار ليمين)", "➔"),
    RADIAL("RADIAL", "شعاعي (مركز للخارج)", "◉")
}

enum class GradientPreset(
    val id: String,
    val titleArabic: String,
    val startColor: Long,
    val endColor: Long,
    val defaultDirection: GradientDirection = GradientDirection.DIAGONAL
) {
    EMERALD_FOREST("EMERALD_FOREST", "زمرد ملكي وأخضر غامق", 0xFF0D472B, 0xFF021B0F),
    MIDNIGHT_OCEAN("MIDNIGHT_OCEAN", "شفق المحيط الكحلي", 0xFF0D253D, 0xFF05101C),
    OBSIDIAN_PURPLE("OBSIDIAN_PURPLE", "ليل داكن وأرجواني", 0xFF241038, 0xFF0D1310),
    ROYAL_GOLD_CHARCOAL("ROYAL_GOLD_CHARCOAL", "ذهب وفحم أندلسي", 0xFF4A3816, 0xFF14120E),
    CRIMSON_RUBY("CRIMSON_RUBY", "عقيق وياقوت أحمر", 0xFF441018, 0xFF180508),
    TEAL_MINT("TEAL_MINT", "فيروزي وسماوي داكن", 0xFF0A3C36, 0xFF041917),
    SUNSET_DUNE("SUNSET_DUNE", "غروب الصحراء الدافئ", 0xFF4A2511, 0xFF241006),
    FROSTED_PEARL("FROSTED_PEARL", "لؤلؤي ضبابي نقي", 0xFFFFFFFF, 0xFFCFD8DC),
    KAABA_GOLD("KAABA_GOLD", "كسوة الكعبة وذهب", 0xFF1A1A1A, 0xFF2E2516),
    OLIVE_BRONZE("OLIVE_BRONZE", "زيتوني عتيق وبرونز", 0xFF22301A, 0xFF0F180B)
}

enum class PatternType(
    val id: String,
    val titleArabic: String,
    val vectorResId: Int? = null,
    val previewBgColor: Long = 0xFF1C1E22
) {
    // First option in the pattern picker
    NONE("NONE", "بدون نقوش", null, 0xFF181A1D),

    // High-quality Islamic geometric patterns
    HEXAGRAM_STARS("HEXAGRAM_STARS", "خاتم سداسي أندلسي", R.drawable.pattern_hexagram_stars),
    OCTAGON_SQUARES("OCTAGON_SQUARES", "شبكة مثمنات ومربعات", R.drawable.pattern_octagon_squares),
    ANDALUSIAN_EIGHT_STAR("ANDALUSIAN_EIGHT_STAR", "نجمة ثمانية موريسكية", R.drawable.pattern_andalusian_eight_star),
    HEX_ROSETTE_KITES("HEX_ROSETTE_KITES", "زهرة سداسية مورقة", R.drawable.pattern_hex_rosette_kites),
    TWELVE_GIRIH_STAR("TWELVE_GIRIH_STAR", "نجمة اثنا عشرية جيريه", R.drawable.pattern_twelve_girih_star),
    ALHAMBRA_KHATAM_STAR("ALHAMBRA_KHATAM_STAR", "نجمة قصر الحمراء الثمانية", R.drawable.pattern_alhambra_khatam_star),
    MOROCCAN_QUATREFOIL("MOROCCAN_QUATREFOIL", "مشبك فاسي مغربي", R.drawable.pattern_moroccan_quatrefoil),
    FLOWER_OF_LIFE("FLOWER_OF_LIFE", "زهرة الحياة الكلاسيكية", R.drawable.pattern_flower_of_life),
    CONCENTRIC_SQUARES_STAR("CONCENTRIC_SQUARES_STAR", "مربعات ودوائر الختم", R.drawable.pattern_concentric_squares_star),

    // Arabic calligraphy palette variants
    ARABIC_CALLIGRAPHY("ARABIC_CALLIGRAPHY", "حروفيات داكنة", null, 0xFF15171A),
    CALLIGRAPHY_AMBER("CALLIGRAPHY_AMBER", "حروفيات عنبرية", null, 0xFF58382B),
    CALLIGRAPHY_BLUE("CALLIGRAPHY_BLUE", "حروفيات ياقوتية", null, 0xFF23354B),

    // Additional geometric / minimal styles
    RADIAL_DOTS("RADIAL_DOTS", "نقاط متدرجة"),
    RADIAL_RIPPLES("RADIAL_RIPPLES", "تموجات دائرية"),
    HEX_HONEYCOMB("HEX_HONEYCOMB", "خلايا النحل"),
    ZIGZAG_CHEVRON("ZIGZAG_CHEVRON", "تعرجات هندسية"),
    MASHRABIYA_STRIPES("MASHRABIYA_STRIPES", "مشربية رأسية"),
    INTERLOCKING_RINGS("INTERLOCKING_RINGS", "حلقات متداخلة"),
    CROSS_STARS("CROSS_STARS", "نجوم متقاطعة"),
    DIAMOND_LATTICE("DIAMOND_LATTICE", "شبكة معينات"),
    SCALE_ARCHES("SCALE_ARCHES", "قشور وأقواس متداخلة"),
    STAR_OCTAGRAM("STAR_OCTAGRAM", "نجمة ثمانية أندلسية"),
    ANDALUSIAN_ROSETTE("ANDALUSIAN_ROSETTE", "زهرة هندسية"),
    STAR_TWELVE("STAR_TWELVE", "نجمة اثنا عشرية"),
    ISLAMIC_ARCH("ISLAMIC_ARCH", "محراب وأقواس"),
    MOROCCAN_ZELLIGE("MOROCCAN_ZELLIGE", "زليج مغربي"),
    CRESCENT_GEOMETRY("CRESCENT_GEOMETRY", "هلال إسلامي"),
    MINIMAL_DOTS("MINIMAL_DOTS", "نقاط هادئة")
}

data class WidgetSettings(
    val font: WidgetFont = WidgetFont.AMIRI,
    val fontSizeSp: Int = 18,
    val colorPreset: GlassColorPreset = GlassColorPreset.EMERALD_SAGE,
    val isGradient: Boolean = false,
    val primaryColor: Long = GlassColorPreset.EMERALD_SAGE.hexColor,
    val secondaryColor: Long = 0xFF021B0F,
    val gradientDirection: GradientDirection = GradientDirection.DIAGONAL,
    val glassOpacity: Float = 0.35f,
    val blurGrade: Float = 0.5f,
    val pattern: PatternType = PatternType.ALHAMBRA_KHATAM_STAR,
    val patternOpacity: Float = 0.28f,
    val patternScale: Float = 1.0f,
    val customImageUri: String? = null,
    val includeQuran: Boolean = true,
    val includeHadith: Boolean = true,
    val includeAzkar: Boolean = true,
    val serviceEnabled: Boolean = true,
    /** When true, cycle pattern + background every [styleRotationHours] hours. */
    val styleRotationEnabled: Boolean = false,
    /** Interval in hours (1–168). Default 6. */
    val styleRotationHours: Int = 6
) {
    fun getAdaptiveTextColor(): Long = AdaptiveContrast.textColor(
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        isGradient = isGradient,
        glassOpacity = glassOpacity
    )

    fun getAdaptiveAccentColor(): Long = AdaptiveContrast.accentColor(
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        isGradient = isGradient,
        glassOpacity = glassOpacity
    )
}

/**
 * Contrast for translucent glass over a typical dark home wallpaper.
 * Solid luminance alone picks black text for frosted white at 35% opacity —
 * wrong once the dark wallpaper shows through.
 */
object AdaptiveContrast {
    const val DARK_TEXT = 0xFF1C1C1EL
    const val LIGHT_TEXT = 0xFFFFFFFFL
    const val DARK_ACCENT = 0xFF1B5E20L
    const val LIGHT_ACCENT = 0xFFA5D6A7L

    /** Typical dark wallpaper / app preview underlay. */
    const val UNDERLAY_LUMINANCE = 0.22f

    fun colorLuminance(color: Long): Float {
        val r = (color shr 16 and 0xFF)
        val g = (color shr 8 and 0xFF)
        val b = (color and 0xFF)
        return (0.299f * r + 0.587f * g + 0.114f * b) / 255f
    }

    fun glassLuminance(
        primaryColor: Long,
        secondaryColor: Long,
        isGradient: Boolean
    ): Float {
        return if (isGradient) {
            (colorLuminance(primaryColor) + colorLuminance(secondaryColor)) / 2f
        } else {
            colorLuminance(primaryColor)
        }
    }

    fun effectiveLuminance(solidLum: Float, opacity: Float): Float {
        val a = opacity.coerceIn(0.10f, 0.95f)
        return solidLum * a + UNDERLAY_LUMINANCE * (1f - a)
    }

    fun textColor(
        primaryColor: Long,
        secondaryColor: Long = primaryColor,
        isGradient: Boolean = false,
        glassOpacity: Float = 0.35f
    ): Long {
        val effective = effectiveLuminance(
            glassLuminance(primaryColor, secondaryColor, isGradient),
            glassOpacity
        )
        // Prefer light text unless the glass is clearly light and fairly opaque.
        return if (effective > 0.58f) DARK_TEXT else LIGHT_TEXT
    }

    fun accentColor(
        primaryColor: Long,
        secondaryColor: Long = primaryColor,
        isGradient: Boolean = false,
        glassOpacity: Float = 0.35f
    ): Long {
        val effective = effectiveLuminance(
            glassLuminance(primaryColor, secondaryColor, isGradient),
            glassOpacity
        )
        return if (effective > 0.58f) DARK_ACCENT else LIGHT_ACCENT
    }
}
