package dev.redicloud.connectors.bukkit

import dev.redicloud.connectors.bukkit.provider.BukkitServerPlayerProvider
import dev.redicloud.service.minecraft.MinecraftServerService
import dev.redicloud.service.minecraft.provider.IServerPlayerProvider
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitConnector(val plugin: JavaPlugin) : MinecraftServerService<JavaPlugin>() {

    private var bukkitShuttingDown = false
    override val serverPlayerProvider: IServerPlayerProvider = BukkitServerPlayerProvider()

    init {
        registerTasks()
    }

    override fun getConnectorPlugin(): JavaPlugin {
        return plugin
    }

    override fun onDisable() {
        if (!bukkitShuttingDown) {
            Bukkit.shutdown()
            return
        }
        super.onDisable()
    }

}