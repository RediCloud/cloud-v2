package dev.redicloud.module.webinterface.service.sessions.user

import com.auth0.jwt.JWT
import dev.redicloud.module.webinterface.WebinterfaceModule.Companion.ALGORITHM
import dev.redicloud.module.webinterface.WebinterfaceModule.Companion.URL
import dev.redicloud.module.webinterface.service.sessions.ISessionService
import dev.redicloud.module.webinterface.service.user.User
import io.ktor.server.sessions.*
import java.io.File
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class UserSessionService : ISessionService<UserSession> {

    override val sessionTime: Duration = 7.days
    override val storage: File = File(".user_sessions")
    override val name: String = "USER_SESSION"
    override val serializer: SessionSerializer<UserSession> = defaultSessionSerializer()

    fun createSession(user: User): UserSession {
        val token = JWT.create()
            .withAudience(URL)
            .withIssuer(URL)
            .withClaim("user_id", user.id.toString())
            .withExpiresAt(Date(System.currentTimeMillis() + sessionTime.inWholeMilliseconds))
            .sign(ALGORITHM)
        return UserSession(token)
    }

}