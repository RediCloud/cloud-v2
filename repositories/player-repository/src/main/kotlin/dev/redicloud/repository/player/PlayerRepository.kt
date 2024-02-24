package dev.redicloud.repository.player

import dev.redicloud.api.events.impl.player.CloudPlayerConnectedEvent
import dev.redicloud.api.events.impl.player.CloudPlayerDisconnectEvent
import dev.redicloud.api.events.impl.player.CloudPlayerSwitchServerEvent
import dev.redicloud.api.player.ICloudPlayer
import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.event.EventManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.cache.CachedDatabaseBucketRepository
import dev.redicloud.api.service.ServiceType
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class PlayerRepository(
    databaseConnection: DatabaseConnection,
    private val eventManager: EventManager,
    packetManager: PacketManager
) : CachedDatabaseBucketRepository<ICloudPlayer, CloudPlayer>(
    databaseConnection,
    "player",
    null,
    ICloudPlayer::class,
    CloudPlayer::class,
    5.minutes,
    packetManager,
    ServiceType.NODE,
    ServiceType.MINECRAFT_SERVER,
    ServiceType.PROXY_SERVER
), ICloudPlayerRepository {

    override suspend fun getPlayer(uniqueId: UUID): CloudPlayer? {
        return get(uniqueId.toString())
    }

    override suspend fun getPlayer(name: String): CloudPlayer? {
        return getAll().firstOrNull { it.name.lowercase() == name.lowercase() }
    }

    override suspend fun createPlayer(cloudPlayer: ICloudPlayer): CloudPlayer {
        return set(cloudPlayer.uniqueId.toString(), cloudPlayer).also {
            if (it.proxyId != null) {
                eventManager.fireEvent(CloudPlayerConnectedEvent(it.uniqueId))
            }
        }
    }

    override suspend fun updatePlayer(cloudPlayer: ICloudPlayer): CloudPlayer {
        val oldPlayer = getPlayer(cloudPlayer.uniqueId) ?: throw IllegalStateException("Player not found")
        return set(cloudPlayer.uniqueId.toString(), cloudPlayer).also {
            if (oldPlayer.serverId != null && it.serverId == null || oldPlayer.proxyId != null && it.proxyId == null) {
                eventManager.fireEvent(CloudPlayerDisconnectEvent(it.uniqueId))
            }else if (oldPlayer.proxyId != it.proxyId) {
                eventManager.fireEvent(CloudPlayerConnectedEvent(it.uniqueId))
            }else if(oldPlayer.serverId != null && it.serverId != null && oldPlayer.serverId != it.serverId) {
                eventManager.fireEvent(CloudPlayerSwitchServerEvent(it.uniqueId, oldPlayer.serverId!!, it.serverId!!))
            }
        }
    }

    override suspend fun deletePlayer(cloudPlayer: ICloudPlayer): Boolean {
        return deletePlayer(cloudPlayer.uniqueId)
    }

    override suspend fun deletePlayer(uniqueId: UUID): Boolean {
       return  delete(uniqueId.toString())
    }

    override suspend fun existsPlayer(uniqueId: UUID): Boolean {
        return exists(uniqueId.toString())
    }

    override suspend fun existsPlayer(name: String): Boolean {
        return getAll().any { it.name.lowercase() == name.lowercase() }
    }

    override suspend fun getConnectedPlayers(): List<CloudPlayer> {
        return getAll().filter { it.proxyId != null && it.serverId != null || it.serverId != null }
    }

    override suspend fun getRegisteredPlayers(): List<CloudPlayer> {
        return getAll()
    }

}