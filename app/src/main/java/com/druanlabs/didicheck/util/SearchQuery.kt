package com.druanlabs.didicheck.util

object SearchQuery {
    private val stopWords = setOf(
        "a", "an", "the", "did", "i", "do", "it", "my", "me", "to", "of", "and",
        "or", "for", "on", "in", "at", "is", "was", "were", "have", "has", "had",
        "already", "today", "yesterday", "please", "just",
    )

    /** Strip question marks / "did i" and drop common filler words. */
    fun keywords(raw: String): List<String> {
        val cleaned = raw.trim()
            .trimEnd('?')
            .replace(Regex("^(did i\\s+)", RegexOption.IGNORE_CASE), "")
            .trim()
        if (cleaned.isBlank()) return emptyList()
        val tokens = cleaned
            .lowercase()
            .split(Regex("[\\s,./\\-_]+"))
            .map { it.trim() }
            .filter { it.length > 1 && it !in stopWords }
        return tokens.ifEmpty {
            cleaned.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        }
    }

    fun primaryPhrase(raw: String): String {
        val keys = keywords(raw)
        return if (keys.isEmpty()) raw.trim().trimEnd('?') else keys.joinToString(" ")
    }
}
