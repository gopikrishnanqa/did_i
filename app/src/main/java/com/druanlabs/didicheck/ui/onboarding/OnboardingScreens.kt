package com.druanlabs.didicheck.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.druanlabs.didicheck.data.model.ItemKind
import com.druanlabs.didicheck.data.model.Routine
import com.druanlabs.didicheck.ui.components.PrimaryAction
import com.druanlabs.didicheck.ui.components.QuietCard
import com.druanlabs.didicheck.ui.components.SecondaryAction
import com.druanlabs.didicheck.ui.theme.BrandBlue
import com.druanlabs.didicheck.ui.theme.BrandBlueSoft
import com.druanlabs.didicheck.ui.theme.BrandGreen
import com.druanlabs.didicheck.ui.theme.BrandGreenSoft

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }

    BackHandler(enabled = state.step != OnboardingStep.Welcome && state.step != OnboardingStep.Done) {
        viewModel.goBack()
    }

    when (state.step) {
        OnboardingStep.Welcome -> WelcomeStep(
            onContinue = viewModel::continueFromWelcome,
        )
        OnboardingStep.Situations -> SituationsStep(
            state = state,
            onBack = viewModel::goBack,
            onToggle = viewModel::toggleSituation,
            onContinue = viewModel::continueFromSituations,
        )
        OnboardingStep.TakeItems -> {
            val situation = state.currentTakeSituation
            if (situation == null) {
                LaunchedEffect(Unit) { viewModel.continueFromTake() }
            } else {
                ItemPickStep(
                    state = state,
                    title = takeTitle(situation),
                    subtitle = "Select the items you often forget.",
                    options = OnboardingCatalog.takeOptions(situation),
                    selected = state.takeBySituation[situation].orEmpty(),
                    onBack = viewModel::goBack,
                    onToggle = viewModel::toggleTakeLabel,
                    onAddCustom = viewModel::addCustomTake,
                    onContinue = viewModel::continueFromTake,
                )
            }
        }
        OnboardingStep.ConfirmItems -> {
            val situation = state.currentConfirmSituation
            if (situation == null) {
                LaunchedEffect(Unit) { viewModel.continueFromConfirm() }
            } else {
                ItemPickStep(
                    state = state,
                    title = confirmTitle(situation),
                    subtitle = "Select the things you often need to confirm.",
                    options = OnboardingCatalog.confirmOptions(situation),
                    selected = state.confirmBySituation[situation].orEmpty(),
                    onBack = viewModel::goBack,
                    onToggle = viewModel::toggleConfirmLabel,
                    onAddCustom = viewModel::addCustomConfirm,
                    onContinue = viewModel::continueFromConfirm,
                    listLayout = true,
                )
            }
        }
        OnboardingStep.DidI -> DidIStep(
            state = state,
            onBack = viewModel::goBack,
            onToggleDidI = viewModel::toggleDidI,
            onAddCustom = viewModel::addCustomDidI,
            onToggleDuringDay = viewModel::toggleDuringDay,
            onContinue = viewModel::continueFromDidI,
        )
        OnboardingStep.Review -> ReviewStep(
            state = state,
            onBack = viewModel::goBack,
            onRemoveItem = viewModel::removeDraftItem,
            onAddItem = viewModel::addDraftItem,
            onContinue = viewModel::continueFromReview,
        )
        OnboardingStep.Extras -> ExtrasStep(
            saving = state.saving,
            onBack = viewModel::goBack,
            onSkip = viewModel::skipExtras,
            onContinue = viewModel::finishExtras,
        )
        OnboardingStep.Done -> DoneStep(
            onGoToApp = viewModel::goToApp,
        )
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(24.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(BrandBlueSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(64.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Welcome to Did I?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Your memory for everyday life.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Column {
            PrimaryAction(
                text = "Let’s get started",
                onClick = onContinue,
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "A quick 1-minute setup to create routines just for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SituationsStep(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onToggle: (OnboardingSituation) -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingScaffold(
        state = state,
        showProgress = true,
        onBack = onBack,
        title = "When would you like help remembering things?",
        subtitle = "Choose all that apply.",
        primaryText = "Continue",
        primaryEnabled = state.situations.isNotEmpty(),
        onPrimary = onContinue,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(OnboardingCatalog.situations, key = { it.first.name }) { (situation, icon) ->
                SelectCard(
                    label = situation.title,
                    icon = icon,
                    selected = situation in state.situations,
                    onClick = { onToggle(situation) },
                )
            }
        }
    }
}

@Composable
private fun ItemPickStep(
    state: OnboardingUiState,
    title: String,
    subtitle: String,
    options: List<CatalogOption>,
    selected: Set<String>,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onAddCustom: (String) -> Unit,
    onContinue: () -> Unit,
    listLayout: Boolean = false,
) {
    var showAdd by remember { mutableStateOf(false) }
    OnboardingScaffold(
        state = state,
        showProgress = true,
        onBack = onBack,
        title = title,
        subtitle = subtitle,
        primaryText = "Continue",
        primaryEnabled = true,
        onPrimary = onContinue,
    ) {
        if (listLayout) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { option ->
                    SelectRow(
                        label = option.label,
                        icon = option.icon,
                        selected = option.label in selected,
                        onClick = { onToggle(option.label) },
                    )
                }
                selected.filter { label -> options.none { it.label == label } }.forEach { custom ->
                    SelectRow(
                        label = custom,
                        icon = Icons.Outlined.Add,
                        selected = true,
                        onClick = { onToggle(custom) },
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(options, key = { it.id }) { option ->
                    SelectCard(
                        label = option.label,
                        icon = option.icon,
                        selected = option.label in selected,
                        onClick = { onToggle(option.label) },
                    )
                }
                selected.filter { label -> options.none { it.label == label } }.forEach { custom ->
                    item(key = "custom-$custom") {
                        SelectCard(
                            label = custom,
                            icon = Icons.Outlined.Add,
                            selected = true,
                            onClick = { onToggle(custom) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SecondaryAction(
            text = "Add something else",
            icon = Icons.Outlined.Add,
            onClick = { showAdd = true },
        )
    }
    if (showAdd) {
        AddLabelDialog(
            title = "Add something else",
            onDismiss = { showAdd = false },
            onConfirm = {
                onAddCustom(it)
                showAdd = false
            },
        )
    }
}

@Composable
private fun DidIStep(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onToggleDidI: (String) -> Unit,
    onAddCustom: (String) -> Unit,
    onToggleDuringDay: (String) -> Unit,
    onContinue: () -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    val showDuringDay = OnboardingSituation.DURING_DAY in state.situations
    OnboardingScaffold(
        state = state,
        showProgress = true,
        onBack = onBack,
        title = "Which things do you sometimes wonder, “Did I already do that?”",
        subtitle = "Select anything that sounds familiar.",
        primaryText = "Continue",
        primaryEnabled = true,
        onPrimary = onContinue,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OnboardingCatalog.didIOptions.forEach { option ->
                SelectRow(
                    label = option.label,
                    icon = option.icon,
                    selected = option.label in state.didILabels,
                    onClick = { onToggleDidI(option.label) },
                )
            }
            state.didILabels.filter { label ->
                OnboardingCatalog.didIOptions.none { it.label == label }
            }.forEach { custom ->
                SelectRow(
                    label = custom,
                    icon = Icons.Outlined.Add,
                    selected = true,
                    onClick = { onToggleDidI(custom) },
                )
            }
            if (showDuringDay) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Throughout the day",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OnboardingCatalog.duringDayQuickOptions.forEach { option ->
                    SelectRow(
                        label = option.label,
                        icon = option.icon,
                        selected = option.label in state.duringDayLabels,
                        onClick = { onToggleDuringDay(option.label) },
                    )
                }
            }
            SecondaryAction(
                text = "Add something else",
                icon = Icons.Outlined.Add,
                onClick = { showAdd = true },
            )
        }
    }
    if (showAdd) {
        AddLabelDialog(
            title = "Add something else",
            onDismiss = { showAdd = false },
            onConfirm = {
                onAddCustom(it)
                showAdd = false
            },
        )
    }
}

@Composable
private fun ReviewStep(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onRemoveItem: (routineId: String, itemId: String) -> Unit,
    onAddItem: (routineId: String, label: String, kind: ItemKind) -> Unit,
    onContinue: () -> Unit,
) {
    var addForRoutine by remember { mutableStateOf<Pair<String, ItemKind>?>(null) }
    OnboardingScaffold(
        state = state,
        showProgress = true,
        onBack = onBack,
        title = "We created these for you",
        subtitle = "Based on your answers, here are your first routines. You can change these anytime.",
        primaryText = "Looks good",
        primaryEnabled = true,
        onPrimary = onContinue,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.draftRoutines.forEach { routine ->
                RoutinePreviewCard(
                    routine = routine,
                    onRemoveItem = { onRemoveItem(routine.id, it) },
                    onAddTake = { addForRoutine = routine.id to ItemKind.TAKE },
                    onAddConfirm = { addForRoutine = routine.id to ItemKind.CONFIRM },
                )
            }
            if (state.draftRoutines.isEmpty()) {
                Text(
                    "We’ll start you with a simple Leaving Home routine.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    addForRoutine?.let { (routineId, kind) ->
        AddLabelDialog(
            title = if (kind == ItemKind.TAKE) "Add something to take" else "Add something to check",
            onDismiss = { addForRoutine = null },
            onConfirm = {
                onAddItem(routineId, it, kind)
                addForRoutine = null
            },
        )
    }
}

@Composable
private fun ExtrasStep(
    saving: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Anything else we should add?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "You can always add or edit later in Settings.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        QuietCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = BrandBlue)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Create a custom routine", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Available anytime from Settings → Manage routines",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        QuietCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = BrandBlue)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Quick actions for Did I Do It?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "We already added the ones you picked. Edit them anytime.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        SecondaryAction(text = "Skip for now", onClick = onSkip)
        Spacer(Modifier.height(10.dp))
        PrimaryAction(
            text = if (saving) "Saving…" else "Continue",
            enabled = !saving,
            onClick = onContinue,
            icon = Icons.AutoMirrored.Outlined.ArrowForward,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DoneStep(onGoToApp: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(BrandGreenSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(64.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "You’re all set!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "We’ll help you remember the little things.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            DoneBullet("Your routines are ready")
            DoneBullet("You can easily mark things you’ve done")
            DoneBullet("You can edit or add more anytime")
        }
        PrimaryAction(
            text = "Go to My App",
            onClick = onGoToApp,
            containerColor = BrandGreen,
            icon = Icons.AutoMirrored.Outlined.ArrowForward,
        )
    }
}

@Composable
private fun DoneBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Check, contentDescription = null, tint = BrandGreen)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RoutinePreviewCard(
    routine: Routine,
    onRemoveItem: (itemId: String) -> Unit,
    onAddTake: () -> Unit,
    onAddConfirm: () -> Unit,
) {
    val take = routine.items.filter { it.kind == ItemKind.TAKE }
    val confirm = routine.items.filter { it.kind == ItemKind.CONFIRM }
    QuietCard {
        Text(routine.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (take.isNotEmpty() || true) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Take with you",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            take.forEach { item ->
                DraftItemRow(label = item.label, onRemove = { onRemoveItem(item.id) })
            }
            TextButton(onClick = onAddTake) { Text("+ Add item") }
        }
        if (confirm.isNotEmpty() || routine.name == "Leaving Home" || routine.name == "Before Bed") {
            Spacer(Modifier.height(8.dp))
            Text(
                "Check before leaving",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            confirm.forEach { item ->
                DraftItemRow(label = item.label, onRemove = { onRemoveItem(item.id) })
            }
            TextButton(onClick = onAddConfirm) { Text("+ Add check") }
        }
    }
}

@Composable
private fun DraftItemRow(label: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Check, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Close, contentDescription = "Remove")
        }
    }
}

@Composable
private fun OnboardingScaffold(
    state: OnboardingUiState,
    showProgress: Boolean,
    onBack: () -> Unit,
    title: String,
    subtitle: String,
    primaryText: String,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            if (showProgress) {
                ProgressSegments(
                    total = state.progressTotal(),
                    index = state.progressIndex(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
            }
            item { content() }
        }
        PrimaryAction(
            text = primaryText,
            enabled = primaryEnabled && !state.saving,
            onClick = onPrimary,
            icon = Icons.AutoMirrored.Outlined.ArrowForward,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ProgressSegments(
    total: Int,
    index: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (i <= index) BrandBlue else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
private fun SelectCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) BrandBlue else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant)
            SelectionMark(selected)
        }
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SelectRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) BrandBlue else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        SelectionMark(selected)
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) BrandBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun AddLabelDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                placeholder = { Text("Type a name") },
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.trim().isNotEmpty(),
                onClick = { onConfirm(value) },
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun takeTitle(situation: OnboardingSituation): String = when (situation) {
    OnboardingSituation.LEAVING -> "What do you usually need before leaving home?"
    OnboardingSituation.WORK -> "What do you usually need before work?"
    OnboardingSituation.SHOPPING -> "What do you usually need when shopping?"
    OnboardingSituation.TRAVELLING -> "What do you usually need when travelling?"
    OnboardingSituation.BED -> "What do you usually need before sleeping?"
    OnboardingSituation.DURING_DAY -> "What do you need during the day?"
}

private fun confirmTitle(situation: OnboardingSituation): String = when (situation) {
    OnboardingSituation.BED -> "What do you need to check before bed?"
    else -> "What do you need to check before leaving?"
}
