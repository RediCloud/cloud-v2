package dev.redicloud.modules.webinterface

import com.auth0.jwt.algorithms.Algorithm
import dev.redicloud.modules.webinterface.plugins.*
import dev.redicloud.modules.webinterface.service.sessions.user.UserSessionService
import dev.redicloud.modules.webinterface.service.user.TestUserService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*


val SECRET: String = System.getenv("SECRET")
val URL: String = System.getenv("URL")
val ALGORITHM = Algorithm.HMAC256(SECRET)

val userSessionService = UserSessionService()
val userService = TestUserService()

fun main() {
    embeddedServer(Netty, port = 8123, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureTemplating()
    configureSerialization()
    configureSessions(userSessionService)
    configureSessionAuthentication(userService)
    configureRouting(userService, userSessionService)
}


