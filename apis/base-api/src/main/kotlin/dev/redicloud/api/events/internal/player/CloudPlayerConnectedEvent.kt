package dev.redicloud.api.events.internal.player

import java.util.UUID

class CloudPlayerConnectedEvent(
    uniqueId: UUID
) : CloudPlayerEvent(uniqueId)