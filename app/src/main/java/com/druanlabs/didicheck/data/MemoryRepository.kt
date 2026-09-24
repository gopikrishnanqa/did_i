package com.druanlabs.didicheck.data

import com.druanlabs.didicheck.data.local.ActionDao
import com.druanlabs.didicheck.data.local.ChecklistRunDao
import com.druanlabs.didicheck.data.local.ChecklistRunEntity
import com.druanlabs.didicheck.data.local.ChecklistRunItemEntity
import com.druanlabs.didicheck.data.local.CompletionDao
import com.druanlabs.didicheck.data.local.CompletionEntity
import com.druanlabs.didicheck.data.local.DidIDatabase
import com.druanlabs.didicheck.data.local.QuickActionEntity
import com.druanlabs.didicheck.data.local.RoutineDao
import com.druanlabs.didicheck.data.local.RoutineEntity
import com.druanlabs.didicheck.data.local.RoutineItemEntity
import com.druanlabs.didicheck.data.local.RoutineReminderTimeEntity
import com.druanlabs.didicheck.data.local.SettingsStore
import com.druanlabs.didicheck.data.model.ChecklistRun
import com.druanlabs.didicheck.data.model.ChecklistRunItem
import com.druanlabs.didicheck.data.model.Completion
import com.druanlabs.didicheck.data.model.CompletionSource
import com.druanlabs.didicheck.data.model.GentleSuggestion
import com.druanlabs.didicheck.data.model.ItemKind
import com.druanlabs.didicheck.data.model.QuickAction
import com.druanlabs.didicheck.data.model.ReminderRepeat
import com.druanlabs.didicheck.data.model.Routine
import com.druanlabs.didicheck.data.model.RoutineItem
import com.druanlabs.didicheck.data.model.RoutineReminderTime
import com.druanlabs.didicheck.util.newId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MemoryRepository(
    database: DidIDatabase,
    private val settings: SettingsStore,
    scope: CoroutineScope,
) {
    private val routineDao: RoutineDao = database.routineDao()
    private val actionDao: ActionDao = database.actionDao()
    private val completionDao: CompletionDao = database.completionDao()
    private val checklistRunDao: ChecklistRunDao = database.checklistRunDao()

    val notificationsEnabled: Flow<Boolean> = settings.notificationsEnabled
    val morningReminderEnabled: Flow<Boolean> = settings.morningReminderEnabled
    val eveningReminderEnabled: Flow<Boolean> = settings.eveningReminderEnabled
    val defaultRoutineId: Flow<String?> = settings.defaultRoutineId
    val onboardingCompleted: Flow<Boolean> = settings.onboardingCompleted

    val routines: Flow<List<Routine>> = combine(
        routineDao.observeRoutines(),
        routineDao.observeItems(),
        routineDao.observeReminderTimes(),
    ) { routines, items, reminders ->
        val grouped = items.groupBy { it.routineId }
        val reminderGrouped = reminders.groupBy { it.routineId }
        routines.map {
            it.toModel(
                items = grouped[it.id].orEmpty(),
                reminders = reminderGrouped[it.id].orEmpty(),
            )
        }
    }

    val quickActions: Flow<List<QuickAction>> = actionDao.observeActions().map { list ->
        list.map { it.toModel() }
    }

    val completions: Flow<List<Completion>> = completionDao.observeAll().map { list ->
        list.map { it.toModel() }
    }

    fun recentCompletions(limit: Int = 5): Flow<List<Completion>> =
        completionDao.observeRecent(limit).map { list -> list.map { it.toModel() } }

    init {
        scope.launch { migrateOnboardingFlagIfNeeded() }
    }

    /**
     * Existing installs already have routines — treat onboarding as done.
     * New installs stay incomplete until [applyOnboardingResult].
     */
    private suspend fun migrateOnboardingFlagIfNeeded() {
        if (settings.onboardingCompleted.first()) return
        if (routineDao.count() > 0) {
            settings.setOnboardingCompleted(true)
        }
    }

    suspend fun needsOnboarding(): Boolean {
        if (settings.onboardingCompleted.first()) return false
        return routineDao.count() == 0
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        settings.setOnboardingCompleted(completed)
    }

    /**
     * Persists personalized routines + quick actions from onboarding, then marks complete.
     * Falls back to a minimal Leaving Home routine if [routines] is empty.
     */
    suspend fun applyOnboardingResult(
        routines: List<Routine>,
        quickActionLabels: List<String>,
    ) {
        val toSave = routines.ifEmpty {
            listOf(minimalLeavingHomeRoutine())
        }.take(3)
        toSave.forEachIndexed { index, routine ->
            saveRoutine(routine.copy(sortOrder = index))
        }
        quickActionLabels
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .forEachIndexed { index, label ->
                saveQuickAction(label = label, pinned = index < 3)
            }
        toSave.firstOrNull()?.let { setDefaultRoutineId(it.id) }
        settings.setOnboardingCompleted(true)
    }

    private fun minimalLeavingHomeRoutine(): Routine {
        val seed = SeedData.routines.first()
        val id = newId()
        return Routine(
            id = id,
            name = seed.name,
            sortOrder = 0,
            lastCompletedAt = null,
            items = seed.items.mapIndexed { index, item ->
                RoutineItem(
                    id = newId(),
                    routineId = id,
                    label = item.label,
                    kind = item.kind,
                    sortOrder = index,
                )
            },
        )
    }

    suspend fun getRoutine(id: String): Routine? {
        val entity = routineDao.getRoutine(id) ?: return null
        return entity.toModel(
            items = routineDao.getItems(id),
            reminders = routineDao.getReminderTimes(id),
        )
    }

    /**
     * Ensures a checklist run exists for [routineId] on [day] (default today).
     * Returns null if the template was deleted.
     */
    suspend fun getOrCreateTodayRun(
        routineId: String,
        day: LocalDate = LocalDate.now(),
    ): ChecklistRun? {
        val routine = getRoutine(routineId) ?: return null
        val epochDay = day.toEpochDay()
        val existing = checklistRunDao.getRun(routineId, epochDay)
        if (existing != null) {
            return existing.toModel(routine.name, checklistRunDao.getItems(existing.id))
        }
        val runId = newId()
        val run = ChecklistRunEntity(
            id = runId,
            routineId = routineId,
            epochDay = epochDay,
            createdAt = System.currentTimeMillis(),
        )
        val items = routine.items.sortedBy { it.sortOrder }.mapIndexed { index, item ->
            ChecklistRunItemEntity(
                id = newId(),
                runId = runId,
                routineItemId = item.id,
                label = item.label,
                sortOrder = index,
                photoRequired = item.photoRequired,
                checkedAt = null,
            )
        }
        checklistRunDao.insertRunWithItems(run, items)
        return run.toModel(routine.name, items)
    }

    suspend fun getTodayRun(routineId: String, day: LocalDate = LocalDate.now()): ChecklistRun? {
        val routine = getRoutine(routineId) ?: return null
        val existing = checklistRunDao.getRun(routineId, day.toEpochDay()) ?: return null
        return existing.toModel(routine.name, checklistRunDao.getItems(existing.id))
    }

    suspend fun getCompletion(id: String): Completion? =
        completionDao.get(id)?.toModel()

    suspend fun suggestedRoutineId(hour: Int = LocalTime.now().hour): String? {
        val all = routineDao.getRoutines()
        if (all.isEmpty()) return null
        val preferredId = settings.defaultRoutineId.first()
        if (!preferredId.isNullOrBlank() && all.any { it.id == preferredId }) {
            return preferredId
        }
        val preferred = when (hour) {
            in 5..10 -> listOf("Going to Work", "Leaving Home")
            in 20..23, in 0..4 -> listOf("Before Sleeping", "Before Bed")
            in 11..16 -> listOf("Going to Work", "Going Shopping")
            else -> listOf("Going Shopping", "Going on a Trip", "Travelling")
        }
        preferred.forEach { name ->
            all.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it.id }
        }
        return all.first().id
    }

    /**
     * Local-first contextual suggestion. Safe to expand later without changing callers.
     */
    suspend fun gentleSuggestion(
        hour: Int = LocalTime.now().hour,
        day: DayOfWeek = LocalDate.now().dayOfWeek,
    ): GentleSuggestion? {
        val routines = routineDao.getRoutines()
        val actions = actionDao.getActions()
        val weekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY

        if (hour in 6..10 && weekday) {
            val work = routines.firstOrNull {
                it.name.contains("work", ignoreCase = true) ||
                    it.name.contains("leaving", ignoreCase = true)
            }
            if (work != null) {
                return GentleSuggestion(
                    title = "Good morning",
                    body = "Ready for your ${work.name} check?",
                    routineId = work.id,
                )
            }
        }

        if (hour in 20..23) {
            val sleep = routines.firstOrNull {
                it.name.contains("sleep", ignoreCase = true) ||
                    it.name.contains("bed", ignoreCase = true)
            }
            if (sleep != null) {
                return GentleSuggestion(
                    title = "Evening",
                    body = "Anything to check before you rest?",
                    routineId = sleep.id,
                )
            }
        }

        val habitual = actions
            .filter { it.useCount >= 2 && it.lastUsedAt > 0 }
            .maxByOrNull { it.useCount }
        if (habitual != null && hour in 7..22) {
            return GentleSuggestion(
                title = "A familiar moment",
                body = "You usually log “${habitual.label}” around this time.",
                actionLabel = habitual.label,
            )
        }
        return null
    }

    suspend fun markDone(
        label: String,
        source: CompletionSource,
        routineName: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        detailItems: List<String> = emptyList(),
        photoPath: String? = null,
    ): Completion {
        val trimmed = label.trim()
        val completion = CompletionEntity(
            id = newId(),
            actionLabel = trimmed,
            timestamp = timestamp,
            source = source.name,
            routineName = routineName,
            detailItems = detailItems.takeIf { it.isNotEmpty() }?.joinToString("\n"),
            photoPath = photoPath,
        )
        completionDao.insert(completion)

        val existing = actionDao.findByLabel(trimmed)
        if (existing != null) {
            actionDao.update(
                existing.copy(
                    lastUsedAt = timestamp,
                    useCount = existing.useCount + 1,
                )
            )
        } else if (source == CompletionSource.QUICK) {
            actionDao.upsert(
                QuickActionEntity(
                    id = newId(),
                    label = trimmed,
                    lastUsedAt = timestamp,
                    useCount = 1,
                    createdAt = timestamp,
                    pinned = false,
                    pinOrder = 0,
                )
            )
        }
        return completion.toModel()
    }

    suspend fun undo(id: String) {
        val existing = completionDao.get(id)
        completionDao.delete(id)
        existing?.photoPath?.let { com.druanlabs.didicheck.util.ProofPhotoStore.deleteQuietly(it) }
    }

    suspend fun deleteCompletion(id: String) {
        val existing = completionDao.get(id)
        completionDao.delete(id)
        existing?.photoPath?.let { com.druanlabs.didicheck.util.ProofPhotoStore.deleteQuietly(it) }
    }

    suspend fun editCompletionTime(id: String, timestamp: Long) {
        completionDao.updateTime(id, timestamp)
    }

    suspend fun setCompletionPhoto(id: String, photoPath: String?) {
        completionDao.updatePhotoPath(id, photoPath)
    }

    /**
     * Saves one expandable routine summary with all checked items,
     * plus searchable CONFIRM marks when not already recorded today.
     * @return the routine summary completion (for attaching photo proof).
     */
    suspend fun completeRoutine(
        routineId: String,
        checkedLabels: List<String>,
        photoPath: String? = null,
    ): Completion? {
        val routine = getRoutine(routineId) ?: return null
        val now = System.currentTimeMillis()
        val labels = checkedLabels.map { it.trim() }.filter { it.isNotEmpty() }
        routineDao.markCompleted(routineId, now)

        val summary = markDone(
            label = "${routine.name} completed",
            source = CompletionSource.ROUTINE,
            routineName = routine.name,
            timestamp = now,
            detailItems = labels,
            photoPath = photoPath,
        )

        val confirmLabels = routine.items
            .filter { it.kind == ItemKind.CONFIRM && it.label in labels }
            .map { it.label }
        confirmLabels.forEach { label ->
            if (!todayHas(label, now)) {
                markDone(label, CompletionSource.ROUTINE, routine.name, now)
            }
        }
        return summary
    }

    suspend fun saveRoutine(routine: Routine) {
        val reminderTimes = if (routine.reminderRepeat == ReminderRepeat.OFF) {
            emptyList()
        } else {
            routine.reminderTimes
                .map { it.copy(hour = it.hour.coerceIn(0, 23), minute = it.minute.coerceIn(0, 59)) }
                .distinctBy { it.hour * 60 + it.minute }
                .sortedWith(compareBy({ it.hour }, { it.minute }))
        }
        val daysToStore = when (routine.reminderRepeat) {
            ReminderRepeat.OFF, ReminderRepeat.DAILY -> null
            ReminderRepeat.WEEKDAYS -> "1,2,3,4,5"
            ReminderRepeat.WEEKLY -> {
                val day = routine.reminderDays.firstOrNull() ?: DayOfWeek.MONDAY
                day.value.toString()
            }
            ReminderRepeat.CUSTOM -> routine.reminderDays
                .sortedBy { it.value }
                .joinToString(",") { it.value.toString() }
                .ifBlank { null }
        }
        val repeat = when {
            reminderTimes.isEmpty() -> ReminderRepeat.OFF
            else -> routine.reminderRepeat
        }
        routineDao.saveRoutine(
            RoutineEntity(
                id = routine.id,
                name = routine.name.trim().ifBlank { "Untitled routine" },
                sortOrder = routine.sortOrder,
                lastCompletedAt = routine.lastCompletedAt,
                reminderRepeat = repeat.name,
                reminderDays = daysToStore,
                askPhotoOnComplete = routine.askPhotoOnComplete,
            ),
            routine.items.mapIndexed { index, item ->
                RoutineItemEntity(
                    id = item.id.ifBlank { newId() },
                    routineId = routine.id,
                    label = item.label.trim(),
                    kind = item.kind.name,
                    sortOrder = index,
                    photoRequired = item.photoRequired,
                )
            }.filter { it.label.isNotBlank() },
            reminderTimes.mapIndexed { index, time ->
                RoutineReminderTimeEntity(
                    id = time.id.ifBlank { newId() },
                    routineId = routine.id,
                    hour = time.hour,
                    minute = time.minute,
                    sortOrder = index,
                )
            },
        )
        syncTodayRunFromTemplate(routine.id)
    }

    /** Keep today's widget run labels/photo flags in sync after template edits. */
    private suspend fun syncTodayRunFromTemplate(routineId: String) {
        val routine = getRoutine(routineId) ?: return
        val existing = checklistRunDao.getRun(routineId, LocalDate.now().toEpochDay()) ?: return
        val byId = routine.items.associateBy { it.id }
        val updated = checklistRunDao.getItems(existing.id).map { row ->
            val template = byId[row.routineItemId] ?: return@map row
            row.copy(
                label = template.label,
                photoRequired = template.photoRequired,
                sortOrder = template.sortOrder,
            )
        }
        checklistRunDao.upsertItems(updated)
    }

    suspend fun deleteRoutine(id: String) {
        routineDao.deleteRoutine(id)
    }

    suspend fun saveQuickAction(label: String, pinned: Boolean = false): QuickAction {
        val trimmed = label.trim()
        val existing = actionDao.findByLabel(trimmed)
        if (existing != null) return existing.toModel()
        val pinOrder = if (pinned) actionDao.maxPinOrder() + 1 else 0
        val entity = QuickActionEntity(
            id = newId(),
            label = trimmed,
            lastUsedAt = 0L,
            useCount = 0,
            createdAt = System.currentTimeMillis(),
            pinned = pinned,
            pinOrder = pinOrder,
        )
        actionDao.upsert(entity)
        return entity.toModel()
    }

    suspend fun renameQuickAction(id: String, label: String) {
        val current = actionDao.getById(id) ?: return
        actionDao.update(current.copy(label = label.trim()))
    }

    suspend fun deleteQuickAction(id: String) {
        actionDao.delete(id)
    }

    suspend fun setPinned(id: String, pinned: Boolean) {
        val current = actionDao.getById(id) ?: return
        val pinOrder = if (pinned) actionDao.maxPinOrder() + 1 else 0
        actionDao.update(current.copy(pinned = pinned, pinOrder = pinOrder))
    }

    suspend fun movePinned(id: String, delta: Int) {
        val pinned = actionDao.getActions().filter { it.pinned }.sortedBy { it.pinOrder }
        val index = pinned.indexOfFirst { it.id == id }
        val target = index + delta
        if (index < 0 || target !in pinned.indices) return
        val reordered = pinned.toMutableList()
        val item = reordered.removeAt(index)
        reordered.add(target, item)
        reordered.forEachIndexed { i, action ->
            actionDao.update(action.copy(pinOrder = i))
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        settings.setNotificationsEnabled(enabled)
    }

    suspend fun setDefaultRoutineId(id: String?) {
        settings.setDefaultRoutineId(id)
    }

    suspend fun setMorningReminderEnabled(enabled: Boolean) {
        settings.setMorningReminderEnabled(enabled)
    }

    suspend fun setEveningReminderEnabled(enabled: Boolean) {
        settings.setEveningReminderEnabled(enabled)
    }

    suspend fun isMorningReminderEnabled(): Boolean = settings.morningReminderEnabled.first()

    suspend fun isEveningReminderEnabled(): Boolean = settings.eveningReminderEnabled.first()

    suspend fun searchCompletions(query: String): List<Completion> {
        if (query.isBlank()) return emptyList()
        return completionDao.search(query.trim()).map { it.toModel() }
    }

    suspend fun todayHas(label: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return completionDao.search(label)
            .any {
                it.actionLabel.equals(label, ignoreCase = true) &&
                    it.timestamp in start..nowMillis
            }
    }
}

