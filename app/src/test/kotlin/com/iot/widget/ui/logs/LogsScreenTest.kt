package com.iot.widget.ui.logs

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.iot.widget.domain.logs.LogLevel
import com.iot.widget.domain.logs.LogQuery
import com.iot.widget.domain.logs.LogSource
import com.iot.widget.ui.theme.SmartDeskTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LogsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(state: LogsUiState, opened: MutableList<Long>) = rule.setContent {
        SmartDeskTheme { LogsContent(state, LogsActions.NONE.copy(onOpen = { opened += it })) }
    }

    @Test
    fun tapRowOpensDetail() {
        val opened = mutableListOf<Long>()
        val row = LogRowUi(7, "14:31", LogSource.DEVICE, LogLevel.ERROR, "Meeting Room Sensor", "connection timeout")
        show(LogsUiState(rows = listOf(row), total = 1, query = LogQuery(), allowed = true, loaded = true), opened)
        rule.onNodeWithText("connection timeout", substring = true).performClick()
        rule.waitForIdle()
        assertEquals(listOf(7L), opened)
    }

    @Test
    fun emptyStateShown() {
        show(LogsUiState(rows = emptyList(), total = 0, query = LogQuery(), allowed = true, loaded = true), mutableListOf())
        rule.onNodeWithText("No entries match these filters").assertExists()
    }

    @Test
    fun cappedFooterShown() {
        val rows = (1..3).map { LogRowUi(it.toLong(), "14:3$it", LogSource.CLIENT, LogLevel.INFO, "Ayşe Yılmaz", "m$it") }
        show(LogsUiState(rows = rows, total = 1234, query = LogQuery(), allowed = true, loaded = true), mutableListOf())
        rule.onNodeWithText("Showing 3 of 1,234", substring = true).assertExists()
    }


    @Test
    fun listIsLazy() {
        val rows = (1L..500L).map { LogRowUi(it, "14:31", LogSource.DEVICE, LogLevel.INFO, "Dev", "line $it") }
        show(LogsUiState(rows = rows, total = 500, query = LogQuery(), allowed = true, loaded = true), mutableListOf())
        // only what fits on screen is composed, not all 500 rows
        val composed = rule.onAllNodesWithText("line ", substring = true).fetchSemanticsNodes().size
        assertTrue("composed $composed rows", composed < 50)
    }

    @Test
    fun customChipShowsDates() {
        val range = com.iot.widget.domain.logs.DateRange.Custom(java.time.LocalDate.of(2026, 10, 20), java.time.LocalDate.of(2026, 10, 27))
        show(LogsUiState(query = LogQuery(range = range), allowed = true, loaded = true), mutableListOf())
        rule.onNodeWithText("20 Oct – 27 Oct").assertExists()
    }

    @Test
    fun exportDisabledWithoutRows() {
        show(LogsUiState(rows = emptyList(), total = 0, query = LogQuery(), allowed = true, loaded = true), mutableListOf())
        rule.onNode(hasText("Export CSV") and hasClickAction()).assertIsNotEnabled()
    }
}
