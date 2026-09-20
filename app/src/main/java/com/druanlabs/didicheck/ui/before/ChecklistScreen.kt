package com.druanlabs.didicheck.ui.before

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.druanlabs.didicheck.data.model.ItemKind
import com.druanlabs.didicheck.data.model.RoutineItem
import com.druanlabs.didicheck.ui.components.CheckRow
import com.druanlabs.didicheck.ui.components.PrimaryAction
import com.druanlabs.didicheck.ui.components.ProofPhotoImage
import com.druanlabs.didicheck.ui.components.ScreenHeader
import com.druanlabs.didicheck.util.ProofPhotoStore
import java.io.File

private enum class CaptureMode { None, Item, Complete }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistRoute(
    routineId: String,
    onBack: () -> Unit,
    onChangeRoutine: () -> Unit,
    onCompleted: (message: String) -> Unit,
    viewModel: ChecklistViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var askProof by remember { mutableStateOf(false) }
    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }
    var pendingItem by remember { mutableStateOf<RoutineItem?>(null) }
    var captureMode by remember { mutableStateOf(CaptureMode.None) }
    var localProofPath by remember { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val temp = pendingCaptureFile
        val item = pendingItem
        val mode = captureMode
        pendingCaptureFile = null
        pendingItem = null
        captureMode = CaptureMode.None
        if (success && temp != null) {
            when (mode) {
                CaptureMode.Item -> {
                    if (item != null) viewModel.checkWithPhoto(item, temp.absolutePath)
                    else runCatching { temp.delete() }
                }
                CaptureMode.Complete -> {
                    localProofPath = temp.absolutePath
                    viewModel.complete(tempPhotoPath = temp.absolutePath)
                }
                CaptureMode.None -> runCatching { temp.delete() }
            }
        } else {
            runCatching { temp?.delete() }
            if (mode == CaptureMode.Complete) {
                viewModel.complete(tempPhotoPath = null)
            }
        }
    }

    val requestCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val mode = captureMode
        if (!granted) {
            if (mode == CaptureMode.Complete) viewModel.complete(tempPhotoPath = null)
            captureMode = CaptureMode.None
            pendingItem = null
            return@rememberLauncherForActivityResult
        }
        val capture = ProofPhotoStore.createCaptureUri(context)
        if (capture == null) {
            if (mode == CaptureMode.Complete) viewModel.complete(tempPhotoPath = null)
            captureMode = CaptureMode.None
            pendingItem = null
            return@rememberLauncherForActivityResult
        }
        pendingCaptureFile = capture.first
        takePictureLauncher.launch(capture.second)
    }

    fun ensureCameraThen(mode: CaptureMode, item: RoutineItem? = null) {
        captureMode = mode
        pendingItem = item
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val capture = ProofPhotoStore.createCaptureUri(context)
            if (capture == null) {
                if (mode == CaptureMode.Complete) viewModel.complete(tempPhotoPath = null)
                captureMode = CaptureMode.None
                pendingItem = null
                return
            }
            pendingCaptureFile = capture.first
            takePictureLauncher.launch(capture.second)
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(routineId) { viewModel.load(routineId) }
    LaunchedEffect(state.completed, state.routine) {
        if (state.completed && state.routine != null) {
            onCompleted("${state.routine!!.name} completed")
        }
    }

    val routine = state.routine
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(routine?.name ?: "Before I Go") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onChangeRoutine) { Text("Change") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (routine == null) return@Scaffold
        val takeItems = routine.items.filter { it.kind == ItemKind.TAKE }
        val confirmItems = routine.items.filter { it.kind == ItemKind.CONFIRM }
        val allChecked = routine.items.isNotEmpty() && routine.items.all { it.id in state.checkedIds }

        fun onItemToggle(item: RoutineItem) {
            val checked = item.id in state.checkedIds
            if (checked) {
                viewModel.toggle(item)
                return
            }
            if (item.photoRequired) {
                ensureCameraThen(CaptureMode.Item, item)
            } else {
                viewModel.toggle(item)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ScreenHeader(
                    eyebrow = routine.name,
                    title = "Ready to go?",
                    subtitle = "Take a quick look before you leave.",
                )
            }
            if (takeItems.isNotEmpty()) {
                item {
                    Text(
                        "REMEMBER TO TAKE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(takeItems, key = { it.id }) { item ->
                    CheckRow(
                        label = item.label,
                        checked = item.id in state.checkedIds,
                        photoRequired = item.photoRequired,
                        onToggle = { onItemToggle(item) },
                    )
                }
            }
            if (confirmItems.isNotEmpty()) {
                item {
                    Text(
                        "CONFIRM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(confirmItems, key = { it.id }) { item ->
                    CheckRow(
                        label = item.label,
                        checked = item.id in state.checkedIds,
                        photoRequired = item.photoRequired,
                        onToggle = { onItemToggle(item) },
                    )
                }
            }
            if (localProofPath != null) {
                item {
                    ProofPhotoImage(path = localProofPath, label = "Photo proof")
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                PrimaryAction(
                    text = "I'm ready",
                    enabled = allChecked && !state.completing,
                    onClick = {
                        if (routine.askPhotoOnComplete) {
                            askProof = true
                        } else {
                            viewModel.complete(tempPhotoPath = null)
                        }
                    },
                )
                if (!allChecked) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Check everything before you go.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (askProof) {
        AlertDialog(
            onDismissRequest = { askProof = false },
            title = { Text("Add a photo as proof?") },
            text = {
                Column {
                    Text(
                        "Optional. Take a quick photo and pin it to this routine check. You can skip and finish without one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        askProof = false
                        ensureCameraThen(CaptureMode.Complete)
                    },
                ) { Text("Take photo") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        askProof = false
                        viewModel.complete(tempPhotoPath = null)
                    },
                ) { Text("Skip") }
            },
        )
    }
}
