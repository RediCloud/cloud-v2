package dev.redicloud.service.base.packets.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.util.UUID

class CloudPlayerHeaderFooterPacket(
    uniqueId: UUID,
    private val jsonHeader: String,
    private val jsonFooter: String
) : CloudPlayerPacket(uniqueId) {

    constructor(uniqueId: UUID, header: Component, footer: Component) : this (
        uniqueId,
        GsonComponentSerializer.gson().serialize(header),
        GsonComponentSerializer.gson().serialize(footer)
    )

    val header: Component
        get() = GsonComponentSerializer.gson().deserialize(jsonHeader)

    val footer: Component
        get() = GsonComponentSerializer.gson().deserialize(jsonFooter)

}