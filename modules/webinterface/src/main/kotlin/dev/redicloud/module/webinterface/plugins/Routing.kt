package dev.redicloud.module.webinterface.plugins

import dev.redicloud.module.webinterface.routes.dashboard.registerHomeRoute
import dev.redicloud.module.webinterface.routes.registerAuthenticationRoutes
import dev.redicloud.module.webinterface.service.sessions.user.UserSessionService
import dev.redicloud.module.webinterface.service.user.IUserService
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*


fun Application.configureRouting(userService: IUserService, userSessionService: UserSessionService) {
    install(Routing) {
        staticResources("/assets", "static")

        registerAuthenticationRoutes(userService, userSessionService)
        registerHomeRoute()
    }
}
