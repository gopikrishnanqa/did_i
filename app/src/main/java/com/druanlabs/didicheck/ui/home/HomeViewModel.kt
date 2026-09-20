package com.druanlabs.didicheck.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.druanlabs.didicheck.DidIApplication
import com.druanlabs.didicheck.data.model.Completion
import com.druanlabs.didicheck.data.model.Routine
import com.druanlabs.didicheck.util.TimeText
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime

data class HomeUiState(
    val greeting: String = TimeText.greeting(),
    val dateLabel: String = TimeText.dateHeadline(),
    val suggested: Routine? = null,
    val recent: List<Completion> = emptyList(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as DidIApplication).repository

    val uiState = combine(
        repo.routines,
        repo.defaultRoutineId,
        repo.recentCompletions(5),
    ) { routines, defaultRoutineId, recent ->
        HomeUiState(
            greeting = TimeText.greeting(),
            dateLabel = TimeText.dateHeadline(),
            suggested = resolveDefaultRoutine(routines, defaultRoutineId, LocalTime.now().hour),
            recent = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun resolveDefaultRoutine(
        routines: List<Routine>,
        defaultRoutineId: String?,
        hour: Int,
    ): Routine? {
        if (routines.isEmpty()) return null
        if (!defaultRoutineId.isNullOrBlank()) {
            routines.firstOrNull { it.id == defaultRoutineId }?.let { return it }
        }
        val preferred = when (hour) {
            in 5..10 -> listOf("Going to Work", "Leaving Home")
            in 20..23, in 0..4 -> listOf("Before Bed", "Before Sleeping")
            in 11..16 -> listOf("Going to Work", "Going Shopping")
            else -> listOf("Going Shopping", "Going on a Trip")
        }
        preferred.forEach { name ->
            routines.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
        }
        return routines.first()
    }
}
