package dev.redicloud.service.base.player

import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.packets.listen
import dev.redicloud.api.player.ICloudPlayerExecutor
import dev.redicloud.service.base.packets.player.*

class PlayerExecutorListener(
    private val playerExecutor: ICloudPlayerExecutor,
    packetManager: IPacketManager
) {

    val bookPacketListener = packetManager.listen<CloudPlayerBookPacket> {
        playerExecutor.showBook(it.uniqueId, it.createBook())
    }

    val bossBarPacketListener = packetManager.listen<CloudPlayerBossBarPacket> {
        if (it.hide) {
            playerExecutor.hideBossBar(it.uniqueId, it.createBossBar())
        } else {
            playerExecutor.showBossBar(it.uniqueId, it.createBossBar())
        }
    }

    val connectServerPacketListener = packetManager.listen<CloudPlayerConnectServerPacket> {
        playerExecutor.connect(it.uniqueId, it.serviceId)
    }

    val headerFooterPacketListener = packetManager.listen<CloudPlayerHeaderFooterPacket> {
        playerExecutor.sendPlayerListHeaderAndFooter(it.uniqueId, it.header, it.footer)
    }

    val kickPacketListener = packetManager.listen<CloudPlayerKickPacket> {
        playerExecutor.kick(it.uniqueId, it.reason)
    }

    val messagePacketLister = packetManager.listen<CloudPlayerMessagePacket> {
        playerExecutor.sendMessage(it.uniqueId, it.component)
    }

    val resourcePacketListener = packetManager.listen<CloudPlayerResourcePackPacket> {
        playerExecutor.sendResourcePacks(it.uniqueId, it.createResourcePackRequest())
    }

    val soundPacketListener = packetManager.listen<CloudPlayerSoundPacket> {
        playerExecutor.playSound(it.uniqueId, it.createSound())
    }

    val titlePacketListener = packetManager.listen<CloudPlayerTitlePacket> {
        playerExecutor.showTitle(it.uniqueId, it.createTitle())
    }
}
