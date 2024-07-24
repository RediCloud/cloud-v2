package dev.redicloud.api.events.internal.player

import dev.redicloud.api.events.CloudEvent
import dev.redicloud.api.events.EventFireType
import java.util.UUID

abstract class CloudPlayerEvent(
    val uniqueId: UUID
) : CloudEvent(EventFireType.GLOBAL)