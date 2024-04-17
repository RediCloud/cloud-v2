package dev.redicloud.module.webinterface.service.sessions.flash

data class FlashMessage(
    var content: String,
    var type: FlashMessageType = FlashMessageType.DEFAULT,
    val identifier: String = ""
)