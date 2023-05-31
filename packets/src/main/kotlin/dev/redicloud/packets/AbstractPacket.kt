package dev.redicloud.packets

import dev.redicloud.utils.ServiceId
import java.util.*

abstract class AbstractPacket {

    val packetId: UUID = UUID.randomUUID()
    var sender: ServiceId? = null
    var allowLocalReceiver: Boolean = false

    fun received() {}

}