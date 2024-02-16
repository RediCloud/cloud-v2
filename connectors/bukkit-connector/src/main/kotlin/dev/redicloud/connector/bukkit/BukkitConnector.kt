package dev.redicloud.connector.bukkit

import dev.redicloud.connector.bukkit.provider.BukkitScreenProvider
import dev.redicloud.connector.bukkit.provider.BukkitServerPlayerProvider
import dev.redicloud.service.minecraft.MinecraftServerService
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import dev.redicloud.service.minecraft.provider.IServerPlayerProvider
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitConnector(val plugin: JavaPlugin) : MinecraftServerService<JavaPlugin>() {

    internal var bukkitShuttingDown = false
    override val serverPlayerProvider: IServerPlayerProvider
    override val screenProvider: AbstractScreenProvider

    init {
        initApi()
        bukkitShuttingDown = false
        serverPlayerProvider = BukkitServerPlayerProvider()
        screenProvider = BukkitScreenProvider(this.packetManager, this.plugin)
        registerTasks()
        runBlocking { moduleHandler.loadModules() }
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