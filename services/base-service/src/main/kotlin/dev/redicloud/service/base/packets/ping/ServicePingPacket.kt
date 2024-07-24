package dev.redicloud.service.base.packets.ping

import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.utils.ioScope
import kotlinx.coroutines.launch

class ServicePingPacket : AbstractPacket() {
    override fun received(manager: IPacketManager) {
        super.received(manager)
        val s = System.currentTimeMillis()
        ioScope.launch {
            manager.publish(ServicePingResponse(s).asAnswerOf(this@ServicePingPacket), sender!!)
        }
    }
}