package dev.redicloud.connector.bukkit.provider

import dev.redicloud.api.provider.IServerPlayerProvider
import org.bukkit.Bukkit
import java.util.*

class BukkitServerPlayerProvider() : IServerPlayerProvider {

    override fun getConnectedPlayerCount(): Int {
        return Bukkit.getOnlinePlayers().size
    }

    override fun getConnectedPlayers(): List<UUID> {
        return Bukkit.getOnlinePlayers().map { it.uniqueId }
    }

    override fun getMaxPlayerCount(): Int {
        return Bukkit.getMaxPlayers()
    }

}