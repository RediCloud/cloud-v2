package dev.redicloud.modules.webinterface.plugins

import dev.redicloud.modules.webinterface.service.sessions.register
import dev.redicloud.modules.webinterface.service.sessions.user.UserSessionService
import io.ktor.server.application.*
import io.ktor.server.sessions.*

fun Application.configureSessions(
    userSessionService: UserSessionService
) {
    install(Sessions) {
        userSessionService.register(this)
    }
}