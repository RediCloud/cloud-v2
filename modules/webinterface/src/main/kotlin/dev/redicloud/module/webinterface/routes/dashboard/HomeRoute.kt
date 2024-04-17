package dev.redicloud.module.webinterface.routes.dashboard

import dev.redicloud.module.webinterface.controller.dashboard.HomeController
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Routing.registerHomeRoute() {
    val controller = HomeController()
    authenticate {
        get("/") {
            controller.home(call)
        }
    }
}