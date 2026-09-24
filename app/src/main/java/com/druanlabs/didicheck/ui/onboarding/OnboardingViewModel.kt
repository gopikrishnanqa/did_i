package com.druanlabs.didicheck.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.druanlabs.didicheck.DidIApplication
import com.druanlabs.didicheck.data.model.ItemKind
import com.druanlabs.didicheck.data.model.Routine
import com.druanlabs.didicheck.data.model.RoutineItem
import com.druanlabs.didicheck.util.newId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    Welcome,
    Situations,
    TakeItems,
    ConfirmItems,
    DidI,
    Review,
    Extras,
    Done,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val situations: Set<OnboardingSituation> = emptySet(),
    /** Which situation we're asking TAKE items for (index into takeQueue). */
    val takeQueueIndex: Int = 0,
    val takeBySituation: Map<OnboardingSituation, Set<String>> = emptyMap(),
    val confirmQueueIndex: Int = 0,
    val confirmBySituation: Map<OnboardingSituation, Set<String>> = emptyMap(),
    val didILabels: Set<String> = emptySet(),
    val duringDayLabels: Set<String> = emptySet(),
    val draftRoutines: List<Routine> = emptyList(),
    val saving: Boolean = false,
    val finished: Boolean = false,
) {
    val takeQueue: List<OnboardingSituation>
        get() = situations.filter { it.asksTake }.sortedBy { it.priority }

    val confirmQueue: List<OnboardingSituation>
        get() = situations.filter { it.asksConfirm }.sortedBy { it.priority }

    val currentTakeSituation: OnboardingSituation?
        get() = takeQueue.getOrNull(takeQueueIndex)

    val currentConfirmSituation: OnboardingSituation?
        get() = confirmQueue.getOrNull(confirmQueueIndex)

    /** Progress among question steps (Situations … Review), 0-based. */
    fun progressIndex(): Int {
        val steps = questionSteps()
        return steps.indexOf(step).coerceAtLeast(0)
    }

    fun progressTotal(): Int = questionSteps().size.coerceAtLeast(1)

    private fun questionSteps(): List<OnboardingStep> {
        val list = mutableListOf(OnboardingStep.Situations)
        if (takeQueue.isNotEmpty()) list += OnboardingStep.TakeItems
        if (confirmQueue.isNotEmpty()) list += OnboardingStep.ConfirmItems
        list += OnboardingStep.DidI
        list += OnboardingStep.Review
        return list
    }
}

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as DidIApplication).repository
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun start() {
        _state.value = OnboardingUiState()
    }

    fun toggleSituation(situation: OnboardingSituation) {
        _state.update { s ->
            val next = s.situations.toMutableSet()
            if (!next.add(situation)) next.remove(situation)
            s.copy(situations = next)
        }
    }

    fun toggleTakeLabel(label: String) {
        val situation = _state.value.currentTakeSituation ?: return
        _state.update { s ->
            val current = s.takeBySituation[situation].orEmpty().toMutableSet()
            if (!current.add(label)) current.remove(label)
            s.copy(takeBySituation = s.takeBySituation + (situation to current))
        }
    }

    fun addCustomTake(label: String) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        val situation = _state.value.currentTakeSituation ?: return
        _state.update { s ->
            val current = s.takeBySituation[situation].orEmpty() + trimmed
            s.copy(takeBySituation = s.takeBySituation + (situation to current))
        }
    }

    fun toggleConfirmLabel(label: String) {
        val situation = _state.value.currentConfirmSituation ?: return
        _state.update { s ->
            val current = s.confirmBySituation[situation].orEmpty().toMutableSet()
            if (!current.add(label)) current.remove(label)
            s.copy(confirmBySituation = s.confirmBySituation + (situation to current))
        }
    }

    fun addCustomConfirm(label: String) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        val situation = _state.value.currentConfirmSituation ?: return
        _state.update { s ->
            val current = s.confirmBySituation[situation].orEmpty() + trimmed
            s.copy(confirmBySituation = s.confirmBySituation + (situation to current))
        }
    }

    fun toggleDidI(label: String) {
        _state.update { s ->
            val next = s.didILabels.toMutableSet()
            if (!next.add(label)) next.remove(label)
            s.copy(didILabels = next)
        }
    }

    fun addCustomDidI(label: String) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        _state.update { s -> s.copy(didILabels = s.didILabels + trimmed) }
    }

    fun toggleDuringDay(label: String) {
        _state.update { s ->
            val next = s.duringDayLabels.toMutableSet()
            if (!next.add(label)) next.remove(label)
            s.copy(duringDayLabels = next)
        }
    }

    fun continueFromWelcome() {
        _state.update { it.copy(step = OnboardingStep.Situations) }
    }

    fun continueFromSituations() {
        val s = _state.value
        if (s.situations.isEmpty()) return
        when {
            s.takeQueue.isNotEmpty() -> _state.update {
                it.copy(step = OnboardingStep.TakeItems, takeQueueIndex = 0)
            }
            s.confirmQueue.isNotEmpty() -> _state.update {
                it.copy(step = OnboardingStep.ConfirmItems, confirmQueueIndex = 0)
            }
            else -> _state.update { it.copy(step = OnboardingStep.DidI) }
        }
    }

    fun continueFromTake() {
        val s = _state.value
        val nextIndex = s.takeQueueIndex + 1
        if (nextIndex < s.takeQueue.size) {
            _state.update { it.copy(takeQueueIndex = nextIndex) }
            return
        }
        if (s.confirmQueue.isNotEmpty()) {
            _state.update { it.copy(step = OnboardingStep.ConfirmItems, confirmQueueIndex = 0) }
        } else {
            _state.update { it.copy(step = OnboardingStep.DidI) }
        }
    }

    fun continueFromConfirm() {
        val s = _state.value
        val nextIndex = s.confirmQueueIndex + 1
        if (nextIndex < s.confirmQueue.size) {
            _state.update { it.copy(confirmQueueIndex = nextIndex) }
            return
        }
        _state.update { it.copy(step = OnboardingStep.DidI) }
    }

    fun continueFromDidI() {
        val s = _state.value
        val takeMap = s.takeBySituation.mapValues { it.value.toList() }
        val confirmMap = s.confirmBySituation.mapValues { it.value.toList() }
        val drafts = OnboardingCatalog.buildRoutines(s.situations, takeMap, confirmMap)
        _state.update { it.copy(step = OnboardingStep.Review, draftRoutines = drafts) }
    }

    fun removeDraftItem(routineId: String, itemId: String) {
        _state.update { s ->
            s.copy(
                draftRoutines = s.draftRoutines.map { routine ->
                    if (routine.id != routineId) routine
                    else routine.copy(items = routine.items.filterNot { it.id == itemId })
                },
            )
        }
    }

    fun addDraftItem(routineId: String, label: String, kind: ItemKind) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        _state.update { s ->
            s.copy(
                draftRoutines = s.draftRoutines.map { routine ->
                    if (routine.id != routineId) routine
                    else routine.copy(
                        items = routine.items + RoutineItem(
                            id = newId(),
                            routineId = routineId,
                            label = trimmed,
                            kind = kind,
                            sortOrder = routine.items.size,
                        ),
                    )
                },
            )
        }
    }

    fun continueFromReview() {
        _state.update { it.copy(step = OnboardingStep.Extras) }
    }

    fun skipExtras() {
        persistAndFinish()
    }

    fun finishExtras() {
        persistAndFinish()
    }

    fun goBack() {
        val s = _state.value
        when (s.step) {
            OnboardingStep.Welcome -> Unit
            OnboardingStep.Situations -> _state.update { it.copy(step = OnboardingStep.Welcome) }
            OnboardingStep.TakeItems -> {
                if (s.takeQueueIndex > 0) {
                    _state.update { it.copy(takeQueueIndex = it.takeQueueIndex - 1) }
                } else {
                    _state.update { it.copy(step = OnboardingStep.Situations) }
                }
            }
            OnboardingStep.ConfirmItems -> {
                if (s.confirmQueueIndex > 0) {
                    _state.update { it.copy(confirmQueueIndex = it.confirmQueueIndex - 1) }
                } else if (s.takeQueue.isNotEmpty()) {
                    _state.update {
                        it.copy(
                            step = OnboardingStep.TakeItems,
                            takeQueueIndex = it.takeQueue.lastIndex.coerceAtLeast(0),
                        )
                    }
                } else {
                    _state.update { it.copy(step = OnboardingStep.Situations) }
                }
            }
            OnboardingStep.DidI -> {
                when {
                    s.confirmQueue.isNotEmpty() -> _state.update {
                        it.copy(
                            step = OnboardingStep.ConfirmItems,
                            confirmQueueIndex = it.confirmQueue.lastIndex.coerceAtLeast(0),
                        )
                    }
                    s.takeQueue.isNotEmpty() -> _state.update {
                        it.copy(
                            step = OnboardingStep.TakeItems,
                            takeQueueIndex = it.takeQueue.lastIndex.coerceAtLeast(0),
                        )
                    }
                    else -> _state.update { it.copy(step = OnboardingStep.Situations) }
                }
            }
            OnboardingStep.Review -> _state.update { it.copy(step = OnboardingStep.DidI) }
            OnboardingStep.Extras -> _state.update { it.copy(step = OnboardingStep.Review) }
            OnboardingStep.Done -> Unit
        }
    }

    private fun persistAndFinish() {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val s = _state.value
            val actions = (s.didILabels + s.duringDayLabels).toList()
            repo.applyOnboardingResult(s.draftRoutines, actions)
            _state.update {
                it.copy(saving = false, step = OnboardingStep.Done, finished = false)
            }
        }
    }

    fun goToApp() {
        _state.update { it.copy(finished = true) }
    }
}
