package com.iot.widget.ui.guide

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.iot.widget.domain.guide.GuideTopic
import com.iot.widget.testutil.FakeOnboardingRepository
import com.iot.widget.ui.theme.SmartDeskTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GuideScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val rows = listOf(
        GuideTopic("scenes", "Scenes", "BriefcaseFill", "One tap sets the desk", listOf("p"), listOf("s"), emptyList()),
        GuideTopic("logs", "Logs", "JournalText", "Search and export", listOf("p"), listOf("s"), emptyList()),
    )

    @Test
    fun tapTopicOpensIt() {
        val opened = mutableListOf<String>()
        rule.setContent { SmartDeskTheme { GuideContent(GuideUiState(topics = rows, loaded = true), GuideActions.NONE.copy(onOpen = { opened += it })) } }
        rule.onNodeWithText("Search and export").performClick()
        rule.waitForIdle()
        assertEquals(listOf("logs"), opened)
    }

    @Test
    fun emptySearchShowsNoTopicsMatch() {
        rule.setContent { SmartDeskTheme { GuideContent(GuideUiState(query = "zzz", topics = emptyList(), loaded = true), GuideActions.NONE) } }
        rule.onNodeWithText("No topics match").assertExists()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun showWelcomeTourResetsOnboarding() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val onboarding = FakeOnboardingRepository(seen = true)
            val vm = GuideViewModel(FakeGuideRepository(rows), onboarding)
            vm.showTour()
            assertFalse(onboarding.seen.first())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun guideQueryUpdatesImmediately() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            // topics still loading (never finish here): typing must not be lost meanwhile
            val slow = object : com.iot.widget.data.GuideRepository {
                override suspend fun topics(): List<GuideTopic> = kotlinx.coroutines.awaitCancellation()
            }
            val vm = GuideViewModel(slow, FakeOnboardingRepository())
            vm.setQuery("cs")
            vm.setQuery("csv")
            assertEquals("csv", vm.query.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun unknownTopicShowsNotFound() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val vm = GuideTopicViewModel(androidx.lifecycle.SavedStateHandle(mapOf("id" to "nope")), FakeGuideRepository(rows))
            backgroundScope.launch { vm.state.collect {} }
            testScheduler.runCurrent()
            assertEquals(GuideTopicState.NotFound, vm.state.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class FakeGuideRepository(private val all: List<GuideTopic>) : com.iot.widget.data.GuideRepository {
    override suspend fun topics() = all

}
