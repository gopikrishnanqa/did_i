package com.druanlabs.didicheck.ui.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.druanlabs.didicheck.data.model.Completion
import com.druanlabs.didicheck.ui.components.CompletionRow
import com.druanlabs.didicheck.ui.components.PrimaryAction
import com.druanlabs.didicheck.ui.components.QuietCard
import com.druanlabs.didicheck.ui.components.SecondaryAction
import com.druanlabs.didicheck.ui.components.SectionLabel
import com.druanlabs.didicheck.ui.theme.BrandBlue
import com.druanlabs.didicheck.ui.theme.BrandBlueWash
import com.druanlabs.didicheck.ui.theme.BrandGreen
import com.druanlabs.didicheck.ui.theme.BrandGreenWash
import com.druanlabs.didicheck.ui.theme.BrandInk
import com.druanlabs.didicheck.ui.theme.BrandMuted
import com.druanlabs.didicheck.util.TimeText

@Composable
fun HomeRoute(
    onStartCheck: (routineId: String) -> Unit,
    onChooseRoutine: () -> Unit,
    onMarkSomething: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCompletion: (Completion) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onStartCheck = {
            state.suggested?.let { onStartCheck(it.id) } ?: onChooseRoutine()
        },
        onChooseRoutine = onChooseRoutine,
        onMarkSomething = onMarkSomething,
        onOpenHistory = onOpenHistory,
        onOpenCompletion = onOpenCompletion,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartCheck: () -> Unit,
    onChooseRoutine: () -> Unit,
    onMarkSomething: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCompletion: (Completion) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HomeHeader(
                dateLabel = state.dateLabel,
                greeting = "${state.greeting} 👋",
            )
        }

        item {
            BeforeIGoCard(
                preview = state.suggested?.itemPreview.orEmpty(),
                reminderHint = state.suggested?.takeIf { it.hasReminders }?.reminderScheduleLabel(),
                onStartCheck = onStartCheck,
                onChooseRoutine = onChooseRoutine,
            )
        }

        item {
            DidIDoItCard(onMarkSomething = onMarkSomething)
        }

        item {
            Text(
                text = "RECENT",
                style = MaterialTheme.typography.labelSmall,
                color = BrandMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (state.recent.isEmpty()) {
            item {
                QuietCard(shadowElevation = 1.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = BrandMuted,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Nothing recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandInk,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Things you check and mark will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandMuted,
                        )
                    }
                }
            }
        } else {
            items(state.recent, key = { it.id }) { completion ->
                QuietCard(onClick = { onOpenCompletion(completion) }) {
                    CompletionRow(
                        completion = completion,
                        trailing = TimeText.clock(completion.timestamp),
                        onClick = null,
                    )
                }
            }
            item {
                SecondaryAction("See all history", onClick = onOpenHistory)
            }
        }
    }
}

@Composable
private fun HomeHeader(
    dateLabel: String,
    greeting: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                dateLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = BrandMuted,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                greeting,
                style = MaterialTheme.typography.headlineLarge,
                color = BrandInk,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "What do you want to remember?",
                style = MaterialTheme.typography.bodyLarge,
                color = BrandMuted,
            )
        }
        SunCloudIllustration(modifier = Modifier.padding(top = 4.dp, start = 8.dp))
    }
}

@Composable
private fun BeforeIGoCard(
    preview: String,
    reminderHint: String?,
    onStartCheck: () -> Unit,
    onChooseRoutine: () -> Unit,
) {
    QuietCard(
        containerColor = BrandBlueWash,
        shadowElevation = 0.dp,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionLabel("Before", color = BrandBlue)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Before I Go",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandInk,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Don't forget important things.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMuted,
                )
                if (preview.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        preview,
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!reminderHint.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Reminder · $reminderHint",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandBlue,
                    )
                }
            }
            BackpackIllustration(modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryAction(
                text = "Start check",
                onClick = onStartCheck,
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            )
            SecondaryAction(
                text = "Choose a routine",
                onClick = onChooseRoutine,
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Outlined.ListAlt,
            )
        }
    }
}

@Composable
private fun DidIDoItCard(onMarkSomething: () -> Unit) {
    QuietCard(
        containerColor = BrandGreenWash,
        shadowElevation = 0.dp,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionLabel("After", color = BrandGreen)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Did I Do It?",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandInk,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Remember what you've already done.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMuted,
                )
            }
            CheckBurstIllustration(modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
        Spacer(Modifier.height(16.dp))
        PrimaryAction(
            text = "Mark something",
            onClick = onMarkSomething,
            containerColor = BrandGreen,
            icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        )
    }
}
