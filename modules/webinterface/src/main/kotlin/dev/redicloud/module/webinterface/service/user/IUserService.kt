package dev.redicloud.module.webinterface.service.user

import dev.redicloud.module.webinterface.WebinterfaceModule
import dev.redicloud.module.webinterface.service.sessions.user.UserSession
import dev.redicloud.module.webinterface.service.sessions.user.decodeUserId
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import java.util.*

interface IUserService {

    suspend fun existsName(username: String): Boolean

    suspend fun existsId(id: UUID): Boolean

    suspend fun existsId(id: String): Boolean {
        return try {
            existsId(UUID.fromString(id))
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    suspend fun create(username: String, password: String): User?

    suspend fun getByName(username: String): User?

    suspend fun getById(id: String): User? {
        return try {
            getById(UUID.fromString(id))
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    suspend fun getById(id: UUID): User?

    suspend fun update(user: User): User?

}

suspend fun ApplicationCall.getUser(): User? {
    val session = sessions.get<UserSession>()
    return session?.decodeUserId()?.let { WebinterfaceModule.INSTANCE.userService.getById(it) }
}