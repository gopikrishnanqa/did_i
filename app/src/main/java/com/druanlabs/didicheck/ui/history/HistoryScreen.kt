package com.druanlabs.didicheck.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.druanlabs.didicheck.data.model.Completion
import com.druanlabs.didicheck.ui.components.CompletionRow
import com.druanlabs.didicheck.ui.components.QuietCard
import com.druanlabs.didicheck.ui.components.RecordMark
import com.druanlabs.didicheck.ui.components.ScreenHeader
import com.druanlabs.didicheck.ui.components.SecondaryAction
import com.druanlabs.didicheck.util.TimeText
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryRoute(
    openCompletionId: String? = null,
    viewModel: HistoryViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Completion?>(null) }
    var editing by remember { mutableStateOf<Completion?>(null) }

    LaunchedEffect(openCompletionId, state.groups) {
        if (openCompletionId.isNullOrBlank()) return@LaunchedEffect
        val found = state.groups.flatMap { it.items }.firstOrNull { it.id == openCompletionId }
        if (found != null) selected = found
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "History",
                subtitle = "Find out whether you already did it.",
            )
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Did I feed the cat?") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            )
        }
        state.answer?.let { answer ->
            item { SearchAnswerCard(answer) }
        }
        if (state.groups.isEmpty() && state.query.isBlank()) {
            item {
                QuietCard {
                    Text("Nothing recorded yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Things you check and mark will appear here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (state.groups.isEmpty() && state.query.isNotBlank()) {
            item {
                QuietCard {
                    Text("No matching record found", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "There is no recorded activity matching this search.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        state.groups.forEach { group ->
            item {
                Text(
                    group.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            items(group.items, key = { it.id }) { completion ->
                CompletionRow(
                    completion = completion,
                    trailing = TimeText.clock(completion.timestamp),
                    onClick = { selected = completion },
                )
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }

    selected?.let { completion ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(completion.actionLabel, style = MaterialTheme.typography.titleLarge)
                Text(
                    TimeText.atTime(completion.timestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (completion.detailItems.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "CHECKED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    completion.detailItems.forEach { label ->
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            RecordMark(true)
                            Text(
                                label,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
                if (completion.hasPhotoProof) {
                    Spacer(Modifier.height(16.dp))
                    com.druanlabs.didicheck.ui.components.ProofPhotoImage(
                        path = completion.photoPath,
                        label = "Photo proof",
                    )
                }
                Spacer(Modifier.height(16.dp))
                SecondaryAction("Undo") {
                    viewModel.undo(completion.id)
                    selected = null
                }
                Spacer(Modifier.height(8.dp))
                SecondaryAction("Edit time") {
                    editing = completion
                    selected = null
                }
                Spacer(Modifier.height(8.dp))
                SecondaryAction("Delete") {
                    viewModel.delete(completion.id)
                    selected = null
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    editing?.let { completion ->
        EditTimeDialog(
            completion = completion,
            onDismiss = { editing = null },
            onConfirm = { millis ->
                viewModel.editTime(completion.id, millis)
                editing = null
            },
        )
    }
}

@Composable
private fun SearchAnswerCard(answer: SearchAnswer) {
    QuietCard {
        val label = answer.matchedLabel
        when {
            label == null -> {
                Text("No matching record found", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "There is no recorded activity matching this search.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            answer.today != null -> {
                Text("Most recent match", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text("Yes", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(answer.today.actionLabel, style = MaterialTheme.typography.titleLarge)
                Text(
                    TimeText.atTime(answer.today.timestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                answer.today.detailPreview?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                Text("Most recent match", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text("Not recorded today", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "There is no record of this for today.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                answer.last?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Last time: ${TimeText.atTime(it.timestamp)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (answer.matches.size > 1) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${answer.matches.size} matching records · newest first",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTimeDialog(
    completion: Completion,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val current = TimeText.toLocalDateTime(completion.timestamp)
    val picker = rememberTimePickerState(
        initialHour = current.hour,
        initialMinute = current.minute,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit time") },
        text = { TimePicker(state = picker) },
        confirmButton = {
            TextButton(
                onClick = {
                    val updated = LocalDateTime.of(
                        current.toLocalDate(),
                        java.time.LocalTime.of(picker.hour, picker.minute),
                    ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onConfirm(updated)
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
