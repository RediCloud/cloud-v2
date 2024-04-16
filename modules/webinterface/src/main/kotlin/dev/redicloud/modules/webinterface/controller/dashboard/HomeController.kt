package dev.redicloud.modules.webinterface.controller.dashboard

import dev.redicloud.modules.webinterface.service.user.getUser
import io.ktor.server.application.*
import io.ktor.server.freemarker.*

class HomeController {

    suspend fun home(call: ApplicationCall) {
        val user = call.getUser()!!
        call.respondTemplate("dashboard/home.ftl", mapOf("username" to user.username))
    }

}