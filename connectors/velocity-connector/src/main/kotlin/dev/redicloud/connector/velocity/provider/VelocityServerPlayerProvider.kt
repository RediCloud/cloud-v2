package dev.redicloud.connector.velocity.provider

import com.velocitypowered.api.proxy.ProxyServer
import dev.redicloud.api.provider.IServerPlayerProvider
import java.util.*

class VelocityServerPlayerProvider(
    private val proxyServer: ProxyServer
) : IServerPlayerProvider {

    override fun getConnectedPlayerCount(): Int {
        return proxyServer.playerCount
    }

    override fun getConnectedPlayers(): List<UUID> {
        return proxyServer.allPlayers.map { it.uniqueId }
    }

    override fun getMaxPlayerCount(): Int {
        return proxyServer.configuration.showMaxPlayers
    }

}