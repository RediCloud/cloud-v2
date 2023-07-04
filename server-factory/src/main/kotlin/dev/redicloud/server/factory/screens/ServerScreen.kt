package dev.redicloud.server.factory.screens

import dev.redicloud.api.server.events.server.CloudServerDisconnectEvent
import dev.redicloud.console.Console
import dev.redicloud.console.utils.Screen
import dev.redicloud.utils.service.ServiceId

class ServerScreen(
    val serviceId: ServiceId,
    name: String,
    console: Console
) : Screen(console, name, mutableListOf("*"), true, 100, 100) {

    private val listener = console.eventManager?.listen<CloudServerDisconnectEvent> {
        if (it.serviceId == serviceId) {
            destroy()
        }
    }

    companion object {
        val SCREEN_LINE_FORMAT = System.getProperty("redicloud.screen.line", "§8[%hc%%name%§8] %tc%%message%")
    }

    override fun println(text: String) {
        super.println(SCREEN_LINE_FORMAT.replace("%name%", name).replace("%message%", text))
    }

    override fun destroy() {
        listener?.unregister()
        super.destroy()
    }

}