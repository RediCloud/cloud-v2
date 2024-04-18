package dev.redicloud.module.webinterface.routes.dashboard.server

import dev.redicloud.module.webinterface.controller.dashboard.server.ServersController
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Route.registerServersRoutes() {
    val controller = ServersController()
    route("/servers") {
        get {
            controller.all(call)
        }
        get("/") {
            controller.all(call)
        }
    }
}