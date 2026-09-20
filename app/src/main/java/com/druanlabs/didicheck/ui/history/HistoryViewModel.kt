package com.druanlabs.didicheck.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.druanlabs.didicheck.DidIApplication
import com.druanlabs.didicheck.data.model.Completion
import com.druanlabs.didicheck.util.SearchQuery
import com.druanlabs.didicheck.util.TimeText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HistoryGroup(
    val label: String,
    val items: List<Completion>,
)

data class SearchAnswer(
    val query: String,
    val matchedLabel: String?,
    val today: Completion?,
    val last: Completion?,
    val matches: List<Completion>,
)

data class HistoryUiState(
    val query: String = "",
    val groups: List<HistoryGroup> = emptyList(),
    val answer: SearchAnswer? = null,
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as DidIApplication).repository
    private val query = MutableStateFlow("")
    private val focusId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(repo.completions, query, focusId) { completions, q, focus ->
        Triple(completions, q, focus)
    }.mapLatest { (completions, q, focus) ->
        val answer = if (q.isBlank()) null else buildAnswer(q, completions)
        val visible = when {
            answer != null -> answer.matches
            else -> completions
        }
        val groups = visible
            .groupBy { TimeText.toLocalDateTime(it.timestamp).toLocalDate() }
            .toSortedMap(compareByDescending { it })
            .map { (date, items) -> HistoryGroup(TimeText.sectionLabel(date), items) }
        // Keep focus id available via selected sheet in UI.
        HistoryUiState(query = q, groups = groups, answer = answer).also {
            // no-op; focus handled in screen via selected completion
        }.let { state ->
            if (focus != null && q.isBlank()) {
                state
            } else {
                state
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun focus(id: String?) {
        focusId.value = id
    }

    fun undo(id: String) {
        viewModelScope.launch { repo.undo(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.deleteCompletion(id) }
    }

    fun editTime(id: String, timestamp: Long) {
        viewModelScope.launch { repo.editCompletionTime(id, timestamp) }
    }

    private fun buildAnswer(raw: String, completions: List<Completion>): SearchAnswer {
        val keys = SearchQuery.keywords(raw)
        val phrase = SearchQuery.primaryPhrase(raw)
        val today = LocalDate.now()

        fun matches(c: Completion): Boolean {
            val haystack = buildString {
                append(c.actionLabel)
                append(' ')
                append(c.routineName.orEmpty())
                append(' ')
                append(c.detailItems.joinToString(" "))
            }.lowercase()
            return if (keys.isEmpty()) {
                haystack.contains(phrase.lowercase())
            } else {
                keys.any { haystack.contains(it) }
            }
        }

        val matchList = completions.filter(::matches)
        val matchedLabel = matchList
            .groupBy { it.actionLabel.lowercase() }
            .maxByOrNull { it.value.size }
            ?.value
            ?.firstOrNull()
            ?.actionLabel
            ?: matchList.firstOrNull()?.actionLabel

        val forLabel = if (matchedLabel == null) {
            matchList
        } else {
            matchList.filter {
                it.actionLabel.equals(matchedLabel, ignoreCase = true) ||
                    it.detailItems.any { d -> d.contains(matchedLabel, ignoreCase = true) } ||
                    keys.any { k ->
                        it.actionLabel.contains(k, ignoreCase = true) ||
                            it.detailItems.any { d -> d.contains(k, ignoreCase = true) }
                    }
            }.ifEmpty { matchList }
        }

        return SearchAnswer(
            query = phrase,
            matchedLabel = matchedLabel,
            today = forLabel.firstOrNull {
                TimeText.toLocalDateTime(it.timestamp).toLocalDate() == today
            },
            last = forLabel.firstOrNull(),
            matches = matchList,
        )
    }
}
