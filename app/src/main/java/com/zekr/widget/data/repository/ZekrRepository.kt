package com.zekr.widget.data.repository

import android.content.Context
import com.zekr.widget.data.model.ContentType
import com.zekr.widget.data.model.ZekrEntity
import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import java.util.Calendar
import kotlin.random.Random

enum class TimePeriod {
    MORNING,
    MIDDAY,
    EVENING,
    NIGHT
}

enum class DhikrCategory(val id: String) {
    SALAWAT("salawat"),
    TASBEEH("tasbeeh"),
    GENERAL("general"),
    WIRD("wird");

    companion object {
        fun fromId(id: String?): DhikrCategory? = values().find { it.id == id }
    }
}

data class NextZekrResult(
    val item: ZekrEntity,
    val updatedSeenIds: Set<Int>
)

object ZekrRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private var cachedItems: List<ZekrEntity>? = null

    private const val CATEGORY_MORNING_ONLY = "أذكار الصباح"
    private const val CATEGORY_EVENING_ONLY = "أذكار المساء"
    private const val CATEGORY_MORNING_EVENING = "أذكار الصباح والمساء"
    private const val CATEGORY_SLEEP = "أذكار النوم"
    private const val CATEGORY_PROTECTION_AYAT = "آيات الحفظ"
    private const val CATEGORY_PROTECTION_HADITH = "الحفظ والوقاية"

    private val ARABIC_DIACRITICS_REGEX = Regex("[\u064B-\u065F\u0670\u06D6-\u06ED]")

    fun getItems(context: Context): List<ZekrEntity> {
        cachedItems?.let { return it }

        return try {
            context.assets.open("data/sahih_content.json").use { stream ->
                val reader = InputStreamReader(stream, Charsets.UTF_8)
                val content = reader.readText().removePrefix("\uFEFF").trim()
                val parsed = json.decodeFromString<List<ZekrEntity>>(content)
                cachedItems = parsed
                parsed
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackItems
        }
    }

    fun getAllItems(context: Context, customItems: List<ZekrEntity> = emptyList()): List<ZekrEntity> {
        val builtIn = getItems(context)
        return if (customItems.isEmpty()) builtIn else customItems + builtIn
    }

    fun getById(
        context: Context,
        id: Int,
        customItems: List<ZekrEntity> = emptyList()
    ): ZekrEntity {
        val items = getAllItems(context, customItems)
        return items.find { it.id == id } ?: items.firstOrNull() ?: fallbackItems.first()
    }

    fun getTimePeriodForHour(hourOfDay: Int): TimePeriod {
        val normalizedHour = ((hourOfDay % 24) + 24) % 24
        return when (normalizedHour) {
            in 4..10 -> TimePeriod.MORNING
            in 11..14 -> TimePeriod.MIDDAY
            in 15..20 -> TimePeriod.EVENING
            else -> TimePeriod.NIGHT
        }
    }

    internal fun currentHourOfDay(): Int {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }

    fun isPriorityForTime(item: ZekrEntity, period: TimePeriod): Boolean {
        return when (period) {
            TimePeriod.MORNING -> item.category == CATEGORY_MORNING_ONLY ||
                item.category == CATEGORY_MORNING_EVENING
            TimePeriod.EVENING -> item.category == CATEGORY_EVENING_ONLY ||
                item.category == CATEGORY_MORNING_EVENING
            TimePeriod.NIGHT -> item.category == CATEGORY_SLEEP ||
                item.category == CATEGORY_PROTECTION_AYAT ||
                item.category == CATEGORY_PROTECTION_HADITH
            TimePeriod.MIDDAY -> false
        }
    }

    fun isOppositeTimeCategory(item: ZekrEntity, period: TimePeriod): Boolean {
        return when (period) {
            TimePeriod.MORNING -> item.category == CATEGORY_EVENING_ONLY ||
                item.category == CATEGORY_SLEEP
            TimePeriod.MIDDAY -> item.category == CATEGORY_MORNING_ONLY ||
                item.category == CATEGORY_EVENING_ONLY ||
                item.category == CATEGORY_SLEEP
            TimePeriod.EVENING -> item.category == CATEGORY_MORNING_ONLY ||
                item.category == CATEGORY_SLEEP
            TimePeriod.NIGHT -> item.category == CATEGORY_MORNING_ONLY ||
                item.category == CATEGORY_EVENING_ONLY
        }
    }

    fun classifyDhikr(item: ZekrEntity): DhikrCategory {
        if (item.type == ContentType.QURAN_AYAH) return DhikrCategory.WIRD
        val formula = normalizeArabicForSearch(item.getEffectiveTasbeehText())
        val category = item.category
        if (category.contains("الصلاة على النبي") || formula.startsWith("اللهم صل")) {
            return DhikrCategory.SALAWAT
        }
        if (category.contains("التسبيح") || category == "الباقيات الصالحات" ||
            listOf("سبحان الله", "الحمد لله", "لا اله الا الله", "الله اكبر", "لا حول ولا قوة الا بالله")
                .any { phrase -> phrase in formula }
        ) {
            return DhikrCategory.TASBEEH
        }
        return DhikrCategory.GENERAL
    }


    fun selectNextForCategory(
        allItems: List<ZekrEntity>,
        category: DhikrCategory,
        seenIds: Set<Int> = emptySet(),
        currentId: Int? = null,
        random: Random = Random.Default
    ): NextZekrResult {
        val pool = allItems.filter { classifyDhikr(it) == category }
            .ifEmpty { allItems.filter { it.type != ContentType.QURAN_AYAH } }
        val poolIds = pool.map { it.id }.toSet()
        val effectiveSeen = (seenIds intersect poolIds).let {
            if (currentId != null && currentId in poolIds) it + currentId else it
        }
        val unseenCandidates = pool.filter { it.id !in effectiveSeen }
        val isDeckReset = unseenCandidates.isEmpty()

        val deckCandidates = if (isDeckReset) {
            if (pool.size > 1 && currentId != null) pool.filter { it.id != currentId } else pool
        } else {
            unseenCandidates
        }

        val baseSeenAfterReset = if (isDeckReset) {
            if (currentId != null && currentId in poolIds && pool.size > 1) setOf(currentId) else emptySet()
        } else {
            effectiveSeen
        }

        val picked = deckCandidates[random.nextInt(deckCandidates.size)]
        return NextZekrResult(
            item = picked,
            updatedSeenIds = baseSeenAfterReset + picked.id
        )
    }

    /**
     * Pure non-repeating deck selector with optional Favorites-only filter and Time-of-Day smart context weighting.
     *
     * - When [favoritesOnlyMode] is true and [favoriteIds] matches items, restricts the pool to favorites.
     * - Excludes items whose category belongs strictly to an opposite time of day when [smartTimeContextEnabled] is true.
     * - Uses [seenIds] as a persistent deck history so no item repeats until the entire active pool has been shown.
     * - When [smartTimeContextEnabled] is true and unseen time-priority items remain, weights selection toward
     *   time-matching items (~65%) while still mixing general Quran/Hadith/Duas.
     */
    fun selectNextFromPool(
        allItems: List<ZekrEntity>,
        includeQuran: Boolean = true,
        includeHadith: Boolean = true,
        includeAzkar: Boolean = true,
        smartTimeContextEnabled: Boolean = true,
        favoritesOnlyMode: Boolean = false,
        favoriteIds: Set<Int> = emptySet(),
        currentId: Int? = null,
        seenIds: Set<Int> = emptySet(),
        hourOfDay: Int = currentHourOfDay(),
        random: Random = Random.Default
    ): NextZekrResult {
        val rawItems = allItems.ifEmpty { fallbackItems }

        // 0. Optional Favorites-only filter
        val baseItems = if (favoritesOnlyMode && favoriteIds.isNotEmpty()) {
            rawItems.filter { it.id in favoriteIds }.ifEmpty { rawItems }
        } else {
            rawItems
        }

        // 1. Filter by enabled content types (if favoritesOnlyMode is active, keep all favorites regardless of category toggle if typeFiltered would be empty)
        val typeFiltered = baseItems.filter { item ->
            if (item.isCustom) {
                true
            } else {
                when (item.type) {
                    ContentType.QURAN_AYAH -> includeQuran
                    ContentType.SAHIH_HADITH -> includeHadith
                    ContentType.AUTHENTIC_ZEKR -> includeAzkar
                }
            }
        }.ifEmpty { baseItems }

        // 2. Apply Time-of-Day opposite-window exclusion if smart context is enabled
        val period = getTimePeriodForHour(hourOfDay)
        val activePool = if (smartTimeContextEnabled) {
            val timeAppropriate = typeFiltered.filterNot { isOppositeTimeCategory(it, period) }
            timeAppropriate.ifEmpty { typeFiltered }
        } else {
            typeFiltered
        }

        val activePoolIds = activePool.map { it.id }.toSet()
        val effectiveSeen = (seenIds intersect activePoolIds).let {
            if (currentId != null && currentId in activePoolIds) it + currentId else it
        }

        // 3. Determine unseen deck; if all active items have been seen, reset the deck!
        val unseenCandidates = activePool.filter { it.id !in effectiveSeen }
        val isDeckReset = unseenCandidates.isEmpty()

        val deckCandidates = if (isDeckReset) {
            if (activePool.size > 1 && currentId != null) {
                activePool.filter { it.id != currentId }
            } else {
                activePool
            }
        } else {
            unseenCandidates
        }

        val baseSeenAfterReset = if (isDeckReset) {
            if (currentId != null && currentId in activePoolIds && activePool.size > 1) {
                setOf(currentId)
            } else {
                emptySet()
            }
        } else {
            effectiveSeen
        }

        // 4. Time-of-day priority weighting among unseen candidates
        val chosenPool = if (smartTimeContextEnabled && period != TimePeriod.MIDDAY) {
            val priorityCandidates = deckCandidates.filter { isPriorityForTime(it, period) }
            val generalCandidates = deckCandidates.filterNot { isPriorityForTime(it, period) }
            when {
                priorityCandidates.isNotEmpty() && generalCandidates.isNotEmpty() -> {
                    if (random.nextFloat() < 0.65f) priorityCandidates else generalCandidates
                }
                priorityCandidates.isNotEmpty() -> priorityCandidates
                else -> deckCandidates
            }
        } else {
            deckCandidates
        }

        val picked = chosenPool[random.nextInt(chosenPool.size)]
        return NextZekrResult(
            item = picked,
            updatedSeenIds = baseSeenAfterReset + picked.id
        )
    }

    fun getRandomItem(
        context: Context,
        includeQuran: Boolean = true,
        includeHadith: Boolean = true,
        includeAzkar: Boolean = true,
        smartTimeContextEnabled: Boolean = true,
        favoritesOnlyMode: Boolean = false,
        favoriteIds: Set<Int> = emptySet(),
        customItems: List<ZekrEntity> = emptyList(),
        currentId: Int? = null,
        seenIds: Set<Int> = emptySet(),
        hourOfDay: Int = currentHourOfDay()
    ): ZekrEntity {
        return selectNextFromPool(
            allItems = getAllItems(context, customItems),
            includeQuran = includeQuran,
            includeHadith = includeHadith,
            includeAzkar = includeAzkar,
            smartTimeContextEnabled = smartTimeContextEnabled,
            favoritesOnlyMode = favoritesOnlyMode,
            favoriteIds = favoriteIds,
            currentId = currentId,
            seenIds = seenIds,
            hourOfDay = hourOfDay
        ).item
    }

    fun normalizeArabicForSearch(text: String): String {
        return ARABIC_DIACRITICS_REGEX.replace(text, "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .trim()
            .lowercase()
    }

    fun matchesSearch(item: ZekrEntity, query: String): Boolean {
        val normQuery = normalizeArabicForSearch(query)
        if (normQuery.isEmpty()) return true
        return normalizeArabicForSearch(item.arabicText).contains(normQuery) ||
            normalizeArabicForSearch(item.source).contains(normQuery) ||
            normalizeArabicForSearch(item.reference).contains(normQuery) ||
            normalizeArabicForSearch(item.category).contains(normQuery)
    }

    fun toArabicDigits(number: Int): String {
        val digits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return number.toString().map { ch ->
            if (ch in '0'..'9') digits[ch - '0'] else ch
        }.joinToString("")
    }

    private val fallbackItems = listOf(
        ZekrEntity(
            id = 1,
            arabicText = "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            type = ContentType.SAHIH_HADITH,
            source = "صحيح البخاري: 6406",
            reference = "باب فضل التسبيح",
            category = "التسبيح",
            englishMeaning = "Glory be to Allah and His praise, Glory be to Allah the Great",
            tasbeehText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            virtueText = "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ",
            documentedCount = null
        ),
        ZekrEntity(
            id = 2,
            arabicText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
            type = ContentType.QURAN_AYAH,
            source = "سورة البقرة: 255",
            reference = "آية الكرسي",
            category = "آيات الحفظ",
            englishMeaning = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of all existence.",
            documentedCount = 1
        )
    )
}
