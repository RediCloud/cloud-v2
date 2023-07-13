package dev.redicloud.connector.bungeecord.provider

import dev.redicloud.service.minecraft.provider.IServerPlayerProvider
import net.md_5.bungee.api.ProxyServer
import java.util.*

class BungeeCordServerPlayerProvider : IServerPlayerProvider {

    override fun getConnectedPlayerCount(): Int {
        return ProxyServer.getInstance().players.size
    }

    override fun getConnectedPlayers(): List<UUID> {
        return ProxyServer.getInstance().players.map { it.uniqueId }
    }

    override fun getMaxPlayerCount(): Int {
        return ProxyServer.getInstance().config.playerLimit
    }

}