private fun ChecklistRunEntity.toModel(
    routineName: String,
    items: List<ChecklistRunItemEntity>,
) = ChecklistRun(
    id = id,
    routineId = routineId,
    routineName = routineName,
    epochDay = epochDay,
    items = items.sortedBy { it.sortOrder }.map {
        ChecklistRunItem(
            id = it.id,
            runId = it.runId,
            routineItemId = it.routineItemId,
            label = it.label,
            sortOrder = it.sortOrder,
            photoRequired = it.photoRequired,
            checkedAt = it.checkedAt,
        )
    },
)

private fun RoutineEntity.toModel(
    items: List<RoutineItemEntity>,
    reminders: List<RoutineReminderTimeEntity> = emptyList(),
): Routine {
    val times = reminders
        .sortedWith(compareBy({ it.sortOrder }, { it.hour }, { it.minute }))
        .map {
            RoutineReminderTime(
                id = it.id,
                hour = it.hour,
                minute = it.minute,
            )
        }
    val days = reminderDays
        ?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.mapNotNull { value -> DayOfWeek.entries.firstOrNull { it.value == value } }
        ?.toSet()
        .orEmpty()
    var repeat = runCatching { ReminderRepeat.valueOf(reminderRepeat) }
        .getOrDefault(ReminderRepeat.OFF)
    if (repeat == ReminderRepeat.OFF && times.isNotEmpty()) {
        repeat = ReminderRepeat.DAILY
    }
    return Routine(
        id = id,
        name = name,
        sortOrder = sortOrder,
        lastCompletedAt = lastCompletedAt,
        items = items.sortedBy { it.sortOrder }.map { it.toModel() },
        reminderTimes = times,
        reminderRepeat = if (times.isEmpty()) ReminderRepeat.OFF else repeat,
        reminderDays = days,
        askPhotoOnComplete = askPhotoOnComplete,
    )
}

