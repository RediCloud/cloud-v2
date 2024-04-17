package dev.redicloud.module.webinterface.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTVerificationException
import dev.redicloud.module.webinterface.WebinterfaceModule.Companion.ALGORITHM
import dev.redicloud.module.webinterface.WebinterfaceModule.Companion.URL
import dev.redicloud.module.webinterface.service.sessions.user.UserSession
import dev.redicloud.module.webinterface.service.user.IUserService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.util.*

fun Application.configureSessionAuthentication(userService: IUserService) {
    install(Authentication) {
        session<UserSession> {
            val verifier = JWT.require(ALGORITHM)
                .withAudience(URL)
                .withIssuer(URL)
                .build()
            challenge {
                call.sessions.clear<UserSession>()
                call.respondRedirect("/auth/login")
            }
            skipWhen { call -> call.sessions.get<UserSession>() != null }
            validate { session ->
                val token = session.token
                val decodedJWT = JWT.decode(token)
                try {
                    if (verifier.verify(decodedJWT) == null) {
                        return@validate null
                    }
                }catch (e: JWTVerificationException) {
                    return@validate null
                }
                val userId = try {
                    UUID.fromString(decodedJWT.getClaim("user_id").asString())
                }catch (e: Exception) {
                    return@validate null
                }
                if (!userService.existsId(userId) || decodedJWT.expiresAt.before(Date())) {
                    return@validate null
                }
                session
            }
        }
    }
}