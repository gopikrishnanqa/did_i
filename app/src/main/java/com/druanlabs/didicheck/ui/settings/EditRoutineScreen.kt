package com.druanlabs.didicheck.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.druanlabs.didicheck.data.model.ItemKind
import com.druanlabs.didicheck.data.model.ReminderRepeat
import com.druanlabs.didicheck.data.model.RoutineReminderTime
import com.druanlabs.didicheck.ui.components.DidISwitch
import com.druanlabs.didicheck.ui.components.PrimaryAction
import com.druanlabs.didicheck.ui.components.QuietCard
import com.druanlabs.didicheck.ui.components.SecondaryAction
import com.druanlabs.didicheck.ui.components.SectionLabel
import com.druanlabs.didicheck.ui.theme.BrandBlue
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Alarm-style order: Sunday → Saturday */
private val AlarmWeekOrder = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoutineRoute(
    routineId: String?,
    onBack: () -> Unit,
    viewModel: EditRoutineViewModel = viewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editingTimeId by remember { mutableStateOf<String?>(null) }
    var showDiscard by remember { mutableStateOf(false) }
    LaunchedEffect(routineId) { viewModel.load(routineId) }

    val remindersOn = draft.reminderRepeat != ReminderRepeat.OFF && draft.reminderTimes.isNotEmpty()
    val selectedDays = draft.activeReminderDays()
    val canSave = draft.name.trim().isNotEmpty()

    fun requestLeave() {
        if (viewModel.isDirty()) {
            showDiscard = true
        } else {
            onBack()
        }
    }

    BackHandler { requestLeave() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (routineId == "new") "New routine" else "Edit routine") },
                navigationIcon = {
                    IconButton(onClick = { requestLeave() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = { viewModel.save(onBack) },
                    ) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = viewModel::setName,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Routine name") },
                    placeholder = { Text("Going to Work") },
                )
            }

            item {
                QuietCard(shadowElevation = 0.dp) {
                    SectionLabel("Reminder")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Optional. Leave off and just open the app to check. Or set days and a clock time like an alarm — tap the notification to open this checklist, or swipe it away.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !remindersOn,
                            onClick = { viewModel.setReminderEnabled(false) },
                            label = { Text("No reminder") },
                        )
                        FilterChip(
                            selected = remindersOn,
                            onClick = { viewModel.setReminderEnabled(true) },
                            label = { Text("Remind me") },
                        )
                    }
                }
            }

            if (remindersOn) {
                item {
                    QuietCard {
                        Text(
                            daySummary(selectedDays),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(14.dp))
                        AlarmDayRow(
                            selected = selectedDays,
                            onToggle = viewModel::toggleAlarmDay,
                        )
                    }
                }

                itemsIndexed(draft.reminderTimes, key = { _, time -> time.id }) { index, time ->
                    QuietCard(onClick = { editingTimeId = time.id }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = BrandBlue,
                            )
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (index == 0) "Alarm time" else "Second alarm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    time.label(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    "Tap to change",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (draft.reminderTimes.size > 1) {
                                IconButton(onClick = { viewModel.removeReminderTime(time.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Remove time")
                                }
                            }
                        }
                    }
                }

                item {
                    if (draft.reminderTimes.size < 2) {
                        SecondaryAction("Add another alarm time") {
                            viewModel.addReminderTime(hour = 20, minute = 0)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    PrimaryAction(
                        text = "Save reminder",
                        enabled = canSave,
                        onClick = { viewModel.save(onBack) },
                    )
                }
            }

            item {
                QuietCard(shadowElevation = 0.dp) {
                    SectionLabel("Photo when finished")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ask for a photo after every item is checked and you tap I’m ready.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Ask for photo on complete",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        DidISwitch(
                            checked = draft.askPhotoOnComplete,
                            onCheckedChange = viewModel::setAskPhotoOnComplete,
                        )
                    }
                }
            }

            item {
                Text(
                    "CHECKLIST ITEMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            itemsIndexed(draft.items, key = { _, item -> item.id }) { index, item ->
                QuietCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (item.kind == ItemKind.TAKE) "Remember to take" else "Confirm",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        IconButton(
                            enabled = index > 0,
                            onClick = { viewModel.moveItem(item.id, -1) },
                        ) { Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up") }
                        IconButton(
                            enabled = index < draft.items.lastIndex,
                            onClick = { viewModel.moveItem(item.id, 1) },
                        ) { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down") }
                        IconButton(onClick = { viewModel.removeItem(item.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Remove")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = if (item.photoRequired) {
                                BrandBlue
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Require photo", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Camera must be used to check this item",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        DidISwitch(
                            checked = item.photoRequired,
                            onCheckedChange = { viewModel.setItemPhotoRequired(item.id, it) },
                        )
                    }
                }
            }
            item {
                SecondaryAction("Add an item") { showAdd = true }
                Spacer(Modifier.height(8.dp))
                PrimaryAction(
                    text = "Save routine",
                    enabled = draft.name.trim().isNotEmpty(),
                    onClick = { viewModel.save(onBack) },
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard changes?") },
            text = {
                Text("You have unsaved changes. Do you want to discard them?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscard = false
                        onBack()
                    },
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) {
                    Text("Keep editing")
                }
            },
        )
    }

    if (showAdd) {
        AddItemDialog(
            onDismiss = { showAdd = false },
            onConfirm = { label, kind, photoRequired ->
                viewModel.addItem(label, kind, photoRequired)
                showAdd = false
            },
        )
    }

    val editingTime = draft.reminderTimes.firstOrNull { it.id == editingTimeId }
    if (editingTime != null) {
        AlarmTimePickerDialog(
            time = editingTime,
            onDismiss = { editingTimeId = null },
            onConfirm = { hour, minute ->
                viewModel.setReminderTime(editingTime.id, hour, minute)
                editingTimeId = null
            },
        )
    }
}

@Composable
private fun AlarmDayRow(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlarmWeekOrder.forEach { day ->
            val isOn = day in selected
            val letter = day.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isOn) BrandBlue else Color.Transparent)
                    .clickable(role = Role.Checkbox) { onToggle(day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    color = when {
                        isOn -> Color.White
                        day == DayOfWeek.SUNDAY -> Color(0xFFC45C4A)
                        else -> MaterialTheme.colorScheme.onBackground
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun daySummary(days: Set<DayOfWeek>): String {
    val ordered = AlarmWeekOrder.filter { it in days }
    return when {
        ordered.size == 7 -> "Every day"
        ordered == listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        ) -> "Every Mon, Tue, Wed, Thu, Fri"
        ordered.size == 1 -> "Every ${ordered.first().getDisplayName(TextStyle.FULL, Locale.getDefault())}"
        ordered.isEmpty() -> "Choose days"
        else -> "Every " + ordered.joinToString(", ") {
            it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmTimePickerDialog(
    time: RoutineReminderTime,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = time.hour,
        initialMinute = time.minute,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set time") },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, ItemKind, Boolean) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(ItemKind.TAKE) }
    var photoRequired by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add an item") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    placeholder = { Text("Phone") },
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == ItemKind.TAKE,
                        onClick = { kind = ItemKind.TAKE },
                        label = { Text("Remember to take") },
                    )
                    FilterChip(
                        selected = kind == ItemKind.CONFIRM,
                        onClick = { kind = ItemKind.CONFIRM },
                        label = { Text("Confirm") },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Require photo",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    DidISwitch(
                        checked = photoRequired,
                        onCheckedChange = { photoRequired = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.trim().isNotEmpty(),
                onClick = { onConfirm(label, kind, photoRequired) },
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
