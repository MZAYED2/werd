package com.zekr.widget.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.ui.theme.sourceTextColor
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun HomeDhikrCard(
    item: ZekrEntity,
    favorite: Boolean,
    onNext: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onRecordRead: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(item.arabicText, fontSize = 23.sp, lineHeight = 39.sp, textAlign = TextAlign.Center)
            if (item.source.isNotBlank()) {
                Text(
                    item.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = sourceTextColor,
                    textAlign = TextAlign.Center
                )
            }
            if (onRecordRead != null) {
                var isReadRecorded by remember(item.id) { mutableStateOf(false) }
                FilledTonalButton(
                    onClick = {
                        if (!isReadRecorded) {
                            isReadRecorded = true
                            onRecordRead()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isReadRecorded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        contentColor = if (isReadRecorded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text(
                        if (isReadRecorded) "تقبل الله · تم تسجيل القراءة ✓" else "تمت القراءة ✓",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        if (favorite) "إزالة من المفضلة" else "إضافة إلى المفضلة"
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, "مشاركة الذكر")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onNext, colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )) { Text("ذكر آخر") }
            }
        }
    }
}

@Composable
internal fun TasbeehNavIcon(iconSize: androidx.compose.ui.unit.Dp = 24.dp) {
    val color = LocalContentColor.current
    Canvas(Modifier.size(iconSize)) {
        val unit = size.minDimension / 24f
        repeat(10) { index ->
            val angle = index * (2.0 * Math.PI / 10)
            drawCircle(color, 1.35f * unit, Offset(
                (12f + 6.6f * cos(angle).toFloat()) * unit,
                (9.5f + 6.6f * sin(angle).toFloat()) * unit
            ))
        }
        drawLine(color, Offset(12f * unit, 16f * unit), Offset(12f * unit, 21f * unit), 1.5f * unit)
        drawCircle(color, 1.6f * unit, Offset(12f * unit, 21f * unit))
    }
}
