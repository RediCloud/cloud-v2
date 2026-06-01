package dev.redicloud.service.base.packets.ping

import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager

class ServicePingPacket : AbstractPacket() {
    override suspend fun received(manager: IPacketManager) {
        super.received(manager)
        val s = System.currentTimeMillis()
        manager.publish(ServicePingResponse(s).asAnswerOf(this), sender!!)
    }
}
