package dev.redicloud.module.webinterface.controller

import dev.redicloud.module.webinterface.service.sessions.flash.flash
import dev.redicloud.module.webinterface.service.sessions.flash.readFlash
import dev.redicloud.module.webinterface.service.sessions.flash.FlashMessageType
import dev.redicloud.module.webinterface.service.sessions.user.UserSession
import dev.redicloud.module.webinterface.service.sessions.user.UserSessionService
import dev.redicloud.module.webinterface.service.user.IUserService
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*

class AuthenticationController(
    private val userService: IUserService,
    private val userSessionService: UserSessionService
) {

    suspend fun postLogin(ctx: ApplicationCall) {
        if (ctx.sessions.get<UserSession>() != null) {
            ctx.respondRedirect("/")
            return
        }
        val formParameters = ctx.receiveParameters()
        val username = formParameters["username"]
        val password = formParameters["password"]
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            ctx.flash {
                type = FlashMessageType.ERROR
                content = "Please enter a username and password!"
            }
            ctx.respondRedirect("/auth/login")
            return
        }
        val user = userService.getByName(username) ?: run {
            ctx.flash {
                type = FlashMessageType.ERROR
                content = "Invalid credentials!"
            }
            ctx.respondRedirect("/auth/login")
            return
        }
        if (!user.checkPassword(password)) {
            ctx.flash {
                type = FlashMessageType.ERROR
                content = "Invalid credentials!"
            }
            ctx.respondRedirect("/auth/login")
            return
        }
        val session = userSessionService.createSession(user)
        ctx.sessions.set(session)

        ctx.respondRedirect("/")
    }

    suspend fun login(ctx: ApplicationCall) {
        if (ctx.sessions.get<UserSession>() != null) {
            ctx.respondRedirect("/")
            return
        }
        val model = mapOf("flash" to ctx.readFlash())
        ctx.respondTemplate("login.ftl", model)
    }

    suspend fun logout(ctx: ApplicationCall) {
        ctx.sessions.clear<UserSession>()
        ctx.flash {
            type = FlashMessageType.INFO
            content = "Successfully logged out!"
        }
        ctx.respondRedirect("/")
    }

}