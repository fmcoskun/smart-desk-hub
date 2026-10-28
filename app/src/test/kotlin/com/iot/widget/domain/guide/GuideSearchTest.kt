package com.iot.widget.domain.guide

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideSearchTest {

    private fun topic(id: String, title: String, body: String = "", keywords: List<String> = emptyList()) =
        GuideTopic(id, title, "Grid1x2Fill", "summary of $id", listOf(body), listOf("step"), keywords)

    private val topics = listOf(
        topic("scenes", "Scenes", "Work, Meet and Leave"),
        topic("logs", "Logs", "Filter and export", keywords = listOf("csv", "spreadsheet")),
        topic("turkish", "IŞIK SENSÖRÜ", "İKİ ışık"),
    )

    @Test
    fun searchFoldsAndUsesKeywords() {
        assertEquals(topics, topics.search("   "))
        assertEquals(listOf("logs"), topics.search("CSV").map { it.id }) // keyword only
        assertEquals(listOf("turkish"), topics.search("ışık").map { it.id })
        assertEquals(listOf("turkish"), topics.search("iki").map { it.id })
        assertEquals(listOf("scenes"), topics.search("  meet ").map { it.id })
        assertEquals(emptyList<GuideTopic>(), topics.search("zzz"))
    }
}
