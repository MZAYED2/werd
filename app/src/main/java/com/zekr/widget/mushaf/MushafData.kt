package com.zekr.widget.mushaf

import com.zekr.widget.data.repository.ZekrRepository

data class SurahMeta(
    val number: Int,
    val nameArabic: String,
    val startPage: Int,
    val totalVerses: Int,
    val isMakki: Boolean
)

data class JuzMeta(
    val number: Int,
    val nameArabic: String,
    val startPage: Int
)

data class PageHeaderInfo(
    val pageNumber: Int,
    val pageNumberArabic: String,
    val surahName: String,
    val juzName: String,
    val juzNumber: Int
)

object MushafData {
    const val TOTAL_PAGES = 604

    val SURAHS: List<SurahMeta> = listOf(
        SurahMeta(1, "الفاتحة", 1, 7, true),
        SurahMeta(2, "البقرة", 2, 286, false),
        SurahMeta(3, "آل عمران", 50, 200, false),
        SurahMeta(4, "النساء", 77, 176, false),
        SurahMeta(5, "المائدة", 106, 120, false),
        SurahMeta(6, "الأنعام", 128, 165, true),
        SurahMeta(7, "الأعراف", 151, 206, true),
        SurahMeta(8, "الأنفال", 177, 75, false),
        SurahMeta(9, "التوبة", 187, 129, false),
        SurahMeta(10, "يونس", 208, 109, true),
        SurahMeta(11, "هود", 221, 123, true),
        SurahMeta(12, "يوسف", 235, 111, true),
        SurahMeta(13, "الرعد", 249, 43, false),
        SurahMeta(14, "إبراهيم", 255, 52, true),
        SurahMeta(15, "الحجر", 262, 99, true),
        SurahMeta(16, "النحل", 267, 128, true),
        SurahMeta(17, "الإسراء", 282, 111, true),
        SurahMeta(18, "الكهف", 293, 110, true),
        SurahMeta(19, "مريم", 305, 98, true),
        SurahMeta(20, "طه", 312, 135, true),
        SurahMeta(21, "الأنبياء", 322, 112, true),
        SurahMeta(22, "الحج", 332, 78, false),
        SurahMeta(23, "المؤمنون", 342, 118, true),
        SurahMeta(24, "النور", 350, 64, false),
        SurahMeta(25, "الفرقان", 359, 77, true),
        SurahMeta(26, "الشعراء", 367, 227, true),
        SurahMeta(27, "النمل", 377, 93, true),
        SurahMeta(28, "القصص", 385, 88, true),
        SurahMeta(29, "العنكبوت", 396, 69, true),
        SurahMeta(30, "الروم", 404, 60, true),
        SurahMeta(31, "لقمان", 411, 34, true),
        SurahMeta(32, "السجدة", 415, 30, true),
        SurahMeta(33, "الأحزاب", 418, 73, false),
        SurahMeta(34, "سبأ", 428, 54, true),
        SurahMeta(35, "فاطر", 434, 45, true),
        SurahMeta(36, "يس", 440, 83, true),
        SurahMeta(37, "الصافات", 446, 182, true),
        SurahMeta(38, "ص", 453, 88, true),
        SurahMeta(39, "الزمر", 458, 75, true),
        SurahMeta(40, "غافر", 467, 85, true),
        SurahMeta(41, "فصلت", 477, 54, true),
        SurahMeta(42, "الشورى", 483, 53, true),
        SurahMeta(43, "الزخرف", 489, 89, true),
        SurahMeta(44, "الدخان", 496, 59, true),
        SurahMeta(45, "الجاثية", 499, 37, true),
        SurahMeta(46, "الأحقاف", 502, 35, true),
        SurahMeta(47, "محمد", 507, 38, false),
        SurahMeta(48, "الفتح", 511, 29, false),
        SurahMeta(49, "الحجرات", 515, 18, false),
        SurahMeta(50, "ق", 518, 45, true),
        SurahMeta(51, "الذاريات", 520, 60, true),
        SurahMeta(52, "الطور", 523, 49, true),
        SurahMeta(53, "النجم", 526, 62, true),
        SurahMeta(54, "القمر", 528, 55, true),
        SurahMeta(55, "الرحمن", 531, 78, false),
        SurahMeta(56, "الواقعة", 534, 96, true),
        SurahMeta(57, "الحديد", 537, 29, false),
        SurahMeta(58, "المجادلة", 542, 22, false),
        SurahMeta(59, "الحشر", 545, 24, false),
        SurahMeta(60, "الممتحنة", 549, 13, false),
        SurahMeta(61, "الصف", 551, 14, false),
        SurahMeta(62, "الجمعة", 553, 11, false),
        SurahMeta(63, "المنافقون", 554, 11, false),
        SurahMeta(64, "التغابن", 556, 18, false),
        SurahMeta(65, "الطلاق", 558, 12, false),
        SurahMeta(66, "التحريم", 560, 12, false),
        SurahMeta(67, "الملك", 562, 30, true),
        SurahMeta(68, "القلم", 564, 52, true),
        SurahMeta(69, "الحاقة", 566, 52, true),
        SurahMeta(70, "المعارج", 568, 44, true),
        SurahMeta(71, "نوح", 570, 28, true),
        SurahMeta(72, "الجن", 572, 28, true),
        SurahMeta(73, "المزمل", 574, 20, true),
        SurahMeta(74, "المدثر", 575, 56, true),
        SurahMeta(75, "القيامة", 577, 40, true),
        SurahMeta(76, "الإنسان", 578, 31, false),
        SurahMeta(77, "المرسلات", 580, 50, true),
        SurahMeta(78, "النبأ", 582, 40, true),
        SurahMeta(79, "النازعات", 583, 46, true),
        SurahMeta(80, "عبس", 585, 42, true),
        SurahMeta(81, "التكوير", 586, 29, true),
        SurahMeta(82, "الانفطار", 587, 19, true),
        SurahMeta(83, "المطففين", 587, 36, true),
        SurahMeta(84, "الانشقاق", 589, 25, true),
        SurahMeta(85, "البروج", 590, 22, true),
        SurahMeta(86, "الطارق", 591, 17, true),
        SurahMeta(87, "الأعلى", 591, 19, true),
        SurahMeta(88, "الغاشية", 592, 26, true),
        SurahMeta(89, "الفجر", 593, 30, true),
        SurahMeta(90, "البلد", 594, 20, true),
        SurahMeta(91, "الشمس", 595, 15, true),
        SurahMeta(92, "الليل", 595, 21, true),
        SurahMeta(93, "الضحى", 596, 11, true),
        SurahMeta(94, "الشرح", 596, 8, true),
        SurahMeta(95, "التين", 597, 8, true),
        SurahMeta(96, "العلق", 597, 19, true),
        SurahMeta(97, "القدر", 598, 5, true),
        SurahMeta(98, "البينة", 598, 8, false),
        SurahMeta(99, "الزلزلة", 599, 8, false),
        SurahMeta(100, "العاديات", 599, 11, true),
        SurahMeta(101, "القارعة", 600, 11, true),
        SurahMeta(102, "التكاثر", 600, 8, true),
        SurahMeta(103, "العصر", 601, 3, true),
        SurahMeta(104, "الهمزة", 601, 9, true),
        SurahMeta(105, "الفيل", 601, 5, true),
        SurahMeta(106, "قريش", 602, 4, true),
        SurahMeta(107, "الماعون", 602, 7, true),
        SurahMeta(108, "الكوثر", 602, 3, true),
        SurahMeta(109, "الكافرون", 603, 6, true),
        SurahMeta(110, "النصر", 603, 3, false),
        SurahMeta(111, "المسد", 603, 5, true),
        SurahMeta(112, "الإخلاص", 604, 4, true),
        SurahMeta(113, "الفلق", 604, 5, true),
        SurahMeta(114, "الناس", 604, 6, true)
    )