private fun RoutineItemEntity.toModel() = RoutineItem(
    id = id,
    routineId = routineId,
    label = label,
    kind = runCatching { ItemKind.valueOf(kind) }.getOrDefault(ItemKind.TAKE),
    sortOrder = sortOrder,
    photoRequired = photoRequired,
)

private fun QuickActionEntity.toModel() = QuickAction(
    id = id,
    label = label,
    lastUsedAt = lastUsedAt,
    useCount = useCount,
    pinned = pinned,
    pinOrder = pinOrder,
)

private fun CompletionEntity.toModel() = Completion(
    id = id,
    actionLabel = actionLabel,
    timestamp = timestamp,
    source = runCatching { CompletionSource.valueOf(source) }.getOrDefault(CompletionSource.QUICK),
    routineName = routineName,
    detailItems = detailItems
        ?.lines()
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty(),
    photoPath = photoPath,
)

private data class SeedRoutine(val name: String, val items: List<SeedItem>)
private data class SeedItem(val label: String, val kind: ItemKind)

private object SeedData {
    val routines = listOf(
        SeedRoutine(
            "Leaving Home",
            listOf(
                SeedItem("Phone", ItemKind.TAKE),
                SeedItem("Wallet", ItemKind.TAKE),
                SeedItem("Keys", ItemKind.TAKE),
                SeedItem("Charger", ItemKind.TAKE),
                SeedItem("Locked the door", ItemKind.CONFIRM),
                SeedItem("Turned off lights", ItemKind.CONFIRM),
            ),
        ),
        SeedRoutine(
            "Going to Work",
            listOf(
                SeedItem("Phone", ItemKind.TAKE),
                SeedItem("Wallet", ItemKind.TAKE),
                SeedItem("Keys", ItemKind.TAKE),
                SeedItem("Laptop", ItemKind.TAKE),
                SeedItem("Lunch", ItemKind.TAKE),
            ),
        ),
        SeedRoutine(
            "Before Bed",
            listOf(
                SeedItem("Lock the door", ItemKind.CONFIRM),
                SeedItem("Set alarm", ItemKind.CONFIRM),
                SeedItem("Charge phone", ItemKind.TAKE),
                SeedItem("Turned off lights", ItemKind.CONFIRM),
            ),
        ),
        SeedRoutine(
            "Going on a Trip",
            listOf(
                SeedItem("Passport", ItemKind.TAKE),
                SeedItem("Wallet", ItemKind.TAKE),
                SeedItem("Tickets", ItemKind.TAKE),
                SeedItem("Charger", ItemKind.TAKE),
            ),
        ),
        SeedRoutine(
            "Going Shopping",
            listOf(
                SeedItem("Wallet", ItemKind.TAKE),
                SeedItem("Bags", ItemKind.TAKE),
                SeedItem("Shopping list", ItemKind.TAKE),
            ),
        ),
    )

    val actions = listOf(
        "Locked the door",
        "Took medicine",
        "Fed the cat",
        "Turned off the stove",
        "Watered plants",
        "Took vitamins",
    )
}
