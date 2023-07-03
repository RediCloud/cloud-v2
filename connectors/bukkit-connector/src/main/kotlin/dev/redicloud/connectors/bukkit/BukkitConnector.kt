package dev.redicloud.connectors.bukkit

import dev.redicloud.service.minecraft.MinecraftServerService
import org.bukkit.plugin.java.JavaPlugin

class BukkitConnector(val plugin: JavaPlugin) : MinecraftServerService<JavaPlugin>() {
    override fun getConnectorPlugin(): JavaPlugin {
        return plugin
    }

}