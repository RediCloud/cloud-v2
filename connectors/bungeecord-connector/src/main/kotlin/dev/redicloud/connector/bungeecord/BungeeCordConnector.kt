package dev.redicloud.connector.bungeecord

import dev.redicloud.connector.bungeecord.listener.CloudPlayerListener
import dev.redicloud.connector.bungeecord.provider.BungeeCordServerPlayerProvider
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.service.minecraft.ProxyServerService
import dev.redicloud.service.minecraft.provider.IServerPlayerProvider
import dev.redicloud.utils.service.ServiceId
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import java.net.InetSocketAddress

class BungeeCordConnector(
    private val plugin: Plugin
) : ProxyServerService<Plugin>() {

    private var bungeecordShuttingDown: Boolean
    override val serverPlayerProvider: IServerPlayerProvider
    private val registered: MutableMap<ServiceId, ServerInfo>

    init {
        bungeecordShuttingDown = false
        serverPlayerProvider = BungeeCordServerPlayerProvider()
        registered = mutableMapOf()
        runBlocking {
            registerTasks()
        }
    }

    override fun registerServer(server: CloudMinecraftServer) {
        if (ProxyServer.getInstance().servers == null) return
        val session = server.currentSession()
            ?: throw IllegalStateException("Server ${serviceId.toName()} is connected but has no active session?")
        val serverInfo = ProxyServer.getInstance().constructServerInfo(
            server.name,
            InetSocketAddress(
                session.ipAddress,
                server.port
            ),
            "RediCloud Server",
            false
        )
        registered[server.serviceId] = serverInfo
        ProxyServer.getInstance().servers[server.name] = serverInfo
    }

    override fun unregisterServer(server: CloudMinecraftServer) {
        if (ProxyServer.getInstance().servers == null) return
        val serverInfo = registered.remove(server.serviceId) ?: return
        ProxyServer.getInstance().servers.remove(server.name, serverInfo)
    }

    override fun onEnable() {
        runBlocking { registerStartedServers() }
        registerListeners()
        super.onEnable()
    }

    override fun onDisable() {
        if (!bungeecordShuttingDown) {
            ProxyServer.getInstance().stop()
            return
        }
        super.onDisable()
    }

    private fun registerListeners() {
        fun register(listener: Listener) {
            ProxyServer.getInstance().pluginManager.registerListener(plugin, listener)
        }
        register(CloudPlayerListener(this.playerRepository, this.serverRepository))
    }

    override fun getConnectorPlugin(): Plugin {
        return plugin
    }

}