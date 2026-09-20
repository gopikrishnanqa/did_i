package com.druanlabs.didicheck.ui.before

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.druanlabs.didicheck.DidIApplication
import com.druanlabs.didicheck.data.model.CompletionSource
import com.druanlabs.didicheck.data.model.ItemKind
import com.druanlabs.didicheck.data.model.Routine
import com.druanlabs.didicheck.data.model.RoutineItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RoutinesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as DidIApplication).repository
    val routines = repo.routines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val defaultRoutineId = repo.defaultRoutineId.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    fun setDefaultRoutine(id: String) {
        viewModelScope.launch { repo.setDefaultRoutineId(id) }
    }
}

data class ChecklistUiState(
    val routine: Routine? = null,
    val checkedIds: Set<String> = emptySet(),
    val completed: Boolean = false,
    val completing: Boolean = false,
)

class ChecklistViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as DidIApplication).repository
    private val completionIds = mutableMapOf<String, String>()

    private val _state = MutableStateFlow(ChecklistUiState())
    val state = _state.asStateFlow()

    fun load(routineId: String) {
        viewModelScope.launch {
            val routine = repo.getRoutine(routineId)
            _state.value = ChecklistUiState(routine = routine)
            completionIds.clear()
        }
    }

    fun toggle(item: RoutineItem) {
        val currentlyChecked = item.id in _state.value.checkedIds
        val nowChecked = !currentlyChecked
        _state.update {
            it.copy(
                checkedIds = if (nowChecked) it.checkedIds + item.id else it.checkedIds - item.id
            )
        }
        viewModelScope.launch {
            if (nowChecked) {
                // CONFIRM items become searchable history entries immediately.
                if (item.kind == ItemKind.CONFIRM) {
                    val recorded = repo.markDone(
                        label = item.label,
                        source = CompletionSource.ROUTINE,
                        routineName = _state.value.routine?.name,
                    )
                    completionIds[item.id] = recorded.id
                }
            } else {
                // Undo any item completion (confirm tick or photo proof).
                completionIds.remove(item.id)?.let { repo.undo(it) }
            }
        }
    }

    /** Check a photo-required item after the camera capture succeeds. */
    fun checkWithPhoto(item: RoutineItem, tempPhotoPath: String) {
        if (item.id in _state.value.checkedIds) return
        viewModelScope.launch {
            val temp = java.io.File(tempPhotoPath).takeIf { it.exists() }
            val proofId = com.druanlabs.didicheck.util.newId()
            val photoPath = if (temp != null) {
                com.druanlabs.didicheck.util.ProofPhotoStore.persistProof(
                    context = getApplication(),
                    source = temp,
                    proofId = proofId,
                )
            } else {
                null
            }
            _state.update { it.copy(checkedIds = it.checkedIds + item.id) }
            val recorded = repo.markDone(
                label = item.label,
                source = CompletionSource.ROUTINE,
                routineName = _state.value.routine?.name,
                photoPath = photoPath,
            )
            completionIds[item.id] = recorded.id
        }
    }

    fun complete(tempPhotoPath: String? = null) {
        val routine = _state.value.routine ?: return
        if (_state.value.completing || _state.value.completed) return
        _state.update { it.copy(completing = true) }
        viewModelScope.launch {
            val checkedLabels = routine.items
                .filter { it.id in _state.value.checkedIds }
                .map { it.label }
            val temp = tempPhotoPath?.let { java.io.File(it) }?.takeIf { it.exists() }
            // Persist under a stable id inside app-private storage.
            val proofId = com.druanlabs.didicheck.util.newId()
            val photoPath = if (temp != null) {
                com.druanlabs.didicheck.util.ProofPhotoStore.persistProof(
                    context = getApplication(),
                    source = temp,
                    proofId = proofId,
                )
            } else {
                null
            }
            repo.completeRoutine(routine.id, checkedLabels, photoPath = photoPath)
            com.druanlabs.didicheck.widget.DidIWidgetUpdater.requestUpdate(getApplication())
            _state.update { it.copy(completed = true, completing = false) }
        }
    }
}
