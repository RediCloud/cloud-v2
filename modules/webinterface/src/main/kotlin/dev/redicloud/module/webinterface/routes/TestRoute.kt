package dev.redicloud.module.webinterface.routes

import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.routing.*

fun Route.registerTests() {
    if (!application.developmentMode) return
    route("/test") {
        route("/components") {
            get("/buttons") {
                call.respondTemplate("base/components/buttons/display-test.ftl")
            }
        }
    }
}