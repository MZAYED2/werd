package com.zekr.widget.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zekr.widget.data.model.ZekrCategory
import com.zekr.widget.data.model.ZekrItem
import com.zekr.widget.data.repository.ZekrRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var selectedCategory by remember { mutableStateOf<ZekrCategory?>(null) }
    val athkarList = remember(selectedCategory) {
        if (selectedCategory == null) {
            ZekrRepository.defaultItems
        } else {
            ZekrRepository.defaultItems.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "تطبيق وويدجت الأذكار",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Widget instructions card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💡 إضافة الويدجت إلى الشاشة الرئيسية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اضغط مطولاً على أي مساحة فارغة في الشاشة الرئيسية لهاتفك، ثم اختر 'Widgets' وابحث عن 'Zekr Widget' وأضفها لتسبيح وقراءة الأذكار مباشرة وبسلاسة.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Interactive Preview Card
            item {
                Text(
                    text = "معاينة الويدجت التفاعلي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                InteractiveWidgetPreview()
            }

            // Categories Filter
            item {
                Text(
                    text = "الأذكار والأدعية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("الكل") }
                    )
                    FilterChip(
                        selected = selectedCategory == ZekrCategory.TASBIH,
                        onClick = { selectedCategory = ZekrCategory.TASBIH },
                        label = { Text("تسبيح") }
                    )
                    FilterChip(
                        selected = selectedCategory == ZekrCategory.MORNING,
                        onClick = { selectedCategory = ZekrCategory.MORNING },
                        label = { Text("صباح") }
                    )
                    FilterChip(
                        selected = selectedCategory == ZekrCategory.EVENING,
                        onClick = { selectedCategory = ZekrCategory.EVENING },
                        label = { Text("مساء") }
                    )
                }
            }

            // Athkar List
            items(athkarList) { item ->
                ZekrCard(item = item)
            }
        }
    }
}

@Composable
fun InteractiveWidgetPreview() {
    var currentIndex by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    val currentItem = ZekrRepository.defaultItems[currentIndex]

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3E2C)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(currentItem.category.titleArabic, color = Color(0xFF81C784)) }
                )
                Text(
                    text = "$count / ${currentItem.targetCount}",
                    color = Color(0xFFE8F5E9),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = currentItem.arabicText,
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        currentIndex = if (currentIndex <= 0) ZekrRepository.defaultItems.lastIndex else currentIndex - 1
                        count = 0
                    }
                ) {
                    Text("السابق", color = Color.White)
                }

                Button(
                    onClick = {
                        if (count < currentItem.targetCount) count++
                    }
                ) {
                    Text("سبّح ($count)")
                }

                OutlinedButton(
                    onClick = {
                        currentIndex = if (currentIndex >= ZekrRepository.defaultItems.lastIndex) 0 else currentIndex + 1
                        count = 0
                    }
                ) {
                    Text("التالي", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ZekrCard(item: ZekrItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = item.arabicText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            if (item.reference.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "المصدر: ${item.reference} • التكرار: ${item.targetCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
