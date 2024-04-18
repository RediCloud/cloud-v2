package dev.redicloud.module.webinterface.controller.dashboard.server

import io.ktor.server.application.*
import io.ktor.server.freemarker.*

class ServersController {


    suspend fun all(call: ApplicationCall) {
        call.respondTemplate("dashboard/servers/all.ftl")
    }

}