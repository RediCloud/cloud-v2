package dev.redicloud.connector.bukkit.bootstrap

import dev.redicloud.connector.bukkit.BukkitConnector
import dev.redicloud.libloader.boot.Bootstrap
import dev.redicloud.libloader.boot.loaders.URLClassLoaderJarLoader
import dev.redicloud.logging.configureLogger
import dev.redicloud.utils.loadProperties
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.net.URLClassLoader
import java.util.logging.Level
import kotlin.system.exitProcess

class BukkitConnectorBootstrap : JavaPlugin() {

    private var connector: BukkitConnector? = null

    override fun onLoad() {
        try {
            loadProperties(this::class.java.classLoader)
            Bootstrap().apply(URLClassLoaderJarLoader(this::class.java.classLoader as URLClassLoader))
            configureLogger("org.redisson", Level.OFF)
            configureLogger("io.netty", Level.INFO)
            connector = BukkitConnector(this)
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onEnable() {
        connector?.onEnable()
    }

    override fun onDisable() {
        if (connector == null) {
            exitProcess(0)
        }
        Bukkit.getLogger().info("Disabling cloud connector...")
        connector!!.bukkitShuttingDown = true
        connector!!.onDisable()
    }

}