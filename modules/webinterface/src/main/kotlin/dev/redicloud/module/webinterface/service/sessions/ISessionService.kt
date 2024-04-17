package dev.redicloud.module.webinterface.service.sessions

import io.ktor.server.sessions.*
import java.io.File
import kotlin.time.Duration


interface ISessionService<T : Any> {
    val name: String
    val sessionTime: Duration
    val storage: File?
    val serializer: SessionSerializer<T>
}

inline fun <reified T : Any> ISessionService<T>.register(sessionsConfig: SessionsConfig) {
    if (storage != null) {
        sessionsConfig.cookie<T>(name) {
            cookie.maxAge = sessionTime
            if (storage != null) {
                directorySessionStorage(storage!!, true)
            }
            serializer = this@register.serializer
        }
    } else {
        sessionsConfig.cookie<T>(name, SessionStorageMemory()) {
            cookie.maxAge = sessionTime
            serializer = this@register.serializer
        }
    }
}