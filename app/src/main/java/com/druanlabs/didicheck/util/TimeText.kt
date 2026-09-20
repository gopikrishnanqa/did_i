package com.druanlabs.didicheck.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeText {
    private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val weekdayFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    private val dayFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

    fun greeting(now: LocalDateTime = LocalDateTime.now()): String {
        return when (now.hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    fun dateHeadline(now: LocalDateTime = LocalDateTime.now()): String {
        return now.format(weekdayFormat).uppercase(Locale.getDefault())
    }

    fun toLocalDateTime(millis: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())

    fun relativeStamp(millis: Long, now: LocalDate = LocalDate.now()): String {
        val time = toLocalDateTime(millis)
        val date = time.toLocalDate()
        val clock = time.format(timeFormat)
        return when (date) {
            now -> "Today · $clock"
            now.minusDays(1) -> "Yesterday · $clock"
            else -> "${date.format(dayFormat)} · $clock"
        }
    }

    fun clock(millis: Long): String = toLocalDateTime(millis).format(timeFormat)

    fun sectionLabel(date: LocalDate, now: LocalDate = LocalDate.now()): String {
        return when (date) {
            now -> "Today"
            now.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
        }
    }

    fun atTime(millis: Long): String {
        val time = toLocalDateTime(millis)
        val date = time.toLocalDate()
        val today = LocalDate.now()
        val clock = time.format(timeFormat)
        return when (date) {
            today -> "Today at $clock"
            today.minusDays(1) -> "Yesterday at $clock"
            else -> "${date.format(dayFormat)} at $clock"
        }
    }
}
