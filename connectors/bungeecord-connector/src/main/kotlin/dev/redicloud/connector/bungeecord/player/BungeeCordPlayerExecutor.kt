package dev.redicloud.connector.bungeecord.player

import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.player.ICloudPlayer
import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.ICloudServer
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.service.base.player.BasePlayerExecutor
import kotlinx.coroutines.runBlocking
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

    override fun executeConnect(cloudPlayer: ICloudPlayer, server: ICloudServer) {
        val player = ProxyServer.getInstance().getPlayer(cloudPlayer.uniqueId)
        if (player == null || !player.isConnected) {
            runBlocking { this@BungeeCordPlayerExecutor.connect(cloudPlayer, server) }
            return
        }
        val serverInfo = ProxyServer.getInstance().getServerInfo(server.name) ?: return
        player.connect(serverInfo)
    }

    override fun executeKick(cloudPlayer: ICloudPlayer, reason: Component) {
        val player = ProxyServer.getInstance().getPlayer(cloudPlayer.uniqueId)
        if (player == null || !player.isConnected) {
            runBlocking { this@BungeeCordPlayerExecutor.executeKick(cloudPlayer, reason) }
            return
        }
        val components = BungeeComponentSerializer.get().serialize(reason)
        player.disconnect(*components)
    }


}