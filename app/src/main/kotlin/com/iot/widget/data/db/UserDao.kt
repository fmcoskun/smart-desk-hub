package com.iot.widget.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Data layer · SQL for the `users` table (see DeskDao for how Room DAOs work). */
@Dao
abstract class UserDao {

    @Query("SELECT * FROM users ORDER BY sortOrder")
    abstract fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY sortOrder")
    abstract suspend fun getAll(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id")
    abstract suspend fun getById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(users: List<UserEntity>)

    @Query("SELECT COUNT(*) FROM users")
    abstract suspend fun count(): Int

    /**
     * Seeds only an empty table (first run). Unlike the desk devices, users can be deleted,
     * and a deleted demo account must not come back the next time the list is read.
     */
    @Transaction
    open suspend fun seedIfEmpty(users: List<UserEntity>) {
        if (count() == 0) insertIfAbsent(users)
    }

    @Upsert
    abstract suspend fun upsert(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Query("UPDATE users SET lastLoginAtMillis = :atMillis WHERE id = :id")
    abstract suspend fun markLoggedIn(id: String, atMillis: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM users")
    abstract suspend fun maxSortOrder(): Int

    /** Keeps an existing user's list position; a new user goes to the end. */
    @Transaction
    open suspend fun save(id: String, build: (sortOrder: Int) -> UserEntity) {
        val order = getById(id)?.sortOrder ?: (maxSortOrder() + 1)
        upsert(build(order))
    }
}
