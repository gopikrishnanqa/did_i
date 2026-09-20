package com.druanlabs.didicheck.ui.didi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.druanlabs.didicheck.DidIApplication
import com.druanlabs.didicheck.data.model.Completion
import com.druanlabs.didicheck.data.model.CompletionSource
import com.druanlabs.didicheck.data.model.QuickAction
import com.druanlabs.didicheck.data.model.Routine
import com.druanlabs.didicheck.widget.DidIWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DidIUiState(
    val pinned: List<QuickAction> = emptyList(),
    val others: List<QuickAction> = emptyList(),
    val last: Completion? = null,
    val defaultRoutine: Routine? = null,
    val snackbar: String? = null,
    val lastId: String? = null,
)

class DidIDoItViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repo = (application as DidIApplication).repository
    private val snackbar = MutableStateFlow<String?>(null)
    private val lastId = MutableStateFlow<String?>(null)

    private val defaultRoutine = combine(repo.routines, repo.defaultRoutineId) { routines, defaultId ->
        when {
            !defaultId.isNullOrBlank() -> routines.firstOrNull { it.id == defaultId }
            else -> null
        } ?: routines.firstOrNull()
    }

    val uiState = combine(
        repo.quickActions,
        repo.recentCompletions(1),
        defaultRoutine,
        snackbar,
        lastId,
    ) { actions, recent, routine, message, id ->
        DidIUiState(
            pinned = actions.filter { it.pinned },
            others = actions.filter { !it.pinned }.take(8),
            last = recent.firstOrNull(),
            defaultRoutine = routine,
            snackbar = message,
            lastId = id,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DidIUiState())

    fun mark(label: String) {
        viewModelScope.launch {
            val recorded = repo.markDone(label, CompletionSource.QUICK)
            lastId.value = recorded.id
            snackbar.value = "✓ ${recorded.actionLabel} · Just now"
            DidIWidgetUpdater.requestUpdate(app)
        }
    }

    fun addAndMark(label: String) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repo.saveQuickAction(trimmed)
            mark(trimmed)
        }
    }

    fun undoLast() {
        val id = lastId.value ?: return
        viewModelScope.launch {
            repo.undo(id)
            lastId.value = null
            snackbar.value = "Undone"
            DidIWidgetUpdater.requestUpdate(app)
        }
    }

    fun consumeSnackbar() {
        snackbar.value = null
    }
}
