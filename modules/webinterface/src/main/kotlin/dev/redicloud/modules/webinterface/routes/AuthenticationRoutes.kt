package dev.redicloud.modules.webinterface.routes

import dev.redicloud.modules.webinterface.controller.AuthenticationController
import dev.redicloud.modules.webinterface.service.sessions.user.UserSessionService
import dev.redicloud.modules.webinterface.service.user.IUserService
import io.ktor.server.application.*
import io.ktor.server.routing.*


fun Routing.registerAuthenticationRoutes(
    userService: IUserService,
    userSessionService: UserSessionService
) {
    val authenticationController = AuthenticationController(userService, userSessionService)
    route("/auth") {

        get("/login") {
            authenticationController.login(call)
        }

        post("/login") {
            authenticationController.postLogin(call)
        }

        post("/logout") {
            authenticationController.logout(call)
        }

        get("/logout") {
            authenticationController.logout(call)
        }

    }
}