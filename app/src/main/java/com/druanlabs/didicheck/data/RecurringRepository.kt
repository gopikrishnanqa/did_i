package com.druanlabs.didicheck.data

import com.druanlabs.didicheck.data.local.RecurringItemDao
import com.druanlabs.didicheck.data.local.RecurringItemEntity
import com.druanlabs.didicheck.data.model.CustomUnit
import com.druanlabs.didicheck.data.model.RecurrenceFrequency
import com.druanlabs.didicheck.data.model.RecurringCategory
import com.druanlabs.didicheck.data.model.RecurringDraft
import com.druanlabs.didicheck.data.model.RecurringItem
import com.druanlabs.didicheck.data.recurrence.RecurrenceEngine
import com.druanlabs.didicheck.util.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate

class RecurringRepository(
    private val dao: RecurringItemDao,
) {
    val items: Flow<List<RecurringItem>> = dao.observeActive().map { list ->
        list.map { it.toModel() }
    }

    suspend fun get(id: String): RecurringItem? = dao.getById(id)?.toModel()

    suspend fun save(draft: RecurringDraft): RecurringItem {
        val id = draft.id.ifBlank { newId() }
        val existing = dao.getById(id)
        val start = draft.startDate
        val desiredDay = when (draft.frequency) {
            RecurrenceFrequency.MONTHLY,
            RecurrenceFrequency.QUARTERLY,
            RecurrenceFrequency.YEARLY,
            -> draft.dayOfMonth
            RecurrenceFrequency.CUSTOM -> if (draft.customUnit == CustomUnit.MONTHS) draft.dayOfMonth else null
            else -> null
        }
        val weekDays = if (draft.frequency == RecurrenceFrequency.WEEKLY) {
            draft.weekDays.ifEmpty { setOf(LocalDate.now().dayOfWeek) }
        } else {
            emptySet()
        }
        val nextDue = draft.nextDueDate.coerceAtLeast(start)
        val entity = RecurringItemEntity(
            id = id,
            title = draft.title.trim().ifBlank { "Untitled" },
            category = draft.category.name,
            frequency = draft.frequency.name,
            intervalCount = draft.intervalCount.coerceAtLeast(1),
            customUnit = draft.customUnit.name,
            weekDays = weekDays.joinToString(",") { it.value.toString() }.ifBlank { null },
            dayOfMonth = desiredDay,
            startDateEpochDay = start.toEpochDay(),
            nextDueEpochDay = nextDue.toEpochDay(),
            reminderHour = draft.reminderHour,
            reminderMinute = draft.reminderMinute,
            remindDaysBefore = draft.remindDaysBefore
                .distinct()
                .sortedDescending()
                .joinToString(",")
                .ifBlank { null },
            notes = draft.notes.trim().ifBlank { null },
            lastCompletedEpochDay = existing?.lastCompletedEpochDay,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            archived = false,
        )
        dao.upsert(entity)
        return entity.toModel()
    }

    suspend fun complete(id: String, on: LocalDate = LocalDate.now()): RecurringItem? {
        val current = dao.getById(id)?.toModel() ?: return null
        val next = RecurrenceEngine.nextAfterCompletion(current, on)
        val updated = current.copy(
            lastCompletedDate = on,
            nextDueDate = next,
        )
        dao.upsert(updated.toEntity())
        return updated
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }

    suspend fun archive(id: String) {
        dao.archive(id)
    }

    suspend fun dueForReminder(today: LocalDate = LocalDate.now()): List<Pair<RecurringItem, Int>> {
        // Returns items paired with which "days before" reminder fires today.
        return dao.getActive().map { it.toModel() }.flatMap { item ->
            item.remindDaysBefore.mapNotNull { daysBefore ->
                val remindOn = item.nextDueDate.minusDays(daysBefore.toLong())
                if (remindOn == today) item to daysBefore else null
            }
        }
    }
}

fun RecurringItemEntity.toModel(): RecurringItem = RecurringItem(
    id = id,
    title = title,
    category = runCatching { RecurringCategory.valueOf(category) }
        .getOrDefault(RecurringCategory.ROUTINE),
    frequency = runCatching { RecurrenceFrequency.valueOf(frequency) }
        .getOrDefault(RecurrenceFrequency.DAILY),
    intervalCount = intervalCount.coerceAtLeast(1),
    customUnit = runCatching { CustomUnit.valueOf(customUnit) }.getOrDefault(CustomUnit.DAYS),
    weekDays = weekDays
        ?.split(",")
        ?.mapNotNull { token ->
            token.trim().toIntOrNull()?.let { v ->
                DayOfWeek.values().firstOrNull { it.value == v }
            }
        }
        ?.toSet()
        .orEmpty(),
    dayOfMonth = dayOfMonth,
    startDate = LocalDate.ofEpochDay(startDateEpochDay),
    nextDueDate = LocalDate.ofEpochDay(nextDueEpochDay),
    reminderHour = reminderHour,
    reminderMinute = reminderMinute,
    remindDaysBefore = remindDaysBefore
        ?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.distinct()
        ?.sortedDescending()
        .orEmpty(),
    notes = notes,
    lastCompletedDate = lastCompletedEpochDay?.let { LocalDate.ofEpochDay(it) },
    createdAt = createdAt,
    archived = archived,
)

fun RecurringItem.toEntity(): RecurringItemEntity = RecurringItemEntity(
    id = id,
    title = title,
    category = category.name,
    frequency = frequency.name,
    intervalCount = intervalCount,
    customUnit = customUnit.name,
    weekDays = weekDays.joinToString(",") { it.value.toString() }.ifBlank { null },
    dayOfMonth = dayOfMonth,
    startDateEpochDay = startDate.toEpochDay(),
    nextDueEpochDay = nextDueDate.toEpochDay(),
    reminderHour = reminderHour,
    reminderMinute = reminderMinute,
    remindDaysBefore = remindDaysBefore.joinToString(",").ifBlank { null },
    notes = notes,
    lastCompletedEpochDay = lastCompletedDate?.toEpochDay(),
    createdAt = createdAt,
    archived = archived,
)

fun RecurringItem.toDraft(): RecurringDraft = RecurringDraft(
    id = id,
    title = title,
    category = category,
    frequency = frequency,
    intervalCount = intervalCount,
    customUnit = customUnit,
    weekDays = weekDays,
    dayOfMonth = dayOfMonth ?: LocalDate.now().dayOfMonth.coerceIn(1, 28),
    startDate = startDate,
    nextDueDate = nextDueDate,
    reminderHour = reminderHour ?: 9,
    reminderMinute = reminderMinute ?: 0,
    remindDaysBefore = remindDaysBefore.ifEmpty { listOf(0) },
    notes = notes.orEmpty(),
)
