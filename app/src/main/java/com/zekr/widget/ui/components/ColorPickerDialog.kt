package com.zekr.widget.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zekr.widget.ui.animation.bouncyClickable

data class IslamicColorSwatch(
    val titleArabic: String,
    val colorLong: Long
)

val CURATED_ISLAMIC_SWATCHES = listOf(
    IslamicColorSwatch("أسود كحل أندلسي", 0xFF0D1310),
    IslamicColorSwatch("زمردي ملكي", 0xFF0A3D28),
    IslamicColorSwatch("كسوة الكعبة", 0xFF1A1A1A),
    IslamicColorSwatch("ذهب أندلسي", 0xFF4A3816),
    IslamicColorSwatch("أزرق ياقوتي", 0xFF0D253D),
    IslamicColorSwatch("عقيق قرمزي", 0xFF3A1118),
    IslamicColorSwatch("مرمر أخضر", 0xFF2E5D4B),
    IslamicColorSwatch("فيروزي بحري", 0xFF0C383B),
    IslamicColorSwatch("عنبر دافئ", 0xFF3D270A),
    IslamicColorSwatch("زيتوني مبارك", 0xFF1D3318),
    IslamicColorSwatch("كثبان الصحراء", 0xFF4A2511),
    IslamicColorSwatch("لؤلؤ ضبابي", 0xFFE0E5E2)
)

/**
 * Modern Islamic Color Picker Dialog supporting:
 * - 6-digit hex input with real-time validation and preview
 * - Red, Green, Blue sliders with numerical indicators
 * - 12 curated Islamic swatches
 * - Live adaptive text contrast indicator
 * - Spring physics and tactile animations
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    initialColor: Long,
    title: String = "اختيار لون مخصص",
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit
) {
    var red by remember { mutableIntStateOf((initialColor shr 16 and 0xFF).toInt()) }
    var green by remember { mutableIntStateOf((initialColor shr 8 and 0xFF).toInt()) }
    var blue by remember { mutableIntStateOf((initialColor and 0xFF).toInt()) }

    var hexText by remember {
        mutableStateOf(String.format("%02X%02X%02X", red, green, blue))
    }
    var hexError by remember { mutableStateOf(false) }

    val currentColorLong = remember(red, green, blue) {
        0xFF000000L or ((red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong())
    }

    val animatedCurrentColor by animateColorAsState(
        targetValue = Color(currentColorLong),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "color_picker_anim"
    )

    // Contrast calculation
    val lum = remember(red, green, blue) {
        (0.299f * red + 0.587f * green + 0.114f * blue) / 255f
    }
    val adaptiveTextColor = if (lum > 0.62f) Color(0xFF1C1C1E) else Color.White
    val contrastLabel = if (lum > 0.62f) "تباين ممتاز (نص داكن)" else "تباين ممتاز (نص فاتح)"

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val compact = configuration.screenWidthDp < 380
    val dialogWidthFraction = if (configuration.screenWidthDp < 360) 0.96f else 0.92f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(dialogWidthFraction)
                    .heightIn(max = (configuration.screenHeightDp * 0.88f).dp)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(if (compact) 16.dp else 20.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Hex أو RGB أو اختر من الدرجات الإسلامية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (compact) 88.dp else 96.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(animatedCurrentColor)
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.20f),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${hexText.uppercase()}",
                                    color = adaptiveTextColor.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = adaptiveTextColor.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = contrastLabel,
                                        color = adaptiveTextColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
                                color = adaptiveTextColor,
                                fontSize = if (compact) 14.sp else 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.take(6)
                            hexText = filtered
                            if (filtered.length == 6) {
                                try {
                                    val parsed = filtered.toLong(16)
                                    red = (parsed shr 16 and 0xFF).toInt()
                                    green = (parsed shr 8 and 0xFF).toInt()
                                    blue = (parsed and 0xFF).toInt()
                                    hexError = false
                                } catch (_: Exception) {
                                    hexError = true
                                }
                            } else {
                                hexError = filtered.isNotEmpty() && filtered.length < 6
                            }
                        },
                        label = { Text("رمز اللون (Hex)") },
                        prefix = { Text("# ", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        isError = hexError,
                        supportingText = {
                            if (hexError) {
                                Text("أدخل 6 خانات Hex صالحة (مثال: 0A3D28)")
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(if (compact) 10.dp else 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الأحمر (R)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFEF5350)
                            )
                            Text(text = "$red", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = red.toFloat(),
                            onValueChange = {
                                red = it.toInt()
                                hexText = String.format("%02X%02X%02X", red, green, blue)
                                hexError = false
                            },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFEF5350),
                                activeTrackColor = Color(0xFFEF5350).copy(alpha = 0.7f)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الأخضر (G)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF66BB6A)
                            )
                            Text(text = "$green", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = green.toFloat(),
                            onValueChange = {
                                green = it.toInt()
                                hexText = String.format("%02X%02X%02X", red, green, blue)
                                hexError = false
                            },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF66BB6A),
                                activeTrackColor = Color(0xFF66BB6A).copy(alpha = 0.7f)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الأزرق (B)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF42A5F5)
                            )
                            Text(text = "$blue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = blue.toFloat(),
                            onValueChange = {
                                blue = it.toInt()
                                hexText = String.format("%02X%02X%02X", red, green, blue)
                                hexError = false
                            },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF42A5F5),
                                activeTrackColor = Color(0xFF42A5F5).copy(alpha = 0.7f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "درجات إسلامية منتقاة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    // FlowRow avoids nested LazyVerticalGrid + verticalScroll crashes/overflow
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = if (compact) 4 else 6
                    ) {
                        CURATED_ISLAMIC_SWATCHES.forEach { swatch ->
                            val isSelected =
                                (currentColorLong and 0x00FFFFFFL) == (swatch.colorLong and 0x00FFFFFFL)
                            Box(
                                modifier = Modifier
                                    .size(if (compact) 34.dp else 36.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch.colorLong))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Gray.copy(alpha = 0.35f)
                                        },
                                        shape = CircleShape
                                    )
                                    .bouncyClickable {
                                        red = (swatch.colorLong shr 16 and 0xFF).toInt()
                                        green = (swatch.colorLong shr 8 and 0xFF).toInt()
                                        blue = (swatch.colorLong and 0xFF).toInt()
                                        hexText = String.format("%02X%02X%02X", red, green, blue)
                                        hexError = false
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("إلغاء")
                        }

                        Button(
                            onClick = {
                                onColorSelected(currentColorLong)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = if (compact) "تأكيد" else "تأكيد الاختيار",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
