package dev.redicloud.server.factory.screens

import dev.redicloud.api.events.internal.server.CloudServerDisconnectedEvent
import dev.redicloud.api.events.listen
import dev.redicloud.api.service.ServiceId
import dev.redicloud.console.Console
import dev.redicloud.console.utils.Screen
import dev.redicloud.packets.PacketManager
import dev.redicloud.service.base.packets.ScreenCommandPacket
import kotlinx.coroutines.launch

class ServerScreen(
    val serviceId: ServiceId,
    name: String,
    console: Console,
    private val packetManager: PacketManager?
) : Screen(console, name, mutableListOf("*"), true, MAX_STORED_LINES, MAX_STORED_LINES) {

    private val listener = console.eventManager?.listen<CloudServerDisconnectedEvent> {
        if (it.serviceId == serviceId) {
            destroy()
        }
    }
    companion object {
        private const val MAX_STORED_LINES = 100
        val SCREEN_LINE_FORMAT: String = System.getProperty("redicloud.screen.line", "§8[%hc%%name%§8] %tc%%message%")
    }

    override fun println(text: String) {
        super.println(SCREEN_LINE_FORMAT.replace("%name%", name).replace("%message%", "§7$text"))
    }

    override fun destroy() {
        listener?.unregister()
        super.destroy()
    }

    fun executeCommand(command: String) {
        console.commandScope.launch { packetManager?.publish(ScreenCommandPacket(command), serviceId) }
    }
}
