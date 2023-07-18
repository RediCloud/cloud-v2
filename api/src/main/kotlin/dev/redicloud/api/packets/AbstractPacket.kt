package dev.redicloud.api.packets

import dev.redicloud.api.service.ServiceId
import java.util.*

abstract class AbstractPacket {

    val packetId: UUID = UUID.randomUUID()
    var sender: ServiceId? = null
    var allowLocalReceiver: Boolean = false
    internal var _referenceId: UUID? = null
    internal var manager: IPacketManager? = null

    val referenceId: UUID?
        get() = _referenceId

    open fun received(manager: IPacketManager) {
        this.manager = manager
    }

    fun asAnswerOf(packet: AbstractPacket): AbstractPacket {
        _referenceId = packet.packetId
        return this
    }

    suspend fun respond(packet: AbstractPacket) {
        if (sender == null) throw IllegalStateException("Sender is null!")
        if (manager == null) throw IllegalStateException("PacketManager is null!")
        manager!!.publish(packet.asAnswerOf(this), sender!!)
    }

}