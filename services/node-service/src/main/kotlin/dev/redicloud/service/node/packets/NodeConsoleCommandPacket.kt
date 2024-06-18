package dev.redicloud.service.node.packets

import dev.redicloud.api.packets.AbstractPacket

data class NodeConsoleCommandPacket(
    val command: String
) : AbstractPacket()