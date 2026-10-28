package com.iot.widget.domain.devices

import com.iot.widget.domain.users.Role
import com.iot.widget.domain.users.RuleResult
import com.iot.widget.domain.users.RuleViolation
import com.iot.widget.domain.users.User
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DeviceRulesTest {

    private val admin = User("ayse", "Ayşe Yılmaz", Role.ADMIN, active = true, lastLoginAtMillis = null)
    private val operator = User("mehmet", "Mehmet Kaya", Role.OPERATOR, active = true, lastLoginAtMillis = null)
    private val guest = User("guest", "Guest", Role.GUEST, active = true, lastLoginAtMillis = null)
    private val info = DeviceInfo("1.0.0", "192.168.1.50", "24:6F:28:A1:00:50", batteryPct = null, signalDbm = -50, lastSeenMillis = 0)
    private val originalLocale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(originalLocale)

    private fun device(id: String) = Device(id, "Device $id", DeviceType.LIGHT, Room.DESK, online = true, isOn = true, info = info)

    @Test
    fun builtInDevicesCannotBeDeleted() {
        BuiltIn.ALL.forEach { assertFalse(it, DeviceRules.canDelete(admin, device(it))) }
        assertTrue(DeviceRules.canDelete(admin, device("dev_123")))
        assertEquals(10, BuiltIn.ALL.size)
    }

    @Test
    fun operatorCanEditNotDelete() {
        assertTrue(DeviceRules.canEdit(operator))
        assertFalse(DeviceRules.canAddRemove(operator))
        assertFalse(DeviceRules.canDelete(operator, device("dev_123")))
    }

    @Test
    fun guestCannotEdit() {
        assertFalse(DeviceRules.canEdit(guest))
        assertFalse(DeviceRules.canAddRemove(guest))
    }

    @Test
    fun deviceNameValidationRejectsBlankLongAndDuplicate() {
        assertEquals(RuleResult.Rejected(RuleViolation.NAME_EMPTY), DeviceRules.validateName("  ", emptyList()))
        assertEquals(RuleResult.Ok, DeviceRules.validateName("a".repeat(30), emptyList()))
        assertEquals(RuleResult.Rejected(RuleViolation.NAME_TOO_LONG), DeviceRules.validateName("a".repeat(31), emptyList()))
        assertEquals(RuleResult.Rejected(RuleViolation.NAME_TAKEN), DeviceRules.validateName("ROOM SENSOR", listOf("Room Sensor")))
        assertEquals(RuleResult.Rejected(RuleViolation.NAME_TAKEN), DeviceRules.validateName("Desk Lamp", listOf("Desk lamp")))
    }

    @Test
    fun turkishFoldingSharedWithUsers() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        assertEquals(RuleResult.Rejected(RuleViolation.NAME_TAKEN), DeviceRules.validateName("IŞIK", listOf("ışık")))
    }
}
