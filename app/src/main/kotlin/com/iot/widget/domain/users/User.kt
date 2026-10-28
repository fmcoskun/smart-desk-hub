package com.iot.widget.domain.users

import java.util.Locale

/**
 * Domain layer · who may do what (spec §5). Plain Kotlin, no Android: the whole permission
 * model can be unit-tested and every screen asks the same single source.
 */
enum class Role { ADMIN, OPERATOR, GUEST }

/** Each capability a screen can check. Seeing the dashboard is allowed for everyone, so it is not one. */
enum class Permission {
    CONTROL_DEVICES,
    VIEW_LIVE_CAMERAS,
    VIEW_RECORDINGS,
    VIEW_DEVICES,
    EDIT_DEVICES,
    ADD_REMOVE_DEVICES,
    VIEW_LOGS,
    MANAGE_USERS,
}

/** The roles table of spec §5. */
val Role.permissions: Set<Permission>
    get() = when (this) {
        Role.ADMIN -> Permission.entries.toSet()
        Role.OPERATOR -> Permission.entries.toSet() - Permission.ADD_REMOVE_DEVICES - Permission.MANAGE_USERS
        Role.GUEST -> setOf(Permission.VIEW_LIVE_CAMERAS, Permission.VIEW_DEVICES)
    }

data class User(
    val id: String,
    val name: String,
    val role: Role,
    val active: Boolean,
    val lastLoginAtMillis: Long?,
)

fun User.can(permission: Permission): Boolean = permission in role.permissions

/**
 * "Ayşe Yılmaz" → "AY": first letter of the first and last word. Locale.ROOT keeps a Turkish
 * phone from turning "i" into "İ" (and the result the same on every device).
 */
fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val letters = listOfNotNull(words.firstOrNull(), words.drop(1).lastOrNull()).map { it.take(1) }
    return letters.joinToString("").uppercase(Locale.ROOT)
}
