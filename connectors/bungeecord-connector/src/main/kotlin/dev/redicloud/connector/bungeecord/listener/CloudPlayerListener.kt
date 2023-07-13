package dev.redicloud.connector.bungeecord.listener

import dev.redicloud.repository.player.PlayerRepository
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.event.ServerConnectedEvent
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority
import java.security.Provider.Service

class CloudPlayerListener(
    private val playerRepository: PlayerRepository,
    private val serverRepository: ServerRepository
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPostLogin(event: PostLoginEvent) = runBlocking {
        val fallback = runBlocking { serverRepository.getFallback(serverRepository.serviceId) }
        if (fallback == null) {
            event.player.disconnect(*ComponentBuilder().append("No fallback server found!").create())
            return@runBlocking
        }
        val player = event.player
        if (playerRepository.existsPlayer(player.uniqueId)) {
            val cloudPlayer = playerRepository.getPlayer(player.uniqueId)!!
            cloudPlayer.lastConnect = System.currentTimeMillis()
            cloudPlayer.version = ServerVersion.versions().firstOrNull { it.protocolId == player.pendingConnection.version }
                    ?: ServerVersion.versions().first { it.isUnknown() }
            cloudPlayer.proxyId = serverRepository.serviceId
            cloudPlayer.name = player.name
            playerRepository.updatePlayer(cloudPlayer)
        } else {
            playerRepository.createPlayer(
                dev.redicloud.repository.player.CloudPlayer(
                    player.uniqueId,
                    player.name,
                    null,
                    serverRepository.serviceId,
                    null,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    ServerVersion.versions().firstOrNull { it.protocolId == player.pendingConnection.version }
                        ?: ServerVersion.versions().first { it.isUnknown() }
                )
            )
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDisconnect(event: PlayerDisconnectEvent) = runBlocking {
        val player = event.player
        val cloudPlayer = playerRepository.getPlayer(player.uniqueId) ?: return@runBlocking
        cloudPlayer.lastDisconnect = System.currentTimeMillis()
        cloudPlayer.proxyId = null
        cloudPlayer.serverId = null
        playerRepository.updatePlayer(cloudPlayer)
    }

    @EventHandler
    fun onServerPreConnect(event: ServerConnectEvent) = runBlocking {
        if (event.isCancelled) return@runBlocking
        val player = event.player
        val targetServer = if (player.server.info.name == "rcfallback") {
            serverRepository.getFallback(serverRepository.serviceId)
        } else serverRepository.getServer(player.server.info.name, ServiceType.MINECRAFT_SERVER)
        if (targetServer == null) {
            event.isCancelled = true
            player.disconnect(*ComponentBuilder().append("No fallback server found!").create())
            return@runBlocking
        }
        val server = ProxyServer.getInstance().getServerInfo(targetServer.name)
        if (server == null) {
            event.isCancelled = true
            player.disconnect(*ComponentBuilder().append("No fallback server found!").create())
            return@runBlocking
        }
        event.target = server
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onServerConnected(event: ServerConnectedEvent) = runBlocking {
        val player = event.player
        val cloudPlayer = playerRepository.getPlayer(player.uniqueId)
        if (cloudPlayer == null) {
            player.disconnect(*ComponentBuilder().append("Player not found!").create())
            throw IllegalStateException("Player ${player.uniqueId} not found!")
        }
        cloudPlayer.serverId = serverRepository.getServer<CloudMinecraftServer>(event.server.info.name, ServiceType.MINECRAFT_SERVER)?.serviceId
        playerRepository.updatePlayer(cloudPlayer)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onKickedFromServer(event: ServerKickEvent) = runBlocking {
        val player = event.player
        val cloudPlayer = playerRepository.getPlayer(player.uniqueId)
        if (cloudPlayer != null) {
            cloudPlayer.serverId = serverRepository.getServer<CloudMinecraftServer>(event.kickedFrom.name, ServiceType.MINECRAFT_SERVER)?.serviceId
            playerRepository.updatePlayer(cloudPlayer)
        }
        val kickedFromServer = serverRepository.getServer<CloudMinecraftServer>(event.kickedFrom.name, ServiceType.MINECRAFT_SERVER)
        val fallback = runBlocking { serverRepository.getFallback(cloudPlayer?.serverId, kickedFromServer?.serviceId) }
        if (fallback == null) {
            event.isCancelled = true
            event.kickReasonComponent = ComponentBuilder().append("You were kicked from the server and no fallback was found!").create()
            player.disconnect(*ComponentBuilder().append("You were kicked from the server and no fallback was found!").create())
            return@runBlocking
        }
        val server = ProxyServer.getInstance().getServerInfo(fallback.name)
        if (server == null) {
            event.isCancelled = true
            event.kickReasonComponent = ComponentBuilder().append("You were kicked from the server and no fallback was found!").create()
            player.disconnect(*ComponentBuilder().append("You were kicked from the server and no fallback was found!").create())
            return@runBlocking
        }
        if (event.kickedFrom.name == server.name) {
            event.isCancelled = true
            event.kickReasonComponent = ComponentBuilder().append("You were kicked from the server and no fallback was found!").create()
            player.disconnect(*ComponentBuilder().append("You were kicked from the server and no fallback was found!").create())
            return@runBlocking
        }
        event.cancelServer = server
    }

}