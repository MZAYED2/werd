package com.zekr.widget.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import com.zekr.widget.R
import com.zekr.widget.data.preferences.WidgetFont

object FontHelper {

    fun getComposeFontFamily(font: WidgetFont): FontFamily {
        val resId = when (font) {
            WidgetFont.AMIRI -> R.font.amiri
            WidgetFont.UTHMANIC -> R.font.uthmanic
            WidgetFont.CAIRO -> R.font.cairo
            WidgetFont.NASKH -> R.font.naskh
        }
        return FontFamily(Font(resId))
    }

    fun getTypeface(context: Context, font: WidgetFont): Typeface {
        val resId = when (font) {
            WidgetFont.AMIRI -> R.font.amiri
            WidgetFont.UTHMANIC -> R.font.uthmanic
            WidgetFont.CAIRO -> R.font.cairo
            WidgetFont.NASKH -> R.font.naskh
        }
        return ResourcesCompat.getFont(context, resId) ?: Typeface.SERIF
    }
}
