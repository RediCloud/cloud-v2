package dev.redicloud.service.base.packets.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.util.*

class CloudPlayerMessagePacket(
    uniqueId: UUID,
    private val jsonComponent: String
) : CloudPlayerPacket(uniqueId) {
    constructor(uniqueId: UUID, component: Component) : this(uniqueId, GsonComponentSerializer.gson().serialize(component))

    val component: Component
        get() = GsonComponentSerializer.gson().deserialize(jsonComponent)

}