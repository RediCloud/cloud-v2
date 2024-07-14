package dev.redicloud.service.node.player

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

class NodePlayerExecutor(
    playerRepository: ICloudPlayerRepository,
    serverRepository: ICloudServerRepository,
    packetManager: IPacketManager,
    thisServiceId: ServiceId
) : BasePlayerExecutor(playerRepository, serverRepository, packetManager, thisServiceId) {

    override fun audience(player: ICloudPlayer): Audience {
        throw UnsupportedOperationException("Not supported for node service")
    }

    override fun executeConnect(cloudPlayer: ICloudPlayer, server: ICloudServer) {
        runBlocking { this@NodePlayerExecutor.connect(cloudPlayer, server) }
    }

    override fun executeKick(cloudPlayer: ICloudPlayer, reason: Component) {
        runBlocking { this@NodePlayerExecutor.kick(cloudPlayer, reason) }
    }

}