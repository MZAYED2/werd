package com.zekr.widget.widget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zekr.widget.data.model.ZekrEntity
import com.zekr.widget.data.preferences.WidgetPreferencesRepository
import com.zekr.widget.data.repository.ZekrRepository
import com.zekr.widget.ui.theme.*
import kotlinx.coroutines.launch

class WidgetMenuActivity : ComponentActivity() {
    companion object {
        const val EXTRA_ITEM_ID = "widget_menu_item_id"
        const val EXTRA_SHOW_DETAILS = "widget_menu_show_details"
    }

    private data class Request(val id: Int, val details: Boolean)
    private var request by mutableStateOf<Request?>(null)

    private fun accept(intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ITEM_ID, -1)
        if (id <= 0) finish()
        else request = Request(id, intent.getBooleanExtra(EXTRA_SHOW_DETAILS, false))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        accept(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        accept(intent)
        val repository = WidgetPreferencesRepository(applicationContext)

        setContent {
            val current = request ?: return@setContent
            val appearance = rememberWerdAppearance().value
            val favorites by repository.favoriteIdsFlow.collectAsState(initial = emptySet())
            val scope = rememberCoroutineScope()
            var item by remember(current.id) { mutableStateOf<ZekrEntity?>(null) }
            var details by rememberSaveable(current) { mutableStateOf(current.details) }
            var saving by remember { mutableStateOf(false) }

            LaunchedEffect(current.id) {
                item = ZekrRepository.getAllItems(applicationContext, repository.getCustomItems())
                    .firstOrNull { it.id == current.id }
                if (item == null) finish()
            }

            ZekrWidgetTheme(darkTheme = rememberWerdDarkTheme(appearance), accent = appearance.accent) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.38f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { finish() },
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .widthIn(max = 330.dp)
                                .fillMaxWidth(0.86f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* Consume clicks to prevent dismissing dialog */ },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            val displayed = item
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Card Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (details) "تفاصيل الذكر وفضله" else "خيارات الذكر",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = ::finish,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 1.dp
                                )

                                if (displayed == null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                } else if (details) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 360.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = displayed.getFormattedArabicText(),
                                            fontSize = 19.sp,
                                            lineHeight = 32.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (displayed.source.isNotBlank()) {
                                            Text(
                                                text = displayed.source,
                                                fontSize = 13.sp,
                                                color = sourceTextColor
                                            )
                                        }
                                        if (displayed.reference.isNotBlank()) {
                                            Text(
                                                text = displayed.reference,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        displayed.virtueText?.takeIf { it.isNotBlank() && it != displayed.arabicText }?.let {
                                            Text(
                                                text = it,
                                                fontSize = 13.sp,
                                                lineHeight = 22.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { details = false },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                                    ) {
                                        Text("العودة للخيارات", fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    // 1. Details
                                    OutlinedButton(
                                        onClick = { details = true },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                                    ) {
                                        Text("📖  تفاصيل الذكر وفضله", fontWeight = FontWeight.SemiBold)
                                    }

                                    // 2. Favorite Toggle
                                    val isFav = displayed.id in favorites
                                    Button(
                                        onClick = {
                                            if (!saving) {
                                                saving = true
                                                scope.launch {
                                                    try {
                                                        repository.toggleFavorite(displayed.id)
                                                        ZekrGlanceWidget.updateAllWidgets(applicationContext)
                                                    } finally {
                                                        saving = false
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !saving,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFav) MaterialTheme.colorScheme.error.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                            contentColor = if (isFav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                                    ) {
                                        Text(
                                            text = if (isFav) "♥  إزالة من المفضلة" else "♡  إضافة إلى المفضلة",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // 3. Share
                                    OutlinedButton(
                                        onClick = {
                                            val share = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    listOf(displayed.arabicText, displayed.source)
                                                        .filter(String::isNotBlank)
                                                        .joinToString("\n\n")
                                                )
                                            }
                                            startActivity(Intent.createChooser(share, "مشاركة الذكر"))
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                                    ) {
                                        Text("⤴  مشاركة الذكر", fontWeight = FontWeight.SemiBold)
                                    }

                                    // 4. Open Werd (فتح وِرد)
                                    Button(
                                        onClick = {
                                            val openWerd = Intent(applicationContext, com.zekr.widget.MainActivity::class.java).apply {
                                                action = Intent.ACTION_VIEW
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                putExtra(com.zekr.widget.MainActivity.EXTRA_NAV_TAB, 0)
                                            }
                                            startActivity(openWerd)
                                            finish()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                                    ) {
                                        Text("🕌  فتح وِرد", fontWeight = FontWeight.Bold)
                                    }

                                    // 5. Close
                                    TextButton(
                                        onClick = ::finish,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("إغلاق", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
