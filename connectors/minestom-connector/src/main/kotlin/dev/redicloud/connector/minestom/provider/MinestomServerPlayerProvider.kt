package dev.redicloud.connector.minestom.provider

import dev.redicloud.api.provider.IServerPlayerProvider
import net.minestom.server.MinecraftServer
import java.util.*

class MinestomServerPlayerProvider() : IServerPlayerProvider {

    override fun getConnectedPlayerCount(): Int {
        return MinecraftServer.getConnectionManager().onlinePlayerCount
    }

    override fun getConnectedPlayers(): List<UUID> {
        return MinecraftServer.getConnectionManager().onlinePlayers.map { it.uuid }
    }

    override fun getMaxPlayerCount(): Int {
        return -1
    }

}