package dev.redicloud.api.events.internal.player

import dev.redicloud.api.service.ServiceId
import java.util.UUID

class CloudPlayerSwitchServerEvent(
    uniqueId: UUID,
    from: ServiceId,
    to: ServiceId
) : CloudPlayerEvent(uniqueId)