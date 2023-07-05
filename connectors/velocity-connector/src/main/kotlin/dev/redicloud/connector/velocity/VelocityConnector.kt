package dev.redicloud.connector.velocity

import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import dev.redicloud.connector.velocity.bootstrap.VelocityConnectorBootstrap
import dev.redicloud.connector.velocity.listener.CloudPlayerListener
import dev.redicloud.connector.velocity.provider.VelocityServerPlayerProvider
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.service.minecraft.ProxyServerService
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

    init {
        velocityShuttingDown = false
        registered = mutableMapOf()
        serverPlayerProvider = VelocityServerPlayerProvider(proxyServer)
        runBlocking {
            registerTasks()
            registerStartedServers()
        }
    }

    override fun registerServer(server: CloudMinecraftServer) {
        if (server.serviceId.type != ServiceType.MINECRAFT_SERVER) return
        val session = server.currentSession()
            ?: throw IllegalStateException("Server ${serviceId.toName()} is connected but has no active session?")
        val serverInfo = ServerInfo(
            server.name,
            InetSocketAddress(
                session.ipAddress,
                server.port
            )
        )
        registered[server.serviceId] = serverInfo
        proxyServer.registerServer(serverInfo)
    }

    override fun unregisterServer(server: CloudMinecraftServer) {
        if (server.serviceId.type != ServiceType.MINECRAFT_SERVER) return
        val serverInfo = registered.remove(server.serviceId) ?: return
        proxyServer.unregisterServer(serverInfo)
    }

    override fun onEnable() {
        registerListeners()
        super.onEnable()
    }

    override fun onDisable() {
        if (!velocityShuttingDown) {
            proxyServer.shutdown()
            return
        }
        super.onDisable()
    }

    private fun registerListeners() {
        proxyServer.eventManager.register(getConnectorPlugin(), CloudPlayerListener(this.playerRepository, this.serverRepository, this.proxyServer))
    }

    override fun getConnectorPlugin(): PluginContainer = proxyServer.pluginManager.getPlugin("redicloud-connector-velocity").get()
}