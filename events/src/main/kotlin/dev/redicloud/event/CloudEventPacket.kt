package dev.redicloud.event

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.packets.AbstractPacket
import dev.redicloud.api.packets.IPacketManager
import dev.redicloud.utils.gson.gson

class CloudEventPacket(val eventData: String, val eventClazz: String, val managerIdentifier: String) : AbstractPacket() {

    override fun received(manager: IPacketManager) {
        super.received(manager)
        val eventManager = EventManager.getManager(managerIdentifier)
        if (eventManager == null) {
            EventManager.LOGGER.severe("Received event packet for unknown manager with identifier $managerIdentifier and packet $eventClazz")
            return
        }
        try {
            val clazz = Class.forName(eventClazz)
            val event = gson.fromJson(eventData, clazz) as CloudEvent
            eventManager.fireLocalEvent(event)
        }catch (e: ClassNotFoundException) {
            EventManager.LOGGER.severe("Error while parsing packet called event $eventClazz", e)
            return
        }
    }

}