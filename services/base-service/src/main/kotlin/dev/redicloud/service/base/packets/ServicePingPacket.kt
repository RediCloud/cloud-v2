package dev.redicloud.service.base.packets

import dev.redicloud.packets.AbstractPacket
import dev.redicloud.utils.defaultScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ServicePingPacket : AbstractPacket() {
    override fun received() {
        defaultScope.launch {
            getManager()!!.publish(ServicePingResponse(), sender!!)
        }
    }
}