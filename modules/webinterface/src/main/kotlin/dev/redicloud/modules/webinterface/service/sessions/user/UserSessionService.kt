package dev.redicloud.modules.webinterface.service.sessions.user

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.redicloud.modules.webinterface.ALGORITHM
import dev.redicloud.modules.webinterface.SECRET
import dev.redicloud.modules.webinterface.URL
import dev.redicloud.modules.webinterface.service.sessions.ISessionService
import dev.redicloud.modules.webinterface.service.user.User
import java.io.File
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class UserSessionService : ISessionService<UserSession> {

    override val sessionTime: Duration = 7.days
    override val path: String = "/"
    override val storage: File = File(".user_sessions")
    override val name: String = "USER_SESSION"

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