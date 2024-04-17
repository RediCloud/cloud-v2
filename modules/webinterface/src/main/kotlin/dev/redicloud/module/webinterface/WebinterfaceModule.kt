package dev.redicloud.module.webinterface

import com.auth0.jwt.algorithms.Algorithm
import dev.redicloud.api.modules.CloudModule
import dev.redicloud.api.modules.ModuleLifeCycle
import dev.redicloud.api.modules.ModuleTask
import dev.redicloud.logging.LogManager
import dev.redicloud.module.webinterface.plugins.*
import dev.redicloud.module.webinterface.service.sessions.flash.FlashMessageService
import dev.redicloud.module.webinterface.service.sessions.user.UserSessionService
import dev.redicloud.module.webinterface.service.user.IUserService
import dev.redicloud.module.webinterface.service.user.TestUserService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

class WebinterfaceModule : CloudModule() {

    companion object {
        val SECRET: String = System.getenv("RC_SECRET")
        val URL: String = System.getenv("RC_URL")
        val ALGORITHM = Algorithm.HMAC256(SECRET)
        lateinit var INSTANCE: WebinterfaceModule
        val LOGGER = LogManager.logger(WebinterfaceModule::class)
    }

    lateinit var userSessionService: UserSessionService
    lateinit var userService: IUserService
    lateinit var flashMessageService: FlashMessageService
    lateinit var application: Application

    @ModuleTask(ModuleLifeCycle.LOAD)
    fun load() {
        INSTANCE = this
        userSessionService = UserSessionService()
        userService = TestUserService()
        flashMessageService = FlashMessageService
        val server = embeddedServer(Netty, port = 8123, host = "0.0.0.0") {
            module(userSessionService, userService, flashMessageService)
            application = this
        }.start()
        val hosts = server.environment.connectors.map { "${it.host}:${it.port}" }
        LOGGER.info("Started webinterface on ${hosts.joinToString(", ")}")
    }

    @ModuleTask(ModuleLifeCycle.UNLOAD)
    fun unload() {
        application.dispose()
    }

}

fun Application.module(
    userSessionService: UserSessionService,
    userService: IUserService,
    flashMessageService: FlashMessageService
) {
    configureTemplating()
    configureSerialization()
    configureSessions(userSessionService, flashMessageService)
    configureSessionAuthentication(userService)
    configureRouting(userService, userSessionService)
}
