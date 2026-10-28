package com.iot.widget.domain.guide

import com.iot.widget.domain.Names
import kotlinx.serialization.Serializable

/**
 * Domain layer · one User Guide topic (spec §6.4), read from `res/raw/guide.json`.
 * [icon] is the name of a `Bi` icon; [keywords] are extra search words that need not appear
 * in the text (e.g. "csv" for the Logs topic).
 */
@Serializable
data class GuideTopic(
    val id: String,
    val title: String,
    val icon: String,
    val summary: String,
    val paragraphs: List<String>,
    val steps: List<String>,
    val keywords: List<String> = emptyList(),
)

/**
 * Topics whose title, summary, text or keywords contain [query]; case-insensitive and
 * Turkish-aware (the same folding as user names and log search). A blank query keeps all.
 */
fun List<GuideTopic>.search(query: String): List<GuideTopic> {
    if (query.isBlank()) return this
    val needle = Names.comparable(query)
    return filter { t ->
        (listOf(t.title, t.summary) + t.paragraphs + t.steps + t.keywords).any { Names.comparable(it).contains(needle) }
    }
}
