package com.druanlabs.didicheck.ui.before

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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.druanlabs.didicheck.data.model.Routine
import com.druanlabs.didicheck.ui.components.QuietCard
import com.druanlabs.didicheck.ui.components.ScreenHeader
import com.druanlabs.didicheck.ui.components.SecondaryAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinePickerRoute(
    onBack: () -> Unit,
    onOpenRoutine: (String) -> Unit,
    onCreateRoutine: () -> Unit,
    viewModel: RoutinesViewModel = viewModel(),
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val defaultRoutineId by viewModel.defaultRoutineId.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Choose a routine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScreenHeader(
                    title = "Which routine?",
                    subtitle = "Tap to start a check, or set one as your default.",
                )
            }
            items(routines, key = { it.id }) { routine ->
                val isDefault = routine.id == defaultRoutineId
                RoutineChoice(
                    routine = routine,
                    isDefault = isDefault,
                    onClick = { onOpenRoutine(routine.id) },
                    onSetDefault = { viewModel.setDefaultRoutine(routine.id) },
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                SecondaryAction("Create a custom routine", onClick = onCreateRoutine)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RoutineChoice(
    routine: Routine,
    isDefault: Boolean,
    onClick: () -> Unit,
    onSetDefault: () -> Unit,
) {
    QuietCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(routine.name, style = MaterialTheme.typography.titleLarge)
                    if (isDefault) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Default routine",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                val preview = routine.takePreview
                if (preview.isNotBlank()) {
                    Text(
                        preview,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (isDefault) {
                    Text(
                        "Default",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    TextButton(
                        onClick = onSetDefault,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Text("Set as default")
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
