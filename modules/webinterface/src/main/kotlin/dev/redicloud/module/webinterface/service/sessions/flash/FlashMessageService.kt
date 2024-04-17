package dev.redicloud.module.webinterface.service.sessions.flash

import dev.redicloud.module.webinterface.WebinterfaceModule
import dev.redicloud.module.webinterface.service.sessions.ISessionService
import dev.redicloud.utils.gson.gson
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object FlashMessageService : ISessionService<FlashSession> {

    override val name: String = "flash"
    override val sessionTime: Duration = 10.seconds
    override val storage: File? = null
    override val serializer: SessionSerializer<FlashSession> = object : SessionSerializer<FlashSession> {
        override fun deserialize(text: String): FlashSession {
            return gson.fromJson(text, FlashSession::class.java)
        }
        override fun serialize(session: FlashSession): String {
            return gson.toJson(session)
        }
    }

    fun flash(call: ApplicationCall, block: FlashMessage.() -> Unit) {
        WebinterfaceModule.LOGGER.info("Flashing message to session")
        val flash = FlashMessage("init").apply(block)
        val session = call.sessions.get<FlashSession>() ?: FlashSession()
        session.messages.add(flash)
        call.sessions.set(session)
    }

    fun read(call: ApplicationCall): List<FlashMessage> {
        WebinterfaceModule.LOGGER.info("Getting flashed messages from session")
        val session = call.sessions.get<FlashSession>() ?: return emptyList()
        val messages = session.messages
        call.sessions.clear<FlashSession>()
        return messages
    }

}

fun ApplicationCall.flash(block: FlashMessage.() -> Unit) {
    FlashMessageService.flash(this, block)
}

fun ApplicationCall.readFlash(): List<FlashMessage> {
    return FlashMessageService.read(this)
}