    val JUZ_LIST: List<JuzMeta> = listOf(
        JuzMeta(1, "الجزء الأول · الم", 1),
        JuzMeta(2, "الجزء الثاني · سيقول", 22),
        JuzMeta(3, "الجزء الثالث · تلك الرسل", 42),
        JuzMeta(4, "الجزء الرابع · لن تنالوا", 62),
        JuzMeta(5, "الجزء الخامس · والمحصنات", 82),
        JuzMeta(6, "الجزء السادس · لا يحب الله", 102),
        JuzMeta(7, "الجزء السابع · وإذا سمعوا", 121),
        JuzMeta(8, "الجزء الثامن · ولو أننا", 142),
        JuzMeta(9, "الجزء التاسع · قال الملأ", 162),
        JuzMeta(10, "الجزء العاشر · واعلموا", 182),
        JuzMeta(11, "الجزء الحادي عشر · يعتذرون", 201),
        JuzMeta(12, "الجزء الثاني عشر · وما من دابة", 222),
        JuzMeta(13, "الجزء الثالث عشر · وما أبرئ", 242),
        JuzMeta(14, "الجزء الرابع عشر · ربما", 262),
        JuzMeta(15, "الجزء الخامس عشر · سبحان الذي", 282),
        JuzMeta(16, "الجزء السادس عشر · قال ألم", 302),
        JuzMeta(17, "الجزء السابع عشر · اقترب للناس", 322),
        JuzMeta(18, "الجزء الثامن عشر · قد أفلح", 342),
        JuzMeta(19, "الجزء التاسع عشر · وقال الذين", 362),
        JuzMeta(20, "الجزء العشرون · أمن خلق", 382),
        JuzMeta(21, "الجزء الحادي والعشرون · اتل ما أوحي", 402),
        JuzMeta(22, "الجزء الثاني والعشرون · ومن يقنت", 422),
        JuzMeta(23, "الجزء الثالث والعشرون · وما أنزلنا", 442),
        JuzMeta(24, "الجزء الرابع والعشرون · فمن أظلم", 462),
        JuzMeta(25, "الجزء الخامس والعشرون · إليه يرد", 482),
        JuzMeta(26, "الجزء السادس والعشرون · حم", 502),
        JuzMeta(27, "الجزء السابع والعشرون · قال فما خطبكم", 522),
        JuzMeta(28, "الجزء الثامن والعشرون · قد سمع الله", 542),
        JuzMeta(29, "الجزء التاسع والعشرون · تبارك الذي", 562),
        JuzMeta(30, "الجزء الثلاثون · عمّ يتساءلون", 582)
    )

    fun getPageHeaderInfo(page: Int): PageHeaderInfo {
        val safePage = page.coerceIn(1, TOTAL_PAGES)

        // Surah on this page is the last Surah whose startPage <= safePage
        val surah = SURAHS.lastOrNull { it.startPage <= safePage } ?: SURAHS.first()

        // Juz on this page
        val juz = JUZ_LIST.lastOrNull { it.startPage <= safePage } ?: JUZ_LIST.first()

        return PageHeaderInfo(
            pageNumber = safePage,
            pageNumberArabic = ZekrRepository.toArabicDigits(safePage),
            surahName = "سورة ${surah.nameArabic}",
            juzName = juz.nameArabic,
            juzNumber = juz.number
        )
    }

    fun findSurahByNumber(num: Int): SurahMeta? = SURAHS.firstOrNull { it.number == num }
}
