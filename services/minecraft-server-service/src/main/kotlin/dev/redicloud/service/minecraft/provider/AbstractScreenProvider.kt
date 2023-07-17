package dev.redicloud.service.minecraft.provider

import dev.redicloud.api.packets.listen
import dev.redicloud.packets.PacketManager
import dev.redicloud.service.base.packets.ScreenCommandPacket

abstract class AbstractScreenProvider(
    packetManager: PacketManager
) {

    private val onPacket = packetManager.listen<ScreenCommandPacket> {
        execute(it.command)
    }

    abstract fun execute(command: String)

}