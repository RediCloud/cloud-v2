package dev.redicloud.service.base.player

import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.player.ICloudPlayer
import dev.redicloud.api.player.ICloudPlayerExecutor
import dev.redicloud.api.player.ICloudPlayerRepository
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.ServiceType
import dev.redicloud.api.service.server.ICloudServer
import dev.redicloud.api.service.server.ICloudServerRepository
import dev.redicloud.service.base.packets.player.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import java.util.*

abstract class BasePlayerExecutor(
    private val playerRepository: ICloudPlayerRepository,
    private val serverRepository: ICloudServerRepository,
    private val packetManager: IPacketManager,
    private val thisServiceId: ServiceId
) : ICloudPlayerExecutor {

    init {
        PlayerExecutorListener(this, this.packetManager)
    }

    override suspend fun sendMessage(uniqueId: UUID, component: Component) {
        val player = playerRepository.getPlayer(uniqueId)
            ?: throw NullPointerException("Player with uniqueId $uniqueId not found")
        this.sendMessage(player, component)
    }

    override suspend fun sendMessage(cloudPlayer: ICloudPlayer, component: Component) {
        if (!cloudPlayer.connected || cloudPlayer.proxyId == null) return
        if (cloudPlayer.proxyId == thisServiceId) {
            this.audience(cloudPlayer).sendMessage(component)
            return
        }
        val packet = CloudPlayerMessagePacket(cloudPlayer.uniqueId, component)
        packetManager.publish(packet, cloudPlayer.proxyId!!)
    }

    override suspend fun showBossBar(uniqueId: UUID, bossBar: BossBar) {
        val player = playerRepository.getPlayer(uniqueId)
            ?: throw NullPointerException("Player with uniqueId $uniqueId not found")
        this.showBossBar(player, bossBar)
    }

    override suspend fun showBossBar(cloudPlayer: ICloudPlayer, bossBar: BossBar) {
        if (!cloudPlayer.connected || cloudPlayer.proxyId == null) return
        if (cloudPlayer.proxyId == thisServiceId) {
            this.audience(cloudPlayer).showBossBar(bossBar)
            return
        }
        val packet = CloudPlayerBossBarPacket(cloudPlayer.uniqueId, bossBar)
        packetManager.publish(packet, cloudPlayer.proxyId!!)
    }

    override suspend fun hideBossBar(uniqueId: UUID, bossBar: BossBar) {
        val player = playerRepository.getPlayer(uniqueId)
            ?: throw NullPointerException("Player with uniqueId $uniqueId not found")
        this.hideBossBar(player, bossBar)
    }

    override suspend fun hideBossBar(cloudPlayer: ICloudPlayer, bossBar: BossBar) {
        if (!cloudPlayer.connected || cloudPlayer.proxyId == null) return
        if (cloudPlayer.proxyId == thisServiceId) {
            this.audience(cloudPlayer).hideBossBar(bossBar)
            return
        }
        val packet = CloudPlayerBossBarPacket(cloudPlayer.uniqueId, bossBar, true)
        packetManager.publish(packet, cloudPlayer.proxyId!!)
    }

    override suspend fun playSound(uniqueId: UUID, sound: Sound) {
        val player = playerRepository.getPlayer(uniqueId)
            ?: throw NullPointerException("Player with uniqueId $uniqueId not found")
        this.playSound(player, sound)
    }

    override suspend fun playSound(
        cloudPlayer: ICloudPlayer,
        sound: Sound
    ) {
        if (!cloudPlayer.connected || cloudPlayer.proxyId == null) return
        if (cloudPlayer.proxyId == thisServiceId) {
            this.audience(cloudPlayer).playSound(sound)
            return
        }
        val packet = CloudPlayerSoundPacket(cloudPlayer.uniqueId, sound)
        packetManager.publish(packet, cloudPlayer.proxyId!!)
    }

    override suspend fun stopSound(uniqueId: UUID, sound: Sound) {
        val player = playerRepository.getPlayer(uniqueId)
            ?: throw NullPointerException("Player with uniqueId $uniqueId not found")
        this.stopSound(player, sound)
    }

    override suspend fun stopSound(cloudPlayer: ICloudPlayer, sound: Sound) {
        if (!cloudPlayer.connected || cloudPlayer.proxyId == null) return
        if (cloudPlayer.proxyId == thisServiceId) {
            this.audience(cloudPlayer).stopSound(sound)
            return
        }
        val packet = CloudPlayerSoundPacket(cloudPlayer.uniqueId, sound, true)
        packetManager.publish(packet, cloudPlayer.proxyId!!)
    }

    override suspend fun showTitle(uniqueId: UUID, title: Title) {
        val player = playerRepository.getPlayer(uniqueId)
            ?: throw NullPointerException("Player with uniqueId $uniqueId not found")
        this.showTitle(player, title)
    }

    override suspend fun showTitle(cloudPlayer: ICloudPlayer, title: Title) {
        if (!cloudPlayer.connected || cloudPlayer.proxyId == null) return
        if (cloudPlayer.proxyId == thisServiceId) {
            this.audience(cloudPlayer).showTitle(title)
            return
        }
        val packet = CloudPlayerTitlePacket(cloudPlayer.uniqueId, title)
        packetManager.publish(packet, cloudPlayer.proxyId!!)
    }

    override suspend fun showBook(uniqueId: UUID, book: Book) {
        val player = playerRepository.getPlayer(uniqueId)
            ?: throw NullPointerException("Player with uniqueId $uniqueId not found")
        this.showBook(player, book)
    }

    override suspend fun showBook(cloudPlayer: ICloudPlayer, book: Book) {
        if (!cloudPlayer.connected || cloudPlayer.proxyId == null) return
        if (cloudPlayer.proxyId == thisServiceId) {
            this.audience(cloudPlayer).openBook(book)
            return
        }
        val packet = CloudPlayerBookPacket(cloudPlayer.uniqueId, book)
        packetManager.publish(packet, cloudPlayer.proxyId!!)
    }

    override suspend fun sendPlayerListHeaderAndFooter(uniqueId: UUID, header: Component, footer: Component) {
        val player = playerRepository.getPlayer(uniqueId)
            ?: throw NullPointerException("Player with uniqueId $uniqueId not found")
        this.sendPlayerListHeaderAndFooter(player, header, footer)
    }

    override suspend fun sendPlayerListHeaderAndFooter(
        cloudPlayer: ICloudPlayer,
        header: Component,
        footer: Component
    ) {
        if (!cloudPlayer.connected || cloudPlayer.proxyId == null) return
        if (cloudPlayer.proxyId == thisServiceId) {
            this.audience(cloudPlayer).sendPlayerListHeaderAndFooter(header, footer)
            return
        }
        val packet = CloudPlayerHeaderFooterPacket(cloudPlayer.uniqueId, header, footer)
        packetManager.publish(packet, cloudPlayer.proxyId!!)
    }

    override suspend fun sendResourcePacks(uniqueId: UUID, resourcePackRequest: ResourcePackRequest) {
        val player = playerRepository.getPlayer(uniqueId)
            ?: throw NullPointerException("Player with uniqueId $uniqueId not found")
        this.sendResourcePacks(player, resourcePackRequest)
    }

    override suspend fun sendResourcePacks(cloudPlayer: ICloudPlayer, resourcePackRequest: ResourcePackRequest) {
        if (!cloudPlayer.connected || cloudPlayer.proxyId == null) return
        if (cloudPlayer.proxyId == thisServiceId) {
            this.audience(cloudPlayer).sendResourcePacks(resourcePackRequest)
            return
        }
        val packet = CloudPlayerResourcePackPacket(cloudPlayer.uniqueId, resourcePackRequest)
        packetManager.publish(packet, cloudPlayer.proxyId!!)
    }

    override suspend fun connect(uniqueId: UUID, serviceId: ServiceId) {
        val player = playerRepository.getPlayer(uniqueId)
            ?: throw NullPointerException("Player with uniqueId $uniqueId not found")
        this.connect(player, serviceId)
    }

    override suspend fun connect(uniqueId: UUID, serverName: String) {
        val player = playerRepository.getPlayer(uniqueId)
            ?: throw NullPointerException("Player with uniqueId $uniqueId not found")
        this.connect(player, serverName)
    }

    override suspend fun connect(cloudPlayer: ICloudPlayer, serverName: String) {
        val server = serverRepository.getServer<ICloudServer>(serverName, ServiceType.MINECRAFT_SERVER)
            ?: throw NullPointerException("Server with name $serverName not found")
        this.connect(cloudPlayer, server)
    }

    override suspend fun connect(cloudPlayer: ICloudPlayer, serviceId: ServiceId) {
        if (serviceId.type != ServiceType.MINECRAFT_SERVER) {
            throw IllegalArgumentException("ServiceId type must be MINECRAFT_SERVER")
        }
        val server = serverRepository.getServer<ICloudServer>(serviceId)
            ?: throw NullPointerException("Server with serviceId $serviceId not found")
        this.connect(cloudPlayer, server)
    }

    override suspend fun connect(cloudPlayer: ICloudPlayer, server: ICloudServer) {
        if (server.serviceId.type != ServiceType.MINECRAFT_SERVER) {
            throw IllegalArgumentException("Server type must be MINECRAFT_SERVER")
        }
        if (!cloudPlayer.connected || cloudPlayer.proxyId == null) return
        if (cloudPlayer.proxyId == thisServiceId) {
            this.executeConnect(cloudPlayer, server)
            return
        }
        val packet = CloudPlayerConnectServerPacket(cloudPlayer.uniqueId, server.serviceId)
        packetManager.publish(packet, cloudPlayer.proxyId!!)
    }

    override suspend fun kick(uniqueId: UUID, reason: Component) {
        val player = playerRepository.getPlayer(uniqueId)
            ?: throw NullPointerException("Player with uniqueId $uniqueId not found")
        this.kick(player, reason)
    }

    override suspend fun kick(cloudPlayer: ICloudPlayer, reason: Component) {
        if (!cloudPlayer.connected || cloudPlayer.proxyId == null) return
        if (cloudPlayer.proxyId == thisServiceId) {
            this.executeKick(cloudPlayer, reason)
            return
        }
        val packet = CloudPlayerKickPacket(cloudPlayer.uniqueId, reason)
        packetManager.publish(packet, cloudPlayer.proxyId!!)
    }

    abstract fun audience(player: ICloudPlayer): Audience

    abstract fun executeConnect(cloudPlayer: ICloudPlayer, server: ICloudServer)

    abstract fun executeKick(cloudPlayer: ICloudPlayer, reason: Component)

}