package dev.redicloud.packets

import com.google.gson.annotations.Expose
import dev.redicloud.utils.service.ServiceId
import java.util.*

abstract class AbstractPacket {

    val packetId: UUID = UUID.randomUUID()
    var sender: ServiceId? = null
    var allowLocalReceiver: Boolean = false
    @Expose(deserialize = false, serialize = false) internal var manager: PacketManager? = null
    internal var referenceId: UUID? = null

    open fun received() {}

    fun asAnswerOf(packet: AbstractPacket): AbstractPacket {
        referenceId = packet.packetId
        return this
    }

    suspend fun respond(packet: AbstractPacket) {
        if (sender == null) throw IllegalStateException("Sender is null!")
        if (manager == null) throw IllegalStateException("PacketManager is null!")
        manager!!.publish(packet.asAnswerOf(this), sender!!)
    }

    fun getReferenceId(): UUID? = referenceId

    fun getManager(): PacketManager? = manager

}