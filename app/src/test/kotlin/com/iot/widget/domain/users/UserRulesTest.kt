package com.iot.widget.domain.users

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class UserRulesTest {

    private val ayse = User("ayse", "Ayşe Yılmaz", Role.ADMIN, active = true, lastLoginAtMillis = null)
    private val mehmet = User("mehmet", "Mehmet Kaya", Role.OPERATOR, active = true, lastLoginAtMillis = null)
    private val zeynep = User("zeynep", "Zeynep Demir", Role.OPERATOR, active = false, lastLoginAtMillis = null)
    private val all = listOf(ayse, mehmet, zeynep)
    private val originalLocale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(originalLocale)

    private fun rejected(v: RuleViolation) = RuleResult.Rejected(v)

    @Test
    fun adminEditsOperatorOk() =
        assertEquals(RuleResult.Ok, UserRules.checkSave(ayse, mehmet, mehmet.copy(active = false), all))

    @Test
    fun selfDeleteRejected() = assertEquals(rejected(RuleViolation.SELF_DELETE), UserRules.checkDelete(ayse, ayse, all))

    @Test
    fun selfDeactivateRejected() =
        assertEquals(rejected(RuleViolation.SELF_DEACTIVATE), UserRules.checkSave(ayse, ayse, ayse.copy(active = false), all))

    @Test
    fun selfDemoteRejected() =
        assertEquals(rejected(RuleViolation.SELF_DEMOTE), UserRules.checkSave(ayse, ayse, ayse.copy(role = Role.OPERATOR), all))

    @Test
    fun operatorActorNotAllowed() {
        assertEquals(rejected(RuleViolation.NOT_ALLOWED), UserRules.checkSave(mehmet, zeynep, zeynep.copy(active = true), all))
        assertEquals(rejected(RuleViolation.NOT_ALLOWED), UserRules.checkDelete(mehmet, zeynep, all))
    }

    @Test
    fun lastAdminGuard() {
        assertEquals(0, UserRules.activeAdminsAfter(listOf(ayse), changed = ayse.copy(active = false)))
        assertEquals(0, UserRules.activeAdminsAfter(listOf(ayse, mehmet), changed = null, deletedId = "ayse"))
        assertEquals(1, UserRules.activeAdminsAfter(all, changed = mehmet.copy(role = Role.GUEST)))
    }

    @Test
    fun nameValidationTrimsAndRejectsBlank() =
        assertEquals(rejected(RuleViolation.NAME_EMPTY), UserRules.validateName("   ", emptyList()))

    @Test
    fun nameValidationRejects41Chars() {
        assertEquals(RuleResult.Ok, UserRules.validateName("a".repeat(40), emptyList()))
        assertEquals(rejected(RuleViolation.NAME_TOO_LONG), UserRules.validateName("a".repeat(41), emptyList()))
    }

    @Test
    fun nameValidationRejectsDuplicateIgnoringCase() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        assertEquals(rejected(RuleViolation.NAME_TAKEN), UserRules.validateName("  AYŞE YILMAZ ", listOf("Ayşe Yılmaz")))
    }

    @Test
    fun newUserNameCheckedAgainstEveryone() =
        assertEquals(rejected(RuleViolation.NAME_TAKEN), UserRules.checkSave(ayse, null, mehmet.copy(id = "new"), all))

    @Test
    fun initialsOfTurkishName() {
        assertEquals("AY", initials("ayşe yılmaz"))
        assertEquals("G", initials("Guest"))
        assertEquals("ZD", initials("  Zeynep   Demir "))
    }


    @Test
    fun namesNormalizedAndCountedByCodePoint() {
        // "Ayşe" typed with a combining cedilla (s + U+0327) is the same name as the stored "Ayşe"
        val decomposed = "Ay" + "s" + Char(0x0327) + "e Yılmaz"
        assertEquals(RuleResult.Rejected(RuleViolation.NAME_TAKEN), UserRules.validateName(decomposed, listOf("Ayşe Yılmaz")))
        // 40 emoji = 80 UTF-16 units but 40 characters: allowed
        val emoji = String(Character.toChars(0x1F600)).repeat(UserRules.MAX_NAME)
        assertEquals(RuleResult.Ok, UserRules.validateName(emoji, emptyList()))
    }
}
