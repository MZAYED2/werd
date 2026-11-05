package com.zekr.widget.data.repository

import com.zekr.widget.data.model.ZekrCategory
import com.zekr.widget.data.model.ZekrItem

object ZekrRepository {

    val defaultItems: List<ZekrItem> = listOf(
        ZekrItem(
            id = 1,
            arabicText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            targetCount = 100,
            reference = "متفق عليه",
            category = ZekrCategory.TASBIH,
            englishMeaning = "Glory be to Allah and His praise, Glory be to Allah the Great"
        ),
        ZekrItem(
            id = 2,
            arabicText = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ الْعَلِيِّ الْعَظِيمِ",
            targetCount = 100,
            reference = "كنز من كنوز الجنة",
            category = ZekrCategory.DAILY,
            englishMeaning = "There is no power nor strength except with Allah, the Most High, the Supreme"
        ),
        ZekrItem(
            id = 3,
            arabicText = "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ وَأَتُوبُ إِلَيْهِ",
            targetCount = 100,
            reference = "سنة نبوية",
            category = ZekrCategory.TASBIH,
            englishMeaning = "I seek forgiveness of Allah the Great and repent to Him"
        ),
        ZekrItem(
            id = 4,
            arabicText = "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
            targetCount = 10,
            reference = "من صلى علي صلاة صلى الله عليه بها عشرا",
            category = ZekrCategory.DAILY,
            englishMeaning = "O Allah, send blessings and peace upon our Prophet Muhammad"
        ),
        ZekrItem(
            id = 5,
            arabicText = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ",
            targetCount = 1,
            reference = "أذكار الصباح",
            category = ZekrCategory.MORNING,
            englishMeaning = "We have entered a new morning and with it upon the whole kingdom belongs to Allah"
        ),
        ZekrItem(
            id = 6,
            arabicText = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ",
            targetCount = 1,
            reference = "أذكار المساء",
            category = ZekrCategory.EVENING,
            englishMeaning = "We have reached the evening and at this very time unto Allah belongs all sovereignty"
        ),
        ZekrItem(
            id = 7,
            arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            targetCount = 3,
            reference = "سورة البقرة: 201",
            category = ZekrCategory.QURANIC_DUA,
            englishMeaning = "Our Lord, give us in this world that which is good and in the Hereafter that which is good"
        )
    )

    fun getById(id: Int): ZekrItem {
        return defaultItems.find { it.id == id } ?: defaultItems.first()
    }

    fun getNextId(currentId: Int): Int {
        val currentIndex = defaultItems.indexOfFirst { it.id == currentId }
        val nextIndex = if (currentIndex == -1 || currentIndex == defaultItems.lastIndex) 0 else currentIndex + 1
        return defaultItems[nextIndex].id
    }

    fun getPreviousId(currentId: Int): Int {
        val currentIndex = defaultItems.indexOfFirst { it.id == currentId }
        val prevIndex = if (currentIndex <= 0) defaultItems.lastIndex else currentIndex - 1
        return defaultItems[prevIndex].id
    }
}
