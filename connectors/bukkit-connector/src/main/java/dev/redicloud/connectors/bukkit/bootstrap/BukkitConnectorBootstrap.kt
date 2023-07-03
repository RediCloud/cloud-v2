package dev.redicloud.connectors.bukkit.bootstrap

import dev.redicloud.connectors.bukkit.BukkitConnector
import dev.redicloud.libloader.boot.Bootstrap
import dev.redicloud.libloader.boot.loaders.URLClassLoaderJarLoader
import org.bukkit.plugin.java.JavaPlugin
import java.net.URLClassLoader

class BukkitConnectorBootstrap : JavaPlugin() {

    private var connector: BukkitConnector? = null

    override fun onLoad() {
        Bootstrap().apply(URLClassLoaderJarLoader(this::class.java.classLoader as URLClassLoader))
        connector = BukkitConnector(this)
    }

    override fun onEnable() {
        connector?.onEnable()
    }

    override fun onDisable() {
        connector?.logger?.info("Disabling cloud connector...")
        connector?.shutdown()
    }

}