package dev.redicloud.module.webinterface.plugins

import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.sessions.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {}
    }
}