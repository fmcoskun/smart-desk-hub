package com.iot.widget.ui.components

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.shell.BottomTab
import com.iot.widget.ui.shell.NeuBottomBar
import com.iot.widget.ui.theme.SmartDeskTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What TalkBack hears: semantics of the shared components. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccessibilityTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun bottomBarIsSelectableGroup() {
        rule.setContent { SmartDeskTheme { NeuBottomBar(listOf(BottomTab("Dashboard", Bi.Grid1x2Fill, true) {}, BottomTab("More", Bi.ThreeDots, false) {})) } }
        // "tab 1 of 2" needs the tabs to be one group
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)).assertExists()
    }

    @Test
    fun decorativeIconHasNoSemantics() {
        rule.setContent { SmartDeskTheme { BiIcon(Bi.Cpu) } }
        // the private-use glyph would otherwise be read out as an unknown character
        val glyph = Bi.Cpu.codepoint.toString()
        assertTrue(rule.onAllNodes(hasText(glyph), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun textFieldReadsTypedText() {
        rule.setContent { SmartDeskTheme { TextFieldNeu(value = "hello", onValueChange = {}, label = "Search logs") } }
        // with text in it, TalkBack reads the text, not the fixed label
        rule.onNodeWithContentDescription("Search logs").assertDoesNotExist()
        rule.onNode(hasText("hello")).assertExists()
    }

    @Test
    fun emptyTextFieldIsDescribedByItsLabel() {
        rule.setContent { SmartDeskTheme { TextFieldNeu(value = "", onValueChange = {}, label = "Search logs") } }
        rule.onNodeWithContentDescription("Search logs").assertExists()
    }

    @Test
    fun activeChipIsSelected() {
        rule.setContent { SmartDeskTheme { NeuChipButton("Today", null, active = true, onClick = {}, selected = true) } }
        rule.onNodeWithText("Today").assertIsSelected()
    }

    @Test
    fun primaryButtonIsNotSelected() {
        // the green "active" look is also used for Save / Continue: TalkBack must not say "selected"
        rule.setContent { SmartDeskTheme { NeuChipButton("Save", null, active = true, onClick = {}) } }
        rule.onNode(hasText("Save") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).assertDoesNotExist()
    }

    @Test
    fun pairingStatusIsLiveRegion() {
        rule.setContent { SmartDeskTheme { LiveStatus("Searching nearby…") } }
        rule.onNode(hasText("Searching nearby…") and SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)).assertExists()
    }


    @Test
    fun typedFieldKeepsItsLabel() {
        rule.setContent { SmartDeskTheme { TextFieldNeu(value = "hello", onValueChange = {}, label = "Search logs") } }
        // TalkBack reads the typed text and still says what the field is for
        rule.onNode(hasText("hello") and SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Search logs")).assertExists()
    }
}
