package dev.redicloud.connector.bukkit.provider

import dev.redicloud.logging.LogManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitScreenProvider(
    packetManager: PacketManager,
    private val plugin: JavaPlugin
) : AbstractScreenProvider(packetManager) {

    override fun execute(command: String) {
        LogManager.rootLogger().info("Executing command: $command")
        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        })
    }

}