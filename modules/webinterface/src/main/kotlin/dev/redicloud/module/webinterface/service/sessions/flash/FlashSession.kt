package dev.redicloud.module.webinterface.service.sessions.flash

data class FlashSession(
    val messages: MutableList<FlashMessage> = mutableListOf()
)