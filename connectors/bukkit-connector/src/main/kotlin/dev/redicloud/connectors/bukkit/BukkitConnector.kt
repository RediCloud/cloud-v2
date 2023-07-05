package dev.redicloud.connectors.bukkit

import dev.redicloud.service.minecraft.MinecraftServerService
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitConnector(val plugin: JavaPlugin) : MinecraftServerService<JavaPlugin>() {

    private var bukkitShuttingDown = false

    override fun getConnectorPlugin(): JavaPlugin {
        return plugin
    }

    override fun onDisable() {
        bukkitShuttingDown = true
        super.onDisable()
    }

    override fun shutdown() {
        if (!bukkitShuttingDown) {
            Bukkit.shutdown()
            return
        }
        super.shutdown()
    }

}