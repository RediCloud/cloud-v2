package dev.redicloud.service.base.packets

import dev.redicloud.packets.AbstractPacket

class ScreenCommandPacket(
    val command: String
) : AbstractPacket()