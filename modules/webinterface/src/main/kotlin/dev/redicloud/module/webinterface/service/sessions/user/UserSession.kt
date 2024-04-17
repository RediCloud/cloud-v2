package dev.redicloud.module.webinterface.service.sessions.user

import com.auth0.jwt.JWT
import io.ktor.server.auth.*
import java.util.UUID

data class UserSession(
    val token: String
): Principal

fun UserSession.decodeUserId(): UUID {
    val decodedJWT = JWT.decode(token)
    return UUID.fromString(decodedJWT.getClaim("user_id").asString())
}