package dev.redicloud.api.player.events

import java.util.UUID

class CloudPlayerDisconnectEvent(
    uniqueId: UUID
) : CloudPlayerEvent(uniqueId)