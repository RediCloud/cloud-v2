package dev.redicloud.service.base.player

import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.api.packets.listen
import dev.redicloud.api.player.ICloudPlayerExecutor
import dev.redicloud.service.base.packets.player.*
import kotlinx.coroutines.runBlocking

class PlayerExecutorListener(
    private val playerExecutor: ICloudPlayerExecutor,
    packetManager: IPacketManager
) {

    val bookPacketListener = packetManager.listen<CloudPlayerBookPacket> {
        runBlocking {
            playerExecutor.showBook(it.uniqueId, it.createBook())
        }
    }

    val bossBarPacketListener = packetManager.listen<CloudPlayerBossBarPacket> {
        runBlocking {
            if (it.hide) {
                playerExecutor.hideBossBar(it.uniqueId, it.createBossBar())
            } else {
                playerExecutor.showBossBar(it.uniqueId, it.createBossBar())
            }
        }
    }

    val connectServerPacketListener = packetManager.listen<CloudPlayerConnectServerPacket> {
        runBlocking {
            playerExecutor.connect(it.uniqueId, it.serviceId)
        }
    }

    val headerFooterPacketListener = packetManager.listen<CloudPlayerHeaderFooterPacket> {
        runBlocking {
            playerExecutor.sendPlayerListHeaderAndFooter(it.uniqueId, it.header, it.footer)
        }
    }

    val kickPacketListener = packetManager.listen<CloudPlayerKickPacket> {
        runBlocking {
            playerExecutor.kick(it.uniqueId, it.reason)
        }
    }

    val messagePacketLister = packetManager.listen<CloudPlayerMessagePacket> {
        runBlocking {
            playerExecutor.sendMessage(it.uniqueId, it.component)
        }
    }

    val resourcePacketListener = packetManager.listen<CloudPlayerResourcePackPacket> {
        runBlocking {
            playerExecutor.sendResourcePacks(it.uniqueId, it.createResourcePackRequest())
        }
    }

    val soundPacketListener = packetManager.listen<CloudPlayerSoundPacket> {
        runBlocking {
            playerExecutor.playSound(it.uniqueId, it.createSound())
        }
    }

    val titlePacketListener = packetManager.listen<CloudPlayerTitlePacket> {
        runBlocking {
            playerExecutor.showTitle(it.uniqueId, it.createTitle())
        }
    }

}