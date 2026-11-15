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

    fun getItems(context: Context): List<ZekrEntity> {
        cachedItems?.let { return it }

        return try {
            context.assets.open("data/sahih_content.json").use { stream ->
                val reader = InputStreamReader(stream)
                val content = reader.readText()
                val parsed = json.decodeFromString<List<ZekrEntity>>(content)
                cachedItems = parsed
                parsed
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackItems
        }
    }

    fun getById(context: Context, id: Int): ZekrEntity {
        val items = getItems(context)
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

    /**
     * Pure non-repeating deck selector with optional Time-of-Day smart context weighting.
     *
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
        currentId: Int? = null,
        seenIds: Set<Int> = emptySet(),
        hourOfDay: Int = currentHourOfDay(),
        random: Random = Random.Default
    ): NextZekrResult {
        val baseItems = allItems.ifEmpty { fallbackItems }

        // 1. Filter by enabled content types
        val typeFiltered = baseItems.filter { item ->
            when (item.type) {
                ContentType.QURAN_AYAH -> includeQuran
                ContentType.SAHIH_HADITH -> includeHadith
                ContentType.AUTHENTIC_ZEKR -> includeAzkar
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
        currentId: Int? = null,
        seenIds: Set<Int> = emptySet(),
        hourOfDay: Int = currentHourOfDay()
    ): ZekrEntity {
        return selectNextFromPool(
            allItems = getItems(context),
            includeQuran = includeQuran,
            includeHadith = includeHadith,
            includeAzkar = includeAzkar,
            smartTimeContextEnabled = smartTimeContextEnabled,
            currentId = currentId,
            seenIds = seenIds,
            hourOfDay = hourOfDay
        ).item
    }

    private val fallbackItems = listOf(
        ZekrEntity(
            id = 1,
            arabicText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            type = ContentType.SAHIH_HADITH,
            source = "صحيح البخاري: 6406",
            reference = "كلمتان خفيفتان على اللسان ثقيلتان في الميزان",
            category = "التسبيح",
            englishMeaning = "Glory be to Allah and His praise, Glory be to Allah the Great"
        ),
        ZekrEntity(
            id = 2,
            arabicText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
            type = ContentType.QURAN_AYAH,
            source = "سورة البقرة: 255",
            reference = "آية الكرسي",
            category = "آيات الحفظ",
            englishMeaning = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of all existence."
        )
    )
}
