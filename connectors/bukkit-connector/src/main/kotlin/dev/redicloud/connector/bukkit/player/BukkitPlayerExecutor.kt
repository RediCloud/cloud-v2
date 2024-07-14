package dev.redicloud.connector.bukkit.player

import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.player.ICloudPlayer
import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.ICloudServer
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.service.base.player.BasePlayerExecutor
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import org.bukkit.plugin.java.JavaPlugin

class BukkitPlayerExecutor(
    private val plugin: JavaPlugin,
    playerRepository: ICloudPlayerRepository,
    serverRepository: ICloudServerRepository,
    packetManager: IPacketManager,
    thisServiceId: ServiceId
) : BasePlayerExecutor(playerRepository, serverRepository, packetManager, thisServiceId) {

    private var _audience: BukkitAudiences? = null

    override fun audience(player: ICloudPlayer): Audience {
        if (_audience == null) {
            _audience = BukkitAudiences.create(plugin)
        }
        return _audience!!.player(player.uniqueId)
    }

    override fun executeConnect(cloudPlayer: ICloudPlayer, server: ICloudServer) {
        runBlocking { this@BukkitPlayerExecutor.connect(cloudPlayer, server) }
    }

    override fun executeKick(cloudPlayer: ICloudPlayer, reason: Component) {
        runBlocking { this@BukkitPlayerExecutor.kick(cloudPlayer, reason) }
    }


}