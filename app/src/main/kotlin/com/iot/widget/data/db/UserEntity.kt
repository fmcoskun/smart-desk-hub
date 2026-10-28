package com.iot.widget.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iot.widget.domain.users.Role

/** Data layer · one row of the `users` table (added in database version 2). */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: Role,
    val active: Boolean,
    val lastLoginAtMillis: Long?,
    val sortOrder: Int,
)
