package dev.redicloud.api.events.internal.server

import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId
import dev.redicloud.api.service.server.CloudServerState

class CloudServerStateChangeEvent(serviceId: ServiceId, val state: CloudServerState) : CloudServerEvent(
    serviceId,
    EventFireType.GLOBAL
)
