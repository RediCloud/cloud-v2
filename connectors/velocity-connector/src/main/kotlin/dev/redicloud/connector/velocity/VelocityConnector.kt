package dev.redicloud.connector.velocity

import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import dev.redicloud.api.provider.IServerPlayerProvider
import dev.redicloud.connector.velocity.bootstrap.VelocityConnectorBootstrap
import dev.redicloud.connector.velocity.listener.CloudPlayerListener
import dev.redicloud.connector.velocity.provider.VelocityScreenProvider
import dev.redicloud.connector.velocity.provider.VelocityServerPlayerProvider
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.service.minecraft.ProxyServerService
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import dev.redicloud.api.service.ServiceId
import dev.redicloud.connector.velocity.listener.CloudServerListener
import dev.redicloud.connector.velocity.player.VelocityPlayerExecutor
import dev.redicloud.service.base.player.BasePlayerExecutor
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

class VelocityConnector(
    private val bootstrap: VelocityConnectorBootstrap,
    private val proxyServer: ProxyServer
) : ProxyServerService<PluginContainer, ServerInfo>() {


    internal var velocityShuttingDown: Boolean = false
    override var playerProvider: IServerPlayerProvider = VelocityServerPlayerProvider(proxyServer)
    override val screenProvider: AbstractScreenProvider = VelocityScreenProvider(this.packetManager, this.proxyServer)
    override val playerExecutor: BasePlayerExecutor = VelocityPlayerExecutor(this.proxyServer, this.playerRepository, this.serverRepository, this.packetManager, this.serviceId)

    init {
        initApi()
        runBlocking {
            registerTasks()
            registerStartedServers()
        }
        runBlocking { moduleHandler.loadModules() }
    }

    override fun registerServer(server: CloudMinecraftServer) {
        val session = server.currentSession
            ?: run {
                LOGGER.severe("Server ${server.serviceId.toName()} has no session set!")
                return
            }
        if (server.port == -1) {
            LOGGER.severe("Server ${server.serviceId.toName()} has no port set!")
            return
        }
        val serverInfo = ServerInfo(
            server.name,
            InetSocketAddress(
                session.ipAddress,
                server.port
            ),
        )
        this.registeredServers[server.serviceId] = serverInfo
        if (this.proxyServer.allServers.any { it.serverInfo.name == server.name }) {
            if (this.proxyServer.allServers.any { it.serverInfo == serverInfo }) {
                return
            }
            this.proxyServer.allServers.filter { it.serverInfo.name == server.name }
                .forEach { this.proxyServer.unregisterServer(it.serverInfo) }
        }
        this.proxyServer.registerServer(serverInfo)
    }

    override fun unregisterServer(serviceId: ServiceId) {
        val serverInfo = registeredServers.remove(serviceId) ?: return
        this.proxyServer.unregisterServer(serverInfo)
    }

    override fun onEnable() {
        registerListeners()
        super.onEnable()
    }

    override fun onDisable() {
        runBlocking {
            proxyServer.allPlayers.forEach { player ->
                playerRepository.getPlayer(player.uniqueId)?.let {
                    it.connected = false
                    it.proxyId = null
                    it.serverId = null
                    it.lastDisconnect = System.currentTimeMillis()
                    playerRepository.updatePlayer(it)
                }
            }
        }
        if (!this.velocityShuttingDown) {
            this.proxyServer.shutdown()
            return
        }
        super.onDisable()
    }

    override fun plattformShutdown() {
        this.velocityShuttingDown = true
        this.proxyServer.shutdown()
    }

    private fun registerListeners() {
        this.proxyServer.eventManager.register(getConnectorPlugin(), CloudPlayerListener(this.serviceId, this.playerRepository, this.serverRepository, this.proxyServer))
        CloudServerListener(this.proxyServer, this.serverRepository, this.nodeRepository, this.eventManager)
    }

    override fun getConnectorPlugin(): PluginContainer = this.proxyServer.pluginManager.getPlugin("redicloud-connector").get()
}