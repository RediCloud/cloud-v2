package dev.redicloud.module.webinterface.plugins

import dev.redicloud.module.webinterface.routes.dashboard.registerHomeRoute
import dev.redicloud.module.webinterface.routes.dashboard.server.registerServersRoutes
import dev.redicloud.module.webinterface.routes.registerAuthenticationRoutes
import dev.redicloud.module.webinterface.routes.registerTests
import dev.redicloud.module.webinterface.service.sessions.user.UserSessionService
import dev.redicloud.module.webinterface.service.user.IUserService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureRouting(userService: IUserService, userSessionService: UserSessionService) {
    install(Routing) {
        staticResources("/assets", "static")

        get {
            call.respondRedirect("/dashboard")
        }

        registerTests()
        registerAuthenticationRoutes(userService, userSessionService)
        authenticate {
            route("/dashboard") {
                registerHomeRoute()
                registerServersRoutes()
            }
        }
    }
}
