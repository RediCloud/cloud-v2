package dev.redicloud.connector.velocity

import com.velocitypowered.api.proxy.server.ServerInfo
import dev.redicloud.connector.velocity.bootstrap.VelocityConnectorBootstrap
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.service.minecraft.ProxyServerService
import dev.redicloud.utils.service.ServiceId
import java.net.InetSocketAddress

class VelocityConnector(private val bootstrap: VelocityConnectorBootstrap) :
    ProxyServerService<VelocityConnectorBootstrap>() {

    private val registered = mutableMapOf<ServiceId, ServerInfo>()

    override fun registerServer(server: CloudMinecraftServer) {
        val session = server.currentSession()
        if (session == null) throw IllegalStateException("Server ${serviceId.toName()} is connected but has no active session?")
        val serverInfo = ServerInfo(
            server.name,
            InetSocketAddress(
                session.ipAddress,
                server.port
            )
        )
        registered[server.serviceId] = serverInfo
        bootstrap.proxyServer.registerServer(serverInfo)
    }

    override fun unregisterServer(server: CloudMinecraftServer) {
        val serverInfo = registered.remove(server.serviceId) ?: return
        bootstrap.proxyServer.unregisterServer(serverInfo)
    }

    override fun getConnectorPlugin(): VelocityConnectorBootstrap = bootstrap
}