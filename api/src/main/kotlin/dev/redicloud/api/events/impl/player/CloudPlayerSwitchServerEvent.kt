package dev.redicloud.api.events.impl.player

import dev.redicloud.utils.service.ServiceId
import java.util.UUID

class CloudPlayerSwitchServerEvent(
    uniqueId: UUID,
    from: ServiceId,
    to: ServiceId
) : CloudPlayerEvent(uniqueId)