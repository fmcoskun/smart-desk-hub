package com.iot.widget.domain.users

import com.iot.widget.domain.Names

/** Why a user change was refused; each value has a message in strings.xml. */
enum class RuleViolation {
    NAME_EMPTY,
    NAME_TOO_LONG,
    NAME_TAKEN,
    SELF_DELETE,
    SELF_DEACTIVATE,
    SELF_DEMOTE,
    LAST_ADMIN,
    NOT_ALLOWED,

    /** The user being edited was deleted meanwhile (e.g. on another screen). */
    USER_GONE,
}

/** Result of checking a change: either fine, or refused with a reason. */
sealed interface RuleResult {
    data object Ok : RuleResult
    data class Rejected(val reason: RuleViolation) : RuleResult
}

/**
 * Domain layer · the user manager's rules (spec §6.5), as pure functions:
 *  - only users who may MANAGE_USERS change users,
 *  - nobody deletes, deactivates or lowers the role of themselves,
 *  - at least one active Admin always remains,
 *  - names are trimmed, 1–40 characters and unique (ignoring case).
 * The ViewModel calls these before saving; nothing is written when a rule says no.
 */
object UserRules {
    const val MAX_NAME = 40

    fun validateName(name: String, otherNames: Collection<String>): RuleResult {
        val trimmed = Names.clean(name)
        return when {
            trimmed.isEmpty() -> RuleResult.Rejected(RuleViolation.NAME_EMPTY)
            Names.length(trimmed) > MAX_NAME -> RuleResult.Rejected(RuleViolation.NAME_TOO_LONG)
            otherNames.any { Names.sameName(it, trimmed) } -> RuleResult.Rejected(RuleViolation.NAME_TAKEN)
            else -> RuleResult.Ok
        }
    }

    /** [before] is null for a new user. Self rules come first, so their message is the specific one. */
    fun checkSave(actor: User, before: User?, after: User, all: List<User>): RuleResult {
        if (!actor.can(Permission.MANAGE_USERS)) return RuleResult.Rejected(RuleViolation.NOT_ALLOWED)
        if (before != null && before.id == actor.id) {
            if (!after.active) return RuleResult.Rejected(RuleViolation.SELF_DEACTIVATE)
            if (after.role != Role.ADMIN) return RuleResult.Rejected(RuleViolation.SELF_DEMOTE)
        }
        val others = all.filter { it.id != before?.id }.map { it.name }
        validateName(after.name, others).let { if (it != RuleResult.Ok) return it }
        if (activeAdminsAfter(all, changed = after) == 0) return RuleResult.Rejected(RuleViolation.LAST_ADMIN)
        return RuleResult.Ok
    }

    fun checkDelete(actor: User, target: User, all: List<User>): RuleResult = when {
        !actor.can(Permission.MANAGE_USERS) -> RuleResult.Rejected(RuleViolation.NOT_ALLOWED)
        target.id == actor.id -> RuleResult.Rejected(RuleViolation.SELF_DELETE)
        activeAdminsAfter(all, changed = null, deletedId = target.id) == 0 -> RuleResult.Rejected(RuleViolation.LAST_ADMIN)
        else -> RuleResult.Ok
    }

    /** How many active Admins there would be after replacing [changed] (by id) and removing [deletedId]. */
    internal fun activeAdminsAfter(all: List<User>, changed: User?, deletedId: String? = null): Int {
        val replaced = all.map { if (changed != null && it.id == changed.id) changed else it }
        val withNew = if (changed != null && all.none { it.id == changed.id }) replaced + changed else replaced
        return withNew.count { it.id != deletedId && it.active && it.role == Role.ADMIN }
    }
}
