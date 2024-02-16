package dev.redicloud.api.events.impl.server

import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

class CloudServerConnectedEvent(serviceId: ServiceId) : CloudServerEvent(serviceId, EventFireType.GLOBAL)