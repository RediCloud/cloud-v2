package dev.redicloud.connector.bukkit.provider

import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import org.bukkit.Bukkit

class BukkitScreenProvider(
    packetManager: PacketManager
) : AbstractScreenProvider(packetManager) {

    override fun execute(command: String) {
        LogManager.rootLogger().info("Executing command: $command")
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }

}