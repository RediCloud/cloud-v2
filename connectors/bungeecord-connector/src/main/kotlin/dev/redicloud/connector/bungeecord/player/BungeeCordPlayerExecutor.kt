package dev.redicloud.connector.bungeecord.player

import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.player.ICloudPlayer
import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.ICloudServer
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.service.base.player.BasePlayerExecutor
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.plugin.Plugin

class BungeeCordPlayerExecutor(
    private val plugin: Plugin,
    playerRepository: ICloudPlayerRepository,
    serverRepository: ICloudServerRepository,
    packetManager: IPacketManager,
    thisServiceId: ServiceId
) : BasePlayerExecutor(playerRepository, serverRepository, packetManager, thisServiceId) {

    private var audiences: BungeeAudiences? = null

    override fun audience(player: ICloudPlayer): Audience {
        if (audiences == null) {
            audiences = BungeeAudiences.create(plugin)
        }
        return audiences!!.player(player.uniqueId)
    }

    override suspend fun executeConnect(cloudPlayer: ICloudPlayer, server: ICloudServer) {
        val player = ProxyServer.getInstance().getPlayer(cloudPlayer.uniqueId)
        if (player == null || !player.isConnected) {
            LOGGER.warning("Player ${cloudPlayer.uniqueId} not found on this proxy despite proxyId matching")
            return
        }
        val serverInfo = ProxyServer.getInstance().getServerInfo(server.name) ?: return
        player.connect(serverInfo)
    }

    override suspend fun executeKick(cloudPlayer: ICloudPlayer, reason: Component) {
        val player = ProxyServer.getInstance().getPlayer(cloudPlayer.uniqueId)
        if (player == null || !player.isConnected) {
            LOGGER.warning("Player ${cloudPlayer.uniqueId} not found on this proxy despite proxyId matching")
            return
        }
        val components = BungeeComponentSerializer.get().serialize(reason)
        player.disconnect(*components)
    }
}
