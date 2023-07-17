package dev.redicloud.api.events.impl.player

import java.util.UUID

class CloudPlayerConnectedEvent(
    uniqueId: UUID
) : CloudPlayerEvent(uniqueId)