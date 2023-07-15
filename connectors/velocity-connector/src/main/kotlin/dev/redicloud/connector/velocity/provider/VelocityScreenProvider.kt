package dev.redicloud.connector.velocity.provider

import com.velocitypowered.api.proxy.ProxyServer
import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider

class VelocityScreenProvider(
    packetManager: PacketManager,
    private val proxyServer: ProxyServer
) : AbstractScreenProvider(packetManager) {

    override fun execute(command: String) {
        LogManager.rootLogger().info("Executing command: $command")
        proxyServer.commandManager.executeAsync(proxyServer.consoleCommandSource, command)
    }

}