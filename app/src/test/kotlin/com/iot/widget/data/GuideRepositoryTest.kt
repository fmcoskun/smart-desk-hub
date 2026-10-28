package com.iot.widget.data

import androidx.test.core.app.ApplicationProvider
import com.iot.widget.ui.icons.Bi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The real guide.json shipped in the app: a typo there fails here, not on a phone. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GuideRepositoryTest {

    private val topics = runBlocking { RawGuideRepository(ApplicationProvider.getApplicationContext()).topics() }

    @Test
    fun bundledGuideParsesWithEightPlusTopics() {
        assertEquals(
            listOf("scenes", "dashboard", "automations", "cameras", "recordings", "devices", "logs", "roles", "accounts", "about-demo"),
            topics.map { it.id },
        )
        topics.forEach { t ->
            assertTrue(t.id, t.paragraphs.isNotEmpty() && t.steps.isNotEmpty() && t.title.isNotBlank() && t.summary.isNotBlank())
            Bi.valueOf(t.icon) // throws if the icon name is wrong
        }
    }

    @Test
    fun automationsTopicStatesScreenLockLimit() {
        val text = topics.single { it.id == "automations" }.paragraphs.joinToString(" ")
        assertTrue(text.contains("screen lock", ignoreCase = true))
        assertTrue(text.contains("while the app is running"))
        // honest about the closed-app case: that lock is missed (spec §9c, ScreenLockReceiver)
        assertTrue(text.contains("missed"))
    }

    @Test
    fun guideUsesTheNamesShownInTheApp() {
        val all = topics.flatMap { listOf(it.title, it.summary) + it.paragraphs + it.steps }.joinToString(" ")
        assertTrue("the lamp card is called Desk Halo LED", !all.contains("desk lamp", ignoreCase = true))
        assertTrue(all.contains("Desk Halo LED"))
    }
}
