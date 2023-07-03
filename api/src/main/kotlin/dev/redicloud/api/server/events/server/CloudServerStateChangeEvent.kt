package dev.redicloud.service.base.events.server

import dev.redicloud.api.server.CloudServerState
import dev.redicloud.event.EventFireType
import dev.redicloud.utils.service.ServiceId

class CloudServerStateChangeEvent(serviceId: ServiceId, val state: CloudServerState) : CloudServerEvent(serviceId, EventFireType.GLOBAL)