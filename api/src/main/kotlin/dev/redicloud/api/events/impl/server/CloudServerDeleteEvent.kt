package dev.redicloud.api.events.impl.server

import dev.redicloud.api.events.EventFireType
import dev.redicloud.api.service.ServiceId

class CloudServerDeleteEvent(serviceId: ServiceId, val name: String) : CloudServerEvent(serviceId, EventFireType.GLOBAL)