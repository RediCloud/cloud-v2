package dev.redicloud.service.base.packets

import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.service.base.BaseService
import java.util.logging.Level

class ClusterMessagePacket(
    val message: String,
    val level: Level = Level.INFO
) : AbstractPacket() {

    //TODO error support
    override fun received(manager: IPacketManager) {
        super.received(manager)
        BaseService.LOGGER.log(level, message)
    }

}