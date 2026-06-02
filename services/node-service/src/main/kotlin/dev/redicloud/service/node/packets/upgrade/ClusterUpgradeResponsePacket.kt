package dev.redicloud.service.node.packets.upgrade

import dev.redicloud.api.packets.AbstractPacket

class ClusterUpgradeResponsePacket(
    val success: Boolean,
    val errorMessage: String? = null
) : AbstractPacket()
