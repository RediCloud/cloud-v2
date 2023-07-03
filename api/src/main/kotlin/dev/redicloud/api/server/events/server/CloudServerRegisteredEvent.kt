package dev.redicloud.service.base.events.server

import dev.redicloud.event.EventFireType
import dev.redicloud.utils.service.ServiceId

class CloudServerRegisteredEvent(serviceId: ServiceId) : CloudServerEvent(serviceId, EventFireType.GLOBAL)