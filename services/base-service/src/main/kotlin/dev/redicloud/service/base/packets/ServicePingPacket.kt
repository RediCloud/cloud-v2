package dev.redicloud.service.base.packets

import dev.redicloud.packets.AbstractPacket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ServicePingPacket : AbstractPacket() {
    override fun received() {
        GlobalScope.launch {
            getManager()!!.publish(ServicePingResponse(), sender!!)
        }
    }
}