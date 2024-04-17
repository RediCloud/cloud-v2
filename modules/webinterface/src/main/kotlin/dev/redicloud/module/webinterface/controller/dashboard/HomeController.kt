package dev.redicloud.module.webinterface.controller.dashboard

import dev.redicloud.module.webinterface.service.user.getUser
import io.ktor.server.application.*
import io.ktor.server.freemarker.*

class HomeController {

    suspend fun home(call: ApplicationCall) {
        val user = call.getUser()!!
        call.respondTemplate("dashboard/home.ftl", mapOf("username" to user.username))
    }

}