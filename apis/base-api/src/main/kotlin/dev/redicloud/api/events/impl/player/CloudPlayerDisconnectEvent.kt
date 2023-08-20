package dev.redicloud.api.events.impl.player

import java.util.UUID

class CloudPlayerDisconnectEvent(
    uniqueId: UUID
) : CloudPlayerEvent(uniqueId)