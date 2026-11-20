package com.zekr.widget.routine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zekr.widget.audio.AudioCatalog
import com.zekr.widget.routine.model.AudioRoutine
import com.zekr.widget.routine.model.BluetoothEventType
import com.zekr.widget.routine.model.ChargingConditionType
import com.zekr.widget.routine.model.ConditionLogic
import com.zekr.widget.routine.model.RoutineAction
import com.zekr.widget.routine.model.RoutineCondition
import com.zekr.widget.routine.model.RoutineSettings
import com.zekr.widget.routine.model.RoutineSummary
import com.zekr.widget.routine.model.TimeConditionType

@Composable
fun RoutineEditorDialog(
    initialRoutine: AudioRoutine? = null,
    onDismiss: () -> Unit,
    onSave: (AudioRoutine) -> Unit
) {
    var stage by remember(initialRoutine) { mutableIntStateOf(1) } // 1: Conditions, 2: Actions, 3: Settings & Save

    var routineName by remember(initialRoutine) {
        mutableStateOf(initialRoutine?.name ?: "روتين صوتي جديد")
    }
    var logic by remember(initialRoutine) {
        mutableStateOf(initialRoutine?.logic ?: ConditionLogic.AND)
    }
    var conditions by remember(initialRoutine) {
        mutableStateOf<List<RoutineCondition>>(initialRoutine?.conditions ?: emptyList())
    }
    var actions by remember(initialRoutine) {
        mutableStateOf<List<RoutineAction>>(initialRoutine?.actions ?: emptyList())
    }
    var settings by remember(initialRoutine) {
        mutableStateOf(initialRoutine?.settings ?: RoutineSettings())
    }

    var showAddConditionSheet by remember { mutableStateOf(false) }
    var showAddActionSheet by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                    Text(
                        text = if (initialRoutine == null) "إنشاء روتين جديد" else "تعديل الروتين",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(48.dp))
                }

                // Stage Stepper
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepIndicator(step = 1, title = "١. الشروط", isCurrent = stage == 1, isCompleted = stage > 1)
                    Text("←", color = MaterialTheme.colorScheme.outlineVariant)
                    StepIndicator(step = 2, title = "٢. الأصوات", isCurrent = stage == 2, isCompleted = stage > 2)
                    Text("←", color = MaterialTheme.colorScheme.outlineVariant)
                    StepIndicator(step = 3, title = "٣. الحفظ", isCurrent = stage == 3, isCompleted = false)
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))

                // Stage Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (stage) {
                        1 -> ConditionsStage(
                            logic = logic,
                            onLogicChange = { logic = it },
                            conditions = conditions,
                            onAddConditionClick = { showAddConditionSheet = true },
                            onDeleteCondition = { index ->
                                val updated = conditions.toMutableList()
                                updated.removeAt(index)
                                conditions = updated
                            }
                        )
                        2 -> ActionsStage(
                            actions = actions,
                            onAddActionClick = { showAddActionSheet = true },
                            onMoveUp = { index ->
                                if (index > 0) {
                                    val updated = actions.toMutableList()
                                    val item = updated.removeAt(index)
                                    updated.add(index - 1, item)
                                    actions = updated
                                }
                            },
                            onMoveDown = { index ->
                                if (index < actions.size - 1) {
                                    val updated = actions.toMutableList()
                                    val item = updated.removeAt(index)
                                    updated.add(index + 1, item)
                                    actions = updated
                                }
                            },
                            onDeleteAction = { index ->
                                val updated = actions.toMutableList()
                                updated.removeAt(index)
                                actions = updated
                            }
                        )
                        3 -> SettingsStage(
                            routineName = routineName,
                            onNameChange = { routineName = it },
                            settings = settings,
                            onSettingsChange = { settings = it },
                            previewRoutine = AudioRoutine(
                                name = routineName.ifBlank { "روتين بدون اسم" },
                                logic = logic,
                                conditions = conditions,
                                actions = actions,
                                settings = settings
                            )
                        )
                    }
                }

                // Stage Bottom Navigation Buttons
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stage > 1) {
                        OutlinedButton(
                            onClick = { stage -= 1 },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("السابق")
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    if (stage < 3) {
                        Button(
                            onClick = { stage += 1 },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("التالي")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowBack, contentDescription = null, Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                val routineToSave = initialRoutine?.copy(
                                    name = routineName.ifBlank { "روتين صوتي" },
                                    logic = logic,
                                    conditions = conditions,
                                    actions = actions,
                                    settings = settings
                                ) ?: AudioRoutine(
                                    name = routineName.ifBlank { "روتين صوتي" },
                                    logic = logic,
                                    conditions = conditions,
                                    actions = actions,
                                    settings = settings
                                )
                                onSave(routineToSave)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("حفظ الروتين")
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet: Add Condition
    if (showAddConditionSheet) {
        AddConditionDialog(
            onDismiss = { showAddConditionSheet = false },
            onConditionSelected = { newCondition ->
                val updated = conditions.toMutableList()
                updated.add(newCondition)
                conditions = updated
                showAddConditionSheet = false
            }
        )
    }

    // Modal Sheet: Add Action
    if (showAddActionSheet) {
        AddActionDialog(
            onDismiss = { showAddActionSheet = false },
            onActionSelected = { newAction ->
                val updated = actions.toMutableList()
                updated.add(newAction)
                actions = updated
                showAddActionSheet = false
            }
        )
    }
}

@Composable
private fun StepIndicator(step: Int, title: String, isCurrent: Boolean, isCompleted: Boolean) {
    val bgColor = if (isCurrent) MaterialTheme.colorScheme.primary
                  else if (isCompleted) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isCurrent) MaterialTheme.colorScheme.onPrimary
                    else if (isCompleted) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConditionsStage(
    logic: ConditionLogic,
    onLogicChange: (ConditionLogic) -> Unit,
    conditions: List<RoutineCondition>,
    onAddConditionClick: () -> Unit,
    onDeleteCondition: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "المرحلة الأولى: متى يبدأ هذا الروتين؟",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "أضف شرطاً أو أكثر (بلوتوث، وقت، أو شحن) وحدد العلاقة بينها.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Logic Switcher (AND vs OR)
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "علاقة الشروط ببعضها:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = logic == ConditionLogic.AND,
                            onClick = { onLogicChange(ConditionLogic.AND) },
                            label = { Text("كل الشروط معاً (AND)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = logic == ConditionLogic.OR,
                            onClick = { onLogicChange(ConditionLogic.OR) },
                            label = { Text("أي شرط يتحقق (OR)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Conditions List
        if (conditions.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لم تتم إضافة أي شروط بعد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "سيكون الروتين متاحاً للتشغيل اليدوي فقط إذا لم تضف شروطاً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            itemsIndexed(conditions) { index, condition ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = when (condition) {
                                    is RoutineCondition.Bluetooth -> "📶"
                                    is RoutineCondition.Time -> "⏰"
                                    is RoutineCondition.Charging -> "⚡"
                                },
                                fontSize = 20.sp
                            )
                            Column {
                                Text(
                                    text = RoutineSummary.describeCondition(condition),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteCondition(index) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "حذف الشرط",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Add Condition Button
        item {
            OutlinedButton(
                onClick = onAddConditionClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("إضافة شرط جديد")
            }
        }
    }
}

@Composable
private fun ActionsStage(
    actions: List<RoutineAction>,
    onAddActionClick: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDeleteAction: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "المرحلة الثانية: ماذا يُشغّل الروتين؟",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "أضف المقاطع الصوتية ورتّبها بالتسلسل المطلوب عند اكتمال كل مقطع.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (actions.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لم تضف أي مقطع صوتي بعد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "اضغط على «إضافة صوت» لاختيار دعاء، ذكر، أو تلاوة قرآنية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            itemsIndexed(actions) { index, action ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column {
                                Text(
                                    text = RoutineSummary.describeAction(action),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onMoveUp(index) },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "تحريك لأعلى")
                            }
                            IconButton(
                                onClick = { onMoveDown(index) },
                                enabled = index < actions.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "تحريك لأسفل")
                            }
                            IconButton(
                                onClick = { onDeleteAction(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "حذف الإجراء",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onAddActionClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("إضافة صوت جديد")
            }
        }
    }
}

@Composable
private fun SettingsStage(
    routineName: String,
    onNameChange: (String) -> Unit,
    settings: RoutineSettings,
    onSettingsChange: (RoutineSettings) -> Unit,
    previewRoutine: AudioRoutine
) {
    var showAdvanced by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "المرحلة الثالثة: اسم الروتين والمراجعة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Routine Name Field
        item {
            OutlinedTextField(
                value = routineName,
                onValueChange = onNameChange,
                label = { Text("اسم الروتين") },
                placeholder = { Text("مثال: روتين ركوب السيارة، ورد الفجر...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Dynamic Live Summary
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📋", fontSize = 18.sp)
                        Text(
                            text = "ملخص الروتين الذكي:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = RoutineSummary.generateSummary(previewRoutine),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Advanced Settings Toggle
        item {
            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (showAdvanced) "إخفاء الإعدادات المتقدمة ▲" else "عرض الإعدادات المتقدمة (منع التكرار، الاستكمال...) ▼",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        if (showAdvanced) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Cooldown
                        Column {
                            Text(
                                text = "مدة منع التكرار (Cooldown):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(15 to "١٥ د", 30 to "٣٠ د", 60 to "ساعة", 720 to "١٢ س").forEach { (min, label) ->
                                    FilterChip(
                                        selected = settings.cooldownMinutes == min,
                                        onClick = { onSettingsChange(settings.copy(cooldownMinutes = min)) },
                                        label = { Text(label) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Once per day
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "تنفيذ مرة واحدة يومياً فقط",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "لن يتكرر الروتين في نفس اليوم حتى لو تحققت الشروط مجدداً.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.oncePerDay,
                                onCheckedChange = { onSettingsChange(settings.copy(oncePerDay = it)) }
                            )
                        }

                        // Allow session resume
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "استكمال الجلسة عند الانقطاع القصير",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "إذا انقطع البلوتوث ثم عاد، يكمل من حيث توقف دون إعادة المقاطع المكتملة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.allowSessionResume,
                                onCheckedChange = { onSettingsChange(settings.copy(allowSessionResume = it)) }
                            )
                        }

                        if (settings.allowSessionResume) {
                            Column {
                                Text(
                                    text = "مهلة الاستكمال الذكي:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(15 to "١٥ د", 30 to "٣٠ د", 45 to "٤٥ د", 90 to "٩٠ د").forEach { (min, label) ->
                                        FilterChip(
                                            selected = settings.sessionResumeTimeoutMinutes == min,
                                            onClick = { onSettingsChange(settings.copy(sessionResumeTimeoutMinutes = min)) },
                                            label = { Text(label) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Fallback Notification
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "إشعار احتياطي عند القيود",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "إذا منع النظام بدء الصوت في الخلفية، يظهر إشعار «روتين جاهز» للتشغيل بنقرة واحدة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.useFallbackNotification,
                                onCheckedChange = { onSettingsChange(settings.copy(useFallbackNotification = it)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddConditionDialog(
    onDismiss: () -> Unit,
    onConditionSelected: (RoutineCondition) -> Unit
) {
    var selectedType by remember { mutableStateOf(1) } // 1: Bluetooth, 2: Time, 3: Charging

    // Bluetooth inputs
    var btEvent by remember { mutableStateOf(BluetoothEventType.CONNECTED) }
    var btDeviceName by remember { mutableStateOf("") }

    // Time inputs
    var timeType by remember { mutableStateOf(TimeConditionType.EXACT_TIME) }
    var startHour by remember { mutableIntStateOf(8) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(11) }
    var endMinute by remember { mutableIntStateOf(0) }

    // Charging inputs
    var chargingType by remember { mutableStateOf(ChargingConditionType.CONNECTED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "إضافة شرط جديد",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedType == 1,
                        onClick = { selectedType = 1 },
                        label = { Text("بلوتوث 📶") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == 2,
                        onClick = { selectedType = 2 },
                        label = { Text("الوقت ⏰") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == 3,
                        onClick = { selectedType = 3 },
                        label = { Text("الشحن ⚡") },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                when (selectedType) {
                    1 -> {
                        // Bluetooth form
                        Text("نوع حدث البلوتوث:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = btEvent == BluetoothEventType.CONNECTED,
                                onClick = { btEvent = BluetoothEventType.CONNECTED },
                                label = { Text("اتصال") }
                            )
                            FilterChip(
                                selected = btEvent == BluetoothEventType.CONNECTED_STATE,
                                onClick = { btEvent = BluetoothEventType.CONNECTED_STATE },
                                label = { Text("بقاء متصلاً") }
                            )
                            FilterChip(
                                selected = btEvent == BluetoothEventType.DISCONNECTED,
                                onClick = { btEvent = BluetoothEventType.DISCONNECTED },
                                label = { Text("انفصال") }
                            )
                        }

                        OutlinedTextField(
                            value = btDeviceName,
                            onValueChange = { btDeviceName = it },
                            label = { Text("اسم جهاز البلوتوث (اختياري)") },
                            placeholder = { Text("اتركه فارغاً لأي جهاز بلوتوث") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> {
                        // Time form
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = timeType == TimeConditionType.EXACT_TIME,
                                onClick = { timeType = TimeConditionType.EXACT_TIME },
                                label = { Text("ساعة محددة") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = timeType == TimeConditionType.TIME_WINDOW,
                                onClick = { timeType = TimeConditionType.TIME_WINDOW },
                                label = { Text("فترة (من - إلى)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الساعة:")
                            OutlinedTextField(
                                value = startHour.toString(),
                                onValueChange = { startHour = it.toIntOrNull()?.coerceIn(0, 23) ?: 0 },
                                modifier = Modifier.width(70.dp),
                                singleLine = true
                            )
                            Text("الدقيقة:")
                            OutlinedTextField(
                                value = startMinute.toString(),
                                onValueChange = { startMinute = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                                modifier = Modifier.width(70.dp),
                                singleLine = true
                            )
                        }

                        if (timeType == TimeConditionType.TIME_WINDOW) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("إلى س:")
                                OutlinedTextField(
                                    value = endHour.toString(),
                                    onValueChange = { endHour = it.toIntOrNull()?.coerceIn(0, 23) ?: 0 },
                                    modifier = Modifier.width(70.dp),
                                    singleLine = true
                                )
                                Text("الدقيقة:")
                                OutlinedTextField(
                                    value = endMinute.toString(),
                                    onValueChange = { endMinute = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                                    modifier = Modifier.width(70.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                    3 -> {
                        // Charging form
                        Text("حالة الشحن:", style = MaterialTheme.typography.labelMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = chargingType == ChargingConditionType.CONNECTED,
                                onClick = { chargingType = ChargingConditionType.CONNECTED },
                                label = { Text("عند توصيل الشاحن ⚡") }
                            )
                            FilterChip(
                                selected = chargingType == ChargingConditionType.DISCONNECTED,
                                onClick = { chargingType = ChargingConditionType.DISCONNECTED },
                                label = { Text("عند فصل الشاحن 🔌") }
                            )
                            FilterChip(
                                selected = chargingType == ChargingConditionType.CHARGING_STATE,
                                onClick = { chargingType = ChargingConditionType.CHARGING_STATE },
                                label = { Text("أثناء الشحن (حالة مستمرة) 🔋") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cond = when (selectedType) {
                        1 -> RoutineCondition.Bluetooth(
                            deviceName = btDeviceName.trim().ifBlank { null },
                            eventType = btEvent
                        )
                        2 -> RoutineCondition.Time(
                            timeType = timeType,
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            daysOfWeek = emptyList()
                        )
                        3 -> RoutineCondition.Charging(
                            chargingType = chargingType
                        )
                        else -> RoutineCondition.Charging()
                    }
                    onConditionSelected(cond)
                }
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun AddActionDialog(
    onDismiss: () -> Unit,
    onActionSelected: (RoutineAction) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(1) } // 1: Athkar & Duas, 2: Quran
    var selectedReciter by remember { mutableStateOf(AudioCatalog.QuranReciter.MINSHAWI) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "إضافة مقطع صوتي للروتين",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == 1,
                        onClick = { selectedCategory = 1 },
                        label = { Text("الأذكار والأدعية 🤲") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCategory == 2,
                        onClick = { selectedCategory = 2 },
                        label = { Text("القرآن الكريم 📖") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedCategory == 1) {
                        items(AudioCatalog.AthkarTracks.size) { index ->
                            val track = AudioCatalog.AthkarTracks[index]
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onActionSelected(
                                            RoutineAction.AudioTrackAction(
                                                trackId = track.id,
                                                title = track.title,
                                                subtitle = track.subtitle
                                            )
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = track.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text("+", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    } else {
                        // Quran Special Actions:
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onActionSelected(
                                            RoutineAction.QuranResumeAction(
                                                reciterId = selectedReciter.id
                                            )
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = "📖 استكمال القرآن من آخر موضع",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "يكمل من آخر سورة ودقيقة استمعت إليها، ثم يتابع السور التالية.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text("+", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // Specific Surahs
                        items(114) { surahIndex ->
                            val surahNumber = surahIndex + 1
                            val surahName = AudioCatalog.SURAH_NAMES.getOrNull(surahIndex) ?: "السورة $surahNumber"
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onActionSelected(
                                            RoutineAction.QuranSurahAction(
                                                surahNumber = surahNumber,
                                                surahName = surahName,
                                                reciterId = selectedReciter.id
                                            )
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "سورة $surahName ($surahNumber)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text("+", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
