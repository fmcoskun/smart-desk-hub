package com.iot.widget.domain.users

import org.junit.Assert.assertEquals
import org.junit.Test

/** The roles table of spec §5, one assertion per role. */
class RolePermissionsTest {

    @Test
    fun adminHasAll() = assertEquals(Permission.entries.toSet(), Role.ADMIN.permissions)

    @Test
    fun operatorCannotManageUsersOrAddRemoveDevices() = assertEquals(
        Permission.entries.toSet() - Permission.MANAGE_USERS - Permission.ADD_REMOVE_DEVICES,
        Role.OPERATOR.permissions,
    )

    @Test
    fun guestOnlyViews() = assertEquals(
        setOf(Permission.VIEW_LIVE_CAMERAS, Permission.VIEW_DEVICES),
        Role.GUEST.permissions,
    )
}
