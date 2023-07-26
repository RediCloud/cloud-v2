package dev.redicloud.connector.bungeecord

import dev.redicloud.connector.bungeecord.listener.CloudPlayerListener
import dev.redicloud.connector.bungeecord.provider.BungeeCordScreenProvider
import dev.redicloud.connector.bungeecord.provider.BungeeCordServerPlayerProvider
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.service.minecraft.ProxyServerService
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import dev.redicloud.service.minecraft.provider.IServerPlayerProvider
import dev.redicloud.api.service.ServiceId
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import java.net.InetSocketAddress

class BungeeCordConnector(
    private val plugin: Plugin
) : ProxyServerService<Plugin>() {

    internal var bungeecordShuttingDown: Boolean
    override val serverPlayerProvider: IServerPlayerProvider
    override val screenProvider: AbstractScreenProvider = BungeeCordScreenProvider(this.packetManager)
    private val registered: MutableMap<ServiceId, ServerInfo>

    init {
        initApi()
        this.bungeecordShuttingDown = false
        this.serverPlayerProvider = BungeeCordServerPlayerProvider()
        this.registered = mutableMapOf()
        runBlocking {
            registerTasks()
        }
        runBlocking { moduleHandler.loadModules() }
    }

    override fun registerServer(server: CloudMinecraftServer) {
        if (ProxyServer.getInstance().servers == null) return
        val session = server.currentSession
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

    override fun unregisterServer(serviceId: ServiceId) {
        if (ProxyServer.getInstance().servers == null) return
        val serverInfo = registered.remove(serviceId) ?: return
        ProxyServer.getInstance().servers.remove(serverInfo.name, serverInfo)
    }

    override fun onEnable() {
        runBlocking { registerStartedServers() }
        registerListeners()
        super.onEnable()
    }

    override fun onDisable() {
        if (!this.bungeecordShuttingDown) {
            ProxyServer.getInstance().stop()
            return
        }
        runBlocking {
            ProxyServer.getInstance().players.forEach { proxiedPlayer ->
                playerRepository.getPlayer(proxiedPlayer.uniqueId)?.let {
                    it.connected = false
                    it.proxyId = null
                    it.serverId = null
                    playerRepository.updatePlayer(it)
                }
            }
        }
        super.onDisable()
    }

    private fun registerListeners() {
        fun register(listener: Listener) {
            ProxyServer.getInstance().pluginManager.registerListener(plugin, listener)
        }
        register(CloudPlayerListener(this.serviceId, this.playerRepository, this.serverRepository, this.plugin))
    }

    override fun getConnectorPlugin(): Plugin {
        return this.plugin
    }

}