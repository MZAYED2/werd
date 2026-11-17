package com.zekr.widget.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zekr.widget.data.model.ContentType
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.WidgetSettings
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.ui.theme.FontHelper

enum class LibraryFilter(val titleArabic: String) {
    ALL("الكل"),
    FAVORITES("المفضلة"),
    CUSTOM("أذكاري"),
    QURAN("القرآن"),
    HADITH("الحديث"),
    AZKAR("الأذكار")
}

@Composable
fun LibraryTabContent(
    allItems: List<ZekrEntity>,
    favoriteIds: Set<Int>,
    pinnedItemId: Int?,
    settings: WidgetSettings,
    onToggleFavoritesOnlyMode: (Boolean) -> Unit,
    onToggleFavorite: (ZekrEntity) -> Unit,
    onTogglePin: (ZekrEntity) -> Unit,
    onShareItem: (ZekrEntity) -> Unit,
    onSaveCustomItem: (ZekrEntity) -> Unit,
    onDeleteCustomItem: (ZekrEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var editingCustomItem by remember { mutableStateOf<ZekrEntity?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(allItems, searchQuery, selectedFilter, favoriteIds) {
        allItems.filter { item ->
            val matchesFilter = when (selectedFilter) {
                LibraryFilter.ALL -> true
                LibraryFilter.FAVORITES -> item.id in favoriteIds
                LibraryFilter.CUSTOM -> item.isCustom
                LibraryFilter.QURAN -> item.type == ContentType.QURAN_AYAH && !item.isCustom
                LibraryFilter.HADITH -> item.type == ContentType.SAHIH_HADITH && !item.isCustom
                LibraryFilter.AZKAR -> item.type == ContentType.AUTHENTIC_ZEKR && !item.isCustom
            }
            matchesFilter && ZekrRepository.matchesSearch(item, searchQuery)
        }
    }

    val customCount = remember(allItems) { allItems.count { it.isCustom } }
    val favCount = remember(allItems, favoriteIds) { allItems.count { it.id in favoriteIds } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Top controls: Add Custom Zekr + Favorites-Only mode switch
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            editingCustomItem = null
                            showCustomDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color(0xFF071510)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إضافة ذكر خاص",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Favorites-only rotation toggle pill
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (settings.favoritesOnlyMode) Color(0xFFFF5C7A).copy(alpha = 0.16f)
                                else Color(0xFF16181E)
                            )
                            .border(
                                1.dp,
                                if (settings.favoritesOnlyMode) Color(0xFFFF5C7A).copy(alpha = 0.55f)
                                else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onToggleFavoritesOnlyMode(!settings.favoritesOnlyMode) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "المفضلة فقط في الويدجت",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (settings.favoritesOnlyMode) Color(0xFFFF9AAD) else Color(0xFFD0D5DD),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (settings.favoritesOnlyMode) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (settings.favoritesOnlyMode) Color(0xFFFF5C7A) else Color(0xFF98A2B3),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "ابحث في الآيات والأحاديث والأذكار...",
                            color = Color(0xFF98A2B3),
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color(0xFF98A2B3)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "مسح",
                                    tint = Color(0xFF98A2B3)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF16181E),
                        unfocusedContainerColor = Color(0xFF16181E),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedTextColor = Color(0xFFF2F4F7),
                        unfocusedTextColor = Color(0xFFF2F4F7)
                    )
                )

                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LibraryFilter.entries) { filter ->
                        val label = when (filter) {
                            LibraryFilter.FAVORITES -> "${filter.titleArabic} (${ZekrRepository.toArabicDigits(favCount)})"
                            LibraryFilter.CUSTOM -> "${filter.titleArabic} (${ZekrRepository.toArabicDigits(customCount)})"
                            else -> filter.titleArabic
                        }
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16181E)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (selectedFilter) {
                                LibraryFilter.FAVORITES -> "لا توجد أذكار مفضلة بعد"
                                LibraryFilter.CUSTOM -> "لم تضف أذكاراً خاصة بعد"
                                else -> "لا توجد نتائج مطابقة للبحث"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE4E7EC),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اضغط على ♥ لحفظ أي ذكر في المفضلة، أو أضف ذكرك الخاص من الزر بالأعلى.",
                            color = Color(0xFF98A2B3),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredItems, key = { it.id }) { item ->
                val isFavorite = item.id in favoriteIds
                val isPinned = pinnedItemId == item.id
                ZekrLibraryCard(
                    item = item,
                    settings = settings,
                    isFavorite = isFavorite,
                    isPinned = isPinned,
                    onToggleFavorite = { onToggleFavorite(item) },
                    onTogglePin = { onTogglePin(item) },
                    onShare = { onShareItem(item) },
                    onEdit = if (item.isCustom) {
                        {
                            editingCustomItem = item
                            showCustomDialog = true
                        }
                    } else null,
                    onDelete = if (item.isCustom) {
                        { onDeleteCustomItem(item) }
                    } else null
                )
            }
        }
    }

    if (showCustomDialog) {
        AddOrEditCustomZekrDialog(
            initialItem = editingCustomItem,
            onDismiss = { showCustomDialog = false },
            onSave = { saved ->
                onSaveCustomItem(saved)
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun ZekrLibraryCard(
    item: ZekrEntity,
    settings: WidgetSettings,
    isFavorite: Boolean,
    isPinned: Boolean,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onShare: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val typeBadge = when {
        item.isCustom -> "ذكر خاص"
        item.type == ContentType.QURAN_AYAH -> "قرآن كريم"
        item.type == ContentType.SAHIH_HADITH -> "حديث شريف"
        else -> item.category.ifBlank { "ذكر" }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned) Color(0xFF142620) else Color(0xFF16181E)
        ),
        border = BorderStroke(
            width = if (isPinned) 1.5.dp else 1.dp,
            color = if (isPinned) Color(0xFF10B981).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Meta Row: Category pill + Repetition pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = typeBadge,
                            fontSize = 10.5.sp,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (isPinned) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "مثبّت في الويدجت",
                                fontSize = 10.5.sp,
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (item.repetition > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFD4AF37).copy(alpha = 0.16f))
                            .border(
                                1.dp,
                                Color(0xFFD4AF37).copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${ZekrRepository.toArabicDigits(item.repetition)} مرات",
                            fontSize = 11.sp,
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Arabic Text
            Text(
                text = item.arabicText,
                fontFamily = FontHelper.getComposeFontFamily(settings.font),
                fontSize = 16.5.sp,
                lineHeight = 27.sp,
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            )

            // Citation
            val citation = item.getConciseCitation(maxChars = 64)
            if (citation.isNotBlank()) {
                Text(
                    text = citation,
                    fontSize = 11.5.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite toggle
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFavorite) Color(0xFFFF5C7A).copy(alpha = 0.18f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isFavorite) Color(0xFFFF6B86) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "مشاركة",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "تعديل",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "حذف",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Pin / Unpin to Widget button
                FilledTonalButton(
                    onClick = onTogglePin,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isPinned) Color(0xFF10B981) else Color.White.copy(alpha = 0.08f),
                        contentColor = if (isPinned) Color(0xFF061610) else Color(0xFFE2E8F0)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.Check else Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPinned) "إلغاء التثبيت" else "تثبيت في الويدجت",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AddOrEditCustomZekrDialog(
    initialItem: ZekrEntity?,
    onDismiss: () -> Unit,
    onSave: (ZekrEntity) -> Unit
) {
    var text by remember(initialItem) { mutableStateOf(initialItem?.arabicText ?: "") }
    var source by remember(initialItem) { mutableStateOf(initialItem?.source ?: "") }
    var category by remember(initialItem) { mutableStateOf(initialItem?.category ?: "عام") }
    var repetitionText by remember(initialItem) { mutableStateOf((initialItem?.repetition ?: 1).toString()) }

    val categories = listOf("عام", "أذكار الصباح", "أذكار المساء", "أذكار النوم")
    val quickReps = listOf(1, 3, 7, 10, 33, 100)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16181E),
        title = {
            Text(
                text = if (initialItem == null) "إضافة ذكر أو دعاء خاص" else "تعديل الذكر الخاص",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF2F4F7),
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("نص الذكر أو الدعاء *") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("المصدر أو العنوان (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "التصنيف الزمني",
                        fontSize = 12.sp,
                        color = Color(0xFFB4BAC6),
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.5.sp) }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "عدد مرات التكرار",
                        fontSize = 12.sp,
                        color = Color(0xFFB4BAC6),
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickReps) { rep ->
                            FilterChip(
                                selected = repetitionText.toIntOrNull() == rep,
                                onClick = { repetitionText = rep.toString() },
                                label = { Text("${ZekrRepository.toArabicDigits(rep)}×", fontSize = 11.5.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = repetitionText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                repetitionText = input
                            }
                        },
                        label = { Text("أو أدخل عدداً مخصصاً") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanText = text.trim()
                    if (cleanText.isNotEmpty()) {
                        val rep = (repetitionText.toIntOrNull() ?: 1).coerceIn(1, 1000)
                        onSave(
                            ZekrEntity(
                                id = initialItem?.id ?: 0,
                                arabicText = cleanText,
                                type = ContentType.AUTHENTIC_ZEKR,
                                source = source.trim().ifBlank { "ذكري الخاص" },
                                reference = "",
                                category = category,
                                repetition = rep,
                                isCustom = true
                            )
                        )
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text("حفظ", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
