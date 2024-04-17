package dev.redicloud.module.webinterface.service.user

import dev.redicloud.utils.toUUID
import java.util.*

class TestUserService : IUserService {

    private val userStorage = mutableMapOf<UUID, User>()

    init {
        val user = User("Paul".toUUID(), "Paul", "test", "salt")
        userStorage[user.id] = user
    }

    override suspend fun existsId(id: UUID): Boolean {
        return userStorage.containsKey(id)
    }

    override suspend fun existsName(username: String): Boolean {
        return userStorage.values.any { it.username == username }
    }

    override suspend fun create(username: String, password: String): User? {
        if (existsName(username)) {
            // TODO: flash error message
            return null
        }
        val id = UUID.randomUUID()
        val salt = "not_set" // TODO: password hashing
        val user = User(id, username, password, salt)
        userStorage[id] = user
        return user
    }

    override suspend fun getByName(username: String): User? {
        return userStorage.values.firstOrNull { it.username == username }
    }

    override suspend fun getById(id: UUID): User? {
        return userStorage[id]
    }

    override suspend fun update(user: User): User? {
        return userStorage.put(user.id, user)
    }

}