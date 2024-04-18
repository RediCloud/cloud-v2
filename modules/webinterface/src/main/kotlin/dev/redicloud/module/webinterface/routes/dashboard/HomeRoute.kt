package dev.redicloud.module.webinterface.routes.dashboard

import dev.redicloud.module.webinterface.controller.dashboard.HomeController
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Route.registerHomeRoute() {
    val controller = HomeController()
    get("/") {
        controller.home(call)
    }
    get {
        controller.home(call)
    }
}