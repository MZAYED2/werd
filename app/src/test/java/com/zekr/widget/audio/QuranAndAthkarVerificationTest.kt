package com.zekr.widget.audio

import com.zekr.widget.mushaf.MushafData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranAndAthkarVerificationTest {

    private val expectedSurahs = listOf(
        "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس",
        "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه",
        "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
        "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
        "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق",
        "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة",
        "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج",
        "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
        "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد",
        "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
        "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر",
        "المسد", "الإخلاص", "الفلق", "الناس"
    )

    @Test
    fun testQuranSurahCountAndNamesOrder() {
        assertEquals("Total Quran Surahs must be exactly 114", 114, AudioCatalog.SURAH_NAMES.size)
        assertEquals("MushafData Surahs count must be exactly 114", 114, MushafData.SURAHS.size)

        val uniqueNames = AudioCatalog.SURAH_NAMES.toSet()
        assertEquals("All 114 Surah names must be unique without duplicates", 114, uniqueNames.size)

        for (i in 0 until 114) {
            val expectedName = expectedSurahs[i]
            val catalogName = AudioCatalog.SURAH_NAMES[i]
            val mushafSurah = MushafData.SURAHS[i]

            assertEquals("Surah $i name in catalog must match expected Quranic index", expectedName, catalogName)
            assertEquals("Surah ${i + 1} number in MushafData must be sequential", i + 1, mushafSurah.number)
            assertEquals("Surah ${i + 1} name in MushafData must match AudioCatalog", catalogName, mushafSurah.nameArabic)
        }
    }

    @Test
    fun testMinshawiTracksIntegrity() {
        val tracks = AudioCatalog.getQuranTracks(AudioCatalog.QuranReciter.MINSHAWI)
        assertEquals(114, tracks.size)

        for (i in 0 until 114) {
            val num = i + 1
            val paddedNum = num.toString().padStart(3, '0')
            val track = tracks[i]

            assertEquals("quran_minshawi_$paddedNum", track.id)
            assertEquals("سورة ${AudioCatalog.SURAH_NAMES[i]}", track.title)
            assertEquals(num, track.surahNumber)
            assertEquals("محمد صديق المنشاوي", track.reciterOrSpeaker)
            assertTrue("Audio URL must target Minshawi folder", track.audioUrl.contains("muhammad_siddeeq_al-minshaawee/$paddedNum.mp3"))
            assertTrue("Audio URL must not contain Abdulbasit folder", !track.audioUrl.contains("abdul_basit_murattal"))
        }
    }

    @Test
    fun testAbdulbasitTracksIntegrity() {
        val tracks = AudioCatalog.getQuranTracks(AudioCatalog.QuranReciter.ABDULBASIT)
        assertEquals(114, tracks.size)

        for (i in 0 until 114) {
            val num = i + 1
            val paddedNum = num.toString().padStart(3, '0')
            val track = tracks[i]

            assertEquals("quran_abdulbasit_$paddedNum", track.id)
            assertEquals("سورة ${AudioCatalog.SURAH_NAMES[i]}", track.title)
            assertEquals(num, track.surahNumber)
            assertEquals("عبد الباسط عبد الصمد", track.reciterOrSpeaker)
            assertTrue("Audio URL must target Abdulbasit folder", track.audioUrl.contains("abdul_basit_murattal/$paddedNum.mp3"))
            assertTrue("Audio URL must not contain Minshawi folder", !track.audioUrl.contains("muhammad_siddeeq_al-minshaawee"))
        }
    }

    @Test
    fun testAthkarTracksIntegrityAndSeparation() {
        val athkar = AudioCatalog.AthkarTracks
        assertTrue("Must have at least 30 verified Athkar tracks", athkar.size >= 30)

        val uniqueIds = athkar.map { it.id }.toSet()
        assertEquals("All Athkar track IDs must be strictly unique", athkar.size, uniqueIds.size)

        // Verify car riding and travel dua separation
        val ridingTrack = AudioCatalog.findTrackById("dua_riding")
        val travelTrack = AudioCatalog.findTrackById("dua_travel")

        assertNotNull("dua_riding must exist", ridingTrack)
        assertNotNull("dua_travel must exist", travelTrack)

        assertTrue("Riding dua must be ar_096", ridingTrack!!.audioUrl.contains("ar_096"))
        assertTrue("Travel dua must be ar_097", travelTrack!!.audioUrl.contains("ar_097"))
        assertEquals("دعاء ركوب السيارة أو الدابة", ridingTrack.title)
        assertEquals("دعاء السفر كاملاً", travelTrack.title)

        // Verify key tracks matching
        val morningEvening = AudioCatalog.findTrackById("athkar_morning_evening")
        assertNotNull(morningEvening)
        assertTrue("Morning/Evening track must point to ar_028", morningEvening!!.audioUrl.contains("ar_028"))

        val sleepTrack = AudioCatalog.findTrackById("athkar_sleep")
        assertNotNull(sleepTrack)
        assertTrue("Sleep track must point to ar_029", sleepTrack!!.audioUrl.contains("ar_029"))

        val afterPrayerTrack = AudioCatalog.findTrackById("athkar_after_prayer")
        assertNotNull(afterPrayerTrack)
        assertTrue("After prayer track must point to ar_026", afterPrayerTrack!!.audioUrl.contains("ar_026"))

        val istikharahTrack = AudioCatalog.findTrackById("dua_istikharah")
        assertNotNull(istikharahTrack)
        assertTrue("Istikharah track must point to ar_027", istikharahTrack!!.audioUrl.contains("ar_027"))

        val worryTrack = AudioCatalog.findTrackById("dua_worry_grief")
        assertNotNull(worryTrack)
        assertTrue("Worry/grief track must point to ar_035", worryTrack!!.audioUrl.contains("ar_035"))

        val istighfarTrack = AudioCatalog.findTrackById("athkar_istighfar")
        assertNotNull(istighfarTrack)
        assertTrue("Istighfar track must point to ar_130", istighfarTrack!!.audioUrl.contains("ar_130"))

        val salawatTrack = AudioCatalog.findTrackById("athkar_salawat")
        assertNotNull(salawatTrack)
        assertTrue("Salawat track must point to ar_108", salawatTrack!!.audioUrl.contains("ar_108"))

        val tasbeehTrack = AudioCatalog.findTrackById("athkar_tasbeeh_virtue")
        assertNotNull(tasbeehTrack)
        assertTrue("Tasbeeh track must point to ar_131", tasbeehTrack!!.audioUrl.contains("ar_131"))
    }
}
