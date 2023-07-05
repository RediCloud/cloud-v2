package dev.redicloud.connectors.bukkit.bootstrap

import dev.redicloud.connectors.bukkit.BukkitConnector
import dev.redicloud.libloader.boot.Bootstrap
import dev.redicloud.libloader.boot.loaders.URLClassLoaderJarLoader
import org.bukkit.plugin.java.JavaPlugin
import java.net.URLClassLoader
import kotlin.system.exitProcess

class BukkitConnectorBootstrap : JavaPlugin() {

    private var connector: BukkitConnector? = null

    override fun onLoad() {
        try {
            Bootstrap().apply(URLClassLoaderJarLoader(this::class.java.classLoader as URLClassLoader))
            connector = BukkitConnector(this)
        }catch (e: Exception) {
            e.printStackTrace()
            onDisable()
        }
    }

    override fun onEnable() {
        connector?.onEnable()
    }

    override fun onDisable() {
        if (connector == null) exitProcess(0)
        connector?.logger?.info("Disabling cloud connector...")
        connector?.onDisable()
    }

}