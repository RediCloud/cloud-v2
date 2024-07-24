package dev.redicloud.api.events.internal.server

import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

class CloudServerTransferredEvent(serverId: ServiceId, val oldNodeId: ServiceId)
    : CloudServerEvent(serverId, EventFireType.GLOBAL)