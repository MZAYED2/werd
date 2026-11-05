package com.zekr.widget.data.repository

import android.content.Context
import com.zekr.widget.data.model.ContentType
import com.zekr.widget.data.model.ZekrEntity
import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import kotlin.random.Random

object ZekrRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private var cachedItems: List<ZekrEntity>? = null

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

    fun getRandomItem(
        context: Context,
        includeQuran: Boolean = true,
        includeHadith: Boolean = true,
        includeAzkar: Boolean = true,
        currentId: Int? = null
    ): ZekrEntity {
        val items = getItems(context)
        val filtered = items.filter { item ->
            when (item.type) {
                ContentType.QURAN_AYAH -> includeQuran
                ContentType.SAHIH_HADITH -> includeHadith
                ContentType.AUTHENTIC_ZEKR -> includeAzkar
            }
        }

        val pool = if (filtered.isEmpty()) items else filtered

        val candidates = if (pool.size > 1 && currentId != null) {
            pool.filter { it.id != currentId }
        } else {
            pool
        }

        return candidates[Random.nextInt(candidates.size)]
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
