package com.zekr.widget.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import com.zekr.widget.R
import com.zekr.widget.data.preferences.WidgetFont

object FontHelper {

    private val typefaceCache = mutableMapOf<WidgetFont, Typeface>()

    private fun fontResId(font: WidgetFont): Int = when (font) {
        WidgetFont.AMIRI -> R.font.amiri
        WidgetFont.UTHMANIC -> R.font.uthmanic
        WidgetFont.NASKH -> R.font.naskh
        WidgetFont.SCHEHERAZADE_NEW -> R.font.scheherazade_new
        WidgetFont.LATEEF -> R.font.lateef
        WidgetFont.AREF_RUQAA -> R.font.aref_ruqaa
        WidgetFont.KATIBEH -> R.font.katibeh
        WidgetFont.REEM_KUFI -> R.font.reem_kufi
        WidgetFont.EL_MESSIRI -> R.font.el_messiri
        WidgetFont.MARKAZI_TEXT -> R.font.markazi_text
        WidgetFont.CAIRO -> R.font.cairo
        WidgetFont.TAJAWAL -> R.font.tajawal
        WidgetFont.ALMARAI -> R.font.almarai
        WidgetFont.IBM_PLEX_SANS_ARABIC -> R.font.ibm_plex_sans_arabic
        WidgetFont.CHANGA -> R.font.changa
        WidgetFont.LALEZAR -> R.font.lalezar
        WidgetFont.JOMHURIA -> R.font.jomhuria
        WidgetFont.RAKKAS -> R.font.rakkas
    }

    private val composeFamilyCache: Map<WidgetFont, FontFamily> =
        WidgetFont.values().associateWith { font ->
            FontFamily(Font(fontResId(font)))
        }

    fun getComposeFontFamily(font: WidgetFont): FontFamily {
        return composeFamilyCache[font] ?: FontFamily.Default
    }

    fun getTypeface(context: Context, font: WidgetFont): Typeface {
        return synchronized(typefaceCache) {
            typefaceCache.getOrPut(font) {
                try {
                    val targetContext = context.applicationContext ?: context
                    ResourcesCompat.getFont(targetContext, fontResId(font)) ?: Typeface.SERIF
                } catch (e: Throwable) {
                    Typeface.SERIF
                }
            }
        }
    }
}
