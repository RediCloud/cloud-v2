package dev.redicloud.service.base.packets

import dev.redicloud.packets.AbstractPacket
import dev.redicloud.service.base.BaseService
import java.util.logging.Level

class ClusterMessagePacket(
    val message: String,
    val level: Level = Level.INFO
) : AbstractPacket() {

    //TODO error support
    override fun received() {
        BaseService.LOGGER.log(level, message)
    }

}