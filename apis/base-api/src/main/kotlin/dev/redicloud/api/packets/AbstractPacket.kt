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
        val s = checkNotNull(sender) { "Sender is null!" }
        val m = checkNotNull(manager) { "PacketManager is null!" }
        m.publish(packet.asAnswerOf(this), s)
    }
}
