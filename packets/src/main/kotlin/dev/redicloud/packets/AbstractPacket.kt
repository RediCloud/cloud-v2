package dev.redicloud.packets

import dev.redicloud.utils.service.ServiceId
import java.util.*

abstract class AbstractPacket {

    val packetId: UUID = UUID.randomUUID()
    var sender: ServiceId? = null
    var allowLocalReceiver: Boolean = false
    internal var manager: PacketManager? = null
    internal var referenceId: UUID? = null

    open fun received() {}

    suspend fun respond(packet: AbstractPacket) {
        if (sender == null) return
        if (manager == null) return
        packet.referenceId = packetId
        manager!!.publish(packet, sender!!)
    }

    fun getReferenceId(): UUID? = referenceId

    fun getManager(): PacketManager? = manager

}