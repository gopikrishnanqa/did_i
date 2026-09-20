package com.druanlabs.didicheck.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.druanlabs.didicheck.DidIApplication
import com.druanlabs.didicheck.data.model.ItemKind
import com.druanlabs.didicheck.data.model.QuickAction
import com.druanlabs.didicheck.data.model.ReminderRepeat
import com.druanlabs.didicheck.data.model.Routine
import com.druanlabs.didicheck.data.model.RoutineItem
import com.druanlabs.didicheck.data.model.RoutineReminderTime
import com.druanlabs.didicheck.reminders.GentleReminderScheduler
import com.druanlabs.didicheck.reminders.RoutineReminderScheduler
import com.druanlabs.didicheck.util.newId
import com.druanlabs.didicheck.widget.DidIWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repo = (application as DidIApplication).repository
    val notifications = repo.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            repo.setNotificationsEnabled(enabled)
            GentleReminderScheduler.sync(app)
        }
    }
}

class ManageRoutinesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repo = (application as DidIApplication).repository
    val routines = repo.routines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch {
            repo.deleteRoutine(id)
            RoutineReminderScheduler.cancelForRoutine(app, id)
            RoutineReminderScheduler.sync(app)
        }
    }
}

class ManageActionsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repo = (application as DidIApplication).repository
    val actions = repo.quickActions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(label: String) {
        viewModelScope.launch {
            repo.saveQuickAction(label)
            DidIWidgetUpdater.requestUpdate(app)
        }
    }

    fun rename(action: QuickAction, label: String) {
        viewModelScope.launch {
            repo.renameQuickAction(action.id, label)
            DidIWidgetUpdater.requestUpdate(app)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repo.deleteQuickAction(id)
            DidIWidgetUpdater.requestUpdate(app)
        }
    }

    fun togglePin(action: QuickAction) {
        viewModelScope.launch {
            repo.setPinned(action.id, !action.pinned)
            DidIWidgetUpdater.requestUpdate(app)
        }
    }

    fun movePinned(id: String, delta: Int) {
        viewModelScope.launch {
            repo.movePinned(id, delta)
            DidIWidgetUpdater.requestUpdate(app)
        }
    }
}

class EditRoutineViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repo = (application as DidIApplication).repository
    private val _draft = MutableStateFlow(emptyDraft())
    private var baseline: Routine = emptyDraft()
    val draft = _draft.asStateFlow()

    fun load(routineId: String?) {
        viewModelScope.launch {
            val loaded = if (routineId.isNullOrBlank() || routineId == "new") {
                emptyDraft()
            } else {
                repo.getRoutine(routineId) ?: emptyDraft()
            }
            baseline = loaded
            _draft.value = loaded
        }
    }

    fun isDirty(): Boolean = _draft.value != baseline

    fun canSave(): Boolean = _draft.value.name.trim().isNotEmpty()

    fun save(onSaved: () -> Unit) {
        if (!canSave()) return
        viewModelScope.launch {
            repo.saveRoutine(_draft.value)
            RoutineReminderScheduler.sync(app)
            baseline = _draft.value
            onSaved()
        }
    }

    fun setName(name: String) {
        _draft.value = _draft.value.copy(name = name)
    }

    fun setReminderEnabled(enabled: Boolean) {
        val current = _draft.value
        if (!enabled) {
            _draft.value = current.copy(
                reminderRepeat = ReminderRepeat.OFF,
                reminderTimes = emptyList(),
                reminderDays = emptySet(),
            )
            return
        }
        val weekdays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
        val days = current.activeReminderDays().ifEmpty { weekdays }
        _draft.value = current.copy(
            reminderRepeat = ReminderRepeat.CUSTOM,
            reminderDays = days,
            reminderTimes = current.reminderTimes.ifEmpty {
                listOf(RoutineReminderTime(newId(), 8, 45))
            },
        )
    }

    fun toggleAlarmDay(day: DayOfWeek) {
        val current = _draft.value
        if (current.reminderRepeat == ReminderRepeat.OFF) return
        val selected = current.activeReminderDays().toMutableSet()
        if (!selected.add(day)) {
            if (selected.size > 1) selected.remove(day)
        }
        _draft.value = current.copy(
            reminderRepeat = ReminderRepeat.CUSTOM,
            reminderDays = selected,
        )
    }

    fun setReminderTime(id: String, hour: Int, minute: Int) {
        updateReminderTime(id, hour, minute)
    }

    fun addReminderTime(hour: Int = 20, minute: Int = 0) {
        val current = _draft.value
        if (current.reminderTimes.size >= 2) return
        val exists = current.reminderTimes.any { it.hour == hour && it.minute == minute }
        if (exists) return
        _draft.value = current.copy(
            reminderTimes = current.reminderTimes + RoutineReminderTime(newId(), hour, minute),
        )
    }

    fun updateReminderTime(id: String, hour: Int, minute: Int) {
        _draft.value = _draft.value.copy(
            reminderTimes = _draft.value.reminderTimes.map { time ->
                if (time.id == id) time.copy(hour = hour.coerceIn(0, 23), minute = minute.coerceIn(0, 59))
                else time
            },
        )
    }

    fun removeReminderTime(id: String) {
        val next = _draft.value.reminderTimes.filterNot { it.id == id }
        _draft.value = _draft.value.copy(
            reminderTimes = next,
            reminderRepeat = if (next.isEmpty()) ReminderRepeat.OFF else _draft.value.reminderRepeat,
            reminderDays = if (next.isEmpty()) emptySet() else _draft.value.reminderDays,
        )
    }

    fun addItem(label: String, kind: ItemKind, photoRequired: Boolean = false) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        val current = _draft.value
        _draft.value = current.copy(
            items = current.items + RoutineItem(
                id = newId(),
                routineId = current.id,
                label = trimmed,
                kind = kind,
                sortOrder = current.items.size,
                photoRequired = photoRequired,
            )
        )
    }

    fun setItemPhotoRequired(id: String, required: Boolean) {
        _draft.value = _draft.value.copy(
            items = _draft.value.items.map { item ->
                if (item.id == id) item.copy(photoRequired = required) else item
            },
        )
    }

    fun setAskPhotoOnComplete(enabled: Boolean) {
        _draft.value = _draft.value.copy(askPhotoOnComplete = enabled)
    }

    fun removeItem(id: String) {
        _draft.value = _draft.value.copy(items = _draft.value.items.filterNot { it.id == id })
    }

    fun moveItem(id: String, delta: Int) {
        val items = _draft.value.items.toMutableList()
        val index = items.indexOfFirst { it.id == id }
        val target = index + delta
        if (index < 0 || target !in items.indices) return
        val item = items.removeAt(index)
        items.add(target, item)
        _draft.value = _draft.value.copy(items = items)
    }

    private fun emptyDraft(sortOrder: Int = 100): Routine {
        return Routine(
            id = newId(),
            name = "",
            sortOrder = sortOrder,
            lastCompletedAt = null,
            items = emptyList(),
            reminderTimes = emptyList(),
            reminderRepeat = ReminderRepeat.OFF,
            reminderDays = emptySet(),
            askPhotoOnComplete = false,
        )
    }
}
