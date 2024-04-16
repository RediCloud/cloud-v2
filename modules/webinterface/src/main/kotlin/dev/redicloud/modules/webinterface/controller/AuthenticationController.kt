package dev.redicloud.modules.webinterface.controller

import dev.redicloud.modules.webinterface.service.sessions.user.UserSession
import dev.redicloud.modules.webinterface.service.sessions.user.UserSessionService
import dev.redicloud.modules.webinterface.service.user.IUserService
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
        if (username == null || password == null) {
            // TODO: Flash message for missing parameters
            ctx.respondRedirect("/auth/login")
            return
        }
        val user = userService.getByName(username) ?: run {
            // TODO: Flash message for invalid user
            ctx.respondRedirect("/auth/login")
            return
        }
        if (!user.checkPassword(password)) {
            // TODO: flash message for invalid password
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
        ctx.respondTemplate("login.ftl")
    }

    suspend fun logout(ctx: ApplicationCall) {
        ctx.sessions.clear<UserSession>()
        ctx.respondRedirect("/")
    }

}