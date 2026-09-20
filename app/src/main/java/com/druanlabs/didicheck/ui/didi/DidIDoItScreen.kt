package com.druanlabs.didicheck.ui.didi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.druanlabs.didicheck.data.model.QuickAction
import com.druanlabs.didicheck.data.model.Routine
import com.druanlabs.didicheck.ui.components.PrimaryAction
import com.druanlabs.didicheck.ui.components.QuietCard
import com.druanlabs.didicheck.ui.components.RecordMark
import com.druanlabs.didicheck.ui.components.ScreenHeader
import com.druanlabs.didicheck.ui.components.SecondaryAction
import com.druanlabs.didicheck.ui.components.SectionLabel
import com.druanlabs.didicheck.util.TimeText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DidIDoItRoute(
    onBack: () -> Unit,
    onManageActions: () -> Unit = {},
    onStartRoutine: (String) -> Unit = {},
    onChooseRoutine: () -> Unit = {},
    onCreateRoutine: () -> Unit = {},
    viewModel: DidIDoItViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackbar) {
        val message = state.snackbar ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = if (state.lastId != null && message != "Undone") "Undo" else null,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoLast()
        }
        viewModel.consumeSnackbar()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Did I Do It?") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onManageActions) { Text("Manage") }
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
                ScreenHeader(
                    eyebrow = "Quick actions",
                    title = "What did you do?",
                    subtitle = "One tap records the current time.",
                )
            }
            item {
                RoutineSection(
                    routine = state.defaultRoutine,
                    onStart = {
                        state.defaultRoutine?.id?.let(onStartRoutine)
                    },
                    onChoose = onChooseRoutine,
                    onCreate = onCreateRoutine,
                )
            }
            if (state.pinned.isNotEmpty()) {
                item {
                    Text(
                        "PINNED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(state.pinned, key = { "p-${it.id}" }) { action ->
                    QuickActionCard(action, pinned = true) { viewModel.mark(action.label) }
                }
            }
            if (state.others.isNotEmpty()) {
                item {
                    Text(
                        if (state.pinned.isEmpty()) "QUICK ACTIONS" else "MORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(state.others, key = { "o-${it.id}" }) { action ->
                    QuickActionCard(action, pinned = false) { viewModel.mark(action.label) }
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                SecondaryAction("Add action") { showAdd = true }
            }
            item {
                val last = state.last
                QuietCard {
                    Text(
                        "LAST RECORDED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (last == null) {
                        Text(
                            "Nothing yet. Tap an action when you do it.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RecordMark(true)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(last.actionLabel, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    TimeText.relativeStamp(last.timestamp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (showAdd) {
        AddActionDialog(
            onDismiss = { showAdd = false },
            onConfirm = { label ->
                viewModel.addAndMark(label)
                showAdd = false
            },
        )
    }
}

@Composable
private fun RoutineSection(
    routine: Routine?,
    onStart: () -> Unit,
    onChoose: () -> Unit,
    onCreate: () -> Unit,
) {
    QuietCard {
        SectionLabel("Routines")
        Spacer(Modifier.height(8.dp))
        if (routine == null) {
            Text(
                "No routines yet",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Create a custom routine for the things you check before you go.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            PrimaryAction("Create a custom routine", onClick = onCreate)
        } else {
            Text(
                "Default routine",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(routine.name, style = MaterialTheme.typography.titleLarge)
            val preview = routine.takePreview
            if (preview.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            PrimaryAction("Start check", onClick = onStart)
            Spacer(Modifier.height(10.dp))
            SecondaryAction("Choose a routine", onClick = onChoose)
            Spacer(Modifier.height(8.dp))
            SecondaryAction("Create a custom routine", onClick = onCreate)
        }
    }
}

@Composable
private fun QuickActionCard(
    action: QuickAction,
    pinned: Boolean,
    onClick: () -> Unit,
) {
    QuietCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (pinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                }
                Text(action.label, style = MaterialTheme.typography.titleMedium)
            }
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "I did it: ${action.label}",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AddActionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What did you do?") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                placeholder = { Text("Fed the fish") },
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.trim().isNotEmpty(),
                onClick = { onConfirm(value) },
            ) { Text("I did it") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
