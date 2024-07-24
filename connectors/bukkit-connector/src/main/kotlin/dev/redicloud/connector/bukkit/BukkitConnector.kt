package dev.redicloud.connector.bukkit

import dev.redicloud.api.provider.IServerPlayerProvider
import dev.redicloud.connector.bukkit.player.BukkitPlayerExecutor
import dev.redicloud.connector.bukkit.provider.BukkitScreenProvider
import dev.redicloud.connector.bukkit.provider.BukkitServerPlayerProvider
import dev.redicloud.service.base.player.BasePlayerExecutor
import dev.redicloud.service.minecraft.MinecraftServerService
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class BukkitConnector(val plugin: JavaPlugin) : MinecraftServerService<JavaPlugin>() {

    internal var bukkitShuttingDown = false
    override var playerProvider: IServerPlayerProvider = BukkitServerPlayerProvider()
    override val screenProvider: AbstractScreenProvider = BukkitScreenProvider(this.packetManager, this.plugin)
    override val playerExecutor: BasePlayerExecutor = BukkitPlayerExecutor(this.plugin, this.playerRepository, this.serverRepository, this.packetManager, this.serviceId)

    init {
        initApi()
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

    override fun plattformShutdown() {
        this.bukkitShuttingDown = true
        Bukkit.shutdown()
    }

}