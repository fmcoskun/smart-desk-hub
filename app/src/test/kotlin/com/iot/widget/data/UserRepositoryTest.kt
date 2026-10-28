package com.iot.widget.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.iot.widget.data.db.AppDatabase
import com.iot.widget.domain.users.Role
import com.iot.widget.domain.users.User
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UserRepositoryTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val repo = RoomUserRepository(db.userDao())

    @After
    fun close() = db.close()

    @Test
    fun seedCreatesFourUsersInOrder() = runTest {
        repo.ensureSeeded()
        val all = repo.all()
        assertEquals(listOf("Ayşe Yılmaz", "Mehmet Kaya", "Zeynep Demir", "Guest"), all.map { it.name })
        assertEquals(listOf(Role.ADMIN, Role.OPERATOR, Role.OPERATOR, Role.GUEST), all.map { it.role })
        assertFalse(all.first { it.id == "zeynep" }.active)
    }

    @Test
    fun ensureSeededIsIdempotent() = runTest {
        repo.ensureSeeded()
        repo.save(repo.byId("mehmet")!!.copy(name = "Mehmet K."))
        repo.ensureSeeded()
        assertEquals("Mehmet K.", repo.byId("mehmet")!!.name)
    }

    @Test
    fun saveNewAppendsAtEnd() = runTest {
        repo.ensureSeeded()
        repo.save(User("new-id", "Can Öztürk", Role.OPERATOR, active = true, lastLoginAtMillis = null))
        assertEquals("Can Öztürk", repo.all().last().name)
    }

    @Test
    fun deleteRemoves() = runTest {
        repo.ensureSeeded()
        repo.delete("guest")
        assertNull(repo.byId("guest"))
        assertEquals(3, repo.all().size)
    }

    @Test
    fun markLoggedInStampsTime() = runTest {
        repo.ensureSeeded()
        repo.markLoggedIn("ayse", 1234L)
        assertEquals(1234L, repo.byId("ayse")!!.lastLoginAtMillis)
    }
}
