package dev.redicloud.connector.velocity

import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import dev.redicloud.connector.velocity.bootstrap.VelocityConnectorBootstrap
import dev.redicloud.connector.velocity.listener.CloudPlayerListener
import dev.redicloud.connector.velocity.provider.VelocityScreenProvider
import dev.redicloud.connector.velocity.provider.VelocityServerPlayerProvider
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.service.minecraft.ProxyServerService
import dev.redicloud.service.minecraft.provider.AbstractScreenProvider
import dev.redicloud.service.minecraft.provider.IServerPlayerProvider
import dev.redicloud.api.service.ServiceId
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

class VelocityConnector(
    private val bootstrap: VelocityConnectorBootstrap,
    val proxyServer: ProxyServer
) : ProxyServerService<PluginContainer, ServerInfo>() {


    private var velocityShuttingDown: Boolean = false
    override val serverPlayerProvider: IServerPlayerProvider = VelocityServerPlayerProvider(proxyServer)
    override val screenProvider: AbstractScreenProvider = VelocityScreenProvider(this.packetManager, this.proxyServer)

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
            ?: throw IllegalStateException("Server ${serviceId.toName()} is connected but has no active session?")
        val serverInfo = ServerInfo(
            server.name,
            InetSocketAddress(
                session.ipAddress,
                server.port
            ),
        )
        var registerServer = true
        if (this.proxyServer.allServers.any { it.serverInfo.name == server.name }) {
            if (this.proxyServer.allServers.filter { it.serverInfo.name == server.name }.any { it.serverInfo.address == serverInfo.address }) {
                registerServer = false
            }else {
                this.proxyServer.allServers.filter { it.serverInfo.name == server.name }
                    .forEach { this.proxyServer.unregisterServer(it.serverInfo) }
            }
        }
        this.registeredServers[server.serviceId] = serverInfo
        if (registerServer) {
            this.proxyServer.registerServer(serverInfo)
        }
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
        if (!this.velocityShuttingDown) {
            this.proxyServer.shutdown()
            return
        }
        runBlocking {
            proxyServer.allPlayers.forEach { player ->
                playerRepository.getPlayer(player.uniqueId)?.let {
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
        this.proxyServer.eventManager.register(getConnectorPlugin(), CloudPlayerListener(this.serviceId, this.playerRepository, this.serverRepository, this.proxyServer))
    }

    override fun getConnectorPlugin(): PluginContainer = this.proxyServer.pluginManager.getPlugin("redicloud-connector").get()
}