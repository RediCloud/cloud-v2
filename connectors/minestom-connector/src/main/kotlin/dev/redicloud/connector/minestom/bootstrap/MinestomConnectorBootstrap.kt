package dev.redicloud.connector.minestom.bootstrap

import dev.redicloud.connector.minestom.MinestomConnector
import dev.redicloud.libloader.boot.Bootstrap
import dev.redicloud.libloader.boot.apply.impl.JarResourceLoader
import dev.redicloud.utils.loadProperties
import net.minestom.server.MinecraftServer
import net.minestom.server.extensions.Extension
import net.minestom.server.extensions.ExtensionClassLoader
import kotlin.system.exitProcess

class MinestomConnectorBootstrap : Extension() {

    private var connector: MinestomConnector? = null
    lateinit var classLoader: ExtensionClassLoader

    override fun preInitialize() {
        try {
            classLoader = this.javaClass.classLoader as ExtensionClassLoader
            loadProperties(this::class.java.classLoader)
            Bootstrap().apply({
                classLoader.addURL(it)
            }, classLoader, JarResourceLoader("redicloud-connector", origin.originalJar))
            connector = MinestomConnector(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun initialize() {
        connector?.onEnable()
    }

    override fun terminate() {
        if (connector == null) {
            exitProcess(0)
        } else {
            MinecraftServer.LOGGER.info("Disabling cloud connector...")
        }
        connector?.minestomShuttingDown = true
        connector?.onDisable()
    }


}