package dev.redicloud.connector.bungeecord.bootstrap

import dev.redicloud.connector.bungeecord.BungeeCordConnector
import dev.redicloud.libloader.boot.Bootstrap
import dev.redicloud.libloader.boot.loaders.URLClassLoaderJarLoader
import dev.redicloud.utils.loadProperties
import net.md_5.bungee.api.plugin.Plugin
import java.net.URLClassLoader
import kotlin.system.exitProcess

class BungeeCordConnectorBootstrap : Plugin() {

    private var connector: BungeeCordConnector? = null

    override fun onLoad() {
        try {
            loadProperties(this.javaClass.classLoader)
            Bootstrap().apply(URLClassLoaderJarLoader(this.javaClass.classLoader as URLClassLoader))
            connector = BungeeCordConnector(this)
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