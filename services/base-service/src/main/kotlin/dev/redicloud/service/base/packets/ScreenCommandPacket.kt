package dev.redicloud.service.base.packets

import dev.redicloud.api.packets.AbstractPacket

class ScreenCommandPacket(
    val command: String
) : AbstractPacket()