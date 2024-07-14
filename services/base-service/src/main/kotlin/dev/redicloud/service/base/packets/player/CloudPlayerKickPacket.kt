package dev.redicloud.service.base.packets.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.util.UUID

class CloudPlayerKickPacket(
    uniqueId: UUID,
    private val jsonReason: String
) : CloudPlayerPacket(uniqueId) {

    constructor(uniqueId: UUID, reason: Component) : this(
        uniqueId,
        GsonComponentSerializer.gson().serialize(reason)
    )

    val reason: Component
        get() = GsonComponentSerializer.gson().deserialize(jsonReason)

}