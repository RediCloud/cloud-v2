package dev.redicloud.api.events.impl.server

import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

class CloudServerRegisteredEvent(serviceId: ServiceId) : CloudServerEvent(serviceId, EventFireType.GLOBAL)