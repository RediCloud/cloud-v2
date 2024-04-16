package dev.redicloud.modules.webinterface.service.sessions

import io.ktor.server.sessions.*
import java.io.File
import kotlin.time.Duration


interface ISessionService<T : Any> {
    val name: String
    val sessionTime: Duration
    val path: String
    val storage: File

}

inline fun <reified T : Any> ISessionService<T>.register(sessionsConfig: SessionsConfig) {
    sessionsConfig.cookie<T>(name) {
        cookie.path = path
        cookie.extensions["SameSite"] = "Lax"
        cookie.maxAge = sessionTime
        directorySessionStorage(storage, true)
    }
}