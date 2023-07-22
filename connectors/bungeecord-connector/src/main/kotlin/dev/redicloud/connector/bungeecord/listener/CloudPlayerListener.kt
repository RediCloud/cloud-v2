package dev.redicloud.connector.bungeecord.listener

import dev.redicloud.repository.player.PlayerRepository
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.utils.defaultScope
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.event.ServerConnectedEvent
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

class CloudPlayerListener(
    private val serviceId: ServiceId,
    private val playerRepository: PlayerRepository,
    private val serverRepository: ServerRepository,
    private val plugin: Plugin
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onLogin(event: LoginEvent) {
        event.registerIntent(plugin)
        defaultScope.launch {
            try {
                val fallback = serverRepository.getFallback()
                if (fallback == null) {
                    event.connection.disconnect(*ComponentBuilder().append("No fallback server found!").create())
                    return@launch
                }
                if (playerRepository.existsPlayer(event.connection.uniqueId)) {
                    val cloudPlayer = playerRepository.getPlayer(event.connection.uniqueId)!!
                    if (cloudPlayer.connected) {
                        event.setCancelled(true)
                        event.setCancelReason(*ComponentBuilder().append("You are already connected!").create())
                        return@launch
                    }
                    cloudPlayer.connected = true
                    cloudPlayer.lastConnect = System.currentTimeMillis()
                    cloudPlayer.protocolId = event.connection.version
                    cloudPlayer.proxyId = serviceId
                    cloudPlayer.name = event.connection.name
                    playerRepository.updatePlayer(cloudPlayer)
                } else {
                    playerRepository.createPlayer(
                        dev.redicloud.repository.player.CloudPlayer(
                            event.connection.uniqueId,
                            event.connection.name,
                            null,
                            serviceId,
                            null,
                            System.currentTimeMillis(),
                            System.currentTimeMillis(),
                            event.connection.version
                        )
                    )
                }
            }finally {
                event.completeIntent(plugin)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDisconnect(event: PlayerDisconnectEvent) = runBlocking {
        val player = event.player
        val cloudPlayer = playerRepository.getPlayer(player.uniqueId) ?: return@runBlocking
        cloudPlayer.lastDisconnect = System.currentTimeMillis()
        cloudPlayer.proxyId = null
        cloudPlayer.serverId = null
        cloudPlayer.connected = false
        playerRepository.updatePlayer(cloudPlayer)
    }

    @EventHandler
    fun onServerPreConnect(event: ServerConnectEvent) = runBlocking {
        if (event.isCancelled) return@runBlocking
        val player = event.player
        val targetServer = if (event.reason == ServerConnectEvent.Reason.JOIN_PROXY ||
            player.server?.info?.name == "rcfallback") {
            serverRepository.getFallback()
        } else if (player.server != null) {
            serverRepository.getServer(player.server.info.name, ServiceType.MINECRAFT_SERVER)
        }else serverRepository.getFallback()
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