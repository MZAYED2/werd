package com.zekr.widget.audio

data class AudioTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val reciterOrSpeaker: String,
    val sourceName: String,
    val sourceUrl: String,
    val audioUrl: String,
    val category: String,
    val surahNumber: Int? = null
)

object AudioCatalog {
    enum class QuranReciter(
        val id: String,
        val displayName: String,
        val sourceUrl: String,
        val cdnFolder: String,
        val shortName: String
    ) {
        MINSHAWI(
            id = "minshawi",
            displayName = "محمد صديق المنشاوي (المرتّل)",
            sourceUrl = "https://quranicaudio.com/quran/6",
            cdnFolder = "muhammad_siddeeq_al-minshaawee",
            shortName = "محمد صديق المنشاوي"
        ),
        ABDULBASIT(
            id = "abdulbasit",
            displayName = "عبد الباسط عبد الصمد (المرتّل)",
            sourceUrl = "https://quranicaudio.com/quran/37",
            cdnFolder = "abdul_basit_murattal",
            shortName = "عبد الباسط عبد الصمد"
        )
    }

    val SURAH_NAMES = listOf(
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

    private val minshawiTracks: List<AudioTrack> by lazy {
        SURAH_NAMES.mapIndexed { index, name ->
            val num = index + 1
            val paddedNum = num.toString().padStart(3, '0')
            AudioTrack(
                id = "quran_minshawi_$paddedNum",
                title = "سورة $name",
                subtitle = "القرآن الكريم · المرتّل · السورة $num",
                reciterOrSpeaker = QuranReciter.MINSHAWI.shortName,
                sourceName = "QuranicAudio",
                sourceUrl = QuranReciter.MINSHAWI.sourceUrl,
                audioUrl = "https://download.quranicaudio.com/quran/${QuranReciter.MINSHAWI.cdnFolder}/$paddedNum.mp3",
                category = "القرآن الكريم",
                surahNumber = num
            )
        }
    }

    private val abdulbasitTracks: List<AudioTrack> by lazy {
        SURAH_NAMES.mapIndexed { index, name ->
            val num = index + 1
            val paddedNum = num.toString().padStart(3, '0')
            AudioTrack(
                id = "quran_abdulbasit_$paddedNum",
                title = "سورة $name",
                subtitle = "القرآن الكريم · المرتّل · السورة $num",
                reciterOrSpeaker = QuranReciter.ABDULBASIT.shortName,
                sourceName = "QuranicAudio",
                sourceUrl = QuranReciter.ABDULBASIT.sourceUrl,
                audioUrl = "https://download.quranicaudio.com/quran/${QuranReciter.ABDULBASIT.cdnFolder}/$paddedNum.mp3",
                category = "القرآن الكريم",
                surahNumber = num
            )
        }
    }

    fun getQuranTracks(reciter: QuranReciter): List<AudioTrack> = when (reciter) {
        QuranReciter.MINSHAWI -> minshawiTracks
        QuranReciter.ABDULBASIT -> abdulbasitTracks
    }

    val AthkarTracks = listOf(
        AudioTrack(
            id = "athkar_istiqadh",
            title = "أذكار الاستيقاظ من النوم",
            subtitle = "حصن المسلم من أذكار الكتاب والسنة",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_002_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار الاستيقاظ"
        ),
        AudioTrack(
            id = "athkar_morning_evening",
            title = "أذكار الصباح والمساء",
            subtitle = "حصن المسلم من أذكار الكتاب والسنة",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_027_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار الصباح والمساء"
        ),
        AudioTrack(
            id = "athkar_sleep",
            title = "أذكار النوم",
            subtitle = "حصن المسلم من أذكار الكتاب والسنة",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_028_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار النوم"
        ),
        AudioTrack(
            id = "athkar_after_prayer",
            title = "أذكار دبر الصلاة المفروضة",
            subtitle = "الأذكار المشروعة بعد السلام من الصلاة",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_025_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار الصلاة"
        ),
        AudioTrack(
            id = "athkar_istighfar",
            title = "الاستغفار والتوبة",
            subtitle = "أدعية الاستغفار وسيد الاستغفار والتوبة",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_128_Hisn_Almuslim_AlDuraihim.mp3",
            category = "الاستغفار والتوبة"
        ),
        AudioTrack(
            id = "athkar_salawat",
            title = "فضل الصلاة على النبي ﷺ وكيفيتها",
            subtitle = "الصيغ المشروعة في الصلاة على النبي ﷺ",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_127_Hisn_Almuslim_AlDuraihim.mp3",
            category = "الصلاة على النبي ﷺ"
        ),
        AudioTrack(
            id = "athkar_tasbeeh_virtue",
            title = "فضل التسبيح والتحميد والتهليل والتكبير",
            subtitle = "الباقيات الصالحات وفضائل الأذكار الجامعة",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_129_Hisn_Almuslim_AlDuraihim.mp3",
            category = "فضل الذكر والتسبيح"
        ),
        AudioTrack(
            id = "dua_travel",
            title = "دعاء السفر",
            subtitle = "دعاء الركوب والسفر والرجوع",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_096_Hisn_Almuslim_AlDuraihim.mp3",
            category = "الأدعية المأثورة"
        ),
        AudioTrack(
            id = "dua_home_exit",
            title = "دعاء الخروج من المنزل",
            subtitle = "ما يقول المسلم إذا خرج من بيته",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_011_Hisn_Almuslim_AlDuraihim.mp3",
            category = "الأدعية المأثورة"
        ),
        AudioTrack(
            id = "dua_home_enter",
            title = "دعاء الدخول إلى المنزل",
            subtitle = "ما يقول المسلم إذا دخل بيته والسلام على أهله",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_012_Hisn_Almuslim_AlDuraihim.mp3",
            category = "الأدعية المأثورة"
        ),
        AudioTrack(
            id = "dua_istikharah",
            title = "دعاء صلاة الاستخارة",
            subtitle = "دعاء الاستخارة في الأمور كلها",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_057_Hisn_Almuslim_AlDuraihim.mp3",
            category = "الأدعية المأثورة"
        ),
        AudioTrack(
            id = "dua_worry_grief",
            title = "دعاء الهم والحزن والكرب",
            subtitle = "أدعية تفريج الكرب والهم والحزن وقضاء الدين",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_034_Hisn_Almuslim_AlDuraihim.mp3",
            category = "الأدعية المأثورة"
        )
    )

    fun getAllTracks(reciter: QuranReciter): List<AudioTrack> {
        return getQuranTracks(reciter) + AthkarTracks
    }

    fun findTrackById(id: String): AudioTrack? {
        val all = getQuranTracks(QuranReciter.MINSHAWI) +
                  getQuranTracks(QuranReciter.ABDULBASIT) +
                  AthkarTracks
        return all.firstOrNull { it.id == id }
    }
}
