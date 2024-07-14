package dev.redicloud.connector.velocity.player

import com.velocitypowered.api.proxy.ProxyServer
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.player.ICloudPlayer
import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.ICloudServer
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.service.base.player.BasePlayerExecutor
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import kotlin.jvm.optionals.getOrNull

class VelocityPlayerExecutor(
    private val proxyServer: ProxyServer,
    playerRepository: ICloudPlayerRepository,
    serverRepository: ICloudServerRepository,
    packetManager: IPacketManager,
    thisServiceId: ServiceId
) : BasePlayerExecutor(playerRepository, serverRepository, packetManager, thisServiceId) {

    override fun audience(player: ICloudPlayer): Audience {
        return proxyServer.getPlayer(player.uniqueId).getOrNull()!!
    }

    override fun executeConnect(cloudPlayer: ICloudPlayer, server: ICloudServer) {
        val player = proxyServer.getPlayer(cloudPlayer.uniqueId).getOrNull()
        if (player == null || !player.isActive) {
            runBlocking { this@VelocityPlayerExecutor.connect(cloudPlayer, server) }
            return
        }
        val serverInfo = proxyServer.getServer(server.name).getOrNull() ?: return
        player.createConnectionRequest(serverInfo).fireAndForget()
    }

    override fun executeKick(cloudPlayer: ICloudPlayer, reason: Component) {
        val player = proxyServer.getPlayer(cloudPlayer.uniqueId).getOrNull()
        if (player == null || !player.isActive) {
            runBlocking { this@VelocityPlayerExecutor.kick(cloudPlayer, reason) }
            return
        }
        player.disconnect(reason)
    }

}