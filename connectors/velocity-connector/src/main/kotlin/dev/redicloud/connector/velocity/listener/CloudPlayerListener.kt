package dev.redicloud.connector.velocity.listener

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.ProxyServer
import dev.redicloud.repository.player.CloudPlayer
import dev.redicloud.repository.player.PlayerRepository
import dev.redicloud.repository.server.CloudMinecraftServer
import dev.redicloud.repository.server.ServerRepository
import dev.redicloud.repository.server.version.utils.ServerVersion
import dev.redicloud.utils.service.ServiceType
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import kotlin.jvm.optionals.getOrElse

class CloudPlayerListener(
    private val playerRepository: PlayerRepository,
    private val serverRepository: ServerRepository,
    private val proxyServer: ProxyServer
) {

    @Subscribe(order = PostOrder.FIRST)
    fun onPostLogin(event: PostLoginEvent) = runBlocking {
        val fallback = runBlocking { serverRepository.getFallback(serverRepository.serviceId) }
        if (fallback == null) {
            event.player.disconnect(Component.text("No fallback server found!"))
            return@runBlocking
        }
        val player = event.player
        if (playerRepository.existsPlayer(player.uniqueId)) {
            val cloudPlayer = playerRepository.getPlayer(player.uniqueId)!!
            cloudPlayer.lastConnect = System.currentTimeMillis()
            cloudPlayer.version = ServerVersion.versions().firstOrNull { it.protocolId == player.protocolVersion.protocol }
                ?: ServerVersion.versions().first { it.isUnknown() }
            cloudPlayer.proxyId = serverRepository.serviceId
            cloudPlayer.name = player.username
            playerRepository.updatePlayer(cloudPlayer)
        } else {
            playerRepository.createPlayer(
                CloudPlayer(
                    player.uniqueId,
                    player.username,
                    null,
                    serverRepository.serviceId,
                    null,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    ServerVersion.versions().firstOrNull { it.protocolId == player.protocolVersion.protocol }
                        ?: ServerVersion.versions().first { it.isUnknown() }
                )
            )
        }
    }

    @Subscribe(order = PostOrder.LAST)
    fun onDisconnect(event: DisconnectEvent) = runBlocking {
        val player = event.player
        val cloudPlayer = playerRepository.getPlayer(player.uniqueId) ?: return@runBlocking
        cloudPlayer.lastDisconnect = System.currentTimeMillis()
        cloudPlayer.proxyId = null
        cloudPlayer.serverId = null
        playerRepository.updatePlayer(cloudPlayer)
    }

    @Subscribe
    fun onServerPreConnect(event: ServerPreConnectEvent) = runBlocking {
        if (!event.result.isAllowed) return@runBlocking
        val player = event.player
        val targetServer = if (event.originalServer.serverInfo.name == "rcfallback") {
            serverRepository.getFallback(serverRepository.serviceId)
        }else serverRepository.getServer(event.originalServer.serverInfo.name, ServiceType.MINECRAFT_SERVER)
        if (targetServer == null) {
            event.player.disconnect(Component.text("No fallback server found!"))
            return@runBlocking
        }
        val server = proxyServer.getServer(targetServer.name)
        if (!server.isPresent) {
            event.player.disconnect(Component.text("No fallback server found!"))
            return@runBlocking
        }
        event.result = ServerPreConnectEvent.ServerResult.allowed(server.get())
    }

    @Subscribe(order = PostOrder.FIRST)
    fun onServerConnected(event: ServerConnectedEvent) = runBlocking {
        val player = event.player
        val cloudPlayer = playerRepository.getPlayer(player.uniqueId)
        if (cloudPlayer == null) {
            player.disconnect(Component.text("Player not found!"))
            throw IllegalStateException("Player ${player.uniqueId} not found!")
        }
        cloudPlayer.serverId = serverRepository.getServer<CloudMinecraftServer>(event.server.serverInfo.name, ServiceType.MINECRAFT_SERVER)?.serviceId
        playerRepository.updatePlayer(cloudPlayer)
    }

    @Subscribe(order = PostOrder.FIRST)
    fun onKickedFromServer(event: KickedFromServerEvent) = runBlocking {
        val player = event.player
        val cloudPlayer = playerRepository.getPlayer(player.uniqueId)
        if (cloudPlayer != null) {
            cloudPlayer.serverId = serverRepository.getServer<CloudMinecraftServer>(event.server.serverInfo.name, ServiceType.MINECRAFT_SERVER)?.serviceId
            playerRepository.updatePlayer(cloudPlayer)
        }

        val fallback = runBlocking { serverRepository.getFallback(serverRepository.serviceId) }
        if (fallback == null) {
            event.result = KickedFromServerEvent.DisconnectPlayer.create(event.serverKickReason.getOrElse { Component.text("You were kicked from the server and no fallback was found!") })
            return@runBlocking
        }
        val server = proxyServer.getServer(fallback.name)
        if (!server.isPresent) {
            event.result = KickedFromServerEvent.DisconnectPlayer.create(event.serverKickReason.getOrElse { Component.text("You were kicked from the server and no fallback was found!") })
            return@runBlocking
        }
        if (event.server.serverInfo.name == server.get().serverInfo.name) {
            event.result = KickedFromServerEvent.DisconnectPlayer.create(event.serverKickReason.getOrElse { Component.text("You were kicked from the server and no fallback was found!") })
            return@runBlocking
        }
        event.result = KickedFromServerEvent.RedirectPlayer.create(server.get())
    }

}