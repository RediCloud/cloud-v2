package dev.redicloud.api.player.events

import java.util.UUID

class CloudPlayerConnectedEvent(
    uniqueId: UUID
) : CloudPlayerEvent(uniqueId)