package dev.redicloud.module.webinterface.plugins

import dev.redicloud.module.webinterface.service.sessions.flash.FlashMessageService
import dev.redicloud.module.webinterface.service.sessions.register
import dev.redicloud.module.webinterface.service.sessions.user.UserSessionService
import io.ktor.server.application.*
import io.ktor.server.sessions.*

fun Application.configureSessions(
    userSessionService: UserSessionService,
    flashMessageService: FlashMessageService
) {
    install(Sessions) {
        userSessionService.register(this)
        flashMessageService.register(this)
    }
}