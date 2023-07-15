package dev.redicloud.connector.bungeecord.provider

import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import net.md_5.bungee.api.ProxyServer

class BungeeCordScreenProvider(
    packetManager: PacketManager
) : AbstractScreenProvider(packetManager) {

    override fun execute(command: String) {
        LogManager.rootLogger().info("Executing command: $command")
        ProxyServer.getInstance().pluginManager.dispatchCommand(ProxyServer.getInstance().console, command)
    }

}