package dev.redicloud.repository.player

import dev.redicloud.api.player.events.CloudPlayerConnectedEvent
import dev.redicloud.api.player.events.CloudPlayerDisconnectEvent
import dev.redicloud.api.player.events.CloudPlayerSwitchServerEvent
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.database.repository.DatabaseBucketRepository
import dev.redicloud.event.EventManager
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.cache.CachedDatabaseBucketRepository
import dev.redicloud.utils.service.ServiceType
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class PlayerRepository(
    databaseConnection: DatabaseConnection,
    private val eventManager: EventManager,
    packetManager: PacketManager
) : CachedDatabaseBucketRepository<CloudPlayer>(
    databaseConnection,
    "player",
    null,
    CloudPlayer::class,
    5.minutes,
    packetManager,
    ServiceType.NODE,
    ServiceType.PROXY_SERVER,
    ServiceType.MINECRAFT_SERVER
) {

    suspend fun getPlayer(uniqueId: UUID): CloudPlayer? {
        return get(uniqueId.toString())
    }

    suspend fun getPlayer(name: String): CloudPlayer? {
        return getAll().firstOrNull { it.name.lowercase() == name.lowercase() }
    }

    suspend fun createPlayer(cloudPlayer: CloudPlayer): CloudPlayer {
        set(cloudPlayer.uniqueId.toString(), cloudPlayer)
        if (cloudPlayer.proxyId != null) {
            eventManager.fireEvent(CloudPlayerConnectedEvent(cloudPlayer.uniqueId))
        }
        return cloudPlayer
    }

    suspend fun updatePlayer(cloudPlayer: CloudPlayer) {
        val oldPlayer = getPlayer(cloudPlayer.uniqueId) ?: throw IllegalStateException("Player not found")
        set(cloudPlayer.uniqueId.toString(), cloudPlayer)
        if (oldPlayer.serverId != null && cloudPlayer.serverId == null || oldPlayer.proxyId != null && cloudPlayer.proxyId == null) {
            eventManager.fireEvent(CloudPlayerDisconnectEvent(cloudPlayer.uniqueId))
        }else if (oldPlayer.proxyId != cloudPlayer.proxyId) {
            eventManager.fireEvent(CloudPlayerConnectedEvent(cloudPlayer.uniqueId))
        }else if(oldPlayer.serverId != null && cloudPlayer.serverId != null && oldPlayer.serverId != cloudPlayer.serverId) {
            eventManager.fireEvent(CloudPlayerSwitchServerEvent(cloudPlayer.uniqueId, oldPlayer.serverId!!, cloudPlayer.serverId!!))
        }
    }

    suspend fun deletePlayer(uniqueId: UUID) {
        delete(uniqueId.toString())
    }

    suspend fun existsPlayer(uniqueId: UUID): Boolean {
        return exists(uniqueId.toString())
    }

    suspend fun existsPlayer(name: String): Boolean {
        return getAll().any { it.name.lowercase() == name.lowercase() }
    }

    suspend fun getConnectedPlayers(): List<CloudPlayer> {
        return getAll().filter { it.proxyId != null && it.serverId != null || it.serverId != null}
    }

    suspend fun getRegisteredPlayers(): List<CloudPlayer> {
        return getAll()
    }

}