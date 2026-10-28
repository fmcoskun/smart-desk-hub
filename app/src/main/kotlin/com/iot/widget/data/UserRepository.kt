package com.iot.widget.data

import com.iot.widget.data.db.UserDao
import com.iot.widget.data.db.UserEntity
import com.iot.widget.domain.users.Role
import com.iot.widget.domain.users.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/** Data layer · the demo user accounts (spec §6.5). An interface so a real backend can replace Room. */
interface UserRepository {
    /** Live list in display order; seeds the demo accounts on first use. */
    val users: Flow<List<User>>
    suspend fun ensureSeeded()
    suspend fun all(): List<User>
    suspend fun byId(id: String): User?
    suspend fun save(user: User)
    suspend fun delete(id: String)
    suspend fun markLoggedIn(id: String, atMillis: Long)
}

@Singleton
class RoomUserRepository @Inject constructor(
    private val dao: UserDao,
) : UserRepository {

    override val users: Flow<List<User>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }.onStart { ensureSeeded() }

    override suspend fun ensureSeeded() = dao.seedIfEmpty(SEED_USERS)

    override suspend fun all(): List<User> {
        ensureSeeded()
        return dao.getAll().map { it.toDomain() }
    }

    override suspend fun byId(id: String): User? {
        ensureSeeded()
        return dao.getById(id)?.toDomain()
    }

    override suspend fun save(user: User) = dao.save(user.id) { order ->
        UserEntity(user.id, user.name.trim(), user.role, user.active, user.lastLoginAtMillis, order)
    }

    override suspend fun delete(id: String) = dao.delete(id)

    override suspend fun markLoggedIn(id: String, atMillis: Long) = dao.markLoggedIn(id, atMillis)

    companion object {
        /** Spec §6.5 sample accounts; Zeynep starts disabled to show the "inactive" state. */
        val SEED_USERS = listOf(
            UserEntity("ayse", "Ayşe Yılmaz", Role.ADMIN, active = true, lastLoginAtMillis = null, sortOrder = 0),
            UserEntity("mehmet", "Mehmet Kaya", Role.OPERATOR, active = true, lastLoginAtMillis = null, sortOrder = 1),
            UserEntity("zeynep", "Zeynep Demir", Role.OPERATOR, active = false, lastLoginAtMillis = null, sortOrder = 2),
            UserEntity("guest", "Guest", Role.GUEST, active = true, lastLoginAtMillis = null, sortOrder = 3),
        )
    }
}

private fun UserEntity.toDomain() = User(id, name, role, active, lastLoginAtMillis)
