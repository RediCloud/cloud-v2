package dev.redicloud.server.factory.screens

import dev.redicloud.api.server.events.server.CloudServerDisconnectedEvent
import dev.redicloud.console.Console
import dev.redicloud.console.utils.Screen
import dev.redicloud.packets.PacketManager
import dev.redicloud.service.base.packets.ScreenCommandPacket
import dev.redicloud.utils.defaultScope
import dev.redicloud.utils.service.ServiceId
import kotlinx.coroutines.launch

class ServerScreen(
    val serviceId: ServiceId,
    name: String,
    console: Console,
    private val packetManager: PacketManager?
) : Screen(console, name, mutableListOf("*"), true, 100, 100) {

    private val listener = console.eventManager?.listen<CloudServerDisconnectedEvent> {
        if (it.serviceId == serviceId) {
            destroy()
        }
    }
    companion object {
        val SCREEN_LINE_FORMAT = System.getProperty("redicloud.screen.line", "§8[%hc%%name%§8] %tc%%message%")
    }

    override fun println(text: String) {
        super.println(SCREEN_LINE_FORMAT.replace("%name%", name).replace("%message%", "§7$text"))
    }

    override fun destroy() {
        listener?.unregister()
        super.destroy()
    }

    fun executeCommand(command: String) {
        defaultScope.launch { packetManager?.publish(ScreenCommandPacket(command), serviceId) }
    }

}