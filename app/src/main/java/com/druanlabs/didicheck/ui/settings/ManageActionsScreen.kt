package com.druanlabs.didicheck.ui.settings

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.druanlabs.didicheck.ui.components.QuietCard
import com.druanlabs.didicheck.ui.components.ScreenHeader
import com.druanlabs.didicheck.ui.components.SecondaryAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageActionsRoute(
    onBack: () -> Unit,
    viewModel: ManageActionsViewModel = viewModel(),
) {
    val actions by viewModel.actions.collectAsStateWithLifecycle()
    val pinned = actions.filter { it.pinned }
    val others = actions.filter { !it.pinned }
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<QuickAction?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Actions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { adding = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add action")
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
                ScreenHeader(
                    title = "Quick actions",
                    subtitle = "Pin the few you use most. Keep the list calm.",
                )
            }
            if (pinned.isNotEmpty()) {
                item {
                    Text(
                        "PINNED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                itemsIndexed(pinned, key = { _, a -> "pin-${a.id}" }) { index, action ->
                    QuietCard(onClick = { renaming = action }) {
                        ActionManageRow(
                            action = action,
                            canMoveUp = index > 0,
                            canMoveDown = index < pinned.lastIndex,
                            onTogglePin = { viewModel.togglePin(action) },
                            onMoveUp = { viewModel.movePinned(action.id, -1) },
                            onMoveDown = { viewModel.movePinned(action.id, 1) },
                            onDelete = { viewModel.delete(action.id) },
                        )
                    }
                }
            }
            if (others.isNotEmpty()) {
                item {
                    Text(
                        "ALL ACTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                itemsIndexed(others, key = { _, a -> "all-${a.id}" }) { _, action ->
                    QuietCard(onClick = { renaming = action }) {
                        ActionManageRow(
                            action = action,
                            canMoveUp = false,
                            canMoveDown = false,
                            onTogglePin = { viewModel.togglePin(action) },
                            onMoveUp = {},
                            onMoveDown = {},
                            onDelete = { viewModel.delete(action.id) },
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                SecondaryAction("Add an action") { adding = true }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (adding) {
        LabelDialog(
            title = "What did you do?",
            confirmLabel = "Add",
            initial = "",
            onDismiss = { adding = false },
            onConfirm = {
                viewModel.add(it)
                adding = false
            },
        )
    }
    renaming?.let { action ->
        LabelDialog(
            title = "Rename action",
            confirmLabel = "Save",
            initial = action.label,
            onDismiss = { renaming = null },
            onConfirm = {
                viewModel.rename(action, it)
                renaming = null
            },
        )
    }
}

@Composable
private fun ActionManageRow(
    action: QuickAction,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTogglePin: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(action.label, style = MaterialTheme.typography.titleMedium)
            if (action.pinned) {
                Text(
                    "Pinned",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        IconButton(onClick = onTogglePin) {
            Icon(
                Icons.Outlined.PushPin,
                contentDescription = if (action.pinned) "Unpin" else "Pin",
                tint = if (action.pinned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (action.pinned) {
            IconButton(enabled = canMoveUp, onClick = onMoveUp) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(enabled = canMoveDown, onClick = onMoveDown) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down")
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete ${action.label}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LabelDialog(
    title: String,
    confirmLabel: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.trim().isNotEmpty(),
                onClick = { onConfirm(value) },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
