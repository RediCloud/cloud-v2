package dev.redicloud.api.events.impl.server

import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

class CloudServerDisconnectedEvent(serviceId: ServiceId) : CloudServerEvent(serviceId, EventFireType.GLOBAL)