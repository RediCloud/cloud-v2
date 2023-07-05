package dev.redicloud.connector.velocity

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import dev.redicloud.connector.velocity.bootstrap.VelocityConnectorBootstrap
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.service.minecraft.ProxyServerService
import dev.redicloud.utils.service.ServiceId
import dev.redicloud.utils.service.ServiceType
import java.net.InetSocketAddress

class VelocityConnector(
    private val bootstrap: VelocityConnectorBootstrap,
    val proxyServer: ProxyServer
) : ProxyServerService<VelocityConnectorBootstrap>() {

    private var velocityShuttingDown = false
    private val registered = mutableMapOf<ServiceId, ServerInfo>()

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

    override fun onDisable() {
        if (!velocityShuttingDown) {
            proxyServer.shutdown()
            return
        }
        super.onDisable()
    }

    override fun getConnectorPlugin(): VelocityConnectorBootstrap = bootstrap
}