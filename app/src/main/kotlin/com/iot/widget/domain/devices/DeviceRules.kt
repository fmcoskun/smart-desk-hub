package com.iot.widget.domain.devices

import com.iot.widget.domain.Names
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.RuleResult
import com.iot.widget.domain.users.RuleViolation
import com.iot.widget.domain.users.User
import com.iot.widget.domain.users.can

/**
 * Domain layer · who may change devices (spec §5 roles table) and what a valid device name is.
 * Rename / change room / restart: EDIT_DEVICES (Admin, Operator).
 * Add / delete: ADD_REMOVE_DEVICES (Admin), and built-in devices are never deleted.
 */
object DeviceRules {
    const val MAX_NAME = 30

    fun canEdit(user: User): Boolean = user.can(Permission.EDIT_DEVICES)

    fun canAddRemove(user: User): Boolean = user.can(Permission.ADD_REMOVE_DEVICES)

    fun canDelete(user: User, device: Device): Boolean = canAddRemove(user) && !device.isBuiltIn

    fun validateName(name: String, otherNames: Collection<String>): RuleResult {
        val trimmed = Names.clean(name)
        return when {
            trimmed.isEmpty() -> RuleResult.Rejected(RuleViolation.NAME_EMPTY)
            Names.length(trimmed) > MAX_NAME -> RuleResult.Rejected(RuleViolation.NAME_TOO_LONG)
            otherNames.any { Names.sameName(it, trimmed) } -> RuleResult.Rejected(RuleViolation.NAME_TAKEN)
            else -> RuleResult.Ok
        }
    }
}
