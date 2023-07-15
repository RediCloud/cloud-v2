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
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

class VelocityConnector(
    private val bootstrap: VelocityConnectorBootstrap,
    val proxyServer: ProxyServer
) : ProxyServerService<PluginContainer>() {


    private var velocityShuttingDown: Boolean
    private val registered: MutableMap<ServiceId, ServerInfo>
    override val serverPlayerProvider: IServerPlayerProvider
    override val screenProvider: AbstractScreenProvider = VelocityScreenProvider(this.packetManager, this.proxyServer)

    init {
        this.velocityShuttingDown = false
        this.registered = mutableMapOf()
        this.serverPlayerProvider = VelocityServerPlayerProvider(proxyServer)
        runBlocking {
            registerTasks()
            registerStartedServers()
        }
    }

    override fun registerServer(server: CloudMinecraftServer) {
        val session = server.currentSession()
            ?: throw IllegalStateException("Server ${serviceId.toName()} is connected but has no active session?")
        val serverInfo = ServerInfo(
            server.name,
            InetSocketAddress(
                session.ipAddress,
                server.port
            ),
        )
        this.registered[server.serviceId] = serverInfo
        this.proxyServer.registerServer(serverInfo)
    }

    override fun unregisterServer(server: CloudMinecraftServer) {
        val serverInfo = registered.remove(server.serviceId) ?: return
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
        super.onDisable()
    }

    private fun registerListeners() {
        this.proxyServer.eventManager.register(getConnectorPlugin(), CloudPlayerListener(this.playerRepository, this.serverRepository, this.proxyServer))
    }

    override fun getConnectorPlugin(): PluginContainer = this.proxyServer.pluginManager.getPlugin("redicloud-connector-velocity").get()
}