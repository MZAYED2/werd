package com.zekr.widget.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.zekr.widget.data.preferences.GlassColorPreset
import com.zekr.widget.data.preferences.PatternType
import com.zekr.widget.data.preferences.WidgetFont
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.preferences.WidgetSettings
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.service.ScreenLockService
import com.zekr.widget.widget.ZekrGlanceWidget
import com.zekr.widget.widget.render.GlassRenderer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefRepo = remember { WidgetPreferencesRepository(context) }
    val settings by prefRepo.settingsFlow.collectAsState(initial = WidgetSettings())

    var previewItemId by remember { mutableIntStateOf(1) }
    val currentItem = remember(previewItemId, settings) {
        ZekrRepository.getById(context, previewItemId)
    }

    // Launch or stop Foreground Service when setting changes
    LaunchedEffect(settings.serviceEnabled) {
        if (settings.serviceEnabled) {
            ScreenLockService.start(context)
        } else {
            ScreenLockService.stop(context)
        }
    }

    fun updateAndSync(newSettings: WidgetSettings) {
        scope.launch {
            prefRepo.updateSettings(newSettings)
            ZekrGlanceWidget().updateAll(context)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "استوديو ويدجت الأذكار والصحيحين",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Glass Preview Card
            item {
                Text(
                    text = "معاينة الويدجت التفاعلي (انقر للتجربة)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                LiveGlassWidgetPreview(
                    itemText = currentItem.arabicText,
                    itemType = currentItem.type.labelArabic,
                    itemSource = if (currentItem.reference.isNotEmpty()) "${currentItem.source} • ${currentItem.reference}" else currentItem.source,
                    settings = settings,
                    onClick = {
                        val next = ZekrRepository.getRandomItem(
                            context = context,
                            includeQuran = settings.includeQuran,
                            includeHadith = settings.includeHadith,
                            includeAzkar = settings.includeAzkar,
                            currentId = previewItemId
                        )
                        previewItemId = next.id
                        scope.launch {
                            prefRepo.setCurrentItemId(next.id)
                            ZekrGlanceWidget().updateAll(context)
                        }
                    }
                )
            }

            // Patterns & Arabesques Section
            item {
                Text(
                    text = "النقوش والزخارف الإسلامية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(PatternType.values()) { pattern ->
                        val isSelected = pattern == settings.pattern
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .height(64.dp)
                                .clickable {
                                    updateAndSync(settings.copy(pattern = pattern))
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pattern.titleArabic,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Glass Colors & Presets Section
            item {
                Text(
                    text = "طابع ولون الزجاج",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GlassColorPreset.values().forEach { preset ->
                        val isSelected = preset == settings.colorPreset
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                updateAndSync(settings.copy(colorPreset = preset))
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(preset.hexColor))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preset.titleArabic,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Glass Transparency Slider
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "درجة شفافية الزجاج",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(settings.glassOpacity * 100).toInt()}%",
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.glassOpacity,
                    onValueChange = { newOpacity ->
                        updateAndSync(settings.copy(glassOpacity = newOpacity))
                    },
                    valueRange = 0.15f..0.90f
                )
            }

            // Typography & Font Size Section
            item {
                Text(
                    text = "نوع وحجم الخط",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetFont.values().forEach { font ->
                        FilterChip(
                            selected = font == settings.font,
                            onClick = { updateAndSync(settings.copy(font = font)) },
                            label = { Text(font.titleArabic, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حجم الخط في الويدجت", style = MaterialTheme.typography.bodyMedium)
                    Text("${settings.fontSizeSp} sp", fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.fontSizeSp.toFloat(),
                    onValueChange = { newSize ->
                        updateAndSync(settings.copy(fontSizeSp = newSize.toInt()))
                    },
                    valueRange = 14f..26f,
                    steps = 11
                )
            }

            // Content Categories Filter
            item {
                Text(
                    text = "المحتوى المعروض (من الصحيحين والقرآن فقط)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = settings.includeQuran,
                        onClick = { updateAndSync(settings.copy(includeQuran = !settings.includeQuran)) },
                        label = { Text("القرآن الكريم") }
                    )
                    FilterChip(
                        selected = settings.includeHadith,
                        onClick = { updateAndSync(settings.copy(includeHadith = !settings.includeHadith)) },
                        label = { Text("الحديث الصحيح") }
                    )
                    FilterChip(
                        selected = settings.includeAzkar,
                        onClick = { updateAndSync(settings.copy(includeAzkar = !settings.includeAzkar)) },
                        label = { Text("الأذكار المأثورة") }
                    )
                }
            }

            // Screen Lock/Unlock Shuffle Switch
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "التبديل التلقائي عند قفل/فتح الشاشة",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تحديث الذكر تلقائياً في كل مرة تغلق أو تفتح فيها شاشة هاتفك",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.serviceEnabled,
                            onCheckedChange = { enabled ->
                                updateAndSync(settings.copy(serviceEnabled = enabled))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveGlassWidgetPreview(
    itemText: String,
    itemType: String,
    itemSource: String,
    settings: WidgetSettings,
    onClick: () -> Unit
) {
    val bitmap: Bitmap = remember(settings) {
        GlassRenderer.createGlassBackgroundBitmap(
            width = 680,
            height = 380,
            colorPreset = settings.colorPreset,
            opacity = settings.glassOpacity,
            pattern = settings.pattern,
            cornerRadius = 56f
        )
    }

    val textColor = Color(settings.colorPreset.textColor)
    val accentColor = Color(settings.colorPreset.accentColor)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⟳ انقر للتبديل",
                    color = accentColor.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
                Text(
                    text = itemType,
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = itemText,
                    color = textColor,
                    fontSize = settings.fontSizeSp.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = (settings.fontSizeSp * 1.45f).sp
                )
            }

            Text(
                text = itemSource,
                color = textColor.copy(alpha = 0.82f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
