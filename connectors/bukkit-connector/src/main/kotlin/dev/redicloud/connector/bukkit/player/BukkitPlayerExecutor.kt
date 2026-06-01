package dev.redicloud.connector.bukkit.player

import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.player.ICloudPlayer
import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.ICloudServer
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.service.base.player.BasePlayerExecutor
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
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

    override suspend fun executeConnect(cloudPlayer: ICloudPlayer, server: ICloudServer) {
        throw UnsupportedOperationException("executeConnect is not supported on a sub server")
    }

    override suspend fun executeKick(cloudPlayer: ICloudPlayer, reason: Component) {
        val player = Bukkit.getPlayer(cloudPlayer.uniqueId)
        if (player == null) {
            LOGGER.warning("Player ${cloudPlayer.uniqueId} not found on this server for kick")
            return
        }
        player.kickPlayer(LegacyComponentSerializer.legacySection().serialize(reason))
    }
}
