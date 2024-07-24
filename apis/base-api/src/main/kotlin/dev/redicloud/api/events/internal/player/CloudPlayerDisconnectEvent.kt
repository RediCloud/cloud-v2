package dev.redicloud.api.events.internal.player

import java.util.UUID

class CloudPlayerDisconnectEvent(
    uniqueId: UUID
) : CloudPlayerEvent(uniqueId)