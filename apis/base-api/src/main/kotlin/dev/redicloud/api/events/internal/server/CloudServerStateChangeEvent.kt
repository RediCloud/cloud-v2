package dev.redicloud.api.events.internal.server

import dev.redicloud.api.service.server.CloudServerState
import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

class CloudServerStateChangeEvent(serviceId: ServiceId, val state: CloudServerState) : CloudServerEvent(serviceId, EventFireType.GLOBAL)