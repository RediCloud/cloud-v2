package dev.redicloud.api.player.events

import dev.redicloud.utils.service.ServiceId
import java.util.UUID

class CloudPlayerSwitchServerEvent(
    uniqueId: UUID,
    from: ServiceId,
    to: ServiceId
) : CloudPlayerEvent(uniqueId)