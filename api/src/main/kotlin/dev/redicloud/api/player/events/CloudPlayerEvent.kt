package dev.redicloud.api.player.events

import dev.redicloud.event.CloudEvent
import dev.redicloud.event.EventFireType
import java.util.UUID

abstract class CloudPlayerEvent(
    val uniqueId: UUID
) : CloudEvent(EventFireType.GLOBAL)