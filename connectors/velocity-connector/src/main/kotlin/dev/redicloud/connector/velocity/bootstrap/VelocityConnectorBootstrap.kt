package dev.redicloud.connector.velocity.bootstrap

import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import dev.redicloud.connector.velocity.VelocityConnector
import dev.redicloud.libloader.boot.Bootstrap
import dev.redicloud.libloader.boot.loaders.URLClassLoaderJarLoader
import dev.redicloud.utils.loadProperties
import kotlinx.coroutines.runBlocking
import java.net.URLClassLoader
import java.util.logging.Logger
import kotlin.system.exitProcess

@Plugin(
    id = "redicloud-connector",
    name = "redicloud-connector-velocity",
    version = "2.4.0-SNAPSHOT",
    url = "https://redicloud.dev",
    authors = ["RediCloud"]
)
class VelocityConnectorBootstrap @Inject constructor(val proxyServer: ProxyServer) {

    private var connector: VelocityConnector? = null

    init {
        @Suppress("TooGenericExceptionCaught")
        try {
            loadProperties(this.javaClass.classLoader)
            Bootstrap().apply(URLClassLoaderJarLoader(this.javaClass.classLoader as URLClassLoader))
            connector = VelocityConnector(proxyServer)
            runBlocking { connector!!.start() }
        } catch (e: Exception) {
            System.err.println("Failed to initialize VelocityConnector: ${e.message}")
            System.err.println(e.stackTraceToString())
            proxyServer.shutdown()
        }
    }

    @Suppress("UnusedParameter")
    @Subscribe(order = PostOrder.FIRST)
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        connector?.onEnable()
    }

    @Suppress("UnusedParameter")
    @Subscribe(order = PostOrder.LAST)
    fun onShutdown(event: ProxyShutdownEvent) {
        if (connector == null) {
            exitProcess(0)
        }
        Logger.getGlobal().info("Disabling cloud connector...")
        connector!!.velocityShuttingDown = true
        connector!!.onDisable()
    }
}
