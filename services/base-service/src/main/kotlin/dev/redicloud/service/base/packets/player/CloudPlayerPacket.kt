package dev.redicloud.service.base.packets.player

import dev.redicloud.api.packets.AbstractPacket
import java.util.UUID

open class CloudPlayerPacket(
    val uniqueId: UUID
) : AbstractPacket()