package dev.redicloud.connector.minestom.provider

import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import net.minestom.server.MinecraftServer
import net.minestom.server.extensions.Extension

class MinestomScreenProvider(
    packetManager: PacketManager,
    private val extension: Extension
) : AbstractScreenProvider(packetManager) {
    override fun execute(command: String) {
        LogManager.rootLogger().info("Executing command: $command")
        MinecraftServer.getCommandManager().dispatcher.execute(
            MinecraftServer.getCommandManager().consoleSender,
            command
        )
    }

}