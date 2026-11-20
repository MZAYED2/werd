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
        // أذكار اليوم والليلة
        AudioTrack(
            id = "athkar_morning_evening",
            title = "أذكار الصباح والمساء",
            subtitle = "الأذكار المأثورة للصباح والمساء كاملة",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_028_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار اليوم والليلة"
        ),
        AudioTrack(
            id = "athkar_sleep",
            title = "أذكار النوم",
            subtitle = "أذكار ما قبل النوم والاضطجاع كاملة",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_029_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار اليوم والليلة"
        ),
        AudioTrack(
            id = "athkar_istiqadh",
            title = "أذكار الاستيقاظ من النوم",
            subtitle = "الحمد لله الذي أحيانا بعد ما أماتنا وإليه النشور",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_002_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار اليوم والليلة"
        ),

        // أذكار الصلاة والمساجد
        AudioTrack(
            id = "athkar_after_prayer",
            title = "الأذكار بعد السلام من الصلاة",
            subtitle = "الأذكار المشروعة دبر الصلوات المكتوبة",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_026_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار الصلاة"
        ),
        AudioTrack(
            id = "athkar_adhan",
            title = "أذكار وسنن الأذان",
            subtitle = "ما يقال عند سماع الأذان والصلاة على النبي ﷺ والوسيلة",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_016_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار الصلاة"
        ),
        AudioTrack(
            id = "dua_istiftah",
            title = "دعاء الاستفتاح في الصلاة",
            subtitle = "اللهم باعد بيني وبين خطاياي / سبحانك اللهم وبحمدك",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_017_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار الصلاة"
        ),
        AudioTrack(
            id = "dua_after_tashahhud",
            title = "الدعاء بعد التشهد الأخير قبل السلام",
            subtitle = "الاستعاذة من عذاب جهنم وعذاب القبر وفتنة المحيا والممات",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_025_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار الصلاة"
        ),
        AudioTrack(
            id = "dua_witr_qunut",
            title = "دعاء قنوت الوتر",
            subtitle = "اللهم اهدني فيمن هديت وعافني فيمن عافيت وتولني فيمن توليت",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_033_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار الصلاة"
        ),
        AudioTrack(
            id = "dua_mosque_enter",
            title = "دعاء دخول المسجد",
            subtitle = "بسم الله والصلاة والسلام على رسول الله اللهم افتح لي أبواب رحمتك",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_014_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار الصلاة"
        ),
        AudioTrack(
            id = "dua_mosque_exit",
            title = "دعاء الخروج من المسجد",
            subtitle = "اللهم إني أسألك من فضلك اللهم اعصمني من الشيطان الرجيم",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_015_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أذكار الصلاة"
        ),

        // تفريج الكروب والهموم
        AudioTrack(
            id = "dua_worry_grief",
            title = "دعاء الهم والحزن",
            subtitle = "اللهم إني أعوذ بك من الهم والحزن والعجز والكسل",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_035_Hisn_Almuslim_AlDuraihim.mp3",
            category = "تفريج الكروب"
        ),
        AudioTrack(
            id = "dua_karb",
            title = "دعاء الكرب وتفريج الشدائد",
            subtitle = "لا إله إلا الله العظيم الحليم لا إله إلا الله رب العرش العظيم",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_036_Hisn_Almuslim_AlDuraihim.mp3",
            category = "تفريج الكروب"
        ),
        AudioTrack(
            id = "dua_debt",
            title = "دعاء قضاء الدين",
            subtitle = "اللهم اكفني بحلالك عن حرامك وأغنني بفضلك عمن سواك",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_042_Hisn_Almuslim_AlDuraihim.mp3",
            category = "تفريج الكروب"
        ),
        AudioTrack(
            id = "dua_difficulty",
            title = "دعاء من استصعب عليه أمر",
            subtitle = "اللهم لا سهل إلا ما جعلته سهلاً وأنت تجعل الحزن إذا شئت سهلاً",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_044_Hisn_Almuslim_AlDuraihim.mp3",
            category = "تفريج الكروب"
        ),
        AudioTrack(
            id = "dua_calamity",
            title = "دعاء من أصيب بمصيبة",
            subtitle = "إنا لله وإنا إليه راجعون اللهم أجرني في مصيبتي وأخلف لي خيراً منها",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_054_Hisn_Almuslim_AlDuraihim.mp3",
            category = "تفريج الكروب"
        ),
        AudioTrack(
            id = "dua_whisper",
            title = "دعاء طرد الشيطان ووساوسه",
            subtitle = "الاستعاذة بالله والانتهاء عما يوسوس به الشيطان",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_046_Hisn_Almuslim_AlDuraihim.mp3",
            category = "تفريج الكروب"
        ),

        // السفر والتنقل
        AudioTrack(
            id = "dua_riding",
            title = "دعاء ركوب السيارة أو الدابة",
            subtitle = "سبحان الذي سخر لنا هذا وما كنا له مقرنين وإنا إلى ربنا لمنقلبون",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_096_Hisn_Almuslim_AlDuraihim.mp3",
            category = "السفر والتنقل"
        ),
        AudioTrack(
            id = "dua_travel",
            title = "دعاء السفر كاملاً",
            subtitle = "اللهم إنا نسألك في سفرنا هذا البر والتقوى ومن العمل ما ترضى",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_097_Hisn_Almuslim_AlDuraihim.mp3",
            category = "السفر والتنقل"
        ),
        AudioTrack(
            id = "dua_return_travel",
            title = "ذكر الرجوع من السفر",
            subtitle = "آيبون تائبون عابدون لربنا حامدون",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_106_Hisn_Almuslim_AlDuraihim.mp3",
            category = "السفر والتنقل"
        ),

        // أدعية الأحوال والتعاملات
        AudioTrack(
            id = "dua_home_exit",
            title = "الذكر عند الخروج من المنزل",
            subtitle = "بسم الله توكلت على الله ولا حول ولا قوة إلا بالله",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_011_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أدعية الأحوال"
        ),
        AudioTrack(
            id = "dua_home_enter",
            title = "الذكر عند دخول المنزل",
            subtitle = "بسم الله ولجنا وبسم الله خرجنا وعلى ربنا توكلنا والسلام على الأهل",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_012_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أدعية الأحوال"
        ),
        AudioTrack(
            id = "dua_market",
            title = "دعاء دخول السوق",
            subtitle = "لا إله إلا الله وحده لا شريك له له الملك وله الحمد يحيي ويميت",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_099_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أدعية الأحوال"
        ),
        AudioTrack(
            id = "dua_food_end",
            title = "الدعاء عند الفراغ من الطعام",
            subtitle = "الحمد لله الذي أطعمني هذا ورزقنيه من غير حول مني ولا قوة",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_071_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أدعية الأحوال"
        ),
        AudioTrack(
            id = "dua_fasting_iftar",
            title = "الدعاء عند إفطار الصائم",
            subtitle = "ذهب الظمأ وابتلت العروق وثبت الأجر إن شاء الله",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_069_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أدعية الأحوال"
        ),
        AudioTrack(
            id = "dua_rain",
            title = "الدعاء إذا نزل المطر",
            subtitle = "اللهم صيباً نافعاً · مطرنا بفضل الله ورحمته",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_065_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أدعية الأحوال"
        ),
        AudioTrack(
            id = "dua_visiting_sick",
            title = "الدعاء للمريض في عيادته",
            subtitle = "لا بأس طهور إن شاء الله · أسأل الله العظيم رب العرش العظيم أن يشفيك",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_050_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أدعية الأحوال"
        ),
        AudioTrack(
            id = "dua_body_pain",
            title = "ما يقول من أحس وجعاً في جسده (الرقية الذاتية)",
            subtitle = "ضع يدك على الذي تألم وقل: بسم الله (ثلاثاً) وأعوذ بالله وقدرته من شر ما أجد وأحاذر",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_125_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أدعية الأحوال"
        ),
        AudioTrack(
            id = "dua_evil_eye",
            title = "دعاء التحصين من العين والحسد",
            subtitle = "ما يقول من خشي أن يصيب شيئاً بعينه والتبريك",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_126_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أدعية الأحوال"
        ),
        AudioTrack(
            id = "dua_majlis_kaffarah",
            title = "دعاء كفارة المجلس",
            subtitle = "سبحانك اللهم وبحمدك أشهد أن لا إله إلا أنت أستغفرك وأتوب إليك",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_086_Hisn_Almuslim_AlDuraihim.mp3",
            category = "أدعية الأحوال"
        ),

        // الاستغفار والتسبيح والأدعية الجامعة
        AudioTrack(
            id = "dua_istikharah",
            title = "دعاء صلاة الاستخارة",
            subtitle = "اللهم إني أستخيرك بعلمك وأستقدرك بقدرتك وأسألك من فضلك العظيم",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_027_Hisn_Almuslim_AlDuraihim.mp3",
            category = "الأدعية المأثورة"
        ),
        AudioTrack(
            id = "athkar_istighfar",
            title = "الاستغفار والتوبة وسيد الاستغفار",
            subtitle = "اللهم أنت ربي لا إله إلا أنت خلقتني وأنا عبدك",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_130_Hisn_Almuslim_AlDuraihim.mp3",
            category = "الاستغفار والتسبيح"
        ),
        AudioTrack(
            id = "athkar_salawat",
            title = "فضل الصلاة على النبي ﷺ وكيفيتها",
            subtitle = "الصيغ المشروعة وفضل الصلاة على النبي ﷺ",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_108_Hisn_Almuslim_AlDuraihim.mp3",
            category = "الاستغفار والتسبيح"
        ),
        AudioTrack(
            id = "athkar_tasbeeh_virtue",
            title = "فضل التسبيح والتحميد والتهليل والتكبير",
            subtitle = "الباقيات الصالحات وأحب الكلام إلى الله تعالى",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_131_Hisn_Almuslim_AlDuraihim.mp3",
            category = "الاستغفار والتسبيح"
        ),
        AudioTrack(
            id = "dua_day_of_arafah",
            title = "الدعاء يوم عرفة",
            subtitle = "خير الدعاء دعاء يوم عرفة وخير ما قلت أنا والنبيون من قبلي",
            reciterOrSpeaker = "الشيخ حمد الدريهم",
            sourceName = "دار الإسلام (IslamHouse)",
            sourceUrl = "https://islamhouse.com/ar/audios/263352",
            audioUrl = "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Hisn_Almuslim/Hisn_Almuslim_AlDuraihim/ar_120_Hisn_Almuslim_AlDuraihim.mp3",
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
