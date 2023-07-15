package dev.redicloud.connector.bukkit

import dev.redicloud.connector.bukkit.provider.BukkitScreenProvider
import dev.redicloud.connector.bukkit.provider.BukkitServerPlayerProvider
import dev.redicloud.service.minecraft.MinecraftServerService
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import dev.redicloud.service.minecraft.provider.IServerPlayerProvider
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitConnector(val plugin: JavaPlugin) : MinecraftServerService<JavaPlugin>() {

    private var bukkitShuttingDown = false
    override val serverPlayerProvider: IServerPlayerProvider = BukkitServerPlayerProvider()
    override val screenProvider: AbstractScreenProvider = BukkitScreenProvider(this.packetManager)
    init {
        registerTasks()
    }

    override fun getConnectorPlugin(): JavaPlugin {
        return this.plugin
    }

    override fun onDisable() {
        if (!this.bukkitShuttingDown) {
            Bukkit.shutdown()
            return
        }
        super.onDisable()
    }

